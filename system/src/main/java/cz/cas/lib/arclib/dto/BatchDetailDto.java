package cz.cas.lib.arclib.dto;

import cz.cas.lib.arclib.domain.BatchState;
import cz.cas.lib.arclib.domain.ingestWorkflow.WorkflowDefinition;
import cz.cas.lib.arclib.domain.profiles.ProducerProfile;
import cz.cas.lib.arclib.domain.profiles.SipProfile;
import cz.cas.lib.arclib.domain.profiles.ValidationProfile;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ManyToOne;
import java.time.Instant;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BatchDetailDto {
    private String id;
    private ProducerProfile producerProfile;
    private ValidationProfileDto initialValidationProfile;
    private SipProfileDto initialSipProfile;
    private WorkflowDefinitionDto initialWorkflowDefinition;
    private String transferAreaPath;
    private Instant created;
    private Instant updated;
    @Enumerated(EnumType.STRING)
    private BatchState state;
    private String workflowConfig;
    private String computedWorkflowConfig;
    private List<BatchDetailIngestWorkflowDto> ingestWorkflows;
}
