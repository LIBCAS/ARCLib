package cz.cas.lib.arclib.api;

import cz.cas.lib.arclib.dto.AipDeletionRequestDto;
import cz.cas.lib.arclib.security.authorization.permission.Permissions;
import cz.cas.lib.arclib.service.DeletionRequestService;
import cz.cas.lib.core.index.dto.Result;
import io.swagger.annotations.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Inject;
import java.util.List;

import static cz.cas.lib.core.util.Utils.checkUUID;

@RestController
@Api(value = "deletion requests", description = "Api for request related to physical deletion of AIP")
@RequestMapping("/api/deletion_request")
@Slf4j
public class DeletionRequestApi {

    private DeletionRequestService deletionRequestService;

    @ApiOperation(value = "Creates deletion request for deletion of AIP at archival storage. [Perm.DELETION_REQUESTS_WRITE]")
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "The specified aip id is not a valid UUID"),
            @ApiResponse(code = 409, message = "Deletion request for this aip id already exists.")
    })
    @PreAuthorize("hasAuthority('" + Permissions.DELETION_REQUESTS_WRITE + "')")
    @RequestMapping(value = "/aip/{aipId}", method = RequestMethod.POST)
    public void createDeletion(
            @ApiParam(value = "AIP id", required = true)
            @PathVariable("aipId") String aipId) {
        checkUUID(aipId);
        deletionRequestService.createDeletionRequest(aipId);
    }

    @ApiOperation(value = "Reverts deletion request made by the same user. [Perm.DELETION_REQUESTS_WRITE]")
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "The specified aip id is not a valid UUID"),
            @ApiResponse(code = 403, message = "if caller is not the creator of the deletion request"),
            @ApiResponse(code = 409, message = "Deletion request for the user does not exist.")
    })
    @PreAuthorize("hasAuthority('" + Permissions.DELETION_REQUESTS_WRITE + "')")
    @RequestMapping(value = "/{id}/revert", method = RequestMethod.POST)
    public void revertDeletion(
            @ApiParam(value = "Deletion request id", required = true)
            @PathVariable("id") String deletionRequestId) {
        checkUUID(deletionRequestId);
        deletionRequestService.revertDeletionRequest(deletionRequestId);
    }

    @ApiOperation(value = "Gets requests for AIP deletion waiting to be resolved that have not yet been acknowledged by the current user. [Perm.DELETION_REQUESTS_READ]",
            response = List.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response")})
    @PreAuthorize("hasAuthority('" + Permissions.DELETION_REQUESTS_READ + "')")
    @RequestMapping(method = RequestMethod.GET)
    public List<AipDeletionRequestDto> listDeletionRequests() {
        return deletionRequestService.listDeletionRequests();
    }

    @ApiOperation(value = "Acknowledge deletion request. [Perm.DELETION_ACKNOWLEDGE_PRIVILEGE]",
            response = Result.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response")})
    @PreAuthorize("hasAuthority('" + Permissions.DELETION_ACKNOWLEDGE_PRIVILEGE + "')")
    @RequestMapping(value = "/{id}/acknowledge", method = RequestMethod.POST)
    public void acknowledgeDeletion(
            @ApiParam(value = "Deletion request id", required = true)
            @PathVariable("id") String deletionRequestId) {
        deletionRequestService.acknowledgeDeletion(deletionRequestId);
    }

    @ApiOperation(value = "Disacknowledge deletion request. [Perm.DELETION_ACKNOWLEDGE_PRIVILEGE]",
            response = Result.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response")})
    @PreAuthorize("hasAuthority('" + Permissions.DELETION_ACKNOWLEDGE_PRIVILEGE + "')")
    @RequestMapping(value = "/{id}/disacknowledge", method = RequestMethod.POST)
    public void disacknowledgeDeletion(
            @ApiParam(value = "Deletion request id", required = true)
            @PathVariable("id") String deletionRequestId) {
        deletionRequestService.disacknowledgeDeletion(deletionRequestId);
    }


    @Inject
    public void setDeletionRequestService(DeletionRequestService deletionRequestService) {
        this.deletionRequestService = deletionRequestService;
    }
}
