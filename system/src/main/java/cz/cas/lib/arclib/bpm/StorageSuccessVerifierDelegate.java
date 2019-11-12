package cz.cas.lib.arclib.bpm;

import cz.cas.lib.arclib.domain.IngestToolFunction;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflow;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflowState;
import cz.cas.lib.arclib.index.IndexArclibXmlStore;
import cz.cas.lib.arclib.index.solr.arclibxml.IndexedAipState;
import cz.cas.lib.arclib.service.AipService;
import cz.cas.lib.arclib.service.archivalStorage.ArchivalStorageException;
import cz.cas.lib.arclib.service.archivalStorage.ArchivalStorageService;
import cz.cas.lib.arclib.service.archivalStorage.ArchivalStorageServiceDebug;
import cz.cas.lib.arclib.service.archivalStorage.ObjectState;
import cz.cas.lib.arclib.store.IngestWorkflowStore;
import cz.cas.lib.arclib.domainbase.exception.GeneralException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;

import static cz.cas.lib.arclib.utils.ArclibUtils.*;
import static cz.cas.lib.core.util.Utils.asList;

@Slf4j
@Service
public class StorageSuccessVerifierDelegate extends ArclibDelegate {
    private ArchivalStorageService archivalStorageService;
    private ArchivalStorageServiceDebug archivalStorageServiceDebug;
    private IngestWorkflowStore ingestWorkflowStore;
    private IndexArclibXmlStore indexArclibXmlStore;
    private Boolean deleteSipFromTransferArea;
    private AipService aipService;
    @Getter
    private String toolName = "ARCLib_" + IngestToolFunction.transfer;

