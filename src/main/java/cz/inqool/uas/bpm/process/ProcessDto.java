package cz.inqool.uas.bpm.process;

import cz.inqool.uas.bpm.task.TaskDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

/**
 * Data transfer object for running process instance.
 *
 */
@AllArgsConstructor
@Getter
@Setter
public class ProcessDto {
    private String id;
    private String name;

    private Instant created;
    private Instant ended;

    private String businessKey;

    private List<TaskDto> tasks;
}
