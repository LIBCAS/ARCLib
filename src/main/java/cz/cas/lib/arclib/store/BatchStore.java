package cz.cas.lib.arclib.store;

import cz.cas.lib.arclib.domain.Batch;
import cz.cas.lib.arclib.domain.Producer;
import cz.cas.lib.arclib.domain.QBatch;
import cz.cas.lib.arclib.domain.profiles.ProducerProfile;
import cz.cas.lib.arclib.index.solr.entity.SolrBatch;
import cz.cas.lib.core.index.solr.SolrDatedStore;
import cz.cas.lib.core.index.solr.SolrDomainObject;
import cz.cas.lib.core.store.Transactional;
import org.springframework.data.solr.core.query.Criteria;
import org.springframework.data.solr.core.query.SimpleQuery;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
@Transactional
public class BatchStore extends SolrDatedStore<Batch, QBatch, SolrBatch> {

    public BatchStore() {
        super(Batch.class, QBatch.class, SolrBatch.class);
    }

    public Batch create(Batch b, String userId) {
        SolrBatch solrBatch = toIndexObject(b);
        solrBatch.setUserId(userId);
        getTemplate().saveBean(solrBatch);
        getTemplate().commit(getIndexCore());
        save(b);
        return b;
    }

    public Batch findWithIngestWorkflowsFilled(String batchId) {
        return query()
                .select(qObject())
                .where(qObject().id.eq(batchId))
                .leftJoin(qObject().ingestWorkflows)
                .fetchJoin()
                .fetchOne();
    }

    @Override
    public Batch index(Batch obj) {
        SolrBatch newBatch = toIndexObject(obj);
        SimpleQuery q = new SimpleQuery();
        q.addCriteria(Criteria.where("id").in(obj.getId()));
        List<SolrBatch> old = getTemplate().query(getIndexCore(), q, SolrBatch.class).getContent();
        if (!old.isEmpty()) {
            newBatch.setUserId(old.get(0).getUserId());
        }
        getTemplate().saveBean(newBatch);
        getTemplate().commit(getIndexCore());
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
        Map<String, SolrBatch> solrDocs = getTemplate()
                .query(getIndexCore(), q, SolrBatch.class)
                .getContent()
                .stream()
                .collect(Collectors.toMap(SolrDomainObject::getId, b -> b));

        List<SolrBatch> indexObjects = objects.stream()
                .map(b -> {
                            SolrBatch batch = toIndexObject(b);
                            SolrBatch old = solrDocs.get(batch.getId());
                            if (old != null)
                                batch.setUserId(old.getUserId());
                            return batch;
                        }
                ).collect(Collectors.toList());
        getTemplate().saveBeans(indexObjects);
        getTemplate().commit(getIndexCore());
        return objects;
    }


    @Override
    public SolrBatch toIndexObject(Batch obj) {
        SolrBatch indexObject = super.toIndexObject(obj);
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
        return indexObject;
    }
}
