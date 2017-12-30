package cz.inqool.uas.bpm.association;

import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Connects domain objects to BPM
 */
@ConditionalOnProperty(prefix = "bpm", name = "enabled", havingValue = "true")
@Service
public class BpmAssociationFinder {
    private RuntimeService runtimeService;

    private TaskService taskService;

    /**
     * Gets all tasks where the object is used
     * @param variableName Name of the variable that holds the object Id
     * @param value Id of the object
     * @return Set of association between objects and tasks (with processes)
     */
    public Set<BpmAssociation> getBpmAssociations(String variableName, String value) {
        List<ProcessInstance> processInstances = runtimeService.createProcessInstanceQuery()
                                                               .variableValueEquals(variableName, value)
                                                               .list();

        return processInstances.stream()
                               .map(pi -> taskService.createTaskQuery()
                                                     .processInstanceId(pi.getId())
                                                     .list())
                               .flatMap(Collection::stream)
                               .map(task -> {
                                   String processDefinitionId = task.getProcessDefinitionId();

                                   // removes specific version from process definition
                                   String[] processDefinitionIdSplit = processDefinitionId.split(":");
                                   if (processDefinitionIdSplit.length > 0) {
                                       processDefinitionId = processDefinitionIdSplit[0];
                                   }

                                   return new BpmAssociation(
                                           processDefinitionId,
                                           task.getProcessInstanceId(),
                                           task.getTaskDefinitionKey(),
                                           task.getId()
                                   );
                               })
                               .collect(Collectors.toSet());

    }

    /**
     * Gets process Id from BPM association based on process definition id.
     *
     * In case of multiple Process Instances with same defintion Id, any one of the process instance id is returned.
     *
     * @param variableName Name of the variable that holds the object Id
     * @param value Id of the object
     * @param processDefinitionId Id of the process definition
     * @return Id of the task or null
     */
    public String getProcessId(String variableName, String value, String processDefinitionId) {
        Set<BpmAssociation> associations = getBpmAssociations(variableName, value);

        return associations.stream()
                           .filter(a -> a.getProcessDefinitionId().equals(processDefinitionId))
                           .map(BpmAssociation::getProcessId)
                           .findAny()
                           .orElse(null);
    }

    /**
     * Gets task Id from BPM association based on process definition id and task definition id.
     *
     * In case of multiple Task with same Id in the same process instance, any one of the tasks is returned.
     *
     * @param variableName Name of the variable that holds the object Id
     * @param value Id of the object
     * @param processDefinitionId Id of the process definition
     * @param taskDefinitionId Id of the task definition
     * @return Id of the task or null
     */
    public String getTaskId(String variableName, String value, String processDefinitionId, String taskDefinitionId) {
        Set<BpmAssociation> associations = getBpmAssociations(variableName, value);

        return associations.stream()
                    .filter(a -> a.getProcessDefinitionId().equals(processDefinitionId))
                    .filter(a -> a.getTaskDefinitionId().equals(taskDefinitionId))
                    .map(BpmAssociation::getTaskId)
                    .findAny()
                    .orElse(null);
    }

    @Inject
    public void setRuntimeService(RuntimeService runtimeService) {
        this.runtimeService = runtimeService;
    }

    @Inject
    public void setTaskService(TaskService taskService) {
        this.taskService = taskService;
    }
}
