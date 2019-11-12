package cz.cas.lib.arclib.store;

import com.querydsl.jpa.impl.JPAQuery;
import cz.cas.lib.arclib.domain.IngestToolFunction;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestIssue;
import cz.cas.lib.arclib.domain.ingestWorkflow.QIngestIssue;
import cz.cas.lib.arclib.index.solr.entity.IndexedIngestIssue;
import cz.cas.lib.core.index.solr.IndexedDatedStore;
import lombok.Getter;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * WARNING: do not use this store directly when saving issues - ALWAYS USE {@link cz.cas.lib.arclib.service.IngestIssueService}
 * (after saving the issue the exception is thrown so the issue must be saved in own transaction, which is done at service lvl)
 */
@Repository
public class IngestIssueStore extends IndexedDatedStore<IngestIssue, QIngestIssue, IndexedIngestIssue> {

    public IngestIssueStore() {
        super(IngestIssue.class, QIngestIssue.class, IndexedIngestIssue.class);
    }

    @Getter
    private final String indexType = "ingestIssue";
    @Override
    public IndexedIngestIssue toIndexObject(IngestIssue obj) {
        IndexedIngestIssue indexObject = super.toIndexObject(obj);
        if (obj.getFormatDefinition() != null) {
            indexObject.setFormatName(obj.getFormatDefinition().getFormat().getFormatName());
            indexObject.setFormatPuid(obj.getFormatDefinition().getFormat().getPuid());
        }
        indexObject.setToolId(obj.getTool().getId());
        indexObject.setToolName(obj.getTool().getName());
        indexObject.setIngestIssueDefinitionId(obj.getIngestIssueDefinition().getId());
        indexObject.setIngestIssueDefinitionName(obj.getIngestIssueDefinition().getName());
        indexObject.setExternalId(obj.getIngestWorkflow().getExternalId());
        indexObject.setSolvedByConfig(obj.isSuccess());
        indexObject.setIssue(obj.getDescription());
        return indexObject;
    }

    public List<IngestIssue> findByToolFunctionAndExternalId(IngestToolFunction toolFunction, String externalId) {
        QIngestIssue qIngestIssue = QIngestIssue.ingestIssue;
        JPAQuery<IngestIssue> query = query(qIngestIssue)
                .select(qIngestIssue)
                .where(qIngestIssue.tool.toolFunction.eq(toolFunction))
                .where(qIngestIssue.ingestWorkflow.externalId.eq(externalId))
                .where(qIngestIssue.deleted.isNull());
        List<IngestIssue> ingestIssues = query.fetch();
        detachAll();
        return ingestIssues;
    }
}
