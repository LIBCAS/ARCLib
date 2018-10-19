package cz.cas.lib.arclib.dto;

import cz.cas.lib.arclib.domain.Batch;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflow;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class IngestWorkflowDto {
    private IngestWorkflow ingestWorkflow;
    private Map<String, Object> processVariables;
    private List<IncidentInfoDto> incidents;
    private Batch batch;
}
