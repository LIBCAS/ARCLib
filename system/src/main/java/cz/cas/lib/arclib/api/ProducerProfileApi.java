package cz.cas.lib.arclib.api;

import cz.cas.lib.arclib.domain.profiles.ProducerProfile;
import cz.cas.lib.arclib.domainbase.exception.BadArgument;
import cz.cas.lib.arclib.domainbase.exception.ForbiddenObject;
import cz.cas.lib.arclib.domainbase.exception.MissingObject;
import cz.cas.lib.arclib.dto.ProducerProfileDto;
import cz.cas.lib.arclib.security.authorization.permission.Permissions;
import cz.cas.lib.arclib.security.user.UserDetails;
import cz.cas.lib.arclib.service.ProducerProfileService;
import cz.cas.lib.core.index.dto.Filter;
import cz.cas.lib.core.index.dto.FilterOperation;
import cz.cas.lib.core.index.dto.Params;
import cz.cas.lib.core.index.dto.Result;
import io.swagger.annotations.*;
import lombok.Getter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import javax.validation.Valid;

import static cz.cas.lib.arclib.utils.ArclibUtils.hasRole;
import static cz.cas.lib.core.util.Utils.*;

@RestController
@Api(value = "producer profile", description = "Api for interaction with producer profiles")
@RequestMapping("/api/producer_profile")
public class ProducerProfileApi {
    @Getter
    private ProducerProfileService producerProfileService;

    private UserDetails userDetails;

    @ApiOperation(value = "Saves an instance. [Perm.PRODUCER_PROFILE_RECORDS_WRITE]",
            notes = "Returns single instance (possibly with computed attributes). If the calling user is not Roles.SUPER_ADMIN" +
                    " the producer of the user must match the producer of the producer profile.",
            response = ProducerProfile.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = ProducerProfile.class),
            @ApiResponse(code = 400, message = "Specified id does not correspond to the id of the instance"),
            @ApiResponse(code = 403, message = "Id of the producer does not match the id of the producer of the logged on user.")})
    @PreAuthorize("hasAuthority('" + Permissions.PRODUCER_PROFILE_RECORDS_WRITE + "')")
    @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
    public ProducerProfile save(@ApiParam(value = "Id of the instance", required = true)
                                  @PathVariable("id") String id,
                                  @ApiParam(value = "Single instance", required = true)
                                  @RequestBody @Valid ProducerProfile request) {
        eq(id, request.getId(), () -> new BadArgument("id"));

        if (!hasRole(userDetails, Permissions.SUPER_ADMIN_PRIVILEGE) &&
                !userDetails.getProducerId().equals(request.getProducer().getId())) {
            throw new ForbiddenObject(ProducerProfile.class, id);
        }
        return producerProfileService.save(request);
    }

    @ApiOperation(value = "Deletes an instance. [Perm.PRODUCER_PROFILE_RECORDS_WRITE]",
            notes = "If the calling user is not Roles.SUPER_ADMIN the producer of the user must match the producer " +
                    "of the deleted producer profile.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response"),
            @ApiResponse(code = 404, message = "Instance does not exist")})
    @PreAuthorize("hasAuthority('" + Permissions.PRODUCER_PROFILE_RECORDS_WRITE + "')")
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public void delete(@ApiParam(value = "Id of the instance", required = true)
                       @PathVariable("id") String id) {
        ProducerProfile producerProfile = producerProfileService.find(id);
        notNull(producerProfile, () -> new MissingObject(producerProfileService.getType(), id));

        if (!hasRole(userDetails, Permissions.SUPER_ADMIN_PRIVILEGE) &&
                !userDetails.getProducerId().equals(producerProfile.getProducer().getId())) {
            throw new ForbiddenObject(ProducerProfile.class, id);
        }
        producerProfileService.delete(producerProfile);
    }

    @ApiOperation(value = "Gets one instance specified by id [Perm.PRODUCER_PROFILE_RECORDS_READ]",
            notes = "Returns deleted entities as well.", response = ProducerProfile.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = ProducerProfile.class),
            @ApiResponse(code = 404, message = "Instance does not exist")})
    @PreAuthorize("hasAuthority('" + Permissions.PRODUCER_PROFILE_RECORDS_READ + "')")
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public ProducerProfile get(@ApiParam(value = "Id of the instance", required = true)
                               @PathVariable("id") String id) {
        ProducerProfile entity = producerProfileService.findWithDeletedFilteringOff(id);
        notNull(entity, () -> new MissingObject(producerProfileService.getType(), id));

        return entity;
    }

    @ApiOperation(value = "Gets DTOs of all instances that respect the selected parameters [Perm.PRODUCER_PROFILE_RECORDS_READ]",
            notes = "If the calling user is not Roles.SUPER_ADMIN only the producer profiles" +
                    " with producer same as the producer of calling user are returned. \n" +
                    "Filter/Sort fields = id, name, created, updated, producerId, ingestWorkflowExternalId," +
                    " producerName, producerTransferAreaPath, sipProfileId, sipProfileName, hashType," +
                    " workflowDefinitionId, workflowDefinitionName, validationProfileId, validationProfileName, debuggingModeActive",
            response = Result.class)
    @ApiResponses({@ApiResponse(code = 200, message = "Successful response", response = Result.class)})
    @PreAuthorize("hasAuthority('" + Permissions.PRODUCER_PROFILE_RECORDS_READ + "')")
    @RequestMapping(value = "/list_dtos", method = RequestMethod.GET)
    public Result<ProducerProfileDto> listDtos(@ApiParam(value = "Parameters to comply with", required = true)
                                               @ModelAttribute Params params) {
        if (!hasRole(userDetails, Permissions.SUPER_ADMIN_PRIVILEGE)) {
            addPrefilter(params, new Filter("producerId", FilterOperation.EQ, userDetails.getProducerId(), null));
        }
        return producerProfileService.listProducerProfileDtos(params);
    }

    @Inject
    public void setProducerProfileService(ProducerProfileService producerProfileService) {
        this.producerProfileService = producerProfileService;
    }

    @Inject
    public void setUserDetails(UserDetails userDetails) {
        this.userDetails = userDetails;
    }
}
