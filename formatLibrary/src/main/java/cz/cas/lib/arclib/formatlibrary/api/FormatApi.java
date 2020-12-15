package cz.cas.lib.arclib.formatlibrary.api;

import cz.cas.lib.arclib.domainbase.exception.BadArgument;
import cz.cas.lib.arclib.domainbase.exception.MissingObject;
import cz.cas.lib.arclib.domainbase.util.DomainBaseUtils;
import cz.cas.lib.arclib.formatlibrary.Permissions;
import cz.cas.lib.arclib.formatlibrary.domain.Format;
import cz.cas.lib.arclib.formatlibrary.store.FormatStore;
import io.swagger.annotations.*;
import lombok.Getter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;

import static cz.cas.lib.arclib.domainbase.util.DomainBaseUtils.eq;

@RestController
@Api(value = "format", description = "Api for interaction with formats")
@RequestMapping("/api/format")
public class FormatApi {
    @Getter
    private FormatStore store;

    @ApiOperation(value = "Saves or updates an instance. [Perm.FORMAT_RECORDS_WRITE]",
            notes = "Returns single instance (possibly with computed attributes).", response = Format.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = Format.class),
            @ApiResponse(code = 400, message = "Specified id does not correspond to the id of the instance")})
    @PreAuthorize("hasAuthority('" + Permissions.FORMAT_RECORDS_WRITE + "')")
    @PutMapping(value = "/{id}")
    @Transactional
    public Format save(@ApiParam(value = "Id of the instance", required = true)
                       @PathVariable("id") String id,
                       @ApiParam(value = "Single instance", required = true)
                       @RequestBody Format format) {
        eq(id, format.getId(), () -> new BadArgument("id"));
        Format format1 = store.find(id);
        if (format1 != null)
            return store.update(format);
        return store.create(format);
    }

    @ApiOperation(value = "Gets one instance specified by formatId [Perm.FORMAT_RECORDS_READ]", response = Format.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = Format.class),
            @ApiResponse(code = 404, message = "Instance does not exist")})
    @PreAuthorize("hasAuthority('" + Permissions.FORMAT_RECORDS_READ + "')")
    @GetMapping(value = "/{formatId}")
    @Transactional
    public Format get(@ApiParam(value = "formatId of the instance", required = true)
                      @PathVariable("formatId") Integer id) {
        Format entity = store.findByFormatId((id));
        DomainBaseUtils.notNull(entity, () -> new MissingObject(Format.class, String.valueOf(id)));

        return entity;
    }

    @Inject
    public void setStore(FormatStore store) {
        this.store = store;
    }
}
