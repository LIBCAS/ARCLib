package cz.cas.lib.arclib.store;

import com.querydsl.jpa.impl.JPAQuery;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestIssue;
import cz.cas.lib.arclib.domain.ingestWorkflow.QIngestIssue;
import cz.cas.lib.arclib.index.solr.entity.SolrIngestIssue;
import cz.cas.lib.core.index.solr.SolrDatedStore;
import cz.cas.lib.core.store.Transactional;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class IngestIssueStore extends SolrDatedStore<IngestIssue, QIngestIssue, SolrIngestIssue> {

    public IngestIssueStore() {
        super(IngestIssue.class, QIngestIssue.class, SolrIngestIssue.class);
    }

    @Override
    public SolrIngestIssue toIndexObject(IngestIssue obj) {
        SolrIngestIssue indexObject = super.toIndexObject(obj);
        indexObject.setConfigNote(obj.getConfigNote());
        indexObject.setExternalId(obj.getIngestWorkflow().getExternalId());
        indexObject.setSolvedByConfig(obj.isSolvedByConfig());
        indexObject.setIssue(obj.getIssue());
        return indexObject;
    }

    public List<IngestIssue> findByTaskExecutorAndExternalId(Class<?> taskExecutor, String externalId) {
        QIngestIssue qIngestIssue = QIngestIssue.ingestIssue;
        JPAQuery<IngestIssue> query = query(qIngestIssue)
                .select(qIngestIssue)
                .where(qIngestIssue.taskExecutor.eq(taskExecutor))
                .where(qIngestIssue.ingestWorkflow.externalId.eq(externalId))
                .where(qIngestIssue.deleted.isNull());
        List<IngestIssue> ingestIssues = query.fetch();
        detachAll();
        return ingestIssues;
    }
}
