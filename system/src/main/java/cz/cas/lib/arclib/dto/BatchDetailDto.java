package cz.cas.lib.arclib.dto;

import cz.cas.lib.arclib.domain.BatchState;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.time.Instant;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BatchDetailDto {
    private String id;
    private ProducerProfileDto producerProfile;
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
