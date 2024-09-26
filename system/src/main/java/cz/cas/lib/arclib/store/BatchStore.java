package cz.cas.lib.arclib.store;

import cz.cas.lib.arclib.domain.Batch;
import cz.cas.lib.arclib.domain.Producer;
import cz.cas.lib.arclib.domain.QBatch;
import cz.cas.lib.arclib.domain.ingestWorkflow.QIngestWorkflow;
import cz.cas.lib.arclib.domain.profiles.ProducerProfile;
import cz.cas.lib.arclib.index.solr.entity.IndexedBatch;
import cz.cas.lib.core.index.solr.IndexedDatedStore;
import lombok.Getter;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
public class BatchStore extends IndexedDatedStore<Batch, QBatch, IndexedBatch> {

    public BatchStore() {
        super(Batch.class, QBatch.class, IndexedBatch.class);
    }

    @Getter
    private final String indexType = "batch";

    public Batch create(Batch b, String userId) {
        IndexedBatch solrBatch = toIndexObject(b);
        solrBatch.setUserId(userId);
        try {
            getSolrClient().addBean(getIndexCollection(), solrBatch);
            getSolrClient().commit(getIndexCollection());
        } catch (IOException | SolrServerException e) {
            throw new RuntimeException(e);
        }
        save(b);
        return b;
    }

    public Batch findWithIngestWorkflowsFilled(String batchId) {
        QIngestWorkflow qIw = QIngestWorkflow.ingestWorkflow;
        Batch batch = query()
                .select(qObject())
                .where(qObject().id.eq(batchId))
                .where(qObject().deleted.isNull())
                .leftJoin(qObject().ingestWorkflows, qIw)
                .where(qIw.deleted.isNull())
                .fetchJoin()
                .fetchOne();
        detachAll();
        return batch;
    }

    public List<Batch> findAllDeployed() {
        List<Batch> fetch = query()
                .select(qObject())
                .where(qObject().bpmDefDeployed.isTrue())
                .fetch();
        detachAll();
        return fetch;
    }

    @Override
    public Batch index(Batch obj) {
        IndexedBatch newBatch = toIndexObject(obj);
        try {
            SolrQuery q = new SolrQuery("*:*");
            q.addFilterQuery("id:" + obj.getId());
            QueryResponse queryResponse = getSolrClient().query(getIndexCollection(), q);
            List<IndexedBatch> old = queryResponse.getBeans(IndexedBatch.class);
            if (!old.isEmpty()) {
                newBatch.setUserId(old.get(0).getUserId());
            }
            getSolrClient().addBean(getIndexCollection(), newBatch);
            getSolrClient().commit(getIndexCollection());
        } catch (IOException | SolrServerException e) {
            throw new RuntimeException(e);
        }
        return obj;
    }

    @Override
    public Collection<? extends Batch> index(Collection<? extends Batch> objects) {
        if (objects.isEmpty()) {
            return objects;
        }
        List<String> objIds = objects.stream().map(Batch::getId).collect(Collectors.toList());

        try {
            SolrQuery q = new SolrQuery("*:*");
            q.addFilterQuery("id:(" + String.join(" ", objIds) + ")");
            Map<String, IndexedBatch> solrDocs = new HashMap<>();
            QueryResponse queryResponse = getSolrClient()
                    .query(getIndexCollection(), q);
            for (IndexedBatch solrBatch : queryResponse
                    .getBeans(IndexedBatch.class)) {
                solrDocs.put(solrBatch.getId(), solrBatch);
            }

            List<IndexedBatch> indexObjects = objects.stream()
                    .map(b -> {
                                IndexedBatch batch = toIndexObject(b);
                                IndexedBatch old = solrDocs.get(batch.getId());
                                if (old != null)
                                    batch.setUserId(old.getUserId());
                                return batch;
                            }
                    ).collect(Collectors.toList());
            getSolrClient().addBeans(getIndexCollection(), indexObjects);
            getSolrClient().commit(getIndexCollection());
        } catch (IOException | SolrServerException e) {
            throw new RuntimeException(e);
        }
        return objects;
    }


    @Override
    public IndexedBatch toIndexObject(Batch obj) {
        IndexedBatch indexObject = super.toIndexObject(obj);
        indexObject.setConfig(obj.getWorkflowConfig());
        if (obj.getState() != null) {
            indexObject.setState(obj.getState().name());
        }

        ProducerProfile producerProfile = obj.getProducerProfile();
        if (producerProfile != null) {
            indexObject.setProducerProfile(obj.getProducerProfile().getName());

            Producer producer = producerProfile.getProducer();
            if (producer != null) {
                indexObject.setProducerId(producer.getId());
                indexObject.setProducerName(producer.getName());
            }
        }
        indexObject.setPendingIncidents(obj.isPendingIncidents());
        if (obj.getInitialSipProfile() != null)
            indexObject.setInitialSipProfile(obj.getInitialSipProfile().getName());
        if (obj.getInitialValidationProfile() != null)
            indexObject.setInitialValidationProfile(obj.getInitialValidationProfile().getName());
        if (obj.getInitialWorkflowDefinition() != null)
            indexObject.setInitialWorkflowDefinition(obj.getInitialWorkflowDefinition().getName());
        return indexObject;
    }
}
