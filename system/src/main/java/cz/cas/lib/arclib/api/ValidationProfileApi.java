package cz.cas.lib.arclib.api;

import cz.cas.lib.arclib.domain.profiles.ValidationProfile;
import cz.cas.lib.arclib.domainbase.exception.BadArgument;
import cz.cas.lib.arclib.domainbase.exception.MissingObject;
import cz.cas.lib.arclib.dto.ValidationProfileDto;
import cz.cas.lib.arclib.security.authorization.permission.Permissions;
import cz.cas.lib.arclib.service.ValidationProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Collection;

import static cz.cas.lib.core.util.Utils.eq;
import static cz.cas.lib.core.util.Utils.notNull;

@RestController
@Tag(name = "validation profile", description = "Api for interaction with validation profiles")
@RequestMapping("/api/validation_profile")
public class ValidationProfileApi {
    @Getter
    private ValidationProfileService service;

    @Operation(summary = "Saves an instance. [Perm.VALIDATION_PROFILE_RECORDS_WRITE]",
            description = "Returns single instance (possibly with computed attributes)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response", content = @Content(schema = @Schema(implementation = ValidationProfile.class))),
            @ApiResponse(responseCode = "400", description = "Specified id does not correspond to the id of the instance")})
    @PreAuthorize("hasAuthority('" + Permissions.VALIDATION_PROFILE_RECORDS_WRITE + "')")
    @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
    public ValidationProfile save(@Parameter(description = "Id of the instance", required = true) @PathVariable("id") String id,
                                  @Parameter(description = "Single instance", required = true) @RequestBody ValidationProfile request) throws IOException {
        eq(id, request.getId(), () -> new BadArgument("id"));
        return service.save(request);
    }

    @Operation(summary = "Deletes an instance. [Perm.VALIDATION_PROFILE_RECORDS_WRITE]")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response"),
            @ApiResponse(responseCode = "404", description = "Instance does not exist")})
    @PreAuthorize("hasAuthority('" + Permissions.VALIDATION_PROFILE_RECORDS_WRITE + "')")
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public void delete(@Parameter(description = "Id of the instance", required = true) @PathVariable("id") String id) {
        ValidationProfile entity = service.find(id);
        notNull(entity, () -> new MissingObject(ValidationProfile.class, id));

        service.delete(entity);
    }

    @Operation(summary = "Gets one instance specified by id [Perm.VALIDATION_PROFILE_RECORDS_READ]",
            description = "Returns deleted entities as well.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response", content = @Content(schema = @Schema(implementation = ValidationProfile.class))),
            @ApiResponse(responseCode = "404", description = "Instance does not exist")})
    @PreAuthorize("hasAuthority('" + Permissions.VALIDATION_PROFILE_RECORDS_READ + "')")
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public ValidationProfile get(@Parameter(description = "Id of the instance", required = true) @PathVariable("id") String id) {
        ValidationProfile entity = service.findWithDeletedFilteringOff(id);
        notNull(entity, () -> new MissingObject(ValidationProfile.class, id));

        return entity;
    }

    @Operation(summary = "Gets DTOs of all instances [Perm.VALIDATION_PROFILE_RECORDS_READ]",
            description = "if the calling user is not [Perm.SUPER_ADMIN_PRIVILEGE] only ValidationProfiles assigned to the user's producer are returned. ")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Successful response", content = @Content(schema = @Schema(implementation = ValidationProfileDto.class)))})
    @PreAuthorize("hasAuthority('" + Permissions.VALIDATION_PROFILE_RECORDS_READ + "')")
    @RequestMapping(path = "/list_dtos", method = RequestMethod.GET)
    public Collection<ValidationProfileDto> listDtos() {
        return service.listValidationProfileDtos();
    }

    @Autowired
    public void setService(ValidationProfileService service) {
        this.service = service;
    }
}
