package cz.cas.lib.arclib.formatlibrary.api;

import cz.cas.lib.arclib.domainbase.exception.BadArgument;
import cz.cas.lib.arclib.domainbase.exception.MissingObject;
import cz.cas.lib.arclib.formatlibrary.domain.Format;
import cz.cas.lib.arclib.formatlibrary.domain.FormatDefinition;
import cz.cas.lib.arclib.formatlibrary.service.FormatDefinitionService;
import io.swagger.annotations.*;
import lombok.Getter;
import org.springframework.web.bind.annotation.*;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;

import static cz.cas.lib.arclib.domainbase.util.DomainBaseUtils.eq;
import static cz.cas.lib.arclib.domainbase.util.DomainBaseUtils.notNull;

@RestController
@Api(value = "format definition", description = "Api for interaction with format definitions")
@RequestMapping("/api/format_definition")
public class FormatDefinitionApi {
    @Getter
    private FormatDefinitionService service;

    @ApiOperation(value = "Saves or updates an instance. Roles.SUPER_ADMIN",
            notes = "Returns single instance (possibly with computed attributes).",
            response = Format.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = Format.class),
            @ApiResponse(code = 400, message = "Specified id does not correspond to the id of the instance")})
    @RolesAllowed({Roles.SUPER_ADMIN})
    @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
    public FormatDefinition save(@ApiParam(value = "Id of the instance", required = true)
                                 @PathVariable("id") String id,
                                 @ApiParam(value = "Single instance", required = true)
                                 @RequestBody FormatDefinition formatDefinition) {
        eq(id, formatDefinition.getId(), () -> new BadArgument("id"));
        FormatDefinition formatDefinition1 = service.find(formatDefinition.getId());
        if (formatDefinition1 != null)
            return service.update(formatDefinition1);
        return service.create(formatDefinition);
    }

    @ApiOperation(value = "Gets one instance specified by id", response = FormatDefinition.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = FormatDefinition.class),
            @ApiResponse(code = 404, message = "Instance does not exist")})
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public FormatDefinition get(@ApiParam(value = "Id of the instance", required = true)
                                @PathVariable("id") String id) {
        FormatDefinition entity = service.find(id);
        notNull(entity, () -> new MissingObject(FormatDefinition.class, id));

        return entity;
    }

    @Inject
    public void setService(FormatDefinitionService service) {
        this.service = service;
    }
}
