package cz.cas.lib.arclib.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.cas.lib.arclib.bpm.BpmConstants;
import cz.cas.lib.arclib.domain.*;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflow;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflowFailureInfo;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflowFailureType;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflowState;
import cz.cas.lib.arclib.domain.packages.AuthorialPackage;
import cz.cas.lib.arclib.domain.packages.Sip;
import cz.cas.lib.arclib.domain.profiles.PathToSipId;
import cz.cas.lib.arclib.domain.profiles.ProducerProfile;
import cz.cas.lib.arclib.domain.profiles.SipProfile;
import cz.cas.lib.arclib.dto.IncidentInfoDto;
import cz.cas.lib.arclib.dto.JmsDto;
import cz.cas.lib.arclib.exception.AuthorialPackageLockedException;
import cz.cas.lib.arclib.exception.InvalidChecksumException;
import cz.cas.lib.arclib.service.fixity.Crc32Counter;
import cz.cas.lib.arclib.service.fixity.FixityCounter;
import cz.cas.lib.arclib.service.fixity.Md5Counter;
import cz.cas.lib.arclib.service.fixity.Sha512Counter;
import cz.cas.lib.arclib.store.AuthorialPackageStore;
import cz.cas.lib.arclib.store.IngestWorkflowStore;
import cz.cas.lib.arclib.store.SipStore;
import cz.cas.lib.arclib.utils.ArclibUtils;
import cz.cas.lib.arclib.utils.JsonUtils;
import cz.cas.lib.arclib.utils.XmlUtils;
import cz.cas.lib.arclib.utils.ZipUtils;
import cz.cas.lib.core.exception.GeneralException;
import cz.cas.lib.core.exception.MissingObject;
import cz.cas.lib.core.store.Transactional;
import cz.cas.lib.core.util.Utils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.camunda.bpm.engine.ManagementService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.Incident;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.inject.Inject;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static cz.cas.lib.arclib.utils.ArclibUtils.*;
import static cz.cas.lib.core.util.Utils.*;

@Slf4j
@Service
public class WorkerService {

    private IngestWorkflowStore ingestWorkflowStore;
    private AuthorialPackageStore authorialPackageStore;
    private SipStore sipStore;
    private BatchService batchService;
    private JmsTemplate template;
    private RuntimeService runtimeService;
    private ManagementService managementService;
    private String workspace;
    private Crc32Counter crc32Counter;
    private Sha512Counter sha512Counter;
    private Md5Counter md5Counter;
    private IngestErrorHandler ingestErrorHandler;
    private AipService aipService;

    private int aipSavedCheckRetries;
    private String aipSavedCheckTimeout;
    private int aipStoreRetries;
    private String aipStoreTimeout;

