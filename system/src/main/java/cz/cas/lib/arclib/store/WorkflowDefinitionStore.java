package cz.cas.lib.arclib.store;

import cz.cas.lib.arclib.domain.ingestWorkflow.QWorkflowDefinition;
import cz.cas.lib.arclib.domain.ingestWorkflow.WorkflowDefinition;
import cz.cas.lib.arclib.domainbase.store.NamedStore;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.util.List;

@Repository
public class WorkflowDefinitionStore extends NamedStore<WorkflowDefinition, QWorkflowDefinition> {
    public WorkflowDefinitionStore() {
        super(WorkflowDefinition.class, QWorkflowDefinition.class);
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

}
