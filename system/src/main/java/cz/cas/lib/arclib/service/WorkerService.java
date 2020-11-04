package cz.cas.lib.arclib.service;

import cz.cas.lib.arclib.domain.Batch;
import cz.cas.lib.arclib.domain.BatchState;
import cz.cas.lib.arclib.domain.Hash;
import cz.cas.lib.arclib.domain.VersioningLevel;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflow;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflowFailureInfo;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflowFailureType;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflowState;
import cz.cas.lib.arclib.domain.packages.AuthorialPackage;
import cz.cas.lib.arclib.domain.packages.Sip;
import cz.cas.lib.arclib.domain.profiles.PathToSipId;
import cz.cas.lib.arclib.domain.profiles.ProducerProfile;
import cz.cas.lib.arclib.domain.profiles.SipProfile;
import cz.cas.lib.arclib.domainbase.exception.GeneralException;
import cz.cas.lib.arclib.domainbase.exception.MissingObject;
import cz.cas.lib.arclib.dto.IncidentInfoDto;
import cz.cas.lib.arclib.dto.JmsDto;
import cz.cas.lib.arclib.exception.AuthorialPackageLockedException;
import cz.cas.lib.arclib.exception.InvalidChecksumException;
import cz.cas.lib.arclib.service.fixity.Crc32Counter;
import cz.cas.lib.arclib.service.fixity.FixityCounter;
import cz.cas.lib.arclib.service.fixity.Md5Counter;
import cz.cas.lib.arclib.service.fixity.Sha512Counter;
import cz.cas.lib.arclib.store.AuthorialPackageStore;
import cz.cas.lib.arclib.store.SipStore;
import cz.cas.lib.arclib.utils.ArclibUtils;
import cz.cas.lib.arclib.utils.XmlUtils;
import cz.cas.lib.arclib.utils.ZipUtils;
import cz.cas.lib.core.util.Utils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.camunda.bpm.engine.ManagementService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.Incident;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.inject.Inject;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static cz.cas.lib.arclib.bpm.BpmConstants.*;
import static cz.cas.lib.arclib.utils.ArclibUtils.*;
import static cz.cas.lib.core.util.Utils.*;

@Slf4j
@Service
public class WorkerService {