    /**
     * Receives JMS message from Coordinator and starts processing of the ingest workflow.
     * a) if more than half of the ingest workflows related to the batch are in the state FAILED,
     * or the batch is not in the state PROCESSING, method stops evaluation
     * b) otherwise, it sets the ingest workflow with state PROCESSING and starts a ingest workflow process
     *
     * @param dto DTO with the external id of the ingest workflow to process and id of the user that triggered the ingest process
     */
    @Async
    @JmsListener(destination = "worker")
    @Transactional
    public void startProcessingOfIngestWorkflow(JmsDto dto) {
        String externalId = dto.getId();

        IngestWorkflow ingestWorkflow = ingestWorkflowStore.findByExternalId(externalId);
        Utils.notNull(ingestWorkflow, () -> new MissingObject(IngestWorkflow.class, externalId));

        try {
            Batch batch = ingestWorkflow.getBatch();
            String batchId = batch.getId();

            log.info("Message received at Worker. Batch id: " + batchId + ", ingest workflow external id: " + externalId);

            //workaround to initialize batch because of the lazy initialization of batch entity
            batch = batchService.get(batchId);
            notNull(batch, () -> {
                throw new MissingObject(Batch.class, batchId);
            });

            if (batch.getState() != BatchState.PROCESSING) {
                log.warn("Cannot proccess ingest workflow " + externalId + " because the batch " + batchId +
                        " is in state " + batch.getState().toString() + ".");
                return;
            }

            if (tooManyFailedIngestWorkflows(batch)) {
                log.error("Processing of batch " + batchId + " stopped because of too many ingest workflow failures.");
                template.convertAndSend("cancel", batchId);
                return;
            }

            ingestWorkflow.setProcessingState(IngestWorkflowState.PROCESSING);
            ingestWorkflowStore.save(ingestWorkflow);
            log.info("State of ingest workflow with external id " + externalId + " changed to PROCESSING.");

            processIngestWorkflow(ingestWorkflow, dto.getUserId());
        } catch (InvalidChecksumException e) {
            ingestErrorHandler.handleError(ingestWorkflow.getExternalId(), new IngestWorkflowFailureInfo(e.getMessage(),
                    e.getStackTrace().toString(), IngestWorkflowFailureType.INVALID_CHECKSUM), null, dto.getUserId());
        } catch (AuthorialPackageLockedException e) {
            ingestErrorHandler.handleError(ingestWorkflow.getExternalId(), new IngestWorkflowFailureInfo(e.getMessage(),
                    e.getStackTrace().toString(), IngestWorkflowFailureType.AUTHORIAL_PACKAGE_LOCKED), null, dto.getUserId());
        } catch (Exception e) {
            String message = e.getMessage() != null ? e.getMessage() : "";
            String stackTrace = e.getStackTrace() != null ? e.getStackTrace().toString() : "";
            ingestErrorHandler.handleError(ingestWorkflow.getExternalId(), new IngestWorkflowFailureInfo(message,
                    stackTrace, IngestWorkflowFailureType.INTERNAL_ERROR), null, dto.getUserId());
        }
    }

    /**
     * Tries to solve incident by new config.
     * <p>
     * First it stores the config which was used when incident occurred to a variable linked with incident id so it can
     * be found later within {@link IncidentInfoDto} as a cause config. Then it overwrites the latest
     * config with the config provided by an user.
     * </p>
     *
     * @param incidentDto dto containing incident id and config
     * @throws MissingObject if incident can't be found by id
     */
    @Async
    @JmsListener(destination = "incident")
    public void solveIncident(SolveIncidentDto incidentDto) {
        log.info("Trying to solve incident with ID " + incidentDto.getIncidentId() + " using config " + incidentDto.getConfig() + ".");
        Incident i = runtimeService
                .createIncidentQuery()
                .incidentId(incidentDto.getIncidentId())
                .singleResult();
        notNull(i, (() -> new MissingObject(Incident.class, incidentDto.getIncidentId())));
        String configUsedWhenIncidentOccured = (String) runtimeService.getVariable(i.getProcessInstanceId(), BpmConstants.ProcessVariables.latestConfig);
        runtimeService.setVariable(i.getProcessInstanceId(), toIncidentConfigVariableName(i.getId()), configUsedWhenIncidentOccured);
        runtimeService.setVariable(i.getProcessInstanceId(), BpmConstants.ProcessVariables.latestConfig, incidentDto.getConfig());
        managementService.setJobRetries(i.getConfiguration(), 1);
    }

