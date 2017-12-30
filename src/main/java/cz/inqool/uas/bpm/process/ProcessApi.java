package cz.inqool.uas.bpm.process;


import cz.inqool.uas.index.dto.Params;
import cz.inqool.uas.index.dto.Result;
import cz.inqool.uas.security.Permissions;
import cz.inqool.uas.store.Transactional;
import io.swagger.annotations.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.validation.Valid;
import java.util.Map;

/**
 * Api for interaction with process instances.
 *
 * <p>
 *     User needs to have BPM role.
 * </p>
 */
@ConditionalOnProperty(prefix = "bpm", name = "enabled", havingValue = "true")
@RolesAllowed(Permissions.BPM)
@RestController
@Api(value = "process", description = "Api for interaction with process instances")
@RequestMapping("/api/processes")
public class ProcessApi {

    private ProcessList processList;

    @ApiOperation(value = "Gets all processes that respect the selected parameters.",
            notes = "Returns sorted list of processes with total number.", response = Result.class)
    @ApiResponses(value = @ApiResponse(code = 200, message = "Successful response", response = Result.class))
    @RequestMapping(method = RequestMethod.GET)
    @Transactional
    public Result<ProcessDto> list(@ApiParam(value = "Parameters to comply with", required = true)
                                       @Valid @ModelAttribute Params params) {
        return processList.list(params);
    }

    @ApiOperation(value = "Gets specified process instance",
            notes = "Returns process instance.", response = Result.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = ProcessDto.class)})
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    @Transactional
    public ProcessDto get(@PathVariable("id") String id) {
        return processList.get(id);
    }

    @ApiOperation(value = "Creates process instance from process definition. Returns new process instance ID.",
            notes = "Only parameters corresponding to form attributes defined on Start task will be stored. " +
                    "Others will be silently ignored.", response = String.class)
    @ApiResponses(value = @ApiResponse(code = 200, message = "Successful response", response = String.class))
    @RequestMapping(method = RequestMethod.POST, consumes="application/json")
    @Transactional
    public String create(@ApiParam(value = "Specified key of workflow", required = true)
                             @RequestParam("key") String key,
                         @ApiParam(value = "Specified business key (reference to external resource)", required = true)
                             @RequestParam("businessKey") String businessKey,
                         @ApiParam(value = "Initial process variables", required = true)
                             @RequestBody Map<String, Object> params) {
        return processList.create(key, businessKey, params);
    }

    @ApiOperation(value = "Cancels specified process instance")
    @ApiResponses(value = @ApiResponse(code = 201, message = "Successful response"))
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    @Transactional
    public void delete(@PathVariable("id") String id, @RequestParam(name = "reason", required = false) String reason) {
        processList.cancel(id, reason);
    }

    @ApiOperation(value = "Gets all the process variables of a running process instance.",
            notes = "Returns map of the variables.",
            response = Map.class)
    @ApiResponses(value = @ApiResponse(code = 200, message = "Successful response", response = Map.class))
    @RequestMapping(value = "/{id}/variables", method = RequestMethod.GET)
    @Transactional
    public Map<String, Object> listVariables(@ApiParam(value = "Id of the process instance", required = true)
                                                 @PathVariable("id") String id) {
        return processList.getVariables(id);
    }

    @ApiOperation(value = "Sets the process variables on a running process instance.")
    @ApiResponses(value = @ApiResponse(code = 200, message = "Successful response"))
    @RequestMapping(value = "/{id}/variables", method = RequestMethod.PUT, consumes="application/json")
    @Transactional
    public void updateVariables(@ApiParam(value = "Id of the process instance", required = true)
                                    @PathVariable("id") String id ,
                                @ApiParam(value = "Map of variables to set", required = true)
                                    @RequestBody Map<String, Object> params) {
        processList.setVariables(id, params);
    }

    @ApiOperation(value = "Signals a BPMN Message to a running process instance.")
    @ApiResponses(value = @ApiResponse(code = 200, message = "Successful response"))
    @RequestMapping(value = "/{id}/messages/{message}", method = RequestMethod.PUT)
    @Transactional
    public void signalMessage(@ApiParam(value = "Id of the process instance.", required = true)
                                  @PathVariable("id") String id,
                              @ApiParam(value = "Message name to signalize", required = true)
                                  @PathVariable("message") String message,
                              @ApiParam(value = "Map of variables to set")
                                  @RequestBody(required = false) Map<String, Object> params) {
        processList.signalMessage(id, message, params);
    }

    @Inject
    public void setProcessList(ProcessList processList) {
        this.processList = processList;
    }
}
