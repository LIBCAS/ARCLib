package cz.cas.lib.arclib.api;

import cz.cas.lib.arclib.dto.AipDeletionRequestDto;
import cz.cas.lib.arclib.security.authorization.permission.Permissions;
import cz.cas.lib.arclib.security.user.UserDetails;
import cz.cas.lib.arclib.service.DeletionRequestService;
import cz.cas.lib.core.index.dto.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static cz.cas.lib.core.util.Utils.checkUUID;

@RestController
@Tag(name = "deletion requests", description = "Api for request related to physical deletion of AIP")
@RequestMapping("/api/deletion_request")
@Slf4j
public class DeletionRequestApi {

    private DeletionRequestService deletionRequestService;
    private UserDetails userDetails;

    @Operation(summary = "Creates deletion request for deletion of AIP at archival storage. [Perm.DELETION_REQUESTS_WRITE]")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "400", description = "The specified aip id is not a valid UUID"),
            @ApiResponse(responseCode = "409", description = "Deletion request for this aip id already exists.")
    })
    @PreAuthorize("hasAuthority('" + Permissions.DELETION_REQUESTS_WRITE + "')")
    @RequestMapping(value = "/aip/{aipId}", method = RequestMethod.POST)
    public void createDeletion(
            @Parameter(description = "AIP id", required = true)
            @PathVariable("aipId") String aipId) {
        checkUUID(aipId);
        deletionRequestService.createDeletionRequest(aipId,userDetails.getId());
    }

    @Operation(summary = "Reverts deletion request made by the same user. [Perm.DELETION_REQUESTS_WRITE]")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "400", description = "The specified aip id is not a valid UUID"),
            @ApiResponse(responseCode = "403", description = "if caller is not the creator of the deletion request"),
            @ApiResponse(responseCode = "409", description = "Deletion request for the user does not exist.")
    })
    @PreAuthorize("hasAuthority('" + Permissions.DELETION_REQUESTS_WRITE + "')")
    @RequestMapping(value = "/{id}/revert", method = RequestMethod.POST)
    public void revertDeletion(
            @Parameter(description = "Deletion request id", required = true)
            @PathVariable("id") String deletionRequestId) {
        checkUUID(deletionRequestId);
        deletionRequestService.revertDeletionRequest(deletionRequestId);
    }

    @Operation(summary = "Gets requests for AIP deletion waiting to be resolved that have not yet been acknowledged by the current user. [Perm.DELETION_REQUESTS_READ]",
            responses = {@ApiResponse(content = @Content(schema = @Schema(implementation = List.class)))})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response")})
    @PreAuthorize("hasAuthority('" + Permissions.DELETION_REQUESTS_READ + "')")
    @RequestMapping(method = RequestMethod.GET)
    public List<AipDeletionRequestDto> listDeletionRequests() {
        return deletionRequestService.listDeletionRequests();
    }

    @Operation(summary = "Acknowledge deletion request. [Perm.DELETION_ACKNOWLEDGE_PRIVILEGE]",
            responses = {@ApiResponse(content = @Content(schema = @Schema(implementation = Result.class)))})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response")})
    @PreAuthorize("hasAuthority('" + Permissions.DELETION_ACKNOWLEDGE_PRIVILEGE + "')")
    @RequestMapping(value = "/{id}/acknowledge", method = RequestMethod.POST)
    public void acknowledgeDeletion(
            @Parameter(description = "Deletion request id", required = true)
            @PathVariable("id") String deletionRequestId) {
        deletionRequestService.acknowledgeDeletion(deletionRequestId);
    }

    @Operation(summary = "Disacknowledge deletion request. [Perm.DELETION_ACKNOWLEDGE_PRIVILEGE]",
            responses = {@ApiResponse(content = @Content(schema = @Schema(implementation = Result.class)))})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response")})
    @PreAuthorize("hasAuthority('" + Permissions.DELETION_ACKNOWLEDGE_PRIVILEGE + "')")
    @RequestMapping(value = "/{id}/disacknowledge", method = RequestMethod.POST)
    public void disacknowledgeDeletion(
            @Parameter(description = "Deletion request id", required = true)
            @PathVariable("id") String deletionRequestId) {
        deletionRequestService.disacknowledgeDeletion(deletionRequestId);
    }


    @Autowired
    public void setDeletionRequestService(DeletionRequestService deletionRequestService) {
        this.deletionRequestService = deletionRequestService;
    }

    @Autowired
    public void setUserDetails(UserDetails userDetails) {
        this.userDetails = userDetails;
    }
}
