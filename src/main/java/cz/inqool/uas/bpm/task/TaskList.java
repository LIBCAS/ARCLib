package cz.inqool.uas.bpm.task;

import cz.inqool.uas.exception.BadArgument;
import cz.inqool.uas.exception.ForbiddenObject;
import cz.inqool.uas.exception.MissingObject;
import cz.inqool.uas.index.dto.*;
import cz.inqool.uas.security.UserDetails;
import org.camunda.bpm.engine.FormService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.form.TaskFormData;
import org.camunda.bpm.engine.impl.form.validator.FormFieldValidatorException;
import org.camunda.bpm.engine.runtime.Execution;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.engine.task.TaskQuery;
import org.camunda.bpm.engine.variable.type.ValueType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;

import static cz.inqool.uas.bpm.util.BpmUtils.filterDefinedFields;
import static cz.inqool.uas.util.Utils.*;
import static java.util.Collections.emptyMap;

/**
 * Task manager.
 */
@ConditionalOnProperty(prefix = "bpm", name = "enabled", havingValue = "true")
@Service
public class TaskList {
    private static final String DUE_DATE = "dueDate";
    private static final String CREATION_DATE = "created";
    private static final String ASSIGNEE = "assignee";

    private TaskService service;

    private FormService formService;

    private RuntimeService runtimeService;

    private UserDetails user;

    /**
     * Gets all tasks assigned to calling user that respect the selected {@link Params}.
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
     *     {@link TaskList#filterQuery(TaskQuery, Params)}
     *     and {@link TaskList#sortQuery(TaskQuery, Params)} in
     *     source code.
     * </p>
     *
     * @param params Parameters to comply with
     * @return Sorted {@link List} of active tasks with total number
     */
    public Result<TaskDto> getOwnTasks(Params params) {
        notNull(user, () -> new BadArgument("user"));

        TaskQuery query = createQuery();

        addOwnFilter(params);
        filterQuery(query, params);
        sortQuery(query, params);

        return executeQuery(query, params);
    }

    /**
     * Gets all unassigned tasks assignable to calling user that respect the selected {@link Params}.
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
     *     {@link TaskList#filterQuery(TaskQuery, Params)}
     *     and {@link TaskList#sortQuery(TaskQuery, Params)} in
     *     source code.
     * </p>
     *
     * @param params Parameters to comply with
     * @return Sorted {@link List} of active tasks with total number
     */
    public Result<TaskDto> getUnassignedTasks(Params params) {
        TaskQuery query = createQuery();

        if (unwrap(user) != null) {
            addCandidateFilter(query);
        }

        filterQuery(query, params);
        sortQuery(query, params);

        return executeQuery(query, params);
    }

    /**
     * Gets all unassigned tasks assignable to calling user (via candidate group) that respect the selected {@link Params}.
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
     *     {@link TaskList#filterQuery(TaskQuery, Params)}
     *     and {@link TaskList#sortQuery(TaskQuery, Params)} in
     *     source code.
     * </p>
     *
     * @param params Parameters to comply with
     * @return Sorted {@link List} of active tasks with total number
     */
    public Result<TaskDto> getUnassignedGroupTasks(Params params) {
        TaskQuery query = createQuery();

        if (unwrap(user) != null) {
            addCandidateGroupFilter(query);
        }

        filterQuery(query, params);
        sortQuery(query, params);

        return executeQuery(query, params);
    }

    /**
     * Gets one task for specified id.
     *
     * <p>
     *     Task must be assigned or assignable to calling user (directly or via candidate group).
     * </p>
     * <p>
     *     This method will fail if no user is logged in.
     * </p>
     *
     * @param id Id of the task
     * @return A task
     */
    public TaskDto get(String id) {
        Task task = getOwnTask(id);

        if (task == null) {
            task = getUnassignedTask(id);
        }

        if (task == null) {
            task = getUnassignedGroupTask(id);
        }

        if (task != null) {
            Map<String, String> businessKeys = getProcessBusinessKeys(asSet(task.getProcessInstanceId()));

            return new TaskDto(task.getId(), task.getTaskDefinitionKey(), task.getName(),
                    task.getDescription(), task.getProcessInstanceId(),
                    toInstant(task.getDueDate()), toInstant(task.getCreateTime()), null,
                    businessKeys.get(task.getProcessInstanceId()),
                    task.getAssignee(), null);
        } else {
            throw new MissingObject(TaskDto.class, id);
        }
    }

