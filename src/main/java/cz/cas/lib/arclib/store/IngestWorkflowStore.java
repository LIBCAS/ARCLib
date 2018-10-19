package cz.cas.lib.arclib.store;

import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflow;
import cz.cas.lib.arclib.domain.ingestWorkflow.QIngestWorkflow;
import cz.cas.lib.core.sequence.Generator;
import cz.cas.lib.core.store.DatedStore;
import lombok.Getter;
import org.springframework.stereotype.Repository;

import javax.inject.Inject;
import java.util.List;

@Repository
public class IngestWorkflowStore extends DatedStore<IngestWorkflow, QIngestWorkflow> {
    public IngestWorkflowStore() {
        super(IngestWorkflow.class, QIngestWorkflow.class);
    }

    @Getter
    private static final String SEQUENCE_ID = "16ea6aab-ff6f-46d3-84a3-5afd6db216b3";
    @Getter
    private static final String EXTERNAL_ID_PREFIX = "ARCLIB_";
    @Getter
    private static final int LEADING_ZEROS_NUMBER = 9;

    private Generator generator;

    @Override
    public IngestWorkflow save(IngestWorkflow entity) {
        if (entity.getExternalId() == null) {
            entity.setExternalId(generator.generate(SEQUENCE_ID, EXTERNAL_ID_PREFIX, LEADING_ZEROS_NUMBER));
        }
        return super.save(entity);
    }

    public IngestWorkflow findByExternalId(String externalId) {
        QIngestWorkflow ingestWorkflow = qObject();

        IngestWorkflow ingestWorkflowFound = query()
                .select(ingestWorkflow)
                .where(ingestWorkflow.externalId.eq(externalId))
                .fetchFirst();

        detachAll();
        return ingestWorkflowFound;
    }

    public List<IngestWorkflow> findByAuthorialPackageId(String authorialPackageId) {
        QIngestWorkflow ingestWorkflow = qObject();

        List<IngestWorkflow> ingestWorkflowsFound = query()
                .select(ingestWorkflow)
                .where(ingestWorkflow.sip.authorialPackage.id.eq(authorialPackageId))
                .fetch();

        detachAll();
        return ingestWorkflowsFound;
    }

    @Inject
    public void setGenerator(Generator generator) {
        this.generator = generator;
    }
}
