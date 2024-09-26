package cz.cas.lib.arclib.bpm;

import cz.cas.lib.arclib.domain.*;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflow;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflowState;
import cz.cas.lib.arclib.domain.packages.AuthorialPackage;
import cz.cas.lib.arclib.domain.packages.Sip;
import cz.cas.lib.arclib.domainbase.exception.ConflictException;
import cz.cas.lib.arclib.index.CreateIndexRecordDto;
import cz.cas.lib.arclib.index.IndexedArclibXmlStore;
import cz.cas.lib.arclib.index.SetLatestFlagsDto;
import cz.cas.lib.arclib.index.solr.arclibxml.IndexedAipState;
import cz.cas.lib.arclib.index.solr.arclibxml.IndexedArclibXmlDocument;
import cz.cas.lib.arclib.service.AipService;
import cz.cas.lib.arclib.service.DeletionRequestService;
import cz.cas.lib.arclib.service.archivalStorage.ArchivalStorageException;
import cz.cas.lib.arclib.service.archivalStorage.ArchivalStorageService;
import cz.cas.lib.arclib.service.archivalStorage.ArchivalStorageServiceDebug;
import cz.cas.lib.arclib.service.archivalStorage.ObjectState;
import cz.cas.lib.arclib.store.AuthorialPackageStore;
import cz.cas.lib.arclib.store.ProducerStore;
import cz.cas.lib.arclib.store.SipStore;
import cz.cas.lib.arclib.store.UserStore;
import cz.cas.lib.arclib.utils.ArclibUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static cz.cas.lib.arclib.domain.VersioningLevel.*;
import static cz.cas.lib.arclib.utils.ArclibUtils.*;

@Slf4j
@Service
public class StorageSuccessVerifierDelegate extends ArclibDelegate {
    private static final String DELETE_PREVIOUS_SIP_VERSION_CONFIG_PATH = "/deletePreviousSipVersion";
    private ArchivalStorageService archivalStorageService;
    private ArchivalStorageServiceDebug archivalStorageServiceDebug;
    private IndexedArclibXmlStore indexArclibXmlStore;
    private Boolean deleteSipFromTransferArea;
    private AipService aipService;
    private ProducerStore producerStore;
    private UserStore userStore;
    private AuthorialPackageStore authorialPackageStore;
    private DeletionRequestService deletionRequestService;
    private SipStore sipStore;
    @Getter
    private String toolName = "ARCLib_archival_storage_verification";

