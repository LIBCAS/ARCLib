package cz.cas.lib.arclib.formatlibrary.api;

import cz.cas.lib.arclib.domainbase.exception.BadArgument;
import cz.cas.lib.arclib.domainbase.exception.MissingObject;
import cz.cas.lib.arclib.formatlibrary.Permissions;
import cz.cas.lib.arclib.formatlibrary.domain.Format;
import cz.cas.lib.arclib.formatlibrary.domain.FormatDefinition;
import cz.cas.lib.arclib.formatlibrary.service.FormatDefinitionService;
import io.swagger.annotations.*;
import lombok.Getter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;

import static cz.cas.lib.arclib.domainbase.util.DomainBaseUtils.eq;
import static cz.cas.lib.arclib.domainbase.util.DomainBaseUtils.notNull;

@RestController
@Api(value = "format definition", description = "Api for interaction with format definitions")
@RequestMapping("/api/format_definition")
public class FormatDefinitionApi {
    @Getter
    private FormatDefinitionService service;

    @ApiOperation(value = "Saves or updates an instance. [Perm.FORMAT_RECORDS_WRITE]",
            notes = "Returns single instance (possibly with computed attributes).",
            response = Format.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = FormatDefinition.class),
            @ApiResponse(code = 400, message = "Specified id does not correspond to the id of the instance")})
    @PreAuthorize("hasAuthority('" + Permissions.FORMAT_RECORDS_WRITE + "')")
    @PutMapping(value = "/{id}")
    @Transactional
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

    @ApiOperation(value = "Gets one instance specified by id [Perm.FORMAT_RECORDS_READ]", response = FormatDefinition.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = FormatDefinition.class),
            @ApiResponse(code = 404, message = "Instance does not exist")})
    @PreAuthorize("hasAuthority('" + Permissions.FORMAT_RECORDS_READ + "')")
    @GetMapping(value = "/{id}")
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
