package cz.cas.lib.arclib.domain.ingestWorkflow;

import cz.cas.lib.core.domain.NamedObject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * Definícia ingest workflow
 */
@Getter
@Setter
@BatchSize(size = 100)
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "arclib_workflow_definition")
public class WorkflowDefinition extends NamedObject {
    /**
     * Definícia ingest workflow vo formáte BPMN
     */
    @Column(length = 10485760)
    protected String bpmnDefinition;
}
