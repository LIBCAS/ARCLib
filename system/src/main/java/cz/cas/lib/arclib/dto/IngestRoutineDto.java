package cz.cas.lib.arclib.dto;

import cz.cas.lib.arclib.domain.Producer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class IngestRoutineDto {
    private String id;
    private String name;
    private Producer producer;
    private String producerProfileName;
    private String transferAreaPath;
    private String cronExpression;
    private boolean active;
}
