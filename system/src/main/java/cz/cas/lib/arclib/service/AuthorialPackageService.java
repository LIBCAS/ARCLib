package cz.cas.lib.arclib.service;

import cz.cas.lib.arclib.domain.Batch;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflow;
import cz.cas.lib.arclib.domain.packages.AuthorialPackage;
import cz.cas.lib.arclib.domain.packages.Sip;
import cz.cas.lib.arclib.domainbase.domain.DatedObject;
import cz.cas.lib.arclib.index.IndexedArclibXmlStore;
import cz.cas.lib.arclib.service.archivalStorage.ArchivalStorageServiceDebug;
import cz.cas.lib.arclib.store.AuthorialPackageStore;
import cz.cas.lib.arclib.store.SipStore;
import cz.cas.lib.core.store.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.*;

@Service
@Slf4j
public class AuthorialPackageService {

    private IngestWorkflowService ingestWorkflowService;
    private BatchService batchService;
    private IndexedArclibXmlStore indexArclibXmlStore;
    private SipStore sipStore;
    private AuthorialPackageStore authorialPackageStore;
    private ArchivalStorageServiceDebug archivalStorageServiceDebug;

    @Transactional
    public void forgetAuthorialPackage(String authorialPackageId) {
        log.info("trying to forget authorial package with database id: " + authorialPackageId);
        List<IngestWorkflow> byAuthorialPackageId = ingestWorkflowService.findByAuthorialPackageId(authorialPackageId);
        byAuthorialPackageId.sort(Comparator.comparing(DatedObject::getCreated));
        Collections.reverse(byAuthorialPackageId);
        for (IngestWorkflow ingestWorkflow : byAuthorialPackageId) {
            if (!ingestWorkflow.wasIngestedInDebugMode()) {
                throw new IllegalArgumentException("Cannot forget ingest workflow that has not been processed in the debugging mode. " + "Ingest workflow: " + ingestWorkflow.getExternalId() + " batch: " + ingestWorkflow.getBatch().getId());
            }
        }
        Set<Sip> sips = new HashSet<>();
        byAuthorialPackageId.forEach(ingestWorkflow -> {
            sips.add(ingestWorkflow.getSip());
            ingestWorkflowService.hardDelete(ingestWorkflow);
            indexArclibXmlStore.removeIndex(ingestWorkflow.getExternalId());
            log.info("Ingest workflow with external id " + ingestWorkflow.getExternalId() + " has been deleted.");
            Batch relatedBatch = batchService.get(ingestWorkflow.getBatch().getId());
            if (relatedBatch.getIngestWorkflows().isEmpty()) {
                batchService.hardDelete(relatedBatch);
                log.info("Batch " + relatedBatch.getId() + " has been deleted.");
            }
        });
        for (Sip sip : sips) {
            sipStore.hardDelete(sip);
            log.info("SIP " + sip.getId() + " has been deleted.");
        }
        AuthorialPackage authorialPackageToBeDeleted = new AuthorialPackage();
        authorialPackageToBeDeleted.setId(authorialPackageId);
        authorialPackageStore.hardDelete(authorialPackageToBeDeleted);
        try {
            for (Sip sip : sips) {
                archivalStorageServiceDebug.deleteSipAndItsXmlsFromWorkspace(sip.getId());
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
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
    public void setIndexArclibXmlStore(IndexedArclibXmlStore indexArclibXmlStore) {
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

    @Inject
    public void setArchivalStorageServiceDebug(ArchivalStorageServiceDebug archivalStorageServiceDebug) {
        this.archivalStorageServiceDebug = archivalStorageServiceDebug;
    }
}
