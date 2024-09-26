package cz.cas.lib.arclib.api;

import cz.cas.lib.arclib.domain.ingestWorkflow.WorkflowDefinition;
import cz.cas.lib.arclib.domainbase.exception.BadArgument;
import cz.cas.lib.arclib.domainbase.exception.MissingObject;
import cz.cas.lib.arclib.dto.WorkflowDefinitionDto;
import cz.cas.lib.arclib.security.authorization.permission.Permissions;
import cz.cas.lib.arclib.service.WorkflowDefinitionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

import static cz.cas.lib.core.util.Utils.eq;
import static cz.cas.lib.core.util.Utils.notNull;

@RestController
@Tag(name = "workflow definition", description = "Api for interaction with workflow definitions")
@RequestMapping("/api/workflow_definition")
public class WorkflowDefinitionApi {

    @Getter
    private WorkflowDefinitionService service;

    @Operation(summary = "Saves an instance. [Perm.WORKFLOW_DEFINITION_RECORDS_WRITE]",
            description = "Returns single instance (possibly with computed attributes)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response", content = @Content(schema = @Schema(implementation = WorkflowDefinition.class))),
            @ApiResponse(responseCode = "400", description = "Specified id does not correspond to the id of the instance")})
    @PreAuthorize("hasAuthority('" + Permissions.WORKFLOW_DEFINITION_RECORDS_WRITE + "')")
    @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
    public WorkflowDefinition save(@Parameter(description = "Id of the instance", required = true) @PathVariable("id") String id,
                                   @Parameter(description = "Single instance", required = true) @RequestBody WorkflowDefinition request) {
        eq(id, request.getId(), () -> new BadArgument("id"));
        return service.save(request);
    }

    @Operation(summary = "Deletes an instance. [Perm.WORKFLOW_DEFINITION_RECORDS_WRITE]")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response"),
            @ApiResponse(responseCode = "404", description = "Instance does not exist")})
    @PreAuthorize("hasAuthority('" + Permissions.WORKFLOW_DEFINITION_RECORDS_WRITE + "')")
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public void delete(@Parameter(description = "Id of the instance", required = true)
                       @PathVariable("id") String id) {
        WorkflowDefinition entity = service.find(id);
        notNull(entity, () -> new MissingObject(WorkflowDefinition.class, id));

        service.delete(entity);
    }

    @Operation(summary = "Gets one instance specified by id [Perm.WORKFLOW_DEFINITION_RECORDS_READ]",
            description = "Returns deleted entities as well.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response", content = @Content(schema = @Schema(implementation = WorkflowDefinition.class))),
            @ApiResponse(responseCode = "404", description = "Instance does not exist")})
    @PreAuthorize("hasAuthority('" + Permissions.WORKFLOW_DEFINITION_RECORDS_READ + "')")
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public WorkflowDefinition get(@Parameter(description = "Id of the instance", required = true) @PathVariable("id") String id) {
        WorkflowDefinition entity = service.findWithDeletedFilteringOff(id);
        notNull(entity, () -> new MissingObject(WorkflowDefinition.class, id));

        return entity;
    }

    @Operation(summary = "Gets DTOs of all instances [Perm.WORKFLOW_DEFINITION_RECORDS_READ]",
            description = "if the calling user is not [Perm.SUPER_ADMIN_PRIVILEGE] only WorkflowDefinitions assigned to the user's producer are returned. ")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Successful response", content = @Content(schema = @Schema(implementation = WorkflowDefinitionDto.class)))})
    @PreAuthorize("hasAuthority('" + Permissions.WORKFLOW_DEFINITION_RECORDS_READ + "')")
    @RequestMapping(path = "/list_dtos", method = RequestMethod.GET)
    public Collection<WorkflowDefinitionDto> listDtos() {
        return service.listWorkflowDefinitionDtos();
    }

    @Autowired
    public void setService(WorkflowDefinitionService service) {
        this.service = service;
    }
}
