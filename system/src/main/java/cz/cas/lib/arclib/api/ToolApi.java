package cz.cas.lib.arclib.api;

import cz.cas.lib.arclib.domain.preservationPlanning.Tool;
import cz.cas.lib.arclib.domainbase.exception.BadArgument;
import cz.cas.lib.arclib.domainbase.exception.MissingObject;
import cz.cas.lib.arclib.dto.ToolUpdateDto;
import cz.cas.lib.arclib.security.authorization.permission.Permissions;
import cz.cas.lib.arclib.service.preservationPlanning.ToolService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

import static cz.cas.lib.core.util.Utils.eq;
import static cz.cas.lib.core.util.Utils.notNull;

@RestController
@Tag(name = "tool", description = "Api for interaction with tools")
@RequestMapping("/api/tool")
public class ToolApi {
    private ToolService service;


    @Operation(summary = "Gets all instances [Perm.TOOL_RECORDS_READ]")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Successful response", content = @Content(schema = @Schema(implementation = Collection.class)))})
    @PreAuthorize("hasAuthority('" + Permissions.TOOL_RECORDS_READ + "')")
    @RequestMapping(method = RequestMethod.GET)
    public Collection<Tool> listAll() {
        return service.findAll();
    }

    @Operation(summary = "Gets one instance specified by id. [Perm.TOOL_RECORDS_READ]")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response", content = @Content(schema = @Schema(implementation = Tool.class))),
            @ApiResponse(responseCode = "404", description = "Instance does not exist")})
    @PreAuthorize("hasAuthority('" + Permissions.TOOL_RECORDS_READ + "')")
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public Tool get(@Parameter(description = "Id of the instance", required = true)
                    @PathVariable("id") String id) {
        Tool entity = service.find(id);
        notNull(entity, () -> new MissingObject(Tool.class, id));
        return entity;
    }

//    @Operation(summary = "Saves an instance. Roles.SUPER_ADMIN", description = "Returns single instance (possibly with computed attributes)",
//            responses = {@ApiResponse(content = @Content(schema = @Schema(implementation = Tool.class)))})
//    @ApiResponses(value = {
//            @ApiResponse(responseCode = "200", description = "Successful response", response = Tool.class),
//            @ApiResponse(responseCode = "400", description = "Specified id does not correspond to the id of the instance")})
//    @RolesAllowed({Roles.SUPER_ADMIN})
//    @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
//    @Transactional
//    public Tool save(@Parameter(description = "Id of the instance", required = true)
//                     @PathVariable("id") String id,
//                     @Parameter(description = "Single instance", required = true)
//                     @RequestBody Tool request) {
//        eq(id, request.getId(), () -> new BadArgument("id"));
//
//        return service.save(request);
//    }

//    @Operation(summary = "Deletes an instance. Roles.SUPER_ADMIN")
//    @ApiResponses(value = {
//            @ApiResponse(responseCode = "200", description = "Successful response"),
//            @ApiResponse(responseCode = "404", description = "Instance does not exist")})
//    @RolesAllowed({Roles.SUPER_ADMIN})
//    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
//    @Transactional
//    public void delete(@Parameter(description = "Id of the instance", required = true)
//                       @PathVariable("id") String id) {
//        Tool tool = service.find(id);
//        notNull(tool, () -> new MissingObject(service.getType(), id));
//
//        service.delete(tool);
//    }

    @Operation(summary = "Updates instance [Perm.TOOL_RECORDS_WRITE]")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response", content = @Content(schema = @Schema(implementation = Tool.class))),
            @ApiResponse(responseCode = "400", description = "Specified id does not correspond to the id of the instance")})
    @PreAuthorize("hasAuthority('" + Permissions.TOOL_RECORDS_WRITE + "')")
    @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
    public Tool update(@Parameter(description = "Id of the instance", required = true)
                       @PathVariable("id") String id,
                       @Parameter(description = "Single instance", required = true)
                       @RequestBody ToolUpdateDto request) {
        eq(id, request.getId(), () -> new BadArgument("id"));
        return service.update(request);
    }


    @Autowired
    public void setService(ToolService service) {
        this.service = service;
    }
}
