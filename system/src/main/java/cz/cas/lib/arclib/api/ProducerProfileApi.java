package cz.cas.lib.arclib.api;

import cz.cas.lib.arclib.domain.profiles.ProducerProfile;
import cz.cas.lib.arclib.domainbase.exception.BadArgument;
import cz.cas.lib.arclib.dto.ProducerProfileDto;
import cz.cas.lib.arclib.security.authorization.permission.Permissions;
import cz.cas.lib.arclib.service.ProducerProfileService;
import cz.cas.lib.core.index.dto.Params;
import cz.cas.lib.core.index.dto.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import static cz.cas.lib.core.util.Utils.eq;

@RestController
@Tag(name = "producer profile", description = "Api for interaction with producer profiles")
@RequestMapping("/api/producer_profile")
public class ProducerProfileApi {

    private ProducerProfileService service;

    @Operation(summary = "Saves an instance. [Perm.PRODUCER_PROFILE_RECORDS_WRITE]",
            description = "Returns single instance (possibly with computed attributes). If the calling user is not Roles.SUPER_ADMIN" +
                    " the producer of the user must match the producer of the producer profile.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response", content = @Content(schema = @Schema(implementation = ProducerProfile.class))),
            @ApiResponse(responseCode = "400", description = "Specified id does not correspond to the id of the instance | Producers of related entities differ from producer of given producer profile"),
            @ApiResponse(responseCode = "403", description = "Id of the producer does not match the id of the producer of the logged on user.")})
    @PreAuthorize("hasAuthority('" + Permissions.PRODUCER_PROFILE_RECORDS_WRITE + "')")
    @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
    public ProducerProfile save(@Parameter(description = "Id of the instance", required = true) @PathVariable("id") String id,
                                @Parameter(description = "Single instance", required = true) @RequestBody @Valid ProducerProfile producerProfile) {
        eq(id, producerProfile.getId(), () -> new BadArgument("id"));
        return service.save(producerProfile);
    }

    @Operation(summary = "Deletes an instance. [Perm.PRODUCER_PROFILE_RECORDS_WRITE]",
            description = "If the calling user is not Roles.SUPER_ADMIN the producer of the user must match the producer " +
                    "of the deleted producer profile.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response"),
            @ApiResponse(responseCode = "404", description = "Instance does not exist")})
    @PreAuthorize("hasAuthority('" + Permissions.PRODUCER_PROFILE_RECORDS_WRITE + "')")
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public void delete(@Parameter(description = "Id of the instance", required = true) @PathVariable("id") String id) {
        service.delete(id);
    }

    @Operation(summary = "Gets one instance specified by id [Perm.PRODUCER_PROFILE_RECORDS_READ]",
            description = "Returns deleted entities as well.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response", content = @Content(schema = @Schema(implementation = ProducerProfile.class))),
            @ApiResponse(responseCode = "404", description = "Instance does not exist")})
    @PreAuthorize("hasAuthority('" + Permissions.PRODUCER_PROFILE_RECORDS_READ + "')")
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public ProducerProfile get(@Parameter(description = "Id of the instance", required = true) @PathVariable("id") String id) {
        return service.get(id);
    }

    @Operation(summary = "Gets DTOs of all instances that respect the selected parameters from Index [Perm.PRODUCER_PROFILE_RECORDS_READ]",
            description = "If the calling user is not Roles.SUPER_ADMIN only the producer profiles" +
                    " with producer same as the producer of calling user are returned. \n" +
                    "Filter/Sort fields = id, name, created, updated, producerId, ingestWorkflowExternalId," +
                    " producerName, producerTransferAreaPath, sipProfileId, sipProfileName, hashType," +
                    " workflowDefinitionId, workflowDefinitionName, validationProfileId, validationProfileName, debuggingModeActive")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Successful response", content = @Content(schema = @Schema(implementation = ProducerProfileDto.class)))})
    @PreAuthorize("hasAuthority('" + Permissions.PRODUCER_PROFILE_RECORDS_READ + "')")
    @RequestMapping(value = "/list_dtos", method = RequestMethod.GET)
    public Result<ProducerProfileDto> listDtos(@Parameter(description = "Parameters to comply with", required = true) @ModelAttribute Params params) {
        return service.listProducerProfileDtosFromIndex(params);
    }

    @Operation(summary = "Gets DTOs of all instances from DB [Perm.PRODUCER_PROFILE_RECORDS_READ]",
            description = "If the calling user is not Roles.SUPER_ADMIN only the producer profiles" +
                    " with producer same as the producer of calling user are returned. ")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Successful response", content = @Content(schema = @Schema(implementation = ProducerProfileDto.class)))})
    @PreAuthorize("hasAuthority('" + Permissions.PRODUCER_PROFILE_RECORDS_READ + "')")
    @RequestMapping(value = "/list_dtos/all", method = RequestMethod.GET)
    public Result<ProducerProfileDto> listAllDtos() {
        return service.listProducerProfileDtosFromDatabase();
    }

    @Autowired
    public void setService(ProducerProfileService service) {
        this.service = service;
    }

}
