package cz.inqool.uas.bpm.process;

import cz.inqool.uas.bpm.task.TaskDto;
import cz.inqool.uas.bpm.workflow.WorkflowDto;
import cz.inqool.uas.exception.BadArgument;
import cz.inqool.uas.exception.ForbiddenOperation;
import cz.inqool.uas.exception.MissingObject;
import cz.inqool.uas.index.dto.*;
import cz.inqool.uas.security.UserDetails;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.*;
import org.camunda.bpm.engine.form.StartFormData;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.history.HistoricProcessInstanceQuery;
import org.camunda.bpm.engine.history.HistoricTaskInstance;
import org.camunda.bpm.engine.history.HistoricVariableInstance;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.engine.runtime.MessageCorrelationBuilder;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.IdentityLink;
import org.camunda.bpm.engine.task.IdentityLinkType;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;

import static cz.inqool.uas.bpm.util.BpmUtils.canStart;
import static cz.inqool.uas.bpm.util.BpmUtils.filterDefinedFields;
import static cz.inqool.uas.util.Utils.*;

/**
 * Process instance manager.
 */
@Slf4j
@ConditionalOnProperty(prefix = "bpm", name = "enabled", havingValue = "true")
@Service
public class ProcessList {
    private static final String NAME = "name";
    private static final String BUSINESS_KEY = "businessKey";
    private static final String CREATION_DATE = "created";
    private static final String ID = "id";
    private static final String END_DATE = "ended";
    private static final String INITIATOR = "initiator";
    private static final String FINISHED = "finished";
    private static final String WORKFLOW_KEY = "workflowKey";

    private RuntimeService runtimeService;

    private HistoryService historyService;

    private FormService formService;

    private RepositoryService repositoryService;

    private TaskService taskService;

    private UserDetails user;

    /**
     * Creates process instance from process definition.
     *
     * <p>
     *     Only parameters corresponding to form attributes defined on Start task will be stored.
     *     Others will be silently ignored.
     * </p>
     *
     * @param key Specified key of {@link WorkflowDto}
     * @param businessKey Specified business key (reference to external resource)
     * @param params Initial process variables
     * @return New process instance ID
     */
    public String create(String key, String businessKey, Map<String, Object> params) {
        ProcessDefinition definition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey(key)
                .latestVersion()
                .singleResult();

        notNull(definition, () -> new MissingObject(ProcessDefinition.class, key));

        BpmnModelInstance modelInstance = repositoryService.getBpmnModelInstance(definition.getId());
        if(!canStart(modelInstance, unwrap(user))) {
            throw new ForbiddenOperation(null);
        }

        StartFormData formData = formService.getStartFormData(definition.getId());
        Map<String, Object> filteredParams = filterDefinedFields(formData, params);


        ProcessInstance instance = formService.submitStartForm(
                definition.getId(), businessKey, filteredParams
        );

        return instance.getId();
    }

    /**
     * Finds all process instances that respect the selected {@link Params}.
     *
     * <p>
     *     Though {@link Params} one could specify filtering, sorting and paging. For further explanation
     *     see {@link Params}.
     * </p>
     * <p>
     *     Returning also the total number of instances passed through the filtering phase.
     * </p>
     * <p>
     *     Only a limited number of sort and filters are supported. For current state of affairs see
     *     {@link ProcessList#filterQuery(HistoricProcessInstanceQuery, Params)}
     *     and {@link ProcessList#sortQuery(HistoricProcessInstanceQuery, Params)} in
     *     source code.
     * </p>
     *
     * @param params Parameters to comply with
     * @return Sorted {@link List} of process instances with total number
     */
    public Result<ProcessDto> list(Params params) {
        HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery();

        addOwnFilter(params);

        filterQuery(query, params);
        sortQuery(query, params);

        return executeQuery(query, params);
    }

    private Set<String> getSubprocesses(String instanceId) {
        Set<String> children = historyService.createHistoricProcessInstanceQuery()
                                             .superProcessInstanceId(instanceId)
                                             .list()
                                             .stream()
                                             .map(HistoricProcessInstance::getId)
                                             .collect(Collectors.toSet());

        Set<String> nested = children.stream()
                                      .map(this::getSubprocesses)
                                      .flatMap(Collection::stream)
                                      .collect(Collectors.toSet());

        Set<String> all = new HashSet<>();
        all.addAll(children);
        all.addAll(nested);

        return all;
    }

