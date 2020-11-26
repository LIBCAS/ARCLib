package cz.cas.lib.arclib.api;

import cz.cas.lib.arclib.domain.profiles.ValidationProfile;
import cz.cas.lib.arclib.domainbase.exception.BadArgument;
import cz.cas.lib.arclib.domainbase.exception.MissingObject;
import cz.cas.lib.arclib.dto.ValidationProfileDto;
import cz.cas.lib.arclib.security.authorization.data.Permissions;
import cz.cas.lib.arclib.service.ValidationProfileService;
import io.swagger.annotations.*;
import lombok.Getter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Collection;

import static cz.cas.lib.core.util.Utils.eq;
import static cz.cas.lib.core.util.Utils.notNull;

@RestController
@Api(value = "validation profile", description = "Api for interaction with validation profiles")
@RequestMapping("/api/validation_profile")
public class ValidationProfileApi {
    @Getter
    private ValidationProfileService service;

    @ApiOperation(value = "Saves an instance. [Perm.VALIDATION_PROFILE_RECORDS_WRITE]",
            notes = "Returns single instance (possibly with computed attributes)",
            response = ValidationProfile.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = ValidationProfile.class),
            @ApiResponse(code = 400, message = "Specified id does not correspond to the id of the instance")})
    @PreAuthorize("hasAuthority('" + Permissions.VALIDATION_PROFILE_RECORDS_WRITE + "')")
    @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
    public ValidationProfile save(@ApiParam(value = "Id of the instance", required = true) @PathVariable("id") String id,
                                  @ApiParam(value = "Single instance", required = true) @RequestBody ValidationProfile request) throws IOException {
        eq(id, request.getId(), () -> new BadArgument("id"));
        return service.save(request);
    }

    @ApiOperation(value = "Deletes an instance. [Perm.VALIDATION_PROFILE_RECORDS_WRITE]")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response"),
            @ApiResponse(code = 404, message = "Instance does not exist")})
    @PreAuthorize("hasAuthority('" + Permissions.VALIDATION_PROFILE_RECORDS_WRITE + "')")
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public void delete(@ApiParam(value = "Id of the instance", required = true) @PathVariable("id") String id) {
        ValidationProfile entity = service.find(id);
        notNull(entity, () -> new MissingObject(ValidationProfile.class, id));

        service.delete(entity);
    }

    @ApiOperation(value = "Gets one instance specified by id [Perm.VALIDATION_PROFILE_RECORDS_READ]", response = ValidationProfile.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = ValidationProfile.class),
            @ApiResponse(code = 404, message = "Instance does not exist")})
    @PreAuthorize("hasAuthority('" + Permissions.VALIDATION_PROFILE_RECORDS_READ + "')")
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public ValidationProfile get(@ApiParam(value = "Id of the instance", required = true) @PathVariable("id") String id) {
        ValidationProfile entity = service.find(id);
        notNull(entity, () -> new MissingObject(ValidationProfile.class, id));

        return entity;
    }

    @ApiOperation(value = "Gets DTOs of all instances [Perm.VALIDATION_PROFILE_RECORDS_READ]",
            notes = "if the calling user is not [Perm.SUPER_ADMIN_PRIVILEGE] only ValidationProfiles assigned to the user's producer are returned. ",
            response = ValidationProfileDto.class)
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful response", response = ValidationProfileDto.class)})
    @PreAuthorize("hasAuthority('" + Permissions.VALIDATION_PROFILE_RECORDS_READ + "')")
    @RequestMapping(path = "/list_dtos", method = RequestMethod.GET)
    public Collection<ValidationProfileDto> listDtos() {
        return service.listValidationProfileDtos();
    }

    @Inject
    public void setService(ValidationProfileService service) {
        this.service = service;
    }
}
