package cz.cas.lib.arclib.formatlibrary.api;

import cz.cas.lib.arclib.domainbase.exception.BadArgument;
import cz.cas.lib.arclib.domainbase.exception.MissingObject;
import cz.cas.lib.arclib.formatlibrary.Permissions;
import cz.cas.lib.arclib.formatlibrary.domain.Format;
import cz.cas.lib.arclib.formatlibrary.domain.FormatDefinition;
import cz.cas.lib.arclib.formatlibrary.service.FormatDefinitionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;



import static cz.cas.lib.arclib.domainbase.util.DomainBaseUtils.eq;
import static cz.cas.lib.arclib.domainbase.util.DomainBaseUtils.notNull;

@RestController
@Tag(name = "format definition", description = "Api for interaction with format definitions")
@RequestMapping("/api/format_definition")
public class FormatDefinitionApi {

    private FormatDefinitionService service;

    @Operation(summary = "Saves or updates an instance. [Perm.FORMAT_RECORDS_WRITE]",
            description = "Returns single instance (possibly with computed attributes).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful response", content = @Content(schema = @Schema(implementation = FormatDefinition.class))),
            @ApiResponse(responseCode = "400", description = "Specified id does not correspond to the id of the instance")})
    @PreAuthorize("hasAuthority('" + Permissions.FORMAT_RECORDS_WRITE + "')")
    @PutMapping(value = "/{id}")
    @Transactional
    public FormatDefinition save(@Parameter(description = "Id of the instance", required = true) @PathVariable("id") String id,
                                 @Parameter(description = "Single instance", required = true) @RequestBody FormatDefinition receivedFormatDef) {
        eq(id, receivedFormatDef.getId(), () -> new BadArgument("id"));
        FormatDefinition formatDefFromDb = service.find(receivedFormatDef.getId());
        if (formatDefFromDb != null)
            return service.update(receivedFormatDef);
        return service.create(receivedFormatDef);
    }

    @Operation(summary = "Gets one instance specified by id [Perm.FORMAT_RECORDS_READ]")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response", content = @Content(schema = @Schema(implementation = FormatDefinition.class))),
            @ApiResponse(responseCode = "404", description = "Instance does not exist")})
    @PreAuthorize("hasAuthority('" + Permissions.FORMAT_RECORDS_READ + "')")
    @GetMapping(value = "/{id}")
    public FormatDefinition get(@Parameter(description = "Id of the instance", required = true) @PathVariable("id") String id) {
        FormatDefinition entity = service.find(id);
        notNull(entity, () -> new MissingObject(FormatDefinition.class, id));

        return entity;
    }

    @Autowired
    public void setService(FormatDefinitionService service) {
        this.service = service;
    }
}