    private List<HistoricTaskInstance> getTasks(String processId) {
        return historyService.createHistoricTaskInstanceQuery()
                             .activityInstanceIdIn()
                             .processInstanceId(processId)
                             .list();
    }

    private List<HistoricTaskInstance> getAllTasks(String mainProcessId) {
        Set<String> subprocesses = getSubprocesses(mainProcessId);
        subprocesses.add(mainProcessId);

        return subprocesses.stream()
                           .map(this::getTasks)
                           .flatMap(Collection::stream)
                           .sorted(Comparator.comparing(HistoricTaskInstance::getStartTime))
                           .collect(Collectors.toList());
    }

    private String[] getCandidates(HistoricTaskInstance task) {
        if (task.getEndTime() == null) {
            return taskService.getIdentityLinksForTask(task.getId())
                              .stream()
                              .filter(link -> Objects.equals(link.getType(), IdentityLinkType.CANDIDATE))
                              .filter(link -> link.getUserId() != null)
                              .map(IdentityLink::getUserId)
                              .toArray(String[]::new);

        } else {
            return new String[]{};
        }
    }

    /**
     * Cancels whole process instance.
     *
     * Process instance needs to be initiated by canceling user.
     *
     * @param id ID of process instance
     */
    public void cancel(String id, String reason) {
        ProcessDto processDto = get(id);
        runtimeService.deleteProcessInstance(processDto.getId(), reason);
    }

    public ProcessDto get(String id) {
        notNull(user, () -> new BadArgument("user"));

        HistoricProcessInstance instance = historyService.createHistoricProcessInstanceQuery()
            .processInstanceId(id)
            .variableValueEquals("initiator", user.getId())
            .singleResult();
        notNull(instance, () -> new MissingObject(ProcessDto.class, id));

        List<HistoricTaskInstance> tasks = getAllTasks(instance.getId());

        List<TaskDto> dtos = tasks.stream()
                                  .map(task -> new TaskDto(
                                          task.getId(),
                                          task.getTaskDefinitionKey(),
                                          task.getName(),
                                          task.getDescription(),
                                          task.getProcessInstanceId(),
                                          toInstant(task.getDueDate()),
                                          toInstant(task.getStartTime()),
                                          toInstant(task.getEndTime()),
                                          null,
                                          task.getAssignee(),
                                          getCandidates(task)
                                  ))
                                  .collect(Collectors.toList());

        return new ProcessDto(instance.getId(), instance.getProcessDefinitionName(),
                toInstant(instance.getStartTime()), toInstant(instance.getEndTime()),
                instance.getBusinessKey(), dtos);
    }

    /**
     * Sets the process variables on a running process instance.
     *
     * @param id Id of the process instance
     * @param params {@link Map} of variables to set
     */
    public void setVariables(String id, Map<String, Object> params) {
        eq(processExists(id), true, () -> new MissingObject(Process.class, id));

        runtimeService.setVariables(id, params);
    }

    /**
     * Gets all the process variables of a running process instance.
     *
     * @param id Id of the process instance
     * @return {@link Map} of the variables
     */
    public Map<String, Object> getVariables(String id) {
        eq(processExists(id), true, () -> new MissingObject(Process.class, id));

        return historyService.createHistoricVariableInstanceQuery()
                      .processInstanceId(id)
                      .activityInstanceIdIn(id)
                      .list()
                      .stream()
                      .filter(v -> v.getName() != null)
                      .filter(v -> v.getValue() != null)
                      .collect(Collectors.toMap(
                              HistoricVariableInstance::getName,
                              HistoricVariableInstance::getValue
                      ));
    }

    /**
     * Signals a BPMN Message to a running process instance.
     *
     * todo: describe what will happen if non existing message is signalized.
     *
     * @param id Id of a process instance
     * @param messageName Message name to signalize
     */
    public void signalMessage(String id, String messageName) {
        signalMessage(id, messageName, null);
    }

    /**
     * Signals a BPMN Message to a running process instance.
     *
     * todo: describe what will happen if non existing message is signalized.
     *
     * @param id Id of a process instance
     * @param messageName Message name to signalize
     */
    public void signalMessage(String id, String messageName, Map<String, Object> variables) {
        eq(processExists(id), true, () -> new MissingObject(Task.class, id));

        MessageCorrelationBuilder correlation = runtimeService.createMessageCorrelation(messageName);

        if (variables != null) {
            correlation.setVariables(variables);
        }

        correlation.processInstanceId(id).correlateAll();
    }

