package cz.cas.lib.arclib.dto;

import cz.cas.lib.arclib.domain.Producer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ProducerProfileDto {
    private String id;
    private String externalId;
    private String name;
    private Instant created;
    private Instant updated;
    private Producer producer;
    private String sipProfileName;
    private String validationProfileName;
    private String workflowDefinitionName;
}
