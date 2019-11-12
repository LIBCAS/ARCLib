package cz.cas.lib.arclib.domain.ingestWorkflow;

import lombok.Getter;

@Getter
public enum IngestWorkflowState {
    NEW,
    PROCESSING,
    PROCESSED,
    FAILED,
    PERSISTED
}
