package cz.cas.lib.arclib.store;

import cz.cas.lib.arclib.domain.ingestWorkflow.QWorkflowDefinition;
import cz.cas.lib.arclib.domain.ingestWorkflow.WorkflowDefinition;
import cz.cas.lib.core.store.NamedStore;
import org.springframework.stereotype.Repository;

@Repository
public class WorkflowDefinitionStore extends NamedStore<WorkflowDefinition, QWorkflowDefinition> {
    public WorkflowDefinitionStore() {
        super(WorkflowDefinition.class, QWorkflowDefinition.class);
    }
}
