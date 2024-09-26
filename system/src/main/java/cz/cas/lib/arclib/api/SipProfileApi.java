package cz.cas.lib.arclib.api;

import cz.cas.lib.arclib.domain.profiles.SipProfile;
import cz.cas.lib.arclib.domainbase.exception.BadArgument;
import cz.cas.lib.arclib.domainbase.exception.MissingObject;
import cz.cas.lib.arclib.dto.SipProfileDto;
import cz.cas.lib.arclib.security.authorization.permission.Permissions;
import cz.cas.lib.arclib.service.SipProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Getter;
import org.dom4j.DocumentException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

import static cz.cas.lib.core.util.Utils.eq;
import static cz.cas.lib.core.util.Utils.notNull;

@RestController
@Tag(name = "sip profile", description = "Api for interaction with sip profiles")
@RequestMapping("/api/sip_profile")
public class SipProfileApi {
    @Getter
    private SipProfileService service;

    @Operation(summary = "Saves an instance. [Perm.SIP_PROFILE_RECORDS_WRITE]",
            description = "Returns single instance (possibly with computed attributes)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response", content = @Content(schema = @Schema(implementation = SipProfile.class))),
            @ApiResponse(responseCode = "400", description = "Specified id does not correspond to the id of the instance")})
    @PreAuthorize("hasAuthority('" + Permissions.SIP_PROFILE_RECORDS_WRITE + "')")
    @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
    public SipProfile save(@Parameter(description = "Id of the instance", required = true) @PathVariable("id") String id,
                           @Parameter(description = "Single instance", required = true) @RequestBody SipProfile request) throws DocumentException {
        eq(id, request.getId(), () -> new BadArgument("id"));
        return service.save(request);
    }

    @Operation(summary = "Deletes an instance. [Perm.SIP_PROFILE_RECORDS_WRITE]")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response"),
            @ApiResponse(responseCode = "404", description = "Instance does not exist")})
    @PreAuthorize("hasAuthority('" + Permissions.SIP_PROFILE_RECORDS_WRITE + "')")
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public void delete(@Parameter(description = "Id of the instance", required = true) @PathVariable("id") String id) {
        SipProfile entity = service.find(id);
        notNull(entity, () -> new MissingObject(SipProfile.class, id));

        service.delete(entity);
    }

    @Operation(summary = "Gets one instance specified by id [Perm.SIP_PROFILE_RECORDS_READ]",
            description = "Returns deleted entities as well.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response", content = @Content(schema = @Schema(implementation = SipProfile.class))),
            @ApiResponse(responseCode = "404", description = "Instance does not exist")})
    @PreAuthorize("hasAuthority('" + Permissions.SIP_PROFILE_RECORDS_READ + "')")
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public SipProfile get(@Parameter(description = "Id of the instance", required = true) @PathVariable("id") String id) {
        SipProfile entity = service.findWithDeletedFilteringOff(id);
        notNull(entity, () -> new MissingObject(SipProfile.class, id));

        return entity;
    }

    @Operation(summary = "Gets DTOs of all instances [Perm.SIP_PROFILE_RECORDS_READ]",
            description = "if the calling user is not [Perm.SUPER_ADMIN_PRIVILEGE] only SipProfiles assigned to the user's producer are returned. ")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Successful response", content = @Content(schema = @Schema(implementation = SipProfileDto.class)))})
    @PreAuthorize("hasAuthority('" + Permissions.SIP_PROFILE_RECORDS_READ + "')")
    @RequestMapping(value = "/list_dtos", method = RequestMethod.GET)
    public Collection<SipProfileDto> listDtos() {
        return service.listSipProfileDtos();
    }

    @Autowired
    public void setService(SipProfileService service) {
        this.service = service;
    }
}