    /**
     * Sets internal task variables.
     *
     * <p>
     *     Beware of using variable names identical to process variables. If the variable is first saved in Task scope,
     *     it will not be promoted to process instance scope later as expected
     *     (e.g. in {@link TaskList#finishTask(String, Map)}).
     * </p>
     *
     * @param id Id of the task
     * @param params {@link Map} of variables to set
     */
    public void setVariables(String id, Map<String, Object> params) {
        eq(taskExists(id), true, () -> new MissingObject(Task.class, id));
        ownTask(id);

        service.setVariablesLocal(id, params);
    }

    /**
     * Gets all the internal variables of a task.
     *
     * @param id Id of the task
     * @return {@link Map} of the variables
     */
    public Map<String, Object> getVariables(String id) {
        eq(taskExists(id), true, () -> new MissingObject(Task.class, id));
        ownTask(id);

        return service.getVariablesLocal(id);
    }

    /**
     * Marks the task as complete, submits the form attributes and let the process instance continue.
     *
     * <p>
     *     Only own task can be finished. Assignable task must be claimed first with {@link TaskList#claimTask(String)}.
     * </p>
     *
     * @param id Id of the task
     * @param params Form attributes in the form of a {@link Map}
     * @throws BadArgument If no id specified or no user logged in
     * @throws MissingObject If task does not exist
     */
    public void finishTask(String id, Map<String, Object> params) {
        finishTask(id, params, true);
    }

    /**
     * Marks the task as complete, submits the form attributes and let the process instance continue.
     *
     * @param id Id of the task
     * @param params Form attributes in the form of a {@link Map}
     * @param checkOwnership Should application check if this is task owned by signed in user
     * @throws BadArgument If no id specified or no user logged in
     * @throws MissingObject If task does not exist
     */
    public void finishTask(String id, Map<String, Object> params, boolean checkOwnership) {
        notNull(id, () -> new BadArgument(Task.class, id));
        eq(taskExists(id), true, () -> new MissingObject(Task.class, id));
        notNull(user, () -> new BadArgument("user"));

        if (checkOwnership) {
            ownTask(id);
        }

        TaskFormData formData = formService.getTaskFormData(id);

        try {
            formService.submitTaskForm(id, filterDefinedFields(formData, params));
        } catch (FormFieldValidatorException ex) {
            throw new BadArgument("validation");
        }
    }

    /**
     * Claims assignable task from pool of unassigned tasks.
     *
     * @param id Id of the task.
     * @throws BadArgument If no id specified or no user logged in
     * @throws MissingObject If task does not exist
     * @throws ForbiddenObject If task is already assigned
     */
    public void claimTask(String id) {
        notNull(id, () -> new BadArgument(Task.class, id));
        eq(taskExists(id), true, () -> new MissingObject(Task.class, id));
        isNull(taskOwner(id), () -> new ForbiddenObject(Task.class, id));
        notNull(user, () -> new BadArgument("user"));

        service.claim(id, user.getId());
    }

    public void changeAssignee(String id, String assignee) {
        service.setAssignee(id, assignee);
    }

    private Task getOwnTask(String id) {
        notNull(user, () -> new BadArgument("user"));

        TaskQuery query = createQuery();

        query.taskId(id);
        query.taskAssignee(user.getId());
        return query.singleResult();
    }

    private Task getUnassignedTask(String id) {
        TaskQuery query = createQuery();

        query.taskId(id);

        if (unwrap(user) != null) {
            query.taskCandidateUser(user.getId());
        }

        return query.singleResult();
    }

    private Task getUnassignedGroupTask(String id) {
        TaskQuery query = createQuery();

        query.taskId(id);

        if (unwrap(user) != null) {
            query.taskCandidateGroupIn(getUserGroups());
        }

        return query.singleResult();
    }

    private TaskQuery createQuery() {
        return service.createTaskQuery();
    }

    private void addCandidateFilter(TaskQuery query) {
        notNull(user, () -> new BadArgument("user"));

        query.taskUnassigned().taskCandidateUser(user.getId());
    }

    private void addCandidateGroupFilter(TaskQuery query) {
        notNull(user, () -> new BadArgument("user"));

        query.taskUnassigned().taskCandidateGroupIn(getUserGroups());
    }

    private List<String> getUserGroups() {
        Collection<? extends GrantedAuthority> authorities = user.getAuthorities();
        notNull(authorities, () -> new BadArgument("authorities"));

        return authorities.stream()
                          .map(GrantedAuthority::getAuthority)
                          .collect(Collectors.toList());
    }