    /**
     * Processes the ingest workflow:
     * 1. verifies hash of the incoming SIP package
     * 2. copies SIP package content to workspace
     * 3. creates or assigns existing authorial package and SIP package according to the determined level of versioning
     * 4. initializes ingest workflow and the Camunda process variables
     *
     * @param ingestWorkflow ingest workflow to process
     * @param userId         id of the user that triggered the ingest workflow processing
     * @throws IOException SIP package does not exist in temporary area or in workspace
     */
    private void processIngestWorkflow(IngestWorkflow ingestWorkflow, String userId) throws IOException {
        Batch batch = ingestWorkflow.getBatch();

        ProducerProfile producerProfile = batch.getProducerProfile();
        notNull(producerProfile, () -> {
            throw new IllegalArgumentException("null producer profile of batch ID " + batch.getId());
        });
        SipProfile sipProfile = producerProfile.getSipProfile();
        notNull(sipProfile, () -> {
            throw new IllegalArgumentException("null sip profile of producer profile with external id " + producerProfile.getExternalId());
        });

        verifyHash(getSipZipTransferAreaPath(ingestWorkflow).toAbsolutePath(), ingestWorkflow, userId);
        long sipSizeInBytes = copySipToWorkspace(ingestWorkflow, userId);

        Path sipFolderWorkspacePath = getSipFolderWorkspacePath(ingestWorkflow.getExternalId(), workspace, ingestWorkflow.getOriginalFileName());
        List<Utils.Triplet<String, String, String>> rootDirFilesAndFixities = computeRootDirFilesFixities(sipFolderWorkspacePath, HashType.MD5);

        String ingestWorkflowConfig = computeIngestWorkflowConfig(batch.getWorkflowConfig(), producerProfile
                .getWorkflowConfig(), ingestWorkflow.getExternalId());

        List<Utils.Pair<String, String>> filePathsAndFileSizes = listSipFilePathsAndFileSizes(sipFolderWorkspacePath);
        List<String> filePaths = filePathsAndFileSizes.stream().map(Utils.Pair::getL).collect(Collectors.toList());
        createSipAndAuthorialPackages(ingestWorkflow, sipFolderWorkspacePath, filePaths, userId);

        Map<String, Object> initVars = new HashMap<>();
        initVars.put(BpmConstants.Ingestion.sizeInBytes, sipSizeInBytes);
        initVars.put(BpmConstants.Ingestion.originalSipFileName, ingestWorkflow.getOriginalFileName());
        initVars.put(BpmConstants.Ingestion.filePathsAndFileSizes, filePathsAndFileSizes);
        initVars.put(BpmConstants.Ingestion.dateTime, Instant.now().truncatedTo(ChronoUnit.SECONDS).toString());
        initVars.put(BpmConstants.Ingestion.success, true);
        initVars.put(BpmConstants.Ingestion.authorialId, ingestWorkflow.getSip().getAuthorialPackage().getAuthorialId());
        initVars.put(BpmConstants.Ingestion.rootDirFilesAndFixities, rootDirFilesAndFixities);
        initVars.put(BpmConstants.ProcessVariables.sipProfileId, sipProfile.getId());
        initVars.put(BpmConstants.ProcessVariables.ingestWorkflowExternalId, ingestWorkflow.getExternalId());
        initVars.put(BpmConstants.ProcessVariables.batchId, batch.getId());
        initVars.put(BpmConstants.ProcessVariables.latestConfig, ingestWorkflowConfig);
        initVars.put(BpmConstants.ProcessVariables.assignee, userId);
        initVars.put(BpmConstants.ProcessVariables.producerId, producerProfile.getProducer().getId());
        initVars.put(BpmConstants.ProcessVariables.sipId, ingestWorkflow.getSip().getId());
        initVars.put(BpmConstants.ProcessVariables.sipVersion, ingestWorkflow.getSip().getVersionNumber());
        initVars.put(BpmConstants.ProcessVariables.xmlVersion, ingestWorkflow.getXmlVersionNumber());
        initVars.put(BpmConstants.ProcessVariables.debuggingModeActive, producerProfile.getDebuggingModeActive());
        initVars.put(BpmConstants.Validation.validationProfileId, producerProfile.getValidationProfile().getId());
        initVars.put(BpmConstants.ArchivalStorage.aipSavedCheckRetries, aipSavedCheckRetries);
        initVars.put(BpmConstants.ArchivalStorage.aipSavedCheckTimeout, aipSavedCheckTimeout);
        initVars.put(BpmConstants.ArchivalStorage.aipStoreRetries, aipStoreRetries);
        initVars.put(BpmConstants.ArchivalStorage.aipStoreTimeout, aipStoreTimeout);

        runtimeService.startProcessInstanceByKey(toBatchDeploymentName(batch.getId()), ingestWorkflow.getExternalId(), initVars);
    }

