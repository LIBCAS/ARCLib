package cz.cas.lib.arclib.formatlibrary.api;

import cz.cas.lib.arclib.domainbase.exception.BadArgument;
import cz.cas.lib.arclib.domainbase.exception.MissingObject;
import cz.cas.lib.arclib.domainbase.util.DomainBaseUtils;
import cz.cas.lib.arclib.formatlibrary.Permissions;
import cz.cas.lib.arclib.formatlibrary.domain.Format;
import cz.cas.lib.arclib.formatlibrary.store.FormatStore;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;



import static cz.cas.lib.arclib.domainbase.util.DomainBaseUtils.eq;

@RestController
@Tag(name = "format", description = "Api for interaction with formats")
@RequestMapping("/api/format")
public class FormatApi {
    @Getter
    private FormatStore store;

    @Operation(summary = "Saves or updates an instance. [Perm.FORMAT_RECORDS_WRITE]",
            description = "Returns single instance (possibly with computed attributes).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response", content = @Content(schema = @Schema(implementation = Format.class))),
            @ApiResponse(responseCode = "400", description = "Specified id does not correspond to the id of the instance")})
    @PreAuthorize("hasAuthority('" + Permissions.FORMAT_RECORDS_WRITE + "')")
    @PutMapping(value = "/{id}")
    @Transactional
    public Format save(@Parameter(description = "Id of the instance", required = true)
                       @PathVariable("id") String id,
                       @Parameter(description = "Single instance", required = true)
                       @RequestBody Format format) {
        eq(id, format.getId(), () -> new BadArgument("id"));
        Format format1 = store.find(id);
        if (format1 != null)
            return store.update(format);
        return store.create(format);
    }

    @Operation(summary = "Gets one instance specified by formatId [Perm.FORMAT_RECORDS_READ]")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response", content = @Content(schema = @Schema(implementation = Format.class))),
            @ApiResponse(responseCode = "404", description = "Instance does not exist")})
    @PreAuthorize("hasAuthority('" + Permissions.FORMAT_RECORDS_READ + "')")
    @GetMapping(value = "/{formatId}")
    @Transactional
    public Format get(@Parameter(description = "formatId of the instance", required = true)
                      @PathVariable("formatId") Integer id) {
        Format entity = store.findByFormatId((id));
        DomainBaseUtils.notNull(entity, () -> new MissingObject(Format.class, String.valueOf(id)));

        return entity;
    }

    @Autowired
    public void setStore(FormatStore store) {
        this.store = store;
    }
}