    /**
     * Verifies that archival storage has succeeded to persist the SIP (update the ArclibXml).
     * <p>
     * Archival storage is asked for the state of the AIP / AIP XML. In case the state is:
     * a) ARCHIVED:
     * 1. {@link IngestWorkflow#processingState} is set to {@link IngestWorkflowState#PERSISTED} and {@link IndexedArclibXmlDocument#aipState} is set to {@link IndexedAipState#ARCHIVED}
     * 2. JMS message is sent to Coordinator to inform the batch that the ingest workflow process has finished
     * 3. SIP content is deleted from workspace
     * 4. SIP content is deleted from transfer area
     * <p>
     * b) PROCESSING or PRE_PROCESSING: variable <code>aipSavedCheckAttempts</code> is decremented
     * <p>
     * c) ARCHIVAL_FAILURE or ROLLED_BACK: variable <code>aipStoreAttempts</code> is decremented
     * <p>
     * In case of the debugging mode set to active, an internal debugging version of archival storage is used
     * instead of the production archival storage.
     *
     * @param execution delegate execution containing BPM variables
     */
    @Override
    public void executeArclibDelegate(DelegateExecution execution) throws ArchivalStorageException, IOException {
        execution.setVariable(BpmConstants.ArchivalStorage.archivalStorageResult, null);

        String ingestWorkflowExternalId = getIngestWorkflowExternalId(execution);
        IngestWorkflow ingestWorkflow = ingestWorkflowService.findByExternalId(ingestWorkflowExternalId);
        boolean xmlVersioning = ingestWorkflow.getVersioningLevel() == ARCLIB_XML_VERSIONING;
        String aipId = ingestWorkflow.getSip().getId();
        boolean debuggingModeActive = isInDebugMode(execution);
        String objectLogId = xmlVersioning
                ? "AIP XML (version " + ingestWorkflow.getXmlVersionNumber() + ") of AIP: " + ingestWorkflow.getSip().getId()
                : "AIP: " + ingestWorkflow.getSip().getId();
        Integer remainingStateCheckRetries = (Integer) execution.getVariable(BpmConstants.ArchivalStorage.aipSavedCheckAttempts) - 1;

        //check that the aip has been successfully stored
        ObjectState stateInArchivalStorage;
        if (!debuggingModeActive) {
            try {
                stateInArchivalStorage = xmlVersioning
                        ? archivalStorageService.getXmlState(aipId, ingestWorkflow.getXmlVersionNumber())
                        : archivalStorageService.getAipState(aipId);
            } catch (ArchivalStorageException e) {
                execution.setVariable(BpmConstants.ArchivalStorage.archivalStorageResult, BpmConstants.ArchivalStorage.ArchivalStorageResultEnum.PROCESSING);
                execution.setVariable(BpmConstants.ArchivalStorage.aipSavedCheckAttempts, remainingStateCheckRetries);
                //looks like connection to the server is lost, but we assume that the package is still processing (counting seconds, not setting idle point)
                //execution.setVariable(BpmConstants.ProcessVariables.idlePoint, Instant.now());
                log.debug("Non-standard response to {} state request: {}. Number of remaining attempts to check the object state is {}.", objectLogId, e.toString(), remainingStateCheckRetries);
                return;
            }
        } else {
            stateInArchivalStorage = xmlVersioning
                    ? archivalStorageServiceDebug.getXmlState(aipId, ingestWorkflow.getXmlVersionNumber())
                    : archivalStorageServiceDebug.getAipState(aipId);
        }
        log.debug("State of " + objectLogId + " at archival storage has been successfully retrieved." +
                " State is: " + stateInArchivalStorage.toString() + ".");

        switch (stateInArchivalStorage) {
            case ARCHIVED:
                success(ingestWorkflow, debuggingModeActive, execution);
                execution.setVariable(BpmConstants.ArchivalStorage.archivalStorageResult, BpmConstants.ArchivalStorage.ArchivalStorageResultEnum.SUCCESS);
                break;
            case PROCESSING:
            case PRE_PROCESSING:
                execution.setVariable(BpmConstants.ArchivalStorage.archivalStorageResult, BpmConstants.ArchivalStorage.ArchivalStorageResultEnum.PROCESSING);
                execution.setVariable(BpmConstants.ArchivalStorage.aipSavedCheckAttempts, remainingStateCheckRetries);
                log.debug("{} is still in {} state at Archival Storage. Number of remaining attempts to check the object state is {}.", objectLogId, stateInArchivalStorage, remainingStateCheckRetries);
                return;
            case ARCHIVAL_FAILURE:
            case ROLLED_BACK:
            case ROLLBACK_FAILURE:
                Integer remainingStoreAttemptRetries = (Integer) execution.getVariable(BpmConstants.ArchivalStorage.aipStoreAttempts) - 1;
                execution.setVariable(BpmConstants.ArchivalStorage.archivalStorageResult, BpmConstants.ArchivalStorage.ArchivalStorageResultEnum.FAIL);
                execution.setVariable(BpmConstants.ArchivalStorage.aipStoreAttempts, remainingStoreAttemptRetries);
                execution.setVariable(BpmConstants.ProcessVariables.idlePoint, Instant.now().toEpochMilli());
                log.debug("Archival Storage has failed leaving the object {} at state {}. Number of remaining attempts to store the object is {}", objectLogId, stateInArchivalStorage, remainingStoreAttemptRetries);
                return;
            default:
                log.error("{} - unexpected state at the Archival Storage: {}", objectLogId, stateInArchivalStorage);
                break;
        }
    }