    /**
     * Creates or assigns existing SIP package and authorial package to the ingest workflow according to the determined
     * level of versioning. In case the extracted authorial id in combination with the producer profile id matches an existing
     * authorial package in database, the versioning is triggered, otherwise new authorial package is created:
     * <p>
     * a) The xml versioning is performed if: the SIP with the highest version number from the SIPs belonging
     * the authorial package has the same checksum as the checksum of the incoming SIP.
     * <p>
     * b) The sip versioning is performed if: the SIP with the highest version number from the SIPs belonging
     * the authorial package has a different checksum as the checksum of the incoming SIP.
     *
     * @param ingestWorkflow         ingest workflow
     * @param sipFolderWorkspacePath path to the folder with the content of the SIP in workspace
     * @param filePaths              file paths of the files of the SIP
     * @throws AuthorialPackageLockedException if the authorial package is locked
     */
    private void createSipAndAuthorialPackages(IngestWorkflow ingestWorkflow, Path sipFolderWorkspacePath,
                                               List<String> filePaths, String userId) throws IOException {
        ProducerProfile producerProfile = ingestWorkflow.getBatch().getProducerProfile();
        String extractedAuthorialId = extractAuthorialId(ingestWorkflow, sipFolderWorkspacePath, producerProfile.getSipProfile());

        AuthorialPackage authorialPackage = authorialPackageStore.findByAuthorialIdAndProducerProfileId(extractedAuthorialId, producerProfile.getId());
        if (authorialPackage != null) {
            aipService.activateLock(authorialPackage.getId(), false);

            List<IngestWorkflow> workflowsByAuthorialPackageId = ingestWorkflowStore.findByAuthorialPackageId(authorialPackage.getId());
            Optional<Sip> highestVersionNumberPersistedSip = workflowsByAuthorialPackageId.stream()
                    .filter(i -> i.getProcessingState() == IngestWorkflowState.PERSISTED)
                    .map(IngestWorkflow::getSip)
                    .distinct()
                    .max(Comparator.comparing(Sip::getVersionNumber));

            if (highestVersionNumberPersistedSip.isPresent()) {
                Optional<IngestWorkflow> highestVersionNumberIngestWorkflow = workflowsByAuthorialPackageId.stream()
                        .filter(i -> i.getSip().getId().equals(highestVersionNumberPersistedSip.get().getId()))
                        .filter(i -> i.getProcessingState().equals(IngestWorkflowState.PERSISTED))
                        .max(Comparator.comparing(IngestWorkflow::getXmlVersionNumber));
                ingestWorkflow.setRelatedWorkflow(highestVersionNumberIngestWorkflow.get());

                if (highestVersionNumberPersistedSip.get().getHashes().stream()
                        .anyMatch(hash -> hash.getHashValue().equals(ingestWorkflow.getHash().getHashValue()))) {
                    xmlVersioning(ingestWorkflow, highestVersionNumberPersistedSip.get(), highestVersionNumberIngestWorkflow.get());
                } else {
                    sipVersioning(ingestWorkflow, filePaths, sipFolderWorkspacePath, authorialPackage, highestVersionNumberPersistedSip.get());
                }
            } else {
                noVersioning(authorialPackage, ingestWorkflow, filePaths, sipFolderWorkspacePath);
            }
        } else {
            authorialPackage = new AuthorialPackage();
            authorialPackage.setAuthorialId(extractedAuthorialId);
            authorialPackage.setProducerProfile(producerProfile);
            log.info("New authorial package with id " + authorialPackage.getId() + " created.");
            authorialPackageStore.save(authorialPackage);

            aipService.activateLock(authorialPackage.getId(), false);
            noVersioning(authorialPackage, ingestWorkflow, filePaths, sipFolderWorkspacePath);
        }
    }

