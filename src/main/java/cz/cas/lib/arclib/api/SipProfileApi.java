package cz.cas.lib.arclib.api;

import cz.cas.lib.arclib.domain.profiles.SipProfile;
import cz.cas.lib.arclib.security.authorization.Roles;
import cz.cas.lib.arclib.store.SipProfileStore;
import cz.cas.lib.core.exception.BadArgument;
import cz.cas.lib.core.exception.MissingObject;
import cz.cas.lib.core.store.Transactional;
import io.swagger.annotations.*;
import lombok.Getter;
import org.springframework.web.bind.annotation.*;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import java.util.Collection;

import static cz.cas.lib.core.util.Utils.eq;
import static cz.cas.lib.core.util.Utils.notNull;

@RestController
@Api(value = "sip-profile", description = "Api for interaction with sip profiles")
@RequestMapping("/api/sip_profile")
public class SipProfileApi {
    @Getter
    private SipProfileStore store;

    @ApiOperation(value = "Saves an instance. Roles.SUPER_ADMIN, Roles.ADMIN",
            notes = "Returns single instance (possibly with computed attributes)",
            response = SipProfile.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = SipProfile.class),
            @ApiResponse(code = 400, message = "Specified id does not correspond to the id of the instance")})
    @RolesAllowed({Roles.SUPER_ADMIN, Roles.ADMIN})
    @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
    @Transactional
    public SipProfile save(@ApiParam(value = "Id of the instance", required = true)
                           @PathVariable("id") String id,
                           @ApiParam(value = "Single instance", required = true)
                           @RequestBody SipProfile request) {
        eq(id, request.getId(), () -> new BadArgument("id"));
        return store.save(request);
    }

    @ApiOperation(value = "Deletes an instance. Roles.SUPER_ADMIN")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response"),
            @ApiResponse(code = 404, message = "Instance does not exist")})
    @RolesAllowed({Roles.SUPER_ADMIN})
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    @Transactional
    public void delete(@ApiParam(value = "Id of the instance", required = true)
                       @PathVariable("id") String id) {
        SipProfile entity = store.find(id);
        notNull(entity, () -> new MissingObject(store.getType(), id));

        store.delete(entity);
    }

    @ApiOperation(value = "Gets one instance specified by id", response = SipProfile.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = SipProfile.class),
            @ApiResponse(code = 404, message = "Instance does not exist")})
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    @Transactional
    public SipProfile get(@ApiParam(value = "Id of the instance", required = true)
                          @PathVariable("id") String id) {
        SipProfile entity = store.find(id);
        notNull(entity, () -> new MissingObject(store.getType(), id));

        return entity;
    }

    @ApiOperation(value = "Gets all instances", response = Collection.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = Collection.class)})
    @RequestMapping(method = RequestMethod.GET)
    @Transactional
    public Collection<SipProfile> list() {
        return store.findAll();
    }

    @Inject
    public void setStore(SipProfileStore store) {
        this.store = store;
    }
}
