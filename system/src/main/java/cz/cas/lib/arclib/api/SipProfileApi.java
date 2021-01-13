package cz.cas.lib.arclib.api;

import cz.cas.lib.arclib.domain.profiles.SipProfile;
import cz.cas.lib.arclib.domainbase.exception.BadArgument;
import cz.cas.lib.arclib.domainbase.exception.MissingObject;
import cz.cas.lib.arclib.dto.SipProfileDto;
import cz.cas.lib.arclib.security.authorization.permission.Permissions;
import cz.cas.lib.arclib.service.SipProfileService;
import io.swagger.annotations.*;
import lombok.Getter;
import org.dom4j.DocumentException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import java.util.Collection;

import static cz.cas.lib.core.util.Utils.eq;
import static cz.cas.lib.core.util.Utils.notNull;

@RestController
@Api(value = "sip profile", description = "Api for interaction with sip profiles")
@RequestMapping("/api/sip_profile")
public class SipProfileApi {
    @Getter
    private SipProfileService service;

    @ApiOperation(value = "Saves an instance. [Perm.SIP_PROFILE_RECORDS_WRITE]",
            notes = "Returns single instance (possibly with computed attributes)",
            response = SipProfile.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = SipProfile.class),
            @ApiResponse(code = 400, message = "Specified id does not correspond to the id of the instance")})
    @PreAuthorize("hasAuthority('" + Permissions.SIP_PROFILE_RECORDS_WRITE + "')")
    @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
    public SipProfile save(@ApiParam(value = "Id of the instance", required = true) @PathVariable("id") String id,
                           @ApiParam(value = "Single instance", required = true) @RequestBody SipProfile request) throws DocumentException {
        eq(id, request.getId(), () -> new BadArgument("id"));
        return service.save(request);
    }

    @ApiOperation(value = "Deletes an instance. [Perm.SIP_PROFILE_RECORDS_WRITE]")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response"),
            @ApiResponse(code = 404, message = "Instance does not exist")})
    @PreAuthorize("hasAuthority('" + Permissions.SIP_PROFILE_RECORDS_WRITE + "')")
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public void delete(@ApiParam(value = "Id of the instance", required = true) @PathVariable("id") String id) {
        SipProfile entity = service.find(id);
        notNull(entity, () -> new MissingObject(SipProfile.class, id));

        service.delete(entity);
    }

    @ApiOperation(value = "Gets one instance specified by id [Perm.SIP_PROFILE_RECORDS_READ]",
            notes = "Returns deleted entities as well.", response = SipProfile.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = SipProfile.class),
            @ApiResponse(code = 404, message = "Instance does not exist")})
    @PreAuthorize("hasAuthority('" + Permissions.SIP_PROFILE_RECORDS_READ + "')")
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public SipProfile get(@ApiParam(value = "Id of the instance", required = true) @PathVariable("id") String id) {
        SipProfile entity = service.findWithDeletedFilteringOff(id);
        notNull(entity, () -> new MissingObject(SipProfile.class, id));

        return entity;
    }

    @ApiOperation(value = "Gets DTOs of all instances [Perm.SIP_PROFILE_RECORDS_READ]",
            notes = "if the calling user is not [Perm.SUPER_ADMIN_PRIVILEGE] only SipProfiles assigned to the user's producer are returned. ",
            response = SipProfileDto.class)
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful response", response = SipProfileDto.class)})
    @PreAuthorize("hasAuthority('" + Permissions.SIP_PROFILE_RECORDS_READ + "')")
    @RequestMapping(value = "/list_dtos", method = RequestMethod.GET)
    public Collection<SipProfileDto> listDtos() {
        return service.listSipProfileDtos();
    }

    @Inject
    public void setService(SipProfileService service) {
        this.service = service;
    }
}
