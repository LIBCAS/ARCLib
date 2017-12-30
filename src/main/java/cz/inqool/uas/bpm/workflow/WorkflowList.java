package cz.inqool.uas.bpm.workflow;

import cz.inqool.uas.index.dto.Result;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.engine.repository.ProcessDefinitionQuery;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Workflow manager.
 */
@ConditionalOnProperty(prefix = "bpm", name = "enabled", havingValue = "true")
@Service
public class WorkflowList {
    private RepositoryService repositoryService;

    /**
     * Gets currently active process definitions
     *
     * @return {@link} of process definitions
     */
    public Result<WorkflowDto> list() {
        ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery()
                .active()
                .orderByProcessDefinitionName()
                .asc()
                .latestVersion();

        List<ProcessDefinition> workflows = query.list();

        List<WorkflowDto> dtos = workflows.stream()
                .map(workflow -> new WorkflowDto(
                        workflow.getId(),
                        workflow.getKey(),
                        workflow.getName(),
                        workflow.getDescription()
                ))
                .collect(Collectors.toList());


        Result<WorkflowDto> result = new Result<>();
        result.setCount(query.count());
        result.setItems(dtos);

        return result;
    }

    @Inject
    public void setRepositoryService(RepositoryService repositoryService) {
        this.repositoryService = repositoryService;
    }
}
