package cz.cas.lib.arclib.dto;

import cz.cas.lib.arclib.domain.BatchState;
import cz.cas.lib.arclib.domain.Producer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BatchDto {
    private String id;
    private Producer producer;
    private String transferAreaPath;
    private Instant created;
    private Instant updated;
    private boolean pendingIncidents;
    @Enumerated(EnumType.STRING)
    private BatchState state;
    private ProducerProfileDto producerProfile;
    private SipProfileDto initialSipProfile;
    private ValidationProfileDto initialValidationProfile;
    private WorkflowDefinitionDto initialWorkflowDefinition;
}
