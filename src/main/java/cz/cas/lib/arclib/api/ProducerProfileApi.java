package cz.cas.lib.arclib.api;

import cz.cas.lib.arclib.domain.profiles.ProducerProfile;
import cz.cas.lib.arclib.security.authorization.Roles;
import cz.cas.lib.arclib.security.user.UserDetails;
import cz.cas.lib.arclib.store.ProducerProfileStore;
import cz.cas.lib.core.exception.BadArgument;
import cz.cas.lib.core.exception.ForbiddenObject;
import cz.cas.lib.core.exception.MissingObject;
import cz.cas.lib.core.index.dto.Filter;
import cz.cas.lib.core.index.dto.FilterOperation;
import cz.cas.lib.core.index.dto.Params;
import cz.cas.lib.core.index.dto.Result;
import cz.cas.lib.core.store.Transactional;
import io.swagger.annotations.*;
import lombok.Getter;
import org.springframework.web.bind.annotation.*;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import java.util.Collection;

import static cz.cas.lib.arclib.utils.ArclibUtils.hasRole;
import static cz.cas.lib.core.util.Utils.*;

@RestController
@Api(value = "producer-profile", description = "Api for interaction with producer profiles")
@RequestMapping("/api/producer_profile")
public class ProducerProfileApi {
    @Getter
    private ProducerProfileStore store;

    private UserDetails userDetails;

    @ApiOperation(value = "Saves an instance. Roles.SUPER_ADMIN", notes = "Returns single instance (possibly with computed attributes)",
            response = ProducerProfile.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = ProducerProfile.class),
            @ApiResponse(code = 400, message = "Specified id does not correspond to the id of the instance"),
            @ApiResponse(code = 403, message = "Id of the producer does not match the id of the producer of the logged on user.")})
    @RolesAllowed({Roles.SUPER_ADMIN})
    @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
    @Transactional
    public ProducerProfile save(@ApiParam(value = "Id of the instance", required = true)
                                @PathVariable("id") String id,
                                @ApiParam(value = "Single instance", required = true)
                                @RequestBody ProducerProfile request) {
        eq(id, request.getId(), () -> new BadArgument("id"));

        return store.save(request);
    }

    @ApiOperation(value = "Updates an instance. Roles.SUPER_ADMIN, Roles.ADMIN",
            notes = "Returns single instance (possibly with computed attributes). If the calling user is not Roles.SUPER_ADMIN" +
                    " the producer of the user must match the producer of the updated producer profile.",
            response = ProducerProfile.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = ProducerProfile.class),
            @ApiResponse(code = 400, message = "Specified id does not correspond to the id of the instance"),
            @ApiResponse(code = 403, message = "Id of the producer does not match the id of the producer of the logged on user.")})
    @RolesAllowed({Roles.SUPER_ADMIN, Roles.ADMIN})
    @RequestMapping(value = "/update/{id}", method = RequestMethod.PUT)
    @Transactional
    public ProducerProfile update(@ApiParam(value = "Id of the instance", required = true)
                                  @PathVariable("id") String id,
                                  @ApiParam(value = "Single instance", required = true)
                                  @RequestBody ProducerProfile request) {
        eq(id, request.getId(), () -> new BadArgument("id"));

        if (!hasRole(userDetails, Roles.SUPER_ADMIN) &&
                !userDetails.getProducerId().equals(request.getProducer().getId())) {
            throw new ForbiddenObject(ProducerProfile.class, id);
        }
        return store.save(request);
    }

    @ApiOperation(value = "Deletes an instance. Roles.SUPER_ADMIN, Roles.ADMIN",
            notes = "If the calling user is not Roles.SUPER_ADMIN the producer of the user must match the producer " +
                    "of the deleted producer profile.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response"),
            @ApiResponse(code = 404, message = "Instance does not exist")})
    @RolesAllowed({Roles.SUPER_ADMIN, Roles.ADMIN})
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    @Transactional
    public void delete(@ApiParam(value = "Id of the instance", required = true)
                       @PathVariable("id") String id) {
        ProducerProfile producerProfile = store.find(id);
        notNull(producerProfile, () -> new MissingObject(store.getType(), id));

        if (!hasRole(userDetails, Roles.SUPER_ADMIN) &&
                !userDetails.getProducerId().equals(producerProfile.getProducer().getId())) {
            throw new ForbiddenObject(ProducerProfile.class, id);
        }
        store.delete(producerProfile);
    }

    @ApiOperation(value = "Gets one instance specified by id", response = ProducerProfile.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = ProducerProfile.class),
            @ApiResponse(code = 404, message = "Instance does not exist")})
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    @Transactional
    public ProducerProfile get(@ApiParam(value = "Id of the instance", required = true)
                               @PathVariable("id") String id) {
        ProducerProfile entity = store.find(id);
        notNull(entity, () -> new MissingObject(store.getType(), id));

        return entity;
    }

    @ApiOperation(value = "Gets all instances", notes = "If the calling user is not Roles.SUPER_ADMIN only the producer profiles" +
            " with producer same as the producer of calling user are returned.", response = Collection.class)
    @ApiResponses({@ApiResponse(code = 200, message = "Successful response", response = Collection.class)})
    @RequestMapping(path = "/all", method = RequestMethod.GET)
    @Transactional
    public Collection<ProducerProfile> listAll() {
        Params params = new Params();
        params.setPageSize(1000);

        if (!hasRole(userDetails, Roles.SUPER_ADMIN)) {
            addPrefilter(params, new Filter("producerId", FilterOperation.EQ, userDetails.getProducerId(), null));
        }
        Result<ProducerProfile> all = store.findAll(params);
        return all.getItems();
    }

    @ApiOperation(value = "Gets all instances that respect the selected parameters",
            notes = "If the calling user is not Roles.SUPER_ADMIN only the producer profiles" +
                    " with producer same as the producer of calling user are returned. \n" +
                    "Filter/Sort fields = id, name, created, updated, producerId, ingestWorkflowExternalId," +
                    " producerName, producerTransferAreaPath, sipProfileId, sipProfileName, hashType," +
                    " workflowDefinitionId, workflowDefinitionName, validationProfileId, validationProfileName, debuggingModeActive",
            response = Result.class)
    @ApiResponses({@ApiResponse(code = 200, message = "Successful response", response = Result.class)})
    @RequestMapping(method = RequestMethod.GET)
    @Transactional
    public Result<ProducerProfile> list(@ApiParam(value = "Parameters to comply with", required = true)
                                        @ModelAttribute Params params) {
        if (!hasRole(userDetails, Roles.SUPER_ADMIN)) {
            addPrefilter(params, new Filter("producerId", FilterOperation.EQ, userDetails.getProducerId(), null));
        }
        return store.findAll(params);
    }

    @Inject
    public void setStore(ProducerProfileStore store) {
        this.store = store;
    }

    @Inject
    public void setUserDetails(UserDetails userDetails) {
        this.userDetails = userDetails;
    }
}
