package cz.cas.lib.arclib.bpm;

import cz.cas.lib.arclib.domain.Hash;
import cz.cas.lib.arclib.domain.HashType;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflow;
import cz.cas.lib.arclib.index.IndexArclibXmlStore;
import cz.cas.lib.arclib.index.solr.arclibxml.SolrArclibXmlDocument;
import cz.cas.lib.arclib.service.archivalStorage.ArchivalStorageService;
import cz.cas.lib.arclib.service.archivalStorage.ArchivalStorageServiceDebug;
import cz.cas.lib.arclib.store.IngestWorkflowStore;
import cz.cas.lib.core.exception.GeneralException;
import cz.cas.lib.core.store.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

@Slf4j
@Component
public class ArchivalStorageDelegate extends ArclibDelegate implements JavaDelegate {
    private ArchivalStorageService archivalStorageService;
    private ArchivalStorageServiceDebug archivalStorageServiceDebug;
    private IngestWorkflowStore ingestWorkflowStore;
    private IndexArclibXmlStore indexArclibXmlStore;

    /**
     * Stores SIP to archival storage.
     * <p>
     * In case of the debugging mode set to active, an internal debugging version of archival storage is used
     * instead of the production archival storage.
     *
     * @param execution delegate execution storing BPM variables
     */
    @Transactional
    @Override
    public void execute(DelegateExecution execution) throws IOException {
        String ingestWorkflowExternalId = getIngestWorkflowExternalId(execution);
        log.info("Execution of Archival storage delegate started for ingest workflow " + ingestWorkflowExternalId + ".");
        IngestWorkflow ingestWorkflow = ingestWorkflowStore.findByExternalId(ingestWorkflowExternalId);

        Map<String, Object> indexedFields = indexArclibXmlStore.findArclibXmlIndexDocument(ingestWorkflow.getExternalId());

        String arclibXml = (String) ((ArrayList) indexedFields.get(SolrArclibXmlDocument.DOCUMENT)).get(0);

        String sipHashValue = (String) execution.getVariable(BpmConstants.MessageDigestCalculation.checksumSha512);
        Hash sipHash = new Hash(sipHashValue, HashType.Sha512);
        String sipId = (String) execution.getVariable(BpmConstants.ProcessVariables.sipId);
        Boolean debuggingModeActive = (Boolean) execution.getVariable(BpmConstants.ProcessVariables.debuggingModeActive);

        try (FileInputStream sip = new FileInputStream(getSipZipPath(execution).toFile());
             ByteArrayInputStream xml = new ByteArrayInputStream(arclibXml.getBytes())) {
            switch (ingestWorkflow.getVersioningLevel()) {
                case NO_VERSIONING:
                case SIP_PACKAGE_VERSIONING:
                    if (!debuggingModeActive) {
                        ResponseEntity<String> response = archivalStorageService.storeAip(sipId, sip, xml, sipHash,
                                ingestWorkflow.getArclibXmlHash());
                        if (!response.getStatusCode().is2xxSuccessful())
                            throw new GeneralException("Storing of SIP " + sipId + " to archival storage failed." +
                                    "Error code: " + response.getStatusCode() + ", reason: " + response.getBody());
                    } else {
                        archivalStorageServiceDebug.storeAip(sipId, sip, xml);
                    }
                    log.info("SIP " + sipId + " has been stored to archival storage.");
                    break;
                case ARCLIB_XML_VERSIONING:
                    Integer xmlVersionNumber = ingestWorkflow.getXmlVersionNumber();
                    if (!debuggingModeActive) {
                        ResponseEntity<String> response = archivalStorageService.updateXml(sipId, xml,
                                ingestWorkflow.getArclibXmlHash(), xmlVersionNumber, false);
                        if (!response.getStatusCode().is2xxSuccessful())
                            throw new GeneralException("Failed storing of version number " + xmlVersionNumber +
                                    " of ArclibXml of SIP " + sipId + " to archival storage. " + "Reason: " + response.getBody());
                    } else {
                        archivalStorageServiceDebug.updateXml(sipId, xml, xmlVersionNumber);
                    }
                    log.info("Version number " + xmlVersionNumber + " of ArclibXml of SIP " + sipId + " has been stored to archival storage.");
                    break;
                default:
                    throw new GeneralException("Unknown type of ingest workflow versioning level.");
            }
        }
        log.info("Execution of Archival storage delegate finished for ingest workflow " + ingestWorkflowExternalId + ".");
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
}