    private void noVersioning(AuthorialPackage authorialPackage, IngestWorkflow ingestWorkflow,
                              List<String> filePaths, Path sipFolderWorkspacePath) {
        Sip sip = new Sip();
        sip.setHashes(asSet(ingestWorkflow.getHash()));
        sip.setAuthorialPackage(authorialPackage);
        sip.setFolderStructure(filePathsToFolderStructure(filePaths, sipFolderWorkspacePath.getFileName().toString()));
        sip.setPreviousVersionSip(null);
        sip.setVersionNumber(1);
        sipStore.save(sip);
        log.info("New SIP package with id " + sip.getId() + " created.");

        ingestWorkflow.setVersioningLevel(VersioningLevel.NO_VERSIONING);
        ingestWorkflow.setRelatedWorkflow(null);
        ingestWorkflow.setXmlVersionNumber(1);
        ingestWorkflow.setSip(sip);
        ingestWorkflowStore.save(ingestWorkflow);
    }

    private void xmlVersioning(IngestWorkflow ingestWorkflow, Sip highestVersionNumberSip,
                               IngestWorkflow highestVersionNumberIngestWorkflow) {
        log.info("XML versioning is performed for ingest worfklow with external id " + ingestWorkflow.getExternalId() + ".");
        ingestWorkflow.setSip(highestVersionNumberSip);
        ingestWorkflow.setXmlVersionNumber(highestVersionNumberIngestWorkflow.getXmlVersionNumber() + 1);
        ingestWorkflow.setVersioningLevel(VersioningLevel.ARCLIB_XML_VERSIONING);
        ingestWorkflowStore.save(ingestWorkflow);
    }

    private void sipVersioning(IngestWorkflow ingestWorkflow, List<String> filePaths, Path sipFolderWorkspacePath,
                               AuthorialPackage authorialPackage, Sip highestVersionNumberSip) {
        log.info("SIP versioning is performed for ingest worfklow with external id " + ingestWorkflow.getExternalId() + ".");
        Sip sip = new Sip();
        sip.setHashes(asSet(ingestWorkflow.getHash()));
        sip.setVersionNumber(highestVersionNumberSip.getVersionNumber() + 1);
        sip.setAuthorialPackage(authorialPackage);
        sip.setFolderStructure(filePathsToFolderStructure(filePaths, sipFolderWorkspacePath.getFileName().toString()));
        sip.setPreviousVersionSip(highestVersionNumberSip);
        sipStore.save(sip);
        log.info("New SIP package with id " + sip.getId() + " created.");

        ingestWorkflow.setVersioningLevel(VersioningLevel.SIP_PACKAGE_VERSIONING);
        ingestWorkflow.setXmlVersionNumber(1);
        ingestWorkflow.setSip(sip);
        ingestWorkflowStore.save(ingestWorkflow);
    }

    /**
     * Counts the number of ingest workflows with the state FAILED for the given batch. If the count is bigger than 1/2
     * of all the ingest workflows of the batch, sets the batch state to CANCELED and returns true, otherwise returns false.
     *
     * @param batch batch
     * @return
     */
    private boolean tooManyFailedIngestWorkflows(Batch batch) {
        List<IngestWorkflow> ingestWorkflows = batch.getIngestWorkflows();
        int allIngestWorkflowsCount = ingestWorkflows.size();

        long failedIngestWorkflowsCount = ingestWorkflows.stream()
                .filter(sip -> sip.getProcessingState() == IngestWorkflowState.FAILED)
                .count();

        return failedIngestWorkflowsCount > (allIngestWorkflowsCount / 2);
    }

