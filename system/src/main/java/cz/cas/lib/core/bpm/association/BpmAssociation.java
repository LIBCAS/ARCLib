package cz.cas.lib.core.bpm.association;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Connection of the {@link BpmObject} to BPM Processes and Tasks
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BpmAssociation {
    /**
     * Id of the process definition.
     */
    private String processDefinitionId;

    /**
     * Id of the process instance.
     */
    private String processId;

    /**
     * Id of the task definition.
     */
    private String taskDefinitionId;

    /**
     * Id of the task instance.
     */
    private String taskId;
}