    private void success(IngestWorkflow ingestWorkflow, boolean debuggingModeActive, DelegateExecution execution) throws IOException, ArchivalStorageException {
        String ingestWorkflowExternalId = ingestWorkflow.getExternalId();
        Instant NOW = Instant.now();
        ingestWorkflow.setProcessingState(IngestWorkflowState.PERSISTED);
        ingestWorkflow.setLatestVersion(true);
        ingestWorkflow.setEnded(NOW);
        Long idleTimeSum = getLongVariable(execution, BpmConstants.ProcessVariables.idleTime);
        ingestWorkflow.setProcessingTime((NOW.toEpochMilli() - ingestWorkflow.getCreated().toEpochMilli() - idleTimeSum) / 1000);

        String producerId = (String) execution.getVariable(BpmConstants.ProcessVariables.producerId);
        User personResponsibleForIngest = userStore.find(getResponsiblePerson(execution));
        Producer producer = producerStore.find(producerId);
        VersioningLevel versioningLevel = ingestWorkflow.getVersioningLevel();
        Sip sip = ingestWorkflow.getSip();
        Sip previousVersionSip = sip.getPreviousVersionSip();
        boolean newWorkflowIsLatestData = versioningLevel == NO_VERSIONING || versioningLevel == SIP_PACKAGE_VERSIONING && previousVersionSip.isLatestVersion() || versioningLevel == ARCLIB_XML_VERSIONING && sip.isLatestVersion();

        //update latest flags of related entities in DB and create index DTOs
        CreateIndexRecordDto newIndexRecordDto = new CreateIndexRecordDto(Files.readAllBytes(getAipXmlWorkspacePath(ingestWorkflowExternalId, workspace)), producerId, producer.getName(), personResponsibleForIngest.getUsername(), IndexedAipState.ARCHIVED, debuggingModeActive, true, newWorkflowIsLatestData);
        Map<IngestWorkflow, SetLatestFlagsDto> relatedIndexUpdates = new HashMap<>();
        if (newWorkflowIsLatestData && !sip.isLatestVersion()) {
            sip.setLatestVersion(true);
            sipStore.save(sip);
            if (versioningLevel == SIP_PACKAGE_VERSIONING) {
                for (IngestWorkflow iwOfPreviousSip : ingestWorkflowService.findBySipId(previousVersionSip.getId()).stream().filter(iw -> iw.getProcessingState() == IngestWorkflowState.PERSISTED).collect(Collectors.toList())) {
                    relatedIndexUpdates.put(iwOfPreviousSip, new SetLatestFlagsDto(iwOfPreviousSip.getExternalId(), false, false, null));
                }
                previousVersionSip.setLatestVersion(false);
                sipStore.save(previousVersionSip);
            }
        }
        if (ingestWorkflow.getRelatedWorkflow() != null) {
            IngestWorkflow relatedWorkflow = ingestWorkflow.getRelatedWorkflow();
            relatedWorkflow.setLatestVersion(false);
            ingestWorkflowService.save(relatedWorkflow);
            if (!relatedIndexUpdates.containsKey(relatedWorkflow)) {
                relatedIndexUpdates.put(relatedWorkflow, new SetLatestFlagsDto(relatedWorkflow.getExternalId(), false, versioningLevel == ARCLIB_XML_VERSIONING && sip.isLatestVersion(), null));
            }
        }

        ingestWorkflowService.save(ingestWorkflow);
        log.info("Processing of ingest workflow with external id {} has finished. The ingest workflow state changed to {}.", ingestWorkflowExternalId, IngestWorkflowState.PERSISTED);

        AuthorialPackage authorialPackageInDb = sip.getAuthorialPackage();

        //update authorial ID
        String extractedAuthorialId = getStringVariable(execution, BpmConstants.ProcessVariables.extractedAuthorialId);
        if (!extractedAuthorialId.equals(authorialPackageInDb.getAuthorialId())) {
            log.info("changing authorial package id from: {} to: {}", authorialPackageInDb.getAuthorialId(), extractedAuthorialId);
            authorialPackageInDb.setAuthorialId(extractedAuthorialId);
            authorialPackageStore.save(authorialPackageInDb);
        }

        //remove previous SIP version if requested by config
        Pair<Boolean, String> booleanConfig = ArclibUtils.parseBooleanConfig(getConfigRoot(execution), DELETE_PREVIOUS_SIP_VERSION_CONFIG_PATH);
        Sip previousSipVersion = previousVersionSip;
        if (booleanConfig.getLeft() != null && booleanConfig.getLeft() && previousSipVersion != null) {
            try {
                deletionRequestService.createDeletionRequest(previousSipVersion.getId(), personResponsibleForIngest.getId());
            } catch (ConflictException c) {
                log.info("skipped creation of deletion request for previous AIP {} as the deletion request already exists", previousSipVersion.getId());
            }
        }

        aipService.deactivateLock(authorialPackageInDb.getId(), false);

        markChangesInIndex(ingestWorkflowExternalId, newIndexRecordDto, relatedIndexUpdates, debuggingModeActive);
        finishAtWorkspace(ingestWorkflow);
    }

