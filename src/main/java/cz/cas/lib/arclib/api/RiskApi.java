package cz.cas.lib.arclib.api;

import cz.cas.lib.arclib.domain.preservationPlanning.Risk;
import cz.cas.lib.arclib.security.authorization.Roles;
import cz.cas.lib.arclib.security.user.UserDetails;
import cz.cas.lib.arclib.store.RiskStore;
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
@Api(value = "risk", description = "Api for interaction with risks")
@RequestMapping("/api/risk")
public class RiskApi {
    @Getter
    private RiskStore store;

    private UserDetails userDetails;

    @ApiOperation(value = "Saves an instance. Roles.SUPER_ADMIN", notes = "Returns single instance (possibly with computed attributes)",
            response = Risk.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = Risk.class),
            @ApiResponse(code = 400, message = "Specified id does not correspond to the id of the instance")})
    @RolesAllowed({Roles.SUPER_ADMIN})
    @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
    @Transactional
    public Risk save(@ApiParam(value = "Id of the instance", required = true)
                     @PathVariable("id") String id,
                     @ApiParam(value = "Single instance", required = true)
                     @RequestBody Risk request) {
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
        Risk risk = store.find(id);
        notNull(risk, () -> new MissingObject(store.getType(), id));

        store.delete(risk);
    }

    @ApiOperation(value = "Gets one instance specified by id", response = Risk.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = Risk.class),
            @ApiResponse(code = 404, message = "Instance does not exist")})
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    @Transactional
    public Risk get(@ApiParam(value = "Id of the instance", required = true)
                    @PathVariable("id") String id) {
        Risk entity = store.find(id);
        notNull(entity, () -> new MissingObject(store.getType(), id));

        return entity;
    }

    @ApiOperation(value = "Gets all instances that respect the selected parameters",
            response = Collection.class)
    @ApiResponses({@ApiResponse(code = 200, message = "Successful response", response = Collection.class)})
    @RequestMapping(method = RequestMethod.GET)
    @Transactional
    public Collection<Risk> list() {
        return store.findAll();
    }

    @Inject
    public void setStore(RiskStore store) {
        this.store = store;
    }

    @Inject
    public void setUserDetails(UserDetails userDetails) {
        this.userDetails = userDetails;
    }
}
