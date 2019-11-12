package cz.cas.lib.arclib.api;

import cz.cas.lib.arclib.domain.ingestWorkflow.WorkflowDefinition;
import cz.cas.lib.arclib.dto.WorkflowDefinitionDto;
import cz.cas.lib.arclib.security.authorization.Roles;
import cz.cas.lib.arclib.service.WorkflowDefinitionService;
import cz.cas.lib.arclib.domainbase.exception.BadArgument;
import cz.cas.lib.arclib.domainbase.exception.MissingObject;
import io.swagger.annotations.*;
import lombok.Getter;
import org.springframework.web.bind.annotation.*;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import java.util.Collection;

import static cz.cas.lib.core.util.Utils.eq;
import static cz.cas.lib.core.util.Utils.notNull;

@RestController
@Api(value = "workflow definition", description = "Api for interaction with workflow definitions")
@RequestMapping("/api/workflow_definition")
public class WorkflowDefinitionApi {
    @Getter
    private WorkflowDefinitionService service;

    @ApiOperation(value = "Saves an instance. Roles.SUPER_ADMIN, Roles.ADMIN",
            notes = "Returns single instance (possibly with computed attributes)",
            response = WorkflowDefinition.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = WorkflowDefinition.class),
            @ApiResponse(code = 400, message = "Specified id does not correspond to the id of the instance")})
    @RolesAllowed({Roles.SUPER_ADMIN, Roles.ADMIN})
    @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
    public WorkflowDefinition save(@ApiParam(value = "Id of the instance", required = true)
                                   @PathVariable("id") String id,
                                   @ApiParam(value = "Single instance", required = true)
                                   @RequestBody WorkflowDefinition request) {
        eq(id, request.getId(), () -> new BadArgument("id"));

        return service.save(request);
    }

    @ApiOperation(value = "Deletes an instance. Roles.SUPER_ADMIN")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response"),
            @ApiResponse(code = 404, message = "Instance does not exist")})
    @RolesAllowed({Roles.SUPER_ADMIN})
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public void delete(@ApiParam(value = "Id of the instance", required = true)
                       @PathVariable("id") String id) {
        WorkflowDefinition entity = service.find(id);
        notNull(entity, () -> new MissingObject(WorkflowDefinition.class, id));

        service.delete(entity);
    }

    @ApiOperation(value = "Gets one instance specified by id", response = WorkflowDefinition.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = WorkflowDefinition.class),
            @ApiResponse(code = 404, message = "Instance does not exist")})
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public WorkflowDefinition get(@ApiParam(value = "Id of the instance", required = true)
                                  @PathVariable("id") String id) {
        WorkflowDefinition entity = service.find(id);
        notNull(entity, () -> new MissingObject(WorkflowDefinition.class, id));

        return entity;
    }

    @ApiOperation(value = "Gets all instances", response = Collection.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = Collection.class)})
    @RequestMapping(method = RequestMethod.GET)
    public Collection<WorkflowDefinition> list() {
        return service.findAll();
    }

    @ApiOperation(value = "Gets DTOs of all instances", response = Collection.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = Collection.class)})
    @RequestMapping(path = "/list_dtos", method = RequestMethod.GET)
    public Collection<WorkflowDefinitionDto> listDtos() {
        return service.listWorkflowDefinitionDtos();
    }

    @Inject
    public void setService(WorkflowDefinitionService service) {
        this.service = service;
    }
}
