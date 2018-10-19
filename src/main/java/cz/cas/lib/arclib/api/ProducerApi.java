package cz.cas.lib.arclib.api;

import cz.cas.lib.arclib.domain.Producer;
import cz.cas.lib.arclib.security.authorization.Roles;
import cz.cas.lib.arclib.store.ProducerStore;
import cz.cas.lib.core.exception.BadArgument;
import cz.cas.lib.core.exception.ConflictObject;
import cz.cas.lib.core.exception.MissingObject;
import cz.cas.lib.core.index.dto.Result;
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
@Api(value = "producer", description = "Api for interaction with producers")
@RequestMapping("/api/producer")
public class ProducerApi {
    @Getter
    private ProducerStore store;

    @ApiOperation(value = "Saves an " +
            "instance. Roles.ADMIN, Roles.SUPER_ADMIN", notes = "Returns single instance (possibly with computed attributes)",
            response = Producer.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = Producer.class),
            @ApiResponse(code = 400, message = "Specified id does not correspond to the id of the instance")})
    @RolesAllowed({Roles.ADMIN, Roles.SUPER_ADMIN})
    @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
    @Transactional
    public Producer save(@ApiParam(value = "Id of the instance", required = true)
                         @PathVariable("id") String id,
                         @ApiParam(value = "Single instance", required = true)
                         @RequestBody Producer request) {
        eq(id, request.getId(), () -> new BadArgument("id"));

        Producer producerByName = store.findByName(request.getName());
        if (producerByName != null && !producerByName.getId().equals(request.getId()))
            throw new ConflictObject(Producer.class, request.getName());

        return store.save(request);
    }

    @ApiOperation(value = "Deletes an instance. Roles.ADMIN, Roles.SUPER_ADMIN")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response"),
            @ApiResponse(code = 404, message = "Instance does not exist")})
    @RolesAllowed({Roles.ADMIN, Roles.SUPER_ADMIN})
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    @Transactional
    public void delete(@ApiParam(value = "Id of the instance", required = true)
                       @PathVariable("id") String id) {
        Producer entity = store.find(id);
        notNull(entity, () -> new MissingObject(store.getType(), id));

        store.delete(entity);
    }

    @ApiOperation(value = "Gets one instance specified by id. Roles.ADMIN, Roles.SUPER_ADMIN", response = Producer.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = Producer.class),
            @ApiResponse(code = 404, message = "Instance does not exist")})
    @RolesAllowed({Roles.ADMIN, Roles.SUPER_ADMIN})
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    @Transactional
    public Producer get(@ApiParam(value = "Id of the instance", required = true)
                        @PathVariable("id") String id) {
        Producer entity = store.find(id);
        notNull(entity, () -> new MissingObject(store.getType(), id));

        return entity;
    }

    @ApiOperation(value = "Gets all instances", response = Result.class)
    @ApiResponses({@ApiResponse(code = 200, message = "Successful response", response = Collection.class)})
    @RequestMapping(method = RequestMethod.GET)
    @RolesAllowed({Roles.SUPER_ADMIN})
    @Transactional
    public Collection<Producer> list() {
        return store.findAll();
    }

    @Inject
    public void setStore(ProducerStore store) {
        this.store = store;
    }
}
