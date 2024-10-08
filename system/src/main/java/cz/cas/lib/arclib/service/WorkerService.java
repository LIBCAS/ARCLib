package cz.cas.lib.arclib.service;

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
import cz.cas.lib.arclib.domainbase.exception.ConflictException;
import cz.cas.lib.arclib.domainbase.exception.GeneralException;
import cz.cas.lib.arclib.domainbase.exception.MissingObject;
import cz.cas.lib.arclib.dto.IncidentInfoDto;
import cz.cas.lib.arclib.dto.JmsDto;
import cz.cas.lib.arclib.dto.SipCopyInWorkspaceDto;
import cz.cas.lib.arclib.exception.AuthorialPackageLockedException;
import cz.cas.lib.arclib.exception.InvalidChecksumException;
import cz.cas.lib.arclib.service.fixity.FixityCounterFacade;
import cz.cas.lib.arclib.store.AuthorialPackageStore;
import cz.cas.lib.arclib.store.SipStore;
import cz.cas.lib.arclib.utils.XmlUtils;
import cz.cas.lib.arclib.utils.ZipUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.camunda.bpm.engine.ManagementService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.Incident;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
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
    private IngestErrorHandler ingestErrorHandler;
    private AipService aipService;
    private TransactionTemplate transactionTemplate;
    private ExportInfoFileService exportInfoFileService;
    private FixityCounterFacade fixityCounterFacade;

    private int aipSavedCheckAttempts;
    private String aipSavedCheckAttemptsInterval;
    private int aipStoreAttempts;
    private String aipStoreAttemptsInterval;

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
        notNull(ingestWorkflow, () -> new MissingObject(IngestWorkflow.class, externalId));

        try {
            Batch batch = ingestWorkflow.getBatch();
            String batchId = batch.getId();

            log.debug("Message received at Worker. Batch id: " + batchId + ", ingest workflow external id: " + externalId);

            //workaround to initialize batch because of the lazy initialization of batch entity
            batch = batchService.get(batchId);
            notNull(batch, () -> new MissingObject(Batch.class, batchId));

            if (batch.getState() != BatchState.PROCESSING) {
                log.warn("Cannot process ingest workflow " + externalId + " because the batch " + batchId + " is in state " + batch.getState().toString() + ".");
                return;
            }

            if (tooManyFailedIngestWorkflows(batch)) {
                log.error("Processing of batch " + batchId + " stopped because of too many ingest workflow failures.");
                template.convertAndSend("cancel", new JmsDto(batchId, dto.getUserId()));
                return;
            }

            transactionTemplate.execute(t -> {
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
        log.debug("Start of processing of ingest workflow:" + ingestWorkflow.getId() + " of user:" + userId);
        Batch batch = ingestWorkflow.getBatch();

        ProducerProfile producerProfile = batch.getProducerProfile();
        notNull(producerProfile, () -> new IllegalArgumentException("null producer profile of batch ID " + batch.getId()));
        SipProfile sipProfile = producerProfile.getSipProfile();
        notNull(sipProfile, () -> new IllegalArgumentException("null sip profile of producer profile with external id " + producerProfile.getExternalId()));

        Path pathToSip = ingestWorkflow.getBatch().getIngestRoutine() != null && ingestWorkflow.getBatch().getIngestRoutine().isAuto()
                ? getSipZipTransferAreaPathPrefixed(ingestWorkflow, AutoIngestFilePrefix.PROCESSING).toAbsolutePath()
                : getSipZipTransferAreaPath(ingestWorkflow).toAbsolutePath();
        verifyHash(pathToSip, ingestWorkflow);
        SipCopyInWorkspaceDto copiedSipMetadata = copySipToWorkspace(ingestWorkflow);

        Path sipFolderWorkspacePath = getIngestWorkflowWorkspacePath(ingestWorkflow.getExternalId(), workspace)
                .resolve(copiedSipMetadata.getRootFolderName());

        String providedAuthorialPackageUuid = copiedSipMetadata.getExportInfo().get(ExportInfoFileService.KEY_AUTHORIAL_PACKAGE_UUID);
        String extractedAuthorialId;
        if (providedAuthorialPackageUuid != null) {
            AuthorialPackage existingAuthorialPackage = authorialPackageStore.findByUuidAndProducerId(providedAuthorialPackageUuid, producerProfile.getProducer().getId());
            if (existingAuthorialPackage == null) {
                throw new MissingObject(AuthorialPackage.class, providedAuthorialPackageUuid);
            }
            extractedAuthorialId = extractAuthorialId(ingestWorkflow, sipFolderWorkspacePath, producerProfile.getSipProfile(), false);
            if (extractedAuthorialId == null) {
                extractedAuthorialId = existingAuthorialPackage.getAuthorialId();
            } else {
                AuthorialPackage authorialPackageWithExtractedId = authorialPackageStore.findByAuthorialIdAndProducerId(extractedAuthorialId, producerProfile.getProducer().getId());
                if (authorialPackageWithExtractedId != null && !authorialPackageWithExtractedId.equals(existingAuthorialPackage)) {
                    throw new ConflictException("tried to link incoming SIP with authorial package: " + providedAuthorialPackageUuid +
                            "while updating its authorial ID to the extracted ID: " + extractedAuthorialId + " but there already is another" +
                            "authorial package with that authorial ID: " + authorialPackageWithExtractedId.getId());
                }
            }
            createSipAndAuthorialPackages(ingestWorkflow, userId, existingAuthorialPackage.getAuthorialId(), false);
        } else {
            extractedAuthorialId = extractAuthorialId(ingestWorkflow, sipFolderWorkspacePath, producerProfile.getSipProfile(), true);
            createSipAndAuthorialPackages(ingestWorkflow, userId, extractedAuthorialId, true);
        }

        Map<String, Object> initVars = new HashMap<>();
        initVars.put(ProcessVariables.sipFileName, ingestWorkflow.getFileName());
        initVars.put(ProcessVariables.idlePoint, Instant.now().toEpochMilli());
        initVars.put(ProcessVariables.idleTime, 0);
        initVars.put(ProcessVariables.extractedAuthorialId, extractedAuthorialId);
        initVars.put(ProcessVariables.randomPriority, new Random().nextInt(100));
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
        initVars.put(ArchivalStorage.aipSavedCheckAttempts, aipSavedCheckAttempts);
        initVars.put(ArchivalStorage.aipSavedCheckAttemptsInterval, aipSavedCheckAttemptsInterval);
        initVars.put(ArchivalStorage.aipStoreAttempts, aipStoreAttempts);
        initVars.put(ArchivalStorage.aipStoreAttemptsInterval, aipStoreAttemptsInterval);
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
     * a) The xml versioning is performed if: <i>xmlVersioningAllowed=true</i> AND the SIP with the highest version number from the SIPs belonging
     * the authorial package has the same checksum as the checksum of the incoming SIP.
     * <p>
     * b) The sip versioning is performed if: <i>xmlVersioningAllowed=false</i> OR the SIP with the highest version number from the SIPs belonging
     * the authorial package has a different checksum as the checksum of the incoming SIP.
     *
     * @param ingestWorkflow       ingest workflow
     * @param userId               id of related user
     * @param linkingAuthorialId   new authorial ID extracted from SIP metadata or existing authorial id of related package (extracted from SIP metadata or provided)
     * @param xmlVersioningAllowed flag indicating whether XML versioning is allowed
     * @throws AuthorialPackageLockedException if the authorial package is locked
     */
    private void createSipAndAuthorialPackages(IngestWorkflow ingestWorkflow, String userId, String linkingAuthorialId, boolean xmlVersioningAllowed) throws IOException {
        ProducerProfile producerProfile = ingestWorkflow.getBatch().getProducerProfile();

        AuthorialPackage authorialPackage = authorialPackageStore.findByAuthorialIdAndProducerId(linkingAuthorialId, producerProfile.getProducer().getId());
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

                if (xmlVersioningAllowed && highestVersionNumberPersistedSip.get().getHashes().stream()
                        .anyMatch(hash -> hash.getHashValue().equals(ingestWorkflow.getHash().getHashValue()))) {
                    xmlVersioning(ingestWorkflow, highestVersionNumberPersistedSip.get(), highestVersionNumberIngestWorkflow.get());
                } else {
                    sipVersioning(ingestWorkflow, authorialPackage, highestVersionNumberPersistedSip.get());
                }
            } else {
                noVersioning(authorialPackage, ingestWorkflow);
            }
        } else {
            authorialPackage = new AuthorialPackage();
            authorialPackage.setAuthorialId(linkingAuthorialId);
            authorialPackage.setProducerProfile(producerProfile);
            authorialPackageStore.save(authorialPackage);
            log.info("New authorial package with id " + authorialPackage.getId() + " created.");

            aipService.activateLock(authorialPackage.getId(), false, userId);
            noVersioning(authorialPackage, ingestWorkflow);
        }
    }

    private void noVersioning(AuthorialPackage authorialPackage, IngestWorkflow ingestWorkflow) {
        Sip sip = new Sip();
        sip.setHashes(asSet(ingestWorkflow.getHash()));
        sip.setAuthorialPackage(authorialPackage);
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

    private void sipVersioning(IngestWorkflow ingestWorkflow,
                               AuthorialPackage authorialPackage, Sip highestVersionNumberSip) {
        log.debug("SIP versioning is performed for ingest worfklow with external id " + ingestWorkflow.getExternalId() + ".");
        Sip sip = new Sip();
        sip.setHashes(asSet(ingestWorkflow.getHash()));
        sip.setVersionNumber(highestVersionNumberSip.getVersionNumber() + 1);
        sip.setAuthorialPackage(authorialPackage);
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
            switch (expectedHash.getHashType()) {
                case MD5:
                case Crc32:
                case Sha512:
                    computedHash = bytesToHexString(fixityCounterFacade.computeDigest(expectedHash.getHashType(), sipContent));
                    break;
                default:
                    throw new GeneralException("unexpected type of expectedHash");
            }
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
     */
    private SipCopyInWorkspaceDto copySipToWorkspace(IngestWorkflow ingestWorkflow) throws IOException {
        Path sourceSipFilePath = ingestWorkflow.getBatch().getIngestRoutine() != null && ingestWorkflow.getBatch().getIngestRoutine().isAuto()
                ? getSipZipTransferAreaPathPrefixed(ingestWorkflow, AutoIngestFilePrefix.PROCESSING)
                : getSipZipTransferAreaPath(ingestWorkflow);

        Path destinationIngestWorkflowPath = getIngestWorkflowWorkspacePath(ingestWorkflow.getExternalId(), workspace);
        Path destinationSipZipPath = getSipZipWorkspacePath(ingestWorkflow.getExternalId(), workspace, ingestWorkflow.getFileName());

        try {
            Files.createDirectories(destinationIngestWorkflowPath);
            log.debug("Created directory at workspace for ingest workflow external id " + ingestWorkflow.getExternalId() + ".");
        } catch (IOException e) {
            throw new GeneralException("Unable to create directory in workspace " + workspace + " for ingest workflow with external id : " + ingestWorkflow.getExternalId(), e);
        }

        //copy the zip content to workspace
        try (FileInputStream sipContent = new FileInputStream(sourceSipFilePath.toAbsolutePath().toString())) {
            Files.copy(sipContent, destinationSipZipPath);
            verifyHash(getSipZipWorkspacePath(ingestWorkflow.getExternalId(), workspace,
                    ingestWorkflow.getFileName()), ingestWorkflow);
            log.debug("Zip archive with SIP content for ingest workflow external id " + ingestWorkflow.getExternalId() + " has been copied to workspace at path " + destinationSipZipPath + ".");
        } catch (IOException e) {
            throw new GeneralException("Unable to find SIP at path: " + sourceSipFilePath.toAbsolutePath().toString() + " or access the destination path: " + destinationSipZipPath.toAbsolutePath().toString(), e);
        }

        //unzip the zip content in workspace
        String rootFolderName = ZipUtils.unzipSip(destinationSipZipPath, destinationIngestWorkflowPath, ingestWorkflow.getExternalId());

        Map<String, String> parsedInfoFile = new HashMap<>();
        Path arclibExportInfoFile = destinationIngestWorkflowPath.resolve(rootFolderName).resolve(ExportInfoFileService.EXPORT_INFO_FILE_NAME);
        if (arclibExportInfoFile.toFile().isFile()) {
            parsedInfoFile = exportInfoFileService.parse(arclibExportInfoFile);
            arclibExportInfoFile.toFile().delete();
        }

        return new SipCopyInWorkspaceDto(rootFolderName, parsedInfoFile);
    }

    /**
     * Extracts authorial id from SIP
     *
     * @param ingestWorkflow         ingest workflow package with the SIP package
     * @param sipFolderWorkspacePath path to the folder with the SIP content in workspace
     * @param profile                SIP profile storing the path to authorial id
     * @param resultRequired         if false then it is acceptable that no authorial ID is found, if true it must be found or exception is thrown
     * @return authorial id
     * @throws GeneralException file storing the authorial id is inaccessible
     */
    private String extractAuthorialId(IngestWorkflow ingestWorkflow, Path sipFolderWorkspacePath, SipProfile profile, boolean resultRequired) throws IOException {
        PathToSipId pathToSipId = profile.getPathToSipId();
        if (pathToSipId == null && !resultRequired) {
            return null;
        }
        notNull(pathToSipId, () -> new IllegalArgumentException("null path to sip id of sip profile with id " + profile.getId()));

        String pathToXmlRegex = pathToSipId.getPathToXmlRegex();
        if (pathToXmlRegex == null && !resultRequired) {
            return null;
        }
        notNull(pathToXmlRegex, () -> new IllegalArgumentException("null pathToXmlRegex in path to authorial id"));

        String xPathToId = pathToSipId.getXPathToId();
        if (xPathToId == null && !resultRequired) {
            return null;
        }
        notNull(xPathToId, () -> new IllegalArgumentException("null path to id in path to authorial id"));

        List<File> matchingFiles = listFilesMatchingRegex(new File(sipFolderWorkspacePath.toAbsolutePath().toString()), pathToXmlRegex, true);
        if (matchingFiles.size() == 0 && !resultRequired) {
            return null;
        }
        if (matchingFiles.size() == 0)
            throw new GeneralException(String.format("File with metadata for ingest workflow with external id %s does not exist at path given by regex: %s", ingestWorkflow.getExternalId(), pathToXmlRegex));

        if (matchingFiles.size() > 1)
            throw new GeneralException(String.format("Multiple files found at the path given by regex: %s", pathToXmlRegex));

        File metadataFile = matchingFiles.get(0);
        String authorialId;
        try (FileInputStream fis = new FileInputStream(metadataFile)) {
            authorialId = XmlUtils.findSingleNodeWithXPath(fis, xPathToId, null).getTextContent();
        } catch (SAXException | ParserConfigurationException e) {
            throw new GeneralException(String.format("Error in XPath expression to authorial id: %s", xPathToId), e);
        } catch (IOException e) {
            throw new GeneralException(String.format("File with metadata for ingest workflow with external id %s is inaccessible at path: %s", ingestWorkflow.getExternalId(), metadataFile.getPath()), e);
        }
        if (authorialId.isEmpty())
            throw new GeneralException(String.format("Authorial id of SIP at XPath %s in file %s is empty.", xPathToId, pathToXmlRegex));
        log.debug(String.format("Authorial id of SIP for ingest workflow with external id %s has been successfully extracted. Authorial id is: %s.", ingestWorkflow.getExternalId(), authorialId));

        return authorialId;
    }

    @Autowired
    public void setManagementService(ManagementService managementService) {
        this.managementService = managementService;
    }

    @Autowired
    public void setBatchService(BatchService batchService) {
        this.batchService = batchService;
    }

    @Autowired
    public void setTemplate(JmsTemplate template) {
        this.template = template;
    }

    @Autowired
    public void setRuntimeService(RuntimeService runtimeService) {
        this.runtimeService = runtimeService;
    }

    @Autowired
    public void setAuthorialPackageStore(AuthorialPackageStore authorialPackageStore) {
        this.authorialPackageStore = authorialPackageStore;
    }

    @Autowired
    public void setSipStore(SipStore sipStore) {
        this.sipStore = sipStore;
    }

    @Autowired
    public void setIngestErrorHandler(IngestErrorHandler ingestErrorHandler) {
        this.ingestErrorHandler = ingestErrorHandler;
    }


    @Autowired
    public void setAipService(AipService aipService) {
        this.aipService = aipService;
    }

    @Autowired
    public void setWorkspace(@Value("${arclib.path.workspace}") String workspace) {
        this.workspace = workspace;
    }

    @Autowired
    public void setaipSavedCheckAttempts(@Value("${arclib.aipSavedCheckAttempts}")
                                                 int aipSavedCheckAttempts) {
        this.aipSavedCheckAttempts = aipSavedCheckAttempts;
    }

    @Autowired
    public void setaipStoreAttempts(@Value("${arclib.aipStoreAttempts}") int aipStoreAttempts) {
        this.aipStoreAttempts = aipStoreAttempts;
    }

    @Autowired
    public void setaipStoreAttemptsInterval(@Value("${arclib.aipStoreAttemptsInterval}") String aipStoreAttemptsInterval) {
        this.aipStoreAttemptsInterval = aipStoreAttemptsInterval;
    }

    @Autowired
    public void setaipSavedCheckAttemptsInterval(@Value("${arclib.aipSavedCheckAttemptsInterval}") String aipSavedCheckAttemptsInterval) {
        this.aipSavedCheckAttemptsInterval = aipSavedCheckAttemptsInterval;
    }

    @Autowired
    public void setIngestWorkflowService(IngestWorkflowService ingestWorkflowService) {
        this.ingestWorkflowService = ingestWorkflowService;
    }

    @Autowired
    public void setTransactionTemplate(TransactionTemplate transactionTemplate) {
        this.transactionTemplate = transactionTemplate;
    }

    @Autowired
    public void setExportInfoFileService(ExportInfoFileService exportInfoFileService) {
        this.exportInfoFileService = exportInfoFileService;
    }

    @Autowired
    public void setFixityCounterFacade(FixityCounterFacade fixityCounterFacade) {
        this.fixityCounterFacade = fixityCounterFacade;
    }
}
