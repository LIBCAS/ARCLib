package cz.inqool.uas.bpm.task;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Data transfer object for available task.
 *
 */
@AllArgsConstructor
@Getter
@Setter
public class TaskDto {
    private String id;
    private String key;

    private String name;
    private String description;
    private String processId;

    private Instant dueDate;
    private Instant created;
    private Instant ended;

    private String businessKey;
    private String assignee;
    private String[] candidates;
}