    /**
     * Verifies that archival storage has succeeded to persist the SIP (update the ArclibXml).
     * <p>
     * Archival storage is asked for the state of the AIP. In case the state is:
     * a) ARCHIVED:
     * 1. ingest workflow is marked as PERSISTED and indexed arclib xml document state is set to PERSISTED
     * 2. JMS message is sent to Coordinator to inform the batch that the ingest workflow process has finished
     * 3. SIP content is deleted from workspace
     * 4. SIP content is deleted from transfer area
     * <p>
     * b) PROCESSING or PRE_PROCESSING: variable <code>aipSavedCheckRetries</code> is decremented
     * <p>
     * c) ARCHIVAL_FAILURE or ROLLED_BACK: variable <code>aipStoreRetries</code> is decremented
     * <p>
     * In case of the debugging mode set to active, an internal debugging version of archival storage is used
     * instead of the production archival storage.
     *
     * @param execution delegate execution containing BPM variables
     */
    @Override
    public void executeArclibDelegate(DelegateExecution execution) throws ArchivalStorageException, IOException {
        String ingestWorkflowExternalId = (String) execution.getVariable(BpmConstants.ProcessVariables.ingestWorkflowExternalId);
        log.debug("Execution of Storage success verifier delegate started for ingest workflow " + ingestWorkflowExternalId + ".");

        IngestWorkflow ingestWorkflow = ingestWorkflowStore.findByExternalId(ingestWorkflowExternalId);
        String aipId = ingestWorkflow.getSip().getId();
        boolean debuggingModeActive = isInDebugMode(execution);

        //check that the aip has been successfully stored
        ObjectState aipState;
        if (!debuggingModeActive) {
            aipState = archivalStorageService.getAipState(aipId);
        } else {
            aipState = archivalStorageServiceDebug.getAipState(aipId);
        }
        log.debug("State of AIP with ID " + aipId + " at archival storage has been successfully retrieved." +
                " State is: " + aipState.toString() + ".");
        execution.setVariable(BpmConstants.ArchivalStorage.aipState, aipState.toString());

        switch (aipState) {
            case ARCHIVED:
                ingestWorkflow.setProcessingState(IngestWorkflowState.PERSISTED);
                ingestWorkflow.setLatestVersion(true);

                IngestWorkflow relatedWorkflow = ingestWorkflow.getRelatedWorkflow();
                if (relatedWorkflow != null) {
                    relatedWorkflow.setLatestVersion(false);
                    ingestWorkflowStore.save(relatedWorkflow);
                }

                indexArclibXmlStore.changeAipState(ingestWorkflowExternalId, IndexedAipState.ARCHIVED);
                log.info("Index of XML of ingest workflow " + ingestWorkflowExternalId + " has been updated with the ingest workflow state PERSISTED.");

                ingestWorkflowStore.save(asList(ingestWorkflow));
                log.info("Processing of ingest workflow with external id " + ingestWorkflow.getExternalId()
                        + " has finished. The ingest workflow state changed to " + IngestWorkflowState.PERSISTED.toString() + ".");

                //delete sip from workspace
                FileSystemUtils.deleteRecursively(getIngestWorkflowWorkspacePath(ingestWorkflow.getExternalId(), workspace).toAbsolutePath().toFile());
                log.debug("Sip of ingest workflow with external id " + ingestWorkflow.getExternalId() + " has been deleted from workspace.");

                //delete sip from transfer area
                if (deleteSipFromTransferArea) {
                    Files.deleteIfExists(getSipZipTransferAreaPath(ingestWorkflow).toAbsolutePath());
                    Files.deleteIfExists(getSipSumsTransferAreaPath(getSipZipTransferAreaPath(ingestWorkflow)).toAbsolutePath());
                    log.debug("Sip of ingest workflow with external id " + ingestWorkflow.getExternalId() + " has been deleted from transfer area.");
                }
                aipService.deactivateLock(ingestWorkflow.getSip().getAuthorialPackage().getId());
                break;
            case PROCESSING:
            case PRE_PROCESSING:
                Integer aipSavedCheckRetries = (Integer) execution.getVariable(BpmConstants.ArchivalStorage.aipSavedCheckRetries);
                execution.setVariable(BpmConstants.ArchivalStorage.aipSavedCheckRetries, aipSavedCheckRetries - 1);
                log.debug("Number of remaining retries to check the AIP state is " + aipSavedCheckRetries + ".");
                break;
            case ARCHIVAL_FAILURE:
            case ROLLED_BACK:
                Integer aipStoreRetries = (Integer) execution.getVariable(BpmConstants.ArchivalStorage.aipStoreRetries);
                execution.setVariable(BpmConstants.ArchivalStorage.aipStoreRetries, aipStoreRetries - 1);
                log.debug("Number of remaining retries to store the AIP is " + aipStoreRetries + ".");
                break;
            default:
                log.error("AIP saved in the archival storage is in an unexpected state.");
                throw new GeneralException("AIP saved in the archival storage is in an unexpected state.");
        }
        log.info("Execution of Storage success verifier delegate finished for ingest workflow " + ingestWorkflowExternalId + ".");
    }

    @Inject
    public void setArchivalStorageService(ArchivalStorageService archivalStorageService) {
        this.archivalStorageService = archivalStorageService;
    }

    @Inject
    public void setArchivalStorageServiceDebug(ArchivalStorageServiceDebug archivalStorageServiceDebug) {
        this.archivalStorageServiceDebug = archivalStorageServiceDebug;
    }

    @Inject
    public void setIngestWorkflowStore(IngestWorkflowStore ingestWorkflowStore) {
        this.ingestWorkflowStore = ingestWorkflowStore;
    }

    @Inject
    public void setIndexArclibXmlStore(IndexArclibXmlStore indexArclibXmlStore) {
        this.indexArclibXmlStore = indexArclibXmlStore;
    }

    @Inject
    public void setAipService(AipService aipService) {
        this.aipService = aipService;
    }

    @Inject
    public void setDeleteSipFromTransferArea(@Value("${arclib.deleteSipFromTransferArea}") Boolean deleteSipFromTransferArea) {
        this.deleteSipFromTransferArea = deleteSipFromTransferArea;
    }
}
