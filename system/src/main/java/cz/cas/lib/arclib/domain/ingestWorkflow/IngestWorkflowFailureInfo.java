package cz.cas.lib.arclib.domain.ingestWorkflow;

import cz.cas.lib.arclib.domainbase.domain.DomainObject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Getter
@Setter
@BatchSize(size = 100)
@Entity
@Table(name = "arclib_ingest_workflow_failure_info")
@NoArgsConstructor
@AllArgsConstructor
public class IngestWorkflowFailureInfo extends DomainObject {

    private String msg;
    private String stackTrace;
    @Enumerated(value = EnumType.STRING)
    private IngestWorkflowFailureType ingestWorkflowFailureType;
}