    /**
     * Verifies the hash of the SIP of the ingest workflow to the computed value and throws and exception if the hash does not match
     *
     * @param pathToSip      path to sip of which the hash should be verified
     * @param ingestWorkflow ingest workflow
     * @param userId         id of the user that triggered the batch
     * @throws IOException SIP of the ingest workflow at the given path does not exist
     */
    private void verifyHash(Path pathToSip, IngestWorkflow ingestWorkflow, String userId) throws IOException {
        Hash expectedHash = ingestWorkflow.getHash();
        notNull(expectedHash, () -> {
            throw new IllegalArgumentException("null hash of ingest workflow with external id " + ingestWorkflow.getExternalId());
        });
        String computedHash;
        try (FileInputStream sipContent = new FileInputStream(pathToSip.toString())) {
            FixityCounter fixityCounter;
            switch (expectedHash.getHashType()) {
                case MD5:
                    fixityCounter = md5Counter;
                    break;
                case Crc32:
                    fixityCounter = crc32Counter;
                    break;
                case Sha512:
                    fixityCounter = sha512Counter;
                    break;
                default:
                    throw new GeneralException("unexpected type of expectedHash");
            }
            computedHash = bytesToHexString(fixityCounter.computeDigest(sipContent));
        }

        if (!expectedHash.getHashValue().equalsIgnoreCase(computedHash)) {
            String message = "Invalid hash for SIP of ingest workflow with external id " + ingestWorkflow.getExternalId() + ".";
            log.error(message);
            throw new InvalidChecksumException(message);
        }
        log.info("Hash of ingest workflow with external id " + ingestWorkflow.getExternalId() + " was successfully verified.");
    }

    /**
     * Unzips and copies the content of the SIP belonging the ingest workflow to workspace.
     *
     * @param ingestWorkflow ingest workflow
     * @param userId         id of the user
     * @return number of copied bytes
     */
    private long copySipToWorkspace(IngestWorkflow ingestWorkflow, String userId) {
        Path sourceSipFilePath = getSipZipTransferAreaPath(ingestWorkflow);
        Path destinationIngestWorkflowPath = getIngestWorkflowWorkspacePath(ingestWorkflow.getExternalId(), workspace);
        Path destinationSipZipPath = destinationIngestWorkflowPath.resolve(ingestWorkflow.getOriginalFileName() + ArclibUtils.ZIP_EXTENSION);

        try {
            Files.createDirectories(destinationIngestWorkflowPath);
            log.info("Created directory at workspace for ingest workflow external id " + ingestWorkflow.getExternalId() + ".");
        } catch (IOException e) {
            throw new GeneralException("Unable to create directory in workspace " + workspace
                    + " for ingest workflow with external id : " + ingestWorkflow.getExternalId(), e);
        }

        //copy the zip content to workspace
        long numberOfBytesCopied;
        try (FileInputStream sipContent = new FileInputStream(sourceSipFilePath.toAbsolutePath().toString())) {
            numberOfBytesCopied = Files.copy(sipContent, destinationSipZipPath);
            verifyHash(getSipZipWorkspacePath(ingestWorkflow.getExternalId(), workspace,
                    ingestWorkflow.getOriginalFileName() + ArclibUtils.ZIP_EXTENSION), ingestWorkflow, userId);
            log.info("Zip archive with SIP content for ingest workflow external id " + ingestWorkflow.getExternalId() + " has been" +
                    " copied to workspace at path " + destinationSipZipPath + ".");
        } catch (IOException e) {
            throw new GeneralException("Unable to find SIP at path: " + sourceSipFilePath.toAbsolutePath().toString()
                    + " or access the destination path: " + destinationSipZipPath.toAbsolutePath().toString(), e);
        }

        //unzip the zip content in workspace
        try (FileInputStream sipContent = new FileInputStream(destinationSipZipPath.toString())) {
            ZipUtils.unzip(sipContent, destinationIngestWorkflowPath);
            log.info("SIP content for ingest workflow external id " + ingestWorkflow.getExternalId() + " in zip archive has been" +
                    " extracted to workspace.");
        } catch (IOException e) {
            throw new GeneralException("Unable to unzip SIP content for ingest workflow external id "
                    + ingestWorkflow.getExternalId() + " to path: " + sourceSipFilePath.toAbsolutePath().toString(), e);
        }
        return numberOfBytesCopied;
    }

