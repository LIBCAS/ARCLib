package cz.cas.lib.arclib.store;

import cz.cas.lib.arclib.domain.ingestWorkflow.QWorkflowDefinition;
import cz.cas.lib.arclib.domain.ingestWorkflow.WorkflowDefinition;
import cz.cas.lib.arclib.domainbase.store.NamedStore;
import cz.cas.lib.core.sequence.Generator;
import lombok.Getter;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;


import jakarta.transaction.Transactional;
import java.util.List;

@Repository
public class WorkflowDefinitionStore extends NamedStore<WorkflowDefinition, QWorkflowDefinition> {

    @Getter
    private final String SEQUENCE_ID = "71b31bd6-01ce-47a0-ae1d-5fe2277d4ebb";

    private Generator generator;

    public WorkflowDefinitionStore() {
        super(WorkflowDefinition.class, QWorkflowDefinition.class);
    }

    @Override
    public WorkflowDefinition save(WorkflowDefinition entity) {
        if (entity.getExternalId() == null) {
            entity.setExternalId(generator.generate(SEQUENCE_ID));
        }
        return super.save(entity);
    }

    public WorkflowDefinition findByExternalId(@NonNull String number) {
        WorkflowDefinition entity = query().select(qObject()).where(qObject().externalId.eq(number)).fetchOne();
        detachAll();
        return entity;
    }

    @Transactional
    public List<WorkflowDefinition> findByProducerId(String producerId) {
        QWorkflowDefinition workflowDefinition = qObject();

        List<WorkflowDefinition> fetch = query()
                .select(workflowDefinition)
                .where(workflowDefinition.producer.id.eq(producerId))
                .where(workflowDefinition.deleted.isNull())
                .fetch();

        detachAll();
        return fetch;
    }

    @Autowired
    public void setGenerator(Generator generator) {
        this.generator = generator;
    }
}
