package cz.inqool.uas.bpm.workflow;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * Data transfer object for deployed process definition.
 *
 */
@AllArgsConstructor
@Getter
@Setter
public class WorkflowDto {
    private String id;
    private String key;

    private String name;
    private String description;
}
