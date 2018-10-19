package cz.cas.lib.arclib.dto;

import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflow;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AipDetailDto {
    private Map<String, Object> indexedFields;
    private IngestWorkflow ingestWorkflow;
}
