package cz.cas.lib.arclib.api;

import cz.cas.lib.arclib.domain.export.ExportTemplate;
import cz.cas.lib.arclib.domainbase.exception.BadArgument;
import cz.cas.lib.arclib.domainbase.exception.MissingObject;
import cz.cas.lib.arclib.security.authorization.permission.Permissions;
import cz.cas.lib.arclib.service.ExportTemplateService;
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

import java.io.IOException;
import java.util.Collection;

import static cz.cas.lib.core.util.Utils.eq;
import static cz.cas.lib.core.util.Utils.notNull;

@RestController
@Tag(name = "export template")
@RequestMapping("/api/export_template")
public class ExportTemplateApi {
    private ExportTemplateService service;

    @Operation(summary = "Saves an instance. [Perm.EXPORT_TEMPLATE_WRITE]",
            description = "Returns single instance (possibly with computed attributes)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response", content = @Content(schema = @Schema(implementation = ExportTemplate.class))),
            @ApiResponse(responseCode = "400", description = "Specified id does not correspond to the id of the instance")})
    @PreAuthorize("hasAuthority('" + Permissions.EXPORT_ROUTINE_WRITE + "')")
    @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
    public ExportTemplate save(@Parameter(description = "Id of the instance", required = true) @PathVariable("id") String id,
                               @Parameter(description = "Single instance", required = true) @RequestBody ExportTemplate request) throws IOException {
        eq(id, request.getId(), () -> new BadArgument("id"));
        return service.save(request);
    }

    @Operation(summary = "Deletes an instance. [Perm.EXPORT_TEMPLATE_WRITE]")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response"),
            @ApiResponse(responseCode = "404", description = "Instance does not exist")})
    @PreAuthorize("hasAuthority('" + Permissions.EXPORT_TEMPLATE_WRITE + "')")
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public void delete(@Parameter(description = "Id of the instance", required = true) @PathVariable("id") String id) {
        ExportTemplate entity = service.find(id);
        notNull(entity, () -> new MissingObject(ExportTemplate.class, id));

        service.delete(entity);
    }

    @Operation(summary = "Gets one instance specified by id [Perm.EXPORT_TEMPLATE_READ]",
            description = "Returns deleted entities as well.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response", content = @Content(schema = @Schema(implementation = ExportTemplate.class))),
            @ApiResponse(responseCode = "404", description = "Instance does not exist")})
    @PreAuthorize("hasAuthority('" + Permissions.EXPORT_TEMPLATE_READ + "')")
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public ExportTemplate get(@Parameter(description = "Id of the instance", required = true) @PathVariable("id") String id) {
        ExportTemplate entity = service.findWithDeletedFilteringOff(id);
        notNull(entity, () -> new MissingObject(ExportTemplate.class, id));

        return entity;
    }

    @Operation(summary = "Gets DTOs of all instances [Perm.EXPORT_TEMPLATE_READ]",
            description = "if the calling user is not [Perm.SUPER_ADMIN_PRIVILEGE] only ExportTemplates assigned to the user's producer are returned. ")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Successful response", content = @Content(schema = @Schema(implementation = ExportTemplate.class)))})
    @PreAuthorize("hasAuthority('" + Permissions.EXPORT_TEMPLATE_READ + "')")
    @RequestMapping(path = "/list_dtos", method = RequestMethod.GET)
    public Collection<ExportTemplate> listDtos() {
        return service.listExportTemplateDtos();
    }

    @Autowired
    public void setService(ExportTemplateService service) {
        this.service = service;
    }
}
