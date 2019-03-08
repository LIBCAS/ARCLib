package cz.cas.lib.arclib.api;

import cz.cas.lib.arclib.domain.preservationPlanning.Format;
import cz.cas.lib.arclib.security.authorization.Roles;
import cz.cas.lib.arclib.store.FormatStore;
import cz.cas.lib.core.exception.BadArgument;
import cz.cas.lib.core.exception.MissingObject;
import cz.cas.lib.core.index.dto.Params;
import cz.cas.lib.core.index.dto.Result;
import cz.cas.lib.core.store.Transactional;
import io.swagger.annotations.*;
import lombok.Getter;
import org.springframework.web.bind.annotation.*;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;

import static cz.cas.lib.core.util.Utils.eq;
import static cz.cas.lib.core.util.Utils.notNull;

@RestController
@Api(value = "format", description = "Api for interaction with formats")
@RequestMapping("/api/format")
public class FormatApi {
    @Getter
    private FormatStore store;

    @ApiOperation(value = "Saves or updates an instance. Roles.SUPER_ADMIN",
            notes = "Returns single instance (possibly with computed attributes).",
            response = Format.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = Format.class),
            @ApiResponse(code = 400, message = "Specified id does not correspond to the id of the instance")})
    @RolesAllowed({Roles.SUPER_ADMIN})
    @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
    @Transactional
    public Format save(@ApiParam(value = "Id of the instance", required = true)
                       @PathVariable("id") String id,
                       @ApiParam(value = "Single instance", required = true)
                       @RequestBody Format format) {
        eq(id, format.getId(), () -> new BadArgument("id"));

        return store.save(format);
    }

    @ApiOperation(value = "Gets one instance specified by id", response = Format.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = Format.class),
            @ApiResponse(code = 404, message = "Instance does not exist")})
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    @Transactional
    public Format get(@ApiParam(value = "Id of the instance", required = true)
                      @PathVariable("id") String id) {
        Format entity = store.find(id);
        notNull(entity, () -> new MissingObject(store.getType(), id));

        return entity;
    }

    @ApiOperation(value = "Gets all instances that respect the selected parameters",
            notes = "Filter/Sort fields = id, formatId, puid, formatName, formatVersion, internalVersionNumber," +
                    " localDefinition, preferred, internalInformationFilled",
            response = Result.class)
    @ApiResponses({@ApiResponse(code = 200, message = "Successful response", response = Result.class)})
    @RequestMapping(method = RequestMethod.GET)
    public Result<Format> list(@ApiParam(value = "Parameters to comply with", required = true)
                               @ModelAttribute Params params) {
        return store.findAll(params);
    }

    @Inject
    public void setStore(FormatStore store) {
        this.store = store;
    }
}