    private void addOwnFilter(Params params) {
        notNull(user, () -> new BadArgument("user"));

        if (params.getFilter() == null) {
            params.setFilter(new ArrayList<>());
        }

        params.getFilter().add(new Filter(ASSIGNEE, FilterOperation.EQ, user.getId(), null));
    }

    private Result<TaskDto> executeQuery(TaskQuery query, Params params) {
        int first = params.getPage() * params.getPageSize();

        List<Task> tasks = query.listPage(first, params.getPageSize());
        long count = query.count();

        Set<String> processIds = extractProcessIds(tasks);
        Map<String, String> businessKeys = getProcessBusinessKeys(processIds);

        List<TaskDto> dtos = tasks.stream()
                .map(task -> new TaskDto(
                        task.getId(),
                        task.getTaskDefinitionKey(),
                        task.getName(),
                        task.getDescription(),
                        task.getProcessInstanceId(),
                        toInstant(task.getDueDate()),
                        toInstant(task.getCreateTime()),
                        null,
                        businessKeys.get(task.getProcessInstanceId()),
                        task.getAssignee(),
                        null
                ))
                .collect(Collectors.toList());

        Result<TaskDto> result = new Result<>();
        result.setCount(count);
        result.setItems(dtos);

        return result;
    }

    private Set<String> extractProcessIds(Collection<Task> tasks) {
        return tasks.stream()
                .map(Task::getProcessInstanceId)
                .distinct()
                .collect(Collectors.toSet());
    }

    private void sortQuery(TaskQuery query, Params params) {
        String field = params.getSort();
        Order order = params.getOrder();

        switch (field) {
            case DUE_DATE:
                query.orderByDueDate();
                break;
            case CREATION_DATE:
                query.orderByTaskCreateTime();
                break;
            default:
                query.orderByTaskVariable(field, ValueType.STRING);
        }

        if (order == Order.ASC) {
            query.asc();
        } else {
            query.desc();
        }
    }

    private void filterQuery(TaskQuery query, Params params) {
        if (params.getFilter() != null) {
            params.getFilter()
                    .forEach(filter -> {
                        String field = filter.getField();
                        String value = filter.getValue();

                        if (Objects.equals(field, ASSIGNEE)) {
                            query.taskAssignee(value);
                        } else {
                            switch (filter.getOperation()) {
                                case EQ:
                                    query.processVariableValueEquals(field, value);
                                    break;
                                case NEQ:
                                    query.processVariableValueNotEquals(field, value);
                                    break;
                                case GT:
                                    query.processVariableValueGreaterThan(field, value);
                                    break;
                                case GTE:
                                    query.processVariableValueGreaterThanOrEquals(field, value);
                                    break;
                                case LT:
                                    query.processVariableValueLessThan(field, value);
                                    break;
                                case LTE:
                                    query.processVariableValueLessThanOrEquals(field, value);
                                    break;
                                case CONTAINS:
                                    query.processVariableValueLike(field, value);
                                    break;
                            }
                        }
                    });
        }
    }

    private boolean taskExists(String id) {
        return service.createTaskQuery().taskId(id).singleResult() != null;
    }

    private String taskOwner(String id) {
        return service.createTaskQuery().taskId(id).singleResult().getAssignee();
    }

    private void ownTask(String id) {
        notNull(user, () -> new BadArgument("user"));

        eq(taskOwner(id), user.getId(), () -> new ForbiddenObject(Task.class, id));
    }

    private Map<String, String> getProcessBusinessKeys(Set<String> ids) {
        if (ids.isEmpty()) {
            return emptyMap();
        }

        List<ProcessInstance> instances = runtimeService.createProcessInstanceQuery().processInstanceIds(ids).list();

        return instances.stream()
                .filter(instance -> instance.getBusinessKey() != null)
                .collect(Collectors.toMap(Execution::getId, ProcessInstance::getBusinessKey));
    }

    @Inject
    public void setService(TaskService service) {
        this.service = service;
    }

    @Inject
    public void setFormService(FormService formService) {
        this.formService = formService;
    }

    @Inject
    public void setRuntimeService(RuntimeService runtimeService) {
        this.runtimeService = runtimeService;
    }

    @Autowired(required = false)
    public void setUser(UserDetails user) {
        this.user = user;
    }
}