    /**
     * Extracts authorial id from SIP
     *
     * @param ingestWorkflow         ingest workflow package with the SIP package
     * @param sipFolderWorkspacePath path to the folder with the SIP content in workspace
     * @param profile                SIP profile storing the path to authorial id
     * @return authorial id
     * @throws GeneralException file storing the authorial id is inaccessible
     */
    private String extractAuthorialId(IngestWorkflow ingestWorkflow, Path sipFolderWorkspacePath, SipProfile profile) {
        PathToSipId pathToSipId = profile.getPathToSipId();
        notNull(pathToSipId, () -> {
            throw new IllegalArgumentException("null path to sip id of sip profile with id " + profile.getId());
        });

        String pathToXml = pathToSipId.getPathToXml();
        notNull(pathToXml, () -> {
            throw new IllegalArgumentException("null path to xml in path to authorial id");
        });

        String xPathToId = pathToSipId.getXPathToId();
        notNull(xPathToId, () -> {
            throw new IllegalArgumentException("null path to id in path to authorial id");
        });

        NodeList elems;
        Path pathToMetadataXml = sipFolderWorkspacePath.resolve(pathToXml);

        try (FileInputStream metadataFile = new FileInputStream(pathToMetadataXml.toString())) {
            elems = XmlUtils.findWithXPath(metadataFile, xPathToId);
        } catch (SAXException | ParserConfigurationException e) {
            throw new GeneralException("Error in XPath expression to authorial id: " + xPathToId, e);
        } catch (FileNotFoundException e) {
            throw new GeneralException("File with metadata for ingest workflow with external id " + ingestWorkflow.getExternalId()
                    + " does not exist at path: " + pathToMetadataXml, e);
        } catch (IOException e) {
            throw new GeneralException("File with metadata for ingest workflow with external id " + ingestWorkflow.getExternalId()
                    + " is inaccessible at path: " + pathToMetadataXml, e);
        }

        if (elems.getLength() == 0) {
            throw new GeneralException("Non existent path to authorial id: " + xPathToId);
        }
        if (elems.getLength() > 1) {
            throw new GeneralException("Ambiguous path to authorial id: " + xPathToId);
        }
        Node item = elems.item(0);
        String authorialId = item.getTextContent();

        if (authorialId.isEmpty()) throw new GeneralException("Authorial id of SIP at XPath " + xPathToId +
                " in file " + pathToXml + " is empty.");
        log.info("Authorial id of SIP for ingest workflow with external id " + ingestWorkflow.getExternalId() +
                " has been successfully extracted. Authorial id is: " + authorialId + ".");

        return authorialId;
    }

    /**
     * Computes resulting ingest workflow config by merging producer and batch ingest configs.
     * If there are two equally named attributes the batch config has the priority over the producer config.
     *
     * @param batchIngestConfig    ingest config provided with the batch
     * @param producerIngestConfig ingest config of the producer
     * @param externalId           external id of the ingest workflow
     * @return computed ingest config
     */
    private String computeIngestWorkflowConfig(String batchIngestConfig, String producerIngestConfig,
                                               String externalId) throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        if (producerIngestConfig.isEmpty()) {
            throw new IllegalArgumentException("Producer ingest workflow config json for ingest workflow with external id "
                    + externalId + " is empty.");
        }
        log.info("Producer ingest workflow config json for ingest workflow with external id " +
                externalId + ": " + producerIngestConfig);

        JsonNode ingestConfig = mapper.readTree(producerIngestConfig);

        if (batchIngestConfig != null) {
            log.info("Batch ingest workflow config json for ingest workflow with external id " +
                    externalId + ": " + batchIngestConfig);
            JsonNode batchIngestConfigJson = mapper.readTree(batchIngestConfig);
            ingestConfig = JsonUtils.merge(ingestConfig, batchIngestConfigJson);
        } else {
            log.info("Batch ingest workflow config json for ingest workflow with external id " + externalId
                    + " is empty.");
        }

