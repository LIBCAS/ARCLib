package cz.cas.lib.arclib.domain.ingestWorkflow;

import cz.cas.lib.arclib.domain.Producer;
import cz.cas.lib.arclib.domainbase.domain.NamedObject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
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
     * Dodávateľ
     */
    @ManyToOne
    private Producer producer;

    /**
     * Externé id
     */
    private String externalId;

    /**
     * Definícia ingest workflow vo formáte BPMN
     */
    @Column(length = 10485760)
    protected String bpmnDefinition;

    /**
     * Definici je možné editovat
     */
    private boolean editable;

    public WorkflowDefinition(String bpmnDefinition) {
        this.bpmnDefinition = bpmnDefinition;
    }
}
