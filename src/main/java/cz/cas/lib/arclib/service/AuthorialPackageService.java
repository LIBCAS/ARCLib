package cz.cas.lib.arclib.service;

import cz.cas.lib.arclib.domain.Batch;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflow;
import cz.cas.lib.arclib.domain.packages.AuthorialPackage;
import cz.cas.lib.arclib.domain.packages.Sip;
import cz.cas.lib.arclib.index.IndexArclibXmlStore;
import cz.cas.lib.arclib.store.AuthorialPackageStore;
import cz.cas.lib.arclib.store.SipStore;
import cz.cas.lib.core.store.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class AuthorialPackageService {

    private IngestWorkflowService ingestWorkflowService;
    private BatchService batchService;
    private IndexArclibXmlStore indexArclibXmlStore;
    private SipStore sipStore;
    private AuthorialPackageStore authorialPackageStore;

    @Transactional
    public void forgetAuthorialPackage(String authorialPackageId) {
        log.info("trying to forget authorial package with database id: " + authorialPackageId);
        List<IngestWorkflow> byAuthorialPackageId = ingestWorkflowService.findByAuthorialPackageId(authorialPackageId);
        Map<Sip, List<IngestWorkflow>> sipWorkflowMap = new HashMap<>();
        for (IngestWorkflow ingestWorkflow : byAuthorialPackageId) {
            if (!ingestWorkflow.getBatch().isDebuggingModeActive()) {
                throw new IllegalArgumentException("Cannot forget ingest workflow that has not been processed in the debugging mode. " + "Ingest workflow: " + ingestWorkflow.getExternalId() + " batch: " + ingestWorkflow.getBatch().getId());
            }
            List<IngestWorkflow> ingestWorkflows = sipWorkflowMap.computeIfAbsent(ingestWorkflow.getSip(), k -> new ArrayList<>());
            ingestWorkflows.add(ingestWorkflow);
        }

        for (Sip sip : sipWorkflowMap.keySet()) {
            for (IngestWorkflow ingestWorkflow : sipWorkflowMap.get(sip)) {
                Batch batch = batchService.get(ingestWorkflow.getBatch().getId());
                ingestWorkflowService.hardDelete(ingestWorkflow);
                indexArclibXmlStore.removeIndex(ingestWorkflow.getExternalId());
                log.info("Ingest workflow with external id " + ingestWorkflow.getExternalId() + " has been deleted.");

                if (batch.getIngestWorkflows().size() == 1) {
                    batchService.hardDelete(batch);
                    log.info("Batch " + batch.getId() + " has been deleted.");
                }
            }
            sipStore.hardDelete(sip);
            log.info("SIP " + sip.getId() + " has been deleted.");
        }
        AuthorialPackage authorialPackageToBeDeleted = new AuthorialPackage();
        authorialPackageToBeDeleted.setId(authorialPackageId);
        authorialPackageStore.hardDelete(authorialPackageToBeDeleted);
        log.info("authorial package with database id: " + authorialPackageId + " successfully forgot");
    }

    @Inject
    public void setIngestWorkflowService(IngestWorkflowService ingestWorkflowService) {
        this.ingestWorkflowService = ingestWorkflowService;
    }

    @Inject
    public void setBatchService(BatchService batchService) {
        this.batchService = batchService;
    }

    @Inject
    public void setIndexArclibXmlStore(IndexArclibXmlStore indexArclibXmlStore) {
        this.indexArclibXmlStore = indexArclibXmlStore;
    }

    @Inject
    public void setSipStore(SipStore sipStore) {
        this.sipStore = sipStore;
    }

    @Inject
    public void setAuthorialPackageStore(AuthorialPackageStore authorialPackageStore) {
        this.authorialPackageStore = authorialPackageStore;
    }
}
