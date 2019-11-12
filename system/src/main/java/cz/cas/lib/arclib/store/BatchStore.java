package cz.cas.lib.arclib.store;

import cz.cas.lib.arclib.domain.Batch;
import cz.cas.lib.arclib.domain.Producer;
import cz.cas.lib.arclib.domain.QBatch;
import cz.cas.lib.arclib.domain.ingestWorkflow.QIngestWorkflow;
import cz.cas.lib.arclib.domain.profiles.ProducerProfile;
import cz.cas.lib.arclib.index.solr.entity.IndexedBatch;
import cz.cas.lib.core.index.solr.IndexedDatedStore;
import cz.cas.lib.core.store.Transactional;
import lombok.Getter;
import org.springframework.data.solr.core.query.Criteria;
import org.springframework.data.solr.core.query.SimpleQuery;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
@Transactional
public class BatchStore extends IndexedDatedStore<Batch, QBatch, IndexedBatch> {

    public BatchStore() {
        super(Batch.class, QBatch.class, IndexedBatch.class);
    }

    @Getter
    private final String indexType = "batch";

    public Batch create(Batch b, String userId) {
        IndexedBatch solrBatch = toIndexObject(b);
        solrBatch.setUserId(userId);
        getTemplate().saveBean(getIndexCollection(), solrBatch);
        getTemplate().commit(getIndexCollection());
        save(b);
        return b;
    }

    public Batch findWithIngestWorkflowsFilled(String batchId) {
        QIngestWorkflow qIw = QIngestWorkflow.ingestWorkflow;
        return query()
                .select(qObject())
                .where(qObject().id.eq(batchId))
                .where(qObject().deleted.isNull())
                .leftJoin(qObject().ingestWorkflows, qIw)
                .where(qIw.deleted.isNull())
                .fetchJoin()
                .fetchOne();
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
        SimpleQuery q = new SimpleQuery();
        q.addCriteria(Criteria.where("id").in(obj.getId()));
        List<IndexedBatch> old = getTemplate().query(getIndexCollection(), q, IndexedBatch.class).getContent();
        if (!old.isEmpty()) {
            newBatch.setUserId(old.get(0).getUserId());
        }
        getTemplate().saveBean(getIndexCollection(), newBatch);
        getTemplate().commit(getIndexCollection());
        return obj;
    }

    @Override
    public Collection<? extends Batch> index(Collection<? extends Batch> objects) {
        if (objects.isEmpty()) {
            return objects;
        }
        List<String> objIds = objects.stream().map(Batch::getId).collect(Collectors.toList());
        SimpleQuery q = new SimpleQuery();
        q.addCriteria(Criteria.where("id").in(objIds));
        Map<String, IndexedBatch> solrDocs = new HashMap<>();
        for (IndexedBatch solrBatch : getTemplate()
                .query(getIndexCollection(), q, IndexedBatch.class)
                .getContent()) {
            solrDocs.put(solrBatch.getId(),solrBatch);
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
        getTemplate().saveBeans(getIndexCollection(), indexObjects);
        getTemplate().commit(getIndexCollection());
        return objects;
    }


    @Override
    public IndexedBatch toIndexObject(Batch obj) {
        IndexedBatch indexObject = super.toIndexObject(obj);
        indexObject.setConfig(obj.getWorkflowConfig());
        indexObject.setState(obj.getState());

        ProducerProfile producerProfile = obj.getProducerProfile();
        if (producerProfile != null) {
            indexObject.setProducerProfileId(obj.getProducerProfile().getId());
            indexObject.setProducerProfileName(obj.getProducerProfile().getName());

            Producer producer = producerProfile.getProducer();
            if (producer != null) {
                indexObject.setProducerId(producer.getId());
                indexObject.setProducerName(producer.getName());
            }
        }
        indexObject.setPendingIncidents(obj.isPendingIncidents());
        return indexObject;
    }
}