    /**
     * Signals a BPMN Message to whole engine to start new process instance.
     *
     * todo: describe what will happen if non existing message is signalized.
     *
     * @param messageName Message name to signalize
     * @return Id of newly created Process instance
     * @throws BadArgument Thrown if no process definition with specified message start event found
     */
    public String signalMessage(String messageName, Map<String, Object> params) {
        try {
            ProcessInstance instance = runtimeService
                    .createMessageCorrelation(messageName)
                    .setVariables(params)
                    .correlateStartMessage();

            return instance.getId();
        } catch (MismatchingMessageCorrelationException ex) {
            log.error("Failed to correlate message.", ex);
            throw new BadArgument("messageName");
        }
    }

    private Result<ProcessDto> executeQuery(HistoricProcessInstanceQuery query, Params params) {
        int first = params.getPage() * params.getPageSize();

        List<HistoricProcessInstance> processes = query.listPage(first, params.getPageSize());
        long count = query.count();

        List<ProcessDto> dtos = processes.stream()
                .map(process -> new ProcessDto(
                        process.getId(),
                        process.getProcessDefinitionName(),
                        toInstant(process.getStartTime()),
                        toInstant(process.getEndTime()),
                        process.getBusinessKey(),
                        null
                ))
                .collect(Collectors.toList());

        Result<ProcessDto> result = new Result<>();
        result.setCount(count);
        result.setItems(dtos);

        return result;
    }

    private void sortQuery(HistoricProcessInstanceQuery query, Params params) {
        String field = params.getSort();
        Order order = params.getOrder();

        switch (field) {
            case ID:
                query.orderByProcessInstanceId();
                break;
            case CREATION_DATE:
                query.orderByProcessInstanceStartTime();
                break;
            case END_DATE:
                query.orderByProcessInstanceEndTime();
                break;
            case NAME:
                // fixme: this is not a name
                query.orderByProcessInstanceId();
                break;
            case BUSINESS_KEY:
                query.orderByProcessInstanceBusinessKey();
                break;
            default:
                throw new BadArgument("sort");
        }

        if (order == Order.ASC) {
            query.asc();
        } else {
            query.desc();
        }
    }

    private void addOwnFilter(Params params) {
        notNull(user, () -> new BadArgument("user"));

        if (params.getFilter() == null) {
            params.setFilter(new ArrayList<>());
        }

        params.getFilter().add(new Filter(INITIATOR, FilterOperation.EQ, user.getId(), null));
    }

    private void filterQuery(HistoricProcessInstanceQuery query, Params params) {
        if (params.getFilter() != null) {
            params.getFilter()
                    .forEach(filter -> {
                        String field = filter.getField();
                        String value = filter.getValue();

                        if (Objects.equals(field, INITIATOR)) {
                            query.variableValueEquals("initiator", value);

                        } else if (Objects.equals(field, FINISHED)) {
                            if (Boolean.valueOf(value)) {
                                query.finished();
                            } else {
                                query.unfinished();
                            }
                        } else if (Objects.equals(field, WORKFLOW_KEY)) {
                            query.processDefinitionKey(value);
                        } else {

                            switch (filter.getOperation()) {
                                case EQ:
                                    query.variableValueEquals(field, value);
                                    break;
                                case NEQ:
                                    query.variableValueNotEquals(field, value);
                                    break;
                                case GT:
                                    query.variableValueGreaterThan(field, value);
                                    break;
                                case GTE:
                                    query.variableValueGreaterThanOrEqual(field, value);
                                    break;
                                case LT:
                                    query.variableValueLessThan(field, value);
                                    break;
                                case LTE:
                                    query.variableValueLessThanOrEqual(field, value);
                                    break;
                                case CONTAINS:
                                    query.variableValueLike(field, value);
                                    break;
                            }
                        }
                    });
        }
    }

    private boolean processExists(String id) {
        return historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(id)
                .singleResult() != null;
    }

    @Inject
    public void setRuntimeService(RuntimeService runtimeService) {
        this.runtimeService = runtimeService;
    }

    @Inject
    public void setHistoryService(HistoryService historyService) {
        this.historyService = historyService;
    }

    @Inject
    public void setFormService(FormService formService) {
        this.formService = formService;
    }

    @Inject
    public void setRepositoryService(RepositoryService repositoryService) {
        this.repositoryService = repositoryService;
    }

    @Autowired(required = false)
    public void setUser(UserDetails user) {
        this.user = user;
    }

    @Inject
    public void setTaskService(TaskService taskService) {
        this.taskService = taskService;
    }
}
