package cz.inqool.uas.action;

import cz.inqool.uas.exception.MissingObject;
import cz.inqool.uas.rest.DictionaryApi;
import cz.inqool.uas.security.Permissions;
import cz.inqool.uas.store.Transactional;
import io.swagger.annotations.*;
import lombok.Getter;
import org.springframework.web.bind.annotation.*;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import java.util.Map;

import static cz.inqool.uas.util.Utils.notNull;

/**
 * Api for managing and calling scriptable action
 */
@RolesAllowed(Permissions.ACTION)
@RestController
@Api(value = "action", description = "Api for managing and calling scriptable action (main attribute: name).")
@RequestMapping("/api/actions")
public class ActionsApi implements DictionaryApi<Action> {

    @Getter
    private ActionService adapter;

    private ActionExecutor executor;

    /**
     * Executes the selected action
     * @param id Id of the {@link Action}
     * @param params Map of parameters to pass to script
     * @return Returned value from the action script
     * @throws cz.inqool.uas.exception.MissingObject If {@link Action} is not found
     */
    @ApiOperation(value = "Executes the selected action", response = Object.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = Object.class),
            @ApiResponse(code = 404, message = "Action not found")})
    @RequestMapping(value = "/{id}/execute", method = RequestMethod.POST)
    public Object execute(@ApiParam(value = "Id of the action", required = true)
                           @PathVariable("id") String id,
                           @RequestBody Map<String, Object> params) {
        return executor.execute(id, params);
    }

    /**
     * Gets one instance specified by code.
     *
     * @param code Code of the instance
     * @return Single instance
     * @throws MissingObject if instance does not exists
     */
    @ApiOperation(value = "Gets one instance specified by code", response = Action.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = Action.class),
            @ApiResponse(code = 404, message = "Instance does not exist")})
    @RequestMapping(value = "/coded", method = RequestMethod.GET)
    @Transactional
    public Action getCoded(@ApiParam(value = "Code of the instance", required = true) @RequestParam("code") String code) {
        Action entity = getAdapter().findByCode(code);
        notNull(entity, () -> new MissingObject(Action.class, code));

        return entity;
    }

    @Inject
    public void setAdapter(ActionService store) {
        this.adapter = store;
    }

    @Inject
    public void setExecutor(ActionExecutor executor) {
        this.executor = executor;
    }
}
