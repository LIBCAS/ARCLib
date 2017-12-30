package cz.inqool.uas.bpm.task;

import cz.inqool.uas.index.dto.Params;
import cz.inqool.uas.index.dto.Result;
import cz.inqool.uas.security.Permissions;
import io.swagger.annotations.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import java.util.Map;

/**
 * Api for interaction with tasks.
 *
 * <p>
 *     User needs to have BPM role.
 * </p>
 */
@ConditionalOnProperty(prefix = "bpm", name = "enabled", havingValue = "true")
@RolesAllowed(Permissions.BPM)
@RestController
@Api(value = "task", description = "Api for interaction with tasks")
@RequestMapping("/api/tasks")
public class TaskApi {

    private TaskList taskList;

    @ApiOperation(value = "Gets all tasks assigned to calling user that respect the selected parameters.",
            notes = "Returns sorted list of active tasks with total number.", response = Result.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = Result.class)})
    @RequestMapping(value = "/own", method = RequestMethod.GET)
    public Result<TaskDto> own(@ApiParam(value = "Parameters to comply with", required = true)
                                   @ModelAttribute Params params) {
        return taskList.getOwnTasks(params);
    }

    @ApiOperation(value = "Gets all unassigned tasks assignable to calling user (via candidate group) that respect the selected parameters.",
            notes = "Returns sorted list of active tasks with total number.", response = Result.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = Result.class)})
    @RequestMapping(value = "/group-unassigned", method = RequestMethod.GET)
    public Result<TaskDto> pendingGroup(@ApiParam(value = "Parameters to comply with", required = true)
                                   @ModelAttribute Params params) {
        return taskList.getUnassignedGroupTasks(params);
    }

    @ApiOperation(value = "Gets all unassigned tasks assignable to calling user that respect the selected parameters.",
            notes = "Returns sorted list of active tasks with total number.", response = Result.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = Result.class)})
    @RequestMapping(value = "/unassigned", method = RequestMethod.GET)
    public Result<TaskDto> pending(@ApiParam(value = "Parameters to comply with", required = true)
                                       @ModelAttribute Params params) {
        return taskList.getUnassignedTasks(params);
    }

    @ApiOperation(value = "Gets one task for specified id.",
            notes = "Task must be assigned or assignable to calling user. " +
                    "This method will fail if no user is logged in.", response = TaskDto.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = TaskDto.class)})
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public TaskDto get(@ApiParam(value = "Id of the task", required = true)
                           @PathVariable("id") String id) {
        return taskList.get(id);
    }

    @ApiOperation(value = "Claims assignable task from pool of unassigned tasks.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response"),
            @ApiResponse(code = 400, message = "No id specified or no user logged in"),
            @ApiResponse(code = 403, message = "Task is already assigned"),
            @ApiResponse(code = 404, message = "Task does not exist")})
    @RequestMapping(value = "/{id}/claim", method = RequestMethod.POST)
    public void claim(@ApiParam(value = "Id of the task", required = true)
                          @PathVariable("id") String id) {
        taskList.claimTask(id);
    }

    @ApiOperation(value = "Marks the task as complete, submits the form attributes " +
            "and let the process instance continue.", notes = "Only own task can be finished." +
            " Assignable task must be claimed first.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response"),
            @ApiResponse(code = 400, message = "No id specified or no user logged in"),
            @ApiResponse(code = 404, message = "Task does not exist")})
    @RequestMapping(value = "/{id}/finish", method = RequestMethod.POST, consumes="application/json")
    public void finish(@ApiParam(value = "Id of the task", required = true)
                           @PathVariable("id") String id,
                       @ApiParam(value = "Form attributes in the form of a map", required = true)
                           @RequestBody Map<String, Object> params) {
        taskList.finishTask(id, params);
    }

    @RequestMapping(value = "/{id}/assignee",method = RequestMethod.POST)
    public void changeAssignee(@PathVariable("id") String taskId, @RequestBody String assignee) {
        taskList.changeAssignee(taskId, assignee);
    }

    @ApiOperation(value = "Gets all the internal variables of a task.", response = Map.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = Map.class)})
    @RequestMapping(value = "/{id}/variables", method = RequestMethod.GET)
    public Map<String, Object> listVariables(@ApiParam(value = "Id of the task", required = true)
                                                 @PathVariable("id") String id) {
        return taskList.getVariables(id);
    }

    @ApiOperation(value = "Sets internal task variables.", notes = "Beware of using variable names" +
            " identical to process variables. If the variable is first saved in Task scope," +
            "it will not be promoted to process instance scope later as expected")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response")})
    @RequestMapping(value = "/{id}/variables", method = RequestMethod.PUT, consumes="application/json")
    public void updateVariables(@ApiParam(value = "Id of the task", required = true)
                                    @PathVariable("id") String id ,
                                @ApiParam(value = "Map of variables to set", required = true)
                                    @RequestBody Map<String, Object> params) {
        taskList.setVariables(id, params);
    }

    @Inject
    public void setTaskList(TaskList taskList) {
        this.taskList = taskList;
    }
}
