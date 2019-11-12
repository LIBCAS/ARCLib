package cz.cas.lib.arclib.api;

import cz.cas.lib.arclib.domain.profiles.SipProfile;
import cz.cas.lib.arclib.dto.SipProfileDto;
import cz.cas.lib.arclib.security.authorization.Roles;
import cz.cas.lib.arclib.service.SipProfileService;
import cz.cas.lib.arclib.domainbase.exception.BadArgument;
import cz.cas.lib.arclib.domainbase.exception.MissingObject;
import io.swagger.annotations.*;
import lombok.Getter;
import org.dom4j.DocumentException;
import org.springframework.web.bind.annotation.*;

import javax.annotation.security.RolesAllowed;
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

    @ApiOperation(value = "Saves an instance. Roles.SUPER_ADMIN, Roles.ADMIN",
            notes = "Returns single instance (possibly with computed attributes)",
            response = SipProfile.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = SipProfile.class),
            @ApiResponse(code = 400, message = "Specified id does not correspond to the id of the instance")})
    @RolesAllowed({Roles.SUPER_ADMIN, Roles.ADMIN})
    @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
    public SipProfile save(@ApiParam(value = "Id of the instance", required = true)
                           @PathVariable("id") String id,
                           @ApiParam(value = "Single instance", required = true)
                           @RequestBody SipProfile request) throws DocumentException {
        eq(id, request.getId(), () -> new BadArgument("id"));
        return service.validateAndSave(request);
    }

    @ApiOperation(value = "Deletes an instance. Roles.SUPER_ADMIN")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response"),
            @ApiResponse(code = 404, message = "Instance does not exist")})
    @RolesAllowed({Roles.SUPER_ADMIN})
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public void delete(@ApiParam(value = "Id of the instance", required = true)
                       @PathVariable("id") String id) {
        SipProfile entity = service.find(id);
        notNull(entity, () -> new MissingObject(SipProfile.class, id));

        service.delete(entity);
    }

    @ApiOperation(value = "Gets one instance specified by id", response = SipProfile.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = SipProfile.class),
            @ApiResponse(code = 404, message = "Instance does not exist")})
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public SipProfile get(@ApiParam(value = "Id of the instance", required = true)
                          @PathVariable("id") String id) {
        SipProfile entity = service.find(id);
        notNull(entity, () -> new MissingObject(SipProfile.class, id));

        return entity;
    }

    @ApiOperation(value = "Gets all instances", response = Collection.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = Collection.class)})
    @RequestMapping(method = RequestMethod.GET)
    public Collection<SipProfile> list() {
        return service.findAll();
    }

    @ApiOperation(value = "Gets DTOs of all instances", response = Collection.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = Collection.class)})
    @RequestMapping(value = "/list_dtos", method = RequestMethod.GET)
    public Collection<SipProfileDto> listDtos() {
        return service.listSipProfileDtos();
    }

    @Inject
    public void setService(SipProfileService service) {
        this.service = service;
    }
}