    private IngestWorkflowService ingestWorkflowService;
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
    private TransactionTemplate transactionTemplate;

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
    @JmsListener(destination = "worker")
    public void startProcessingOfIngestWorkflow(JmsDto dto) {
        String externalId = dto.getId();

        IngestWorkflow ingestWorkflow = ingestWorkflowService.findByExternalId(externalId);
        Utils.notNull(ingestWorkflow, () -> new MissingObject(IngestWorkflow.class, externalId));

        try {
            Batch batch = ingestWorkflow.getBatch();
            String batchId = batch.getId();

            log.debug("Message received at Worker. Batch id: " + batchId + ", ingest workflow external id: " + externalId);

            //workaround to initialize batch because of the lazy initialization of batch entity
            batch = batchService.get(batchId);
            notNull(batch, () -> {
                throw new MissingObject(Batch.class, batchId);
            });

            if (batch.getState() != BatchState.PROCESSING) {
                log.warn("Cannot process ingest workflow " + externalId + " because the batch " + batchId +
                        " is in state " + batch.getState().toString() + ".");
                return;
            }

            if (tooManyFailedIngestWorkflows(batch)) {
                log.error("Processing of batch " + batchId + " stopped because of too many ingest workflow failures.");
                template.convertAndSend("cancel", new JmsDto(batchId, dto.getUserId()));
                return;
            }

            transactionTemplate.execute((TransactionCallback<Void>) status -> {
                try {
                    //all DB operations in the template will be rolled back in case of any exception
                    ingestWorkflow.setProcessingState(IngestWorkflowState.PROCESSING);
                    ingestWorkflowService.save(ingestWorkflow);
                    processIngestWorkflow(ingestWorkflow, dto.getUserId());
                    log.info("State of ingest workflow with external id " + externalId + " changed to PROCESSING.");
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
                return null;
            });
        } catch (InvalidChecksumException e) {
            ingestErrorHandler.handleError(ingestWorkflow.getExternalId(), new IngestWorkflowFailureInfo(e.getMessage(),
                    ExceptionUtils.getStackTrace(e), IngestWorkflowFailureType.INVALID_CHECKSUM), null, dto.getUserId());
            e.printStackTrace();
        } catch (AuthorialPackageLockedException e) {
            ingestErrorHandler.handleError(ingestWorkflow.getExternalId(), new IngestWorkflowFailureInfo(e.getMessage(),
                    ExceptionUtils.getStackTrace(e), IngestWorkflowFailureType.AUTHORIAL_PACKAGE_LOCKED), null, dto.getUserId());
            e.printStackTrace();
        } catch (Exception e) {
            String message = e.getMessage() != null ? e.getMessage() : "";
            ingestErrorHandler.handleError(ingestWorkflow.getExternalId(), new IngestWorkflowFailureInfo(message,
                    ExceptionUtils.getStackTrace(e), IngestWorkflowFailureType.INTERNAL_ERROR), null, dto.getUserId());
            e.printStackTrace();
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
    @JmsListener(destination = "incident")
    public void solveIncident(SolveIncidentDto incidentDto) {
        log.info("Trying to solve incident with ID " + incidentDto.getIncidentId() + " using config " + incidentDto.getConfig() + ".");
        Incident i = runtimeService
                .createIncidentQuery()
                .incidentId(incidentDto.getIncidentId())
                .singleResult();
        notNull(i, (() -> new MissingObject(Incident.class, incidentDto.getIncidentId())));
        String configUsedWhenIncidentOccurred = (String) runtimeService.getVariable(i.getProcessInstanceId(), ProcessVariables.latestConfig);
        runtimeService.setVariable(i.getProcessInstanceId(), toIncidentConfigVariableName(i.getId()), configUsedWhenIncidentOccurred);
        runtimeService.setVariable(i.getProcessInstanceId(), ProcessVariables.latestConfig, incidentDto.getConfig());
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

        verifyHash(getSipZipTransferAreaPath(ingestWorkflow).toAbsolutePath(), ingestWorkflow);
        Pair<String, Long> rootDirNameAndSizeInBytes = copySipToWorkspace(ingestWorkflow);

        Path sipFolderWorkspacePath = getIngestWorkflowWorkspacePath(ingestWorkflow.getExternalId(), workspace)
                .resolve(rootDirNameAndSizeInBytes.getLeft());

        List<String> filePaths = ArclibUtils.listFilePaths(sipFolderWorkspacePath);
        createSipAndAuthorialPackages(ingestWorkflow, sipFolderWorkspacePath, filePaths, userId, rootDirNameAndSizeInBytes.getRight());

        Map<String, Object> initVars = new HashMap<>();
        initVars.put(Ingestion.sipFileName, ingestWorkflow.getFileName());
        initVars.put(Ingestion.dateTime, Instant.now().truncatedTo(ChronoUnit.SECONDS).toString());
        initVars.put(Ingestion.authorialId, ingestWorkflow.getSip().getAuthorialPackage().getAuthorialId());
        initVars.put(ProcessVariables.sipFolderWorkspacePath, sipFolderWorkspacePath.toString());
        initVars.put(ProcessVariables.ingestWorkflowExternalId, ingestWorkflow.getExternalId());
        initVars.put(ProcessVariables.batchId, batch.getId());
        initVars.put(ProcessVariables.latestConfig, ingestWorkflow.getInitialConfig());
        initVars.put(ProcessVariables.responsiblePerson, userId);
        initVars.put(ProcessVariables.producerId, producerProfile.getProducer().getId());
        initVars.put(ProcessVariables.sipId, ingestWorkflow.getSip().getId());
        initVars.put(ProcessVariables.sipVersion, ingestWorkflow.getSip().getVersionNumber());
        initVars.put(ProcessVariables.xmlVersion, ingestWorkflow.getXmlVersionNumber());
        initVars.put(ProcessVariables.debuggingModeActive, producerProfile.isDebuggingModeActive());
        initVars.put(ProcessVariables.producerProfileExternalId, producerProfile.getExternalId());
        initVars.put(ArchivalStorage.aipSavedCheckRetries, aipSavedCheckRetries);
        initVars.put(ArchivalStorage.aipSavedCheckTimeout, aipSavedCheckTimeout);
        initVars.put(ArchivalStorage.aipStoreRetries, aipStoreRetries);
        initVars.put(ArchivalStorage.aipStoreTimeout, aipStoreTimeout);
        initVars.put(Antivirus.antivirusToolCounter, 0);
        initVars.put(FixityCheck.fixityCheckToolCounter, 0);
        initVars.put(FormatIdentification.mapOfEventIdsToMapsOfFilesToFormats, new HashMap<String, TreeMap<String, Pair<String, String>>>());
        initVars.put(FixityGeneration.mapOfEventIdsToSipSha512, new HashMap<>());
        initVars.put(FixityGeneration.mapOfEventIdsToSipMd5, new HashMap<>());
        initVars.put(FixityGeneration.mapOfEventIdsToSipCrc32, new HashMap<>());
        initVars.put(FixityGeneration.mapOfEventIdsToSipContentFixityData, new HashMap<String, Map<String, Triple<Long, String, String>>>());

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
     * @param sizeInBytes           size of the data part in bytes
     * @throws AuthorialPackageLockedException if the authorial package is locked
     */
    private void createSipAndAuthorialPackages(IngestWorkflow ingestWorkflow, Path sipFolderWorkspacePath,
                                               List<String> filePaths, String userId,
                                               long sizeInBytes) throws IOException {
        ProducerProfile producerProfile = ingestWorkflow.getBatch().getProducerProfile();
        String extractedAuthorialId = extractAuthorialId(ingestWorkflow, sipFolderWorkspacePath, producerProfile.getSipProfile());

        AuthorialPackage authorialPackage = authorialPackageStore.findByAuthorialIdAndProducerProfileId(extractedAuthorialId, producerProfile.getId());
        if (authorialPackage != null) {
            aipService.activateLock(authorialPackage.getId(), false, userId);

            List<IngestWorkflow> workflowsByAuthorialPackageId = ingestWorkflowService.findByAuthorialPackageId(authorialPackage.getId());
            Optional<Sip> highestVersionNumberPersistedSip = workflowsByAuthorialPackageId.stream()
                    .filter(i -> i.getDeleted() == null && i.getProcessingState() == IngestWorkflowState.PERSISTED)
                    .map(IngestWorkflow::getSip)
                    .distinct()
                    .max(Comparator.comparing(Sip::getVersionNumber));

            if (highestVersionNumberPersistedSip.isPresent()) {
                Optional<IngestWorkflow> highestVersionNumberIngestWorkflow = workflowsByAuthorialPackageId.stream()
                        .filter(i -> i.getSip().getId().equals(highestVersionNumberPersistedSip.get().getId()))
                        .filter(i -> i.getProcessingState().equals(IngestWorkflowState.PERSISTED))
                        .filter(i -> i.getDeleted() == null)
                        .max(Comparator.comparing(IngestWorkflow::getXmlVersionNumber));
                ingestWorkflow.setRelatedWorkflow(highestVersionNumberIngestWorkflow.get());

                boolean creatingDebugVersionOfProperIngest = producerProfile.isDebuggingModeActive() && !ingestWorkflow.getRelatedWorkflow().wasIngestedInDebugMode();
                boolean creatingProperVersionOfDebugIngest = !producerProfile.isDebuggingModeActive() && ingestWorkflow.getRelatedWorkflow().wasIngestedInDebugMode();
                if (creatingDebugVersionOfProperIngest || creatingProperVersionOfDebugIngest) {
                    throw new IllegalArgumentException("Can't continue with ingest of authorial package: " + authorialPackage.getAuthorialId() + " with debug mode set to: " + producerProfile.isDebuggingModeActive() + " because this package was already ingested before with debug mode set to: " + ingestWorkflow.getRelatedWorkflow().getBatch().isDebuggingModeActive() + " and resulted in AIP: " + ingestWorkflow.getRelatedWorkflow().getSip().getId()
                            + ". Debug and proper ingests can't mix. There are three options how to solve this problem:" +
                            " 1) change the debuggingModeActive property of the related producer profile: " + producerProfile.getExternalId() +
                            " 2) if the previous ingest was done in debug mode, use FORGET feature, or ask administrator to do so" +
                            " 3) edit the authorial id element of the package that has to be ingested to different value than current: " + authorialPackage.getAuthorialId()
                    );
                }

                if (highestVersionNumberPersistedSip.get().getHashes().stream()
                        .anyMatch(hash -> hash.getHashValue().equals(ingestWorkflow.getHash().getHashValue()))) {
                    xmlVersioning(ingestWorkflow, highestVersionNumberPersistedSip.get(), highestVersionNumberIngestWorkflow.get());
                } else {
                    sipVersioning(ingestWorkflow, filePaths, sipFolderWorkspacePath, authorialPackage, highestVersionNumberPersistedSip.get(), sizeInBytes);
                }
            } else {
                noVersioning(authorialPackage, ingestWorkflow, filePaths, sipFolderWorkspacePath, sizeInBytes);
            }
        } else {
            authorialPackage = new AuthorialPackage();
            authorialPackage.setAuthorialId(extractedAuthorialId);
            authorialPackage.setProducerProfile(producerProfile);
            authorialPackageStore.save(authorialPackage);
            log.info("New authorial package with id " + authorialPackage.getId() + " created.");

            aipService.activateLock(authorialPackage.getId(), false, userId);
            noVersioning(authorialPackage, ingestWorkflow, filePaths, sipFolderWorkspacePath, sizeInBytes);
        }
    }

    private void noVersioning(AuthorialPackage authorialPackage, IngestWorkflow ingestWorkflow,
                              List<String> filePaths, Path sipFolderWorkspacePath, long sizeInBytes) {
        Sip sip = new Sip();
        sip.setSizeInBytes(sizeInBytes);
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
        ingestWorkflowService.save(ingestWorkflow);
    }

    private void xmlVersioning(IngestWorkflow ingestWorkflow, Sip highestVersionNumberSip,
                               IngestWorkflow highestVersionNumberIngestWorkflow) {
        log.debug("XML versioning is performed for ingest workflow with external id " + ingestWorkflow.getExternalId() + ".");
        ingestWorkflow.setSip(highestVersionNumberSip);
        ingestWorkflow.setXmlVersionNumber(highestVersionNumberIngestWorkflow.getXmlVersionNumber() + 1);
        ingestWorkflow.setVersioningLevel(VersioningLevel.ARCLIB_XML_VERSIONING);
        ingestWorkflowService.save(ingestWorkflow);
    }

    private void sipVersioning(IngestWorkflow ingestWorkflow, List<String> filePaths, Path sipFolderWorkspacePath,
                               AuthorialPackage authorialPackage, Sip highestVersionNumberSip, long sizeInBytes) {
        log.debug("SIP versioning is performed for ingest worfklow with external id " + ingestWorkflow.getExternalId() + ".");
        Sip sip = new Sip();
        sip.setSizeInBytes(sizeInBytes);
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
        ingestWorkflowService.save(ingestWorkflow);
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
     * @throws IOException SIP of the ingest workflow at the given path does not exist
     */
    private void verifyHash(Path pathToSip, IngestWorkflow ingestWorkflow) throws IOException {
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
        log.debug("Hash of ingest workflow with external id " + ingestWorkflow.getExternalId() + " was successfully verified.");
    }

    /**
     * Unzips and copies the content of the SIP belonging the ingest workflow to workspace.
     *
     * @param ingestWorkflow ingest workflow
     * @return pair where the left value is name of the root folder of the extracted SIP and right value is number of copied bytes
     */
    private Pair<String, Long> copySipToWorkspace(IngestWorkflow ingestWorkflow) {
        Path sourceSipFilePath = getSipZipTransferAreaPath(ingestWorkflow);
        Path destinationIngestWorkflowPath = getIngestWorkflowWorkspacePath(ingestWorkflow.getExternalId(), workspace);
        Path destinationSipZipPath = destinationIngestWorkflowPath.resolve(ingestWorkflow.getFileName());

        try {
            Files.createDirectories(destinationIngestWorkflowPath);
            log.debug("Created directory at workspace for ingest workflow external id " + ingestWorkflow.getExternalId() + ".");
        } catch (IOException e) {
            throw new GeneralException("Unable to create directory in workspace " + workspace
                    + " for ingest workflow with external id : " + ingestWorkflow.getExternalId(), e);
        }

        //copy the zip content to workspace
        long numberOfBytesCopied;
        try (FileInputStream sipContent = new FileInputStream(sourceSipFilePath.toAbsolutePath().toString())) {
            numberOfBytesCopied = Files.copy(sipContent, destinationSipZipPath);
            verifyHash(getSipZipWorkspacePath(ingestWorkflow.getExternalId(), workspace,
                    ingestWorkflow.getFileName()), ingestWorkflow);
            log.debug("Zip archive with SIP content for ingest workflow external id " + ingestWorkflow.getExternalId() + " has been" +
                    " copied to workspace at path " + destinationSipZipPath + ".");
        } catch (IOException e) {
            throw new GeneralException("Unable to find SIP at path: " + sourceSipFilePath.toAbsolutePath().toString()
                    + " or access the destination path: " + destinationSipZipPath.toAbsolutePath().toString(), e);
        }

        //unzip the zip content in workspace
        String rootFolderName = ZipUtils.unzipSip(destinationSipZipPath, destinationIngestWorkflowPath, ingestWorkflow.getExternalId());
        return Pair.of(rootFolderName, numberOfBytesCopied);
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
    private String extractAuthorialId(IngestWorkflow ingestWorkflow, Path sipFolderWorkspacePath, SipProfile profile) throws IOException {
        PathToSipId pathToSipId = profile.getPathToSipId();
        notNull(pathToSipId, () -> {
            throw new IllegalArgumentException("null path to sip id of sip profile with id " + profile.getId());
        });

        String pathToXmlGlobPattern = pathToSipId.getPathToXmlGlobPattern();
        notNull(pathToXmlGlobPattern, () -> {
            throw new IllegalArgumentException("null pathToXmlGlobPattern in path to authorial id");
        });

        String xPathToId = pathToSipId.getXPathToId();
        notNull(xPathToId, () -> {
            throw new IllegalArgumentException("null path to id in path to authorial id");
        });

        NodeList elems;
        List<File> matchingFiles = listFilesMatchingGlobPattern(new File(sipFolderWorkspacePath.toAbsolutePath().toString()),
                pathToXmlGlobPattern);
        if (matchingFiles.size() == 0) throw new GeneralException("File with metadata for ingest workflow with external id "
                + ingestWorkflow.getExternalId() + " does not exist at path given by glob pattern: " + pathToXmlGlobPattern);

        if (matchingFiles.size() > 1) throw new GeneralException("Multiple files found " +
                "at the path given by glob pattern: " + pathToXmlGlobPattern);

        File metadataFile = matchingFiles.get(0);
        String authorialId;
        try (FileInputStream fis = new FileInputStream(metadataFile)) {
            authorialId = XmlUtils.findSingleNodeWithXPath(fis, xPathToId).getTextContent();
        } catch (SAXException | ParserConfigurationException e) {
            throw new GeneralException("Error in XPath expression to authorial id: " + xPathToId, e);
        } catch (IOException e) {
            throw new GeneralException("File with metadata for ingest workflow with external id " + ingestWorkflow.getExternalId()
                    + " is inaccessible at path: " + metadataFile.getPath(), e);
        }
        if (authorialId.isEmpty()) throw new GeneralException("Authorial id of SIP at XPath " + xPathToId +
                " in file " + pathToXmlGlobPattern + " is empty.");
        log.debug("Authorial id of SIP for ingest workflow with external id " + ingestWorkflow.getExternalId() +
                " has been successfully extracted. Authorial id is: " + authorialId + ".");

        return authorialId;
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
    public void setWorkspace(@Value("${arclib.path.workspace}") String workspace) {
        this.workspace = workspace;
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

    @Inject
    public void setIngestWorkflowService(IngestWorkflowService ingestWorkflowService) {
        this.ingestWorkflowService = ingestWorkflowService;
    }

    @Inject
    public void setTransactionTemplate(TransactionTemplate transactionTemplate) {
        this.transactionTemplate = transactionTemplate;
    }
}
