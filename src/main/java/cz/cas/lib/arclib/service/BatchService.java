package cz.cas.lib.arclib.service;

import cz.cas.lib.arclib.domain.Batch;
import cz.cas.lib.arclib.index.IndexArclibXmlStore;
import cz.cas.lib.arclib.index.solr.arclibxml.IndexedArclibXmlDocumentState;
import cz.cas.lib.arclib.index.solr.arclibxml.SolrArclibXmlDocument;
import cz.cas.lib.arclib.store.BatchStore;
import cz.cas.lib.arclib.store.IngestWorkflowStore;
import cz.cas.lib.core.exception.BadArgument;
import cz.cas.lib.core.exception.MissingObject;
import cz.cas.lib.core.index.dto.Params;
import cz.cas.lib.core.index.dto.Result;
import cz.cas.lib.core.rest.data.DelegateAdapter;
import cz.cas.lib.core.store.Transactional;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Map;

import static cz.cas.lib.core.util.Utils.notNull;

@Slf4j
@Service
public class BatchService implements DelegateAdapter<Batch> {

    @Getter
    private BatchStore delegate;
    private IngestWorkflowService ingestWorkflowService;
    private IndexArclibXmlStore indexArclibXmlStore;

    @Transactional
    public Batch create(Batch b, String userId) {
        return delegate.create(b,userId);
    }

    /**
     * Gets list of batches respecting the <code>params</code>
     *
     * @param params params for filtering
     * @return result containing the list of batches
     */
    @Transactional
    public Result<Batch> getBatches(Params params) {
        return delegate.findAll(params);
    }

    /**
     * Gets batch together with the associated ingest workflows.
     *
     * @param id id of the batch
     * @return batch with the ingest workflows filled
     */
    @Transactional
    public Batch get(String id) {
        Batch batch = delegate.findWithIngestWorkflowsFilled(id);
        notNull(batch, () -> new MissingObject(Batch.class, id));
        return batch;
    }

    /**
     * Deletes batch and all its respective ingest workflows from database.
     * <p>
     * Applicable only for batches processed in the debugging mode.
     *
     * @param id id of the batch
     */
    @Transactional
    public void forget(String id) {
        Batch batch = delegate.findWithIngestWorkflowsFilled(id);
        notNull(batch, () -> new MissingObject(Batch.class, id));
        if (!batch.isDebuggingModeActive()) {
            throw new BadArgument("Cannot forget batch that has not been processed in the debugging mode.");
        }
        batch.getIngestWorkflows().forEach(ingestWorkflow -> {
            ingestWorkflowService.delete(ingestWorkflow);
            log.info("Ingest workflow with external id " + ingestWorkflow.getExternalId() + " has been deleted from database.");

            Map<String, Object> aclibXmlIndexDocument = indexArclibXmlStore.findArclibXmlIndexDocument(ingestWorkflow.getExternalId());
            String producerId = (String) ((ArrayList) aclibXmlIndexDocument.get(SolrArclibXmlDocument.PRODUCER_ID)).get(0);
            String assignee = (String) ((ArrayList) aclibXmlIndexDocument.get(SolrArclibXmlDocument.USER_ID)).get(0);
            String document = (String) ((ArrayList) aclibXmlIndexDocument.get(SolrArclibXmlDocument.DOCUMENT)).get(0);
            indexArclibXmlStore.createIndex(document, producerId, assignee, IndexedArclibXmlDocumentState.DELETED);
            log.info("Index of XML of ingest workflow " + ingestWorkflow.getExternalId() + " has been updated with " +
                    "the ingest workflow state DELETED.");

        });
        delegate.delete(batch);
        log.info("Batch ID " + id + " has been deleted from database.");
    }

    @Inject
    public void setDelegate(BatchStore delegate) {
        this.delegate = delegate;
    }

    @Inject
    public void setIngestWorkflowService(IngestWorkflowService ingestWorkflowService) {
        this.ingestWorkflowService = ingestWorkflowService;
    }

    @Inject
    public void setIndexArclibXmlStore(IndexArclibXmlStore indexArclibXmlStore) {
        this.indexArclibXmlStore = indexArclibXmlStore;
    }
}