    private void markChangesInIndex(String ingestWorkflowExternalId, CreateIndexRecordDto newIndexRecord,
                                    Map<IngestWorkflow, SetLatestFlagsDto> setLatestFlagsDtos,
                                    boolean debuggingModeActive) throws ArchivalStorageException, IOException {
        indexArclibXmlStore.createIndex(newIndexRecord);
        for (Map.Entry<IngestWorkflow, SetLatestFlagsDto> e : setLatestFlagsDtos.entrySet()) {
            e.getValue().setAipXml(IOUtils.toByteArray(getXmlFromStorage(debuggingModeActive, e.getKey())));
            indexArclibXmlStore.setLatestFlags(e.getValue());
            //free RAM
            e.getValue().setAipXml(null);
        }
        log.debug("ArclibXml of IngestWorkflow with external id {} has been indexed.", ingestWorkflowExternalId);
    }

    private void finishAtWorkspace(IngestWorkflow ingestWorkflow) throws IOException {
        //delete data from workspace
        String ingestWorkflowExternalId = ingestWorkflow.getExternalId();
        FileSystemUtils.deleteRecursively(getIngestWorkflowWorkspacePath(ingestWorkflowExternalId, workspace).toAbsolutePath().toFile());
        log.debug("Data of ingest workflow with external id {} has been deleted from workspace.", ingestWorkflowExternalId);

        //delete SIP from transfer area or change prefix of batch
        Batch workflowBatch = ingestWorkflow.getBatch();
        if (deleteSipFromTransferArea) {
            // automatic processing
            if (workflowBatch.getIngestRoutine() != null && workflowBatch.getIngestRoutine().isAuto()) {
                Files.deleteIfExists(getSipZipTransferAreaPathPrefixed(ingestWorkflow, AutoIngestFilePrefix.PROCESSING).toAbsolutePath());
            } else {
                Files.deleteIfExists(getSipZipTransferAreaPath(ingestWorkflow).toAbsolutePath());
            }
            Files.deleteIfExists(getSipSumsTransferAreaPath(getSipZipTransferAreaPath(ingestWorkflow)).toAbsolutePath());
            log.debug("SIP of ingest workflow with external id {} has been deleted from transfer area.", ingestWorkflowExternalId);
        } else if (workflowBatch.getIngestRoutine() != null && workflowBatch.getIngestRoutine().isAuto()) {
            // renaming to ARCHIVED_<file_name>
            log.debug(String.format("Changing prefix of file:'%s' from:'%s' to:'%s'.", ingestWorkflow.getFileName(), AutoIngestFilePrefix.PROCESSING.getPrefix(), AutoIngestFilePrefix.ARCHIVED.getPrefix()));
            changeFilePrefix(AutoIngestFilePrefix.PROCESSING, AutoIngestFilePrefix.ARCHIVED, ingestWorkflow);
        }
    }

    private InputStream getXmlFromStorage(boolean debuggingModeActive, IngestWorkflow iw) throws IOException, ArchivalStorageException {
        return debuggingModeActive
                ? archivalStorageServiceDebug.exportSingleXml(iw.getSip().getId(), iw.getXmlVersionNumber())
                : archivalStorageService.exportSingleXml(iw.getSip().getId(), iw.getXmlVersionNumber());
    }

    @Autowired
    public void setArchivalStorageService(ArchivalStorageService archivalStorageService) {
        this.archivalStorageService = archivalStorageService;
    }

    @Autowired
    public void setArchivalStorageServiceDebug(ArchivalStorageServiceDebug archivalStorageServiceDebug) {
        this.archivalStorageServiceDebug = archivalStorageServiceDebug;
    }

    @Autowired
    public void setIndexArclibXmlStore(IndexedArclibXmlStore indexArclibXmlStore) {
        this.indexArclibXmlStore = indexArclibXmlStore;
    }

    @Autowired
    public void setAipService(AipService aipService) {
        this.aipService = aipService;
    }

    @Autowired
    public void setDeleteSipFromTransferArea(@Value("${arclib.deleteSipFromTransferArea}") Boolean deleteSipFromTransferArea) {
        this.deleteSipFromTransferArea = deleteSipFromTransferArea;
    }

    @Autowired
    public void setProducerStore(ProducerStore producerStore) {
        this.producerStore = producerStore;
    }

    @Autowired
    public void setUserStore(UserStore userStore) {
        this.userStore = userStore;
    }

    @Autowired
    public void setDeletionRequestService(DeletionRequestService deletionRequestService) {
        this.deletionRequestService = deletionRequestService;
    }

    @Autowired
    public void setAuthorialPackageStore(AuthorialPackageStore authorialPackageStore) {
        this.authorialPackageStore = authorialPackageStore;
    }

    @Autowired
    public void setSipStore(SipStore sipStore) {
        this.sipStore = sipStore;
    }
}