        log.info("Result ingest workflow config json for ingest workflow with external id " + externalId + ": "
                + ingestConfig);
        return mapper.writeValueAsString(ingestConfig);
    }


    /**
     * Computes fixities for files located in the root folder of SIP (not nested in other subfolder)
     *
     * @param pathToSipFolder path to the folder with the sip content
     * @param hashType        type of hash to compute
     * @return list of triplets of a file path, type of fixity and computed fixity
     * @throws IOException if some of the files are inaccessible
     */
    private List<Utils.Triplet<String, String, String>> computeRootDirFilesFixities(Path pathToSipFolder, HashType hashType) throws IOException {
        FixityCounter fixityCounter;
        switch (hashType) {
            case MD5:
                fixityCounter = md5Counter;
                break;
            case Crc32:
                fixityCounter = crc32Counter;
                break;
            case Sha512:
                fixityCounter = sha512Counter;
                break;
            default:
                throw new GeneralException("unexpected type of expectedHash");
        }
        List<Utils.Triplet<String, String, String>> filesAndFixities = new ArrayList<>();
        Collection<File> files = FileUtils.listFiles(pathToSipFolder.toFile(), null, false);
        for (File file : files) {
            try (FileInputStream fileContent = new FileInputStream(file.getAbsolutePath())) {
                String computedHash = bytesToHexString(fixityCounter.computeDigest(fileContent));
                filesAndFixities.add(new Utils.Triplet<>(file.getName(), hashType.name(), computedHash));
                log.info("Computed fixity for file " + file.getName() + ", hash type: " + hashType.name() + ", hash value: "
                        + computedHash + ".");
            }
        }
        return filesAndFixities;
    }

    @Inject
    public void setManagementService(ManagementService managementService) {
        this.managementService = managementService;
    }

    @Inject
    public void setBatchService(BatchService batchService) {
        this.batchService = batchService;
    }

    @Inject
    public void setWorkspace(@Value("${arclib.path.workspace}") String workspace) {
        this.workspace = workspace;
    }

    @Inject
    public void setIngestWorkflowStore(IngestWorkflowStore ingestWorkflowStore) {
        this.ingestWorkflowStore = ingestWorkflowStore;
    }

    @Inject
    public void setTemplate(JmsTemplate template) {
        this.template = template;
    }

    @Inject
    public void setRuntimeService(RuntimeService runtimeService) {
        this.runtimeService = runtimeService;
    }

    @Inject
    public void setCrc32Counter(Crc32Counter crc32Counter) {
        this.crc32Counter = crc32Counter;
    }

    @Inject
    public void setSha512Counter(Sha512Counter sha512Counter) {
        this.sha512Counter = sha512Counter;
    }

    @Inject
    public void setMd5Counter(Md5Counter md5Counter) {
        this.md5Counter = md5Counter;
    }

    @Inject
    public void setAuthorialPackageStore(AuthorialPackageStore authorialPackageStore) {
        this.authorialPackageStore = authorialPackageStore;
    }

    @Inject
    public void setSipStore(SipStore sipStore) {
        this.sipStore = sipStore;
    }

    @Inject
    public void setIngestErrorHandler(IngestErrorHandler ingestErrorHandler) {
        this.ingestErrorHandler = ingestErrorHandler;
    }

    @Inject
    public void setAipService(AipService aipService) {
        this.aipService = aipService;
    }

    @Inject
    public void setAipSavedCheckRetries(@Value("${arclib.aipSavedCheckRetries}")
                                                int aipSavedCheckRetries) {
        this.aipSavedCheckRetries = aipSavedCheckRetries;
    }

    @Inject
    public void setAipStoreRetries(@Value("${arclib.aipStoreRetries}") int aipStoreRetries) {
        this.aipStoreRetries = aipStoreRetries;
    }

    @Inject
    public void setAipStoreTimeout(@Value("${arclib.aipStoreTimeout}") String aipStoreTimeout) {
        this.aipStoreTimeout = aipStoreTimeout;
    }

    @Inject
    public void setAipSavedCheckTimeout(@Value("${arclib.aipSavedCheckTimeout}") String aipSavedCheckTimeout) {
        this.aipSavedCheckTimeout = aipSavedCheckTimeout;
    }
}
