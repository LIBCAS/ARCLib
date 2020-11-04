package cz.cas.lib.arclib.dto;

import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflowState;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
public class BatchDetailIngestWorkflowDto {
    private Instant created;
    private Instant updated;
    private String externalId;
    private String sipAuthorialId;
    private IngestWorkflowState processingState;
}
