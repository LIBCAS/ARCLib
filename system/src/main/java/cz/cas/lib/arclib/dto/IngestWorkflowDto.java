package cz.cas.lib.arclib.dto;

import cz.cas.lib.arclib.domain.Batch;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestEvent;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflow;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
public class IngestWorkflowDto {
    private IngestWorkflow ingestWorkflow;
    private Map<String, Object> processVariables;
    private List<IngestEvent> events;
    private Batch batch;
    private String transferAreaPath;

    public IngestWorkflowDto(IngestWorkflow ingestWorkflow, Map<String, Object> processVariables, List<IngestEvent> events, Batch batch) {
        this.ingestWorkflow = ingestWorkflow;
        this.processVariables = processVariables;
        this.events = events;
        this.batch = batch;
        if (batch != null && ingestWorkflow != null)
            transferAreaPath = Paths.get(batch.getTransferAreaPath(), ingestWorkflow.getFileName()).toString();
    }
}
