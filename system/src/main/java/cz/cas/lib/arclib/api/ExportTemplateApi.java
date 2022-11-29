package cz.cas.lib.arclib.api;

import cz.cas.lib.arclib.domain.export.ExportTemplate;
import cz.cas.lib.arclib.domainbase.exception.BadArgument;
import cz.cas.lib.arclib.domainbase.exception.MissingObject;
import cz.cas.lib.arclib.security.authorization.permission.Permissions;
import cz.cas.lib.arclib.service.ExportTemplateService;
import io.swagger.annotations.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Collection;

import static cz.cas.lib.core.util.Utils.eq;
import static cz.cas.lib.core.util.Utils.notNull;

@RestController
@Api(value = "export template")
@RequestMapping("/api/export_template")
public class ExportTemplateApi {
    private ExportTemplateService service;

    @ApiOperation(value = "Saves an instance. [Perm.EXPORT_TEMPLATE_WRITE]",
            notes = "Returns single instance (possibly with computed attributes)",
            response = ExportTemplate.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = ExportTemplate.class),
            @ApiResponse(code = 400, message = "Specified id does not correspond to the id of the instance")})
    @PreAuthorize("hasAuthority('" + Permissions.EXPORT_ROUTINE_WRITE + "')")
    @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
    public ExportTemplate save(@ApiParam(value = "Id of the instance", required = true) @PathVariable("id") String id,
                               @ApiParam(value = "Single instance", required = true) @RequestBody ExportTemplate request) throws IOException {
        eq(id, request.getId(), () -> new BadArgument("id"));
        return service.save(request);
    }

    @ApiOperation(value = "Deletes an instance. [Perm.EXPORT_TEMPLATE_WRITE]")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response"),
            @ApiResponse(code = 404, message = "Instance does not exist")})
    @PreAuthorize("hasAuthority('" + Permissions.EXPORT_TEMPLATE_WRITE + "')")
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public void delete(@ApiParam(value = "Id of the instance", required = true) @PathVariable("id") String id) {
        ExportTemplate entity = service.find(id);
        notNull(entity, () -> new MissingObject(ExportTemplate.class, id));

        service.delete(entity);
    }

    @ApiOperation(value = "Gets one instance specified by id [Perm.EXPORT_TEMPLATE_READ]",
            notes = "Returns deleted entities as well.", response = ExportTemplate.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = ExportTemplate.class),
            @ApiResponse(code = 404, message = "Instance does not exist")})
    @PreAuthorize("hasAuthority('" + Permissions.EXPORT_TEMPLATE_READ + "')")
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public ExportTemplate get(@ApiParam(value = "Id of the instance", required = true) @PathVariable("id") String id) {
        ExportTemplate entity = service.findWithDeletedFilteringOff(id);
        notNull(entity, () -> new MissingObject(ExportTemplate.class, id));

        return entity;
    }

    @ApiOperation(value = "Gets DTOs of all instances [Perm.EXPORT_TEMPLATE_READ]",
            notes = "if the calling user is not [Perm.SUPER_ADMIN_PRIVILEGE] only ExportTemplates assigned to the user's producer are returned. ",
            response = ExportTemplate.class)
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful response", response = ExportTemplate.class)})
    @PreAuthorize("hasAuthority('" + Permissions.EXPORT_TEMPLATE_READ + "')")
    @RequestMapping(path = "/list_dtos", method = RequestMethod.GET)
    public Collection<ExportTemplate> listDtos() {
        return service.listExportTemplateDtos();
    }

    @Inject
    public void setService(ExportTemplateService service) {
        this.service = service;
    }
}
