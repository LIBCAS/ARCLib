package cz.cas.lib.arclib.api;

import cz.cas.lib.arclib.domain.Batch;
import cz.cas.lib.arclib.domain.Hash;
import cz.cas.lib.arclib.dto.BatchDto;
import cz.cas.lib.arclib.dto.JmsDto;
import cz.cas.lib.arclib.exception.BadRequestException;
import cz.cas.lib.arclib.security.authorization.Roles;
import cz.cas.lib.arclib.security.user.UserDetails;
import cz.cas.lib.arclib.service.BatchService;
import cz.cas.lib.arclib.service.CoordinatorService;
import cz.cas.lib.arclib.domainbase.exception.GeneralException;
import cz.cas.lib.core.index.dto.Filter;
import cz.cas.lib.core.index.dto.FilterOperation;
import cz.cas.lib.core.index.dto.Params;
import cz.cas.lib.core.index.dto.Result;
import io.swagger.annotations.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import java.io.IOException;

import static cz.cas.lib.arclib.utils.ArclibUtils.hasRole;
import static cz.cas.lib.core.util.Utils.addPrefilter;

@RestController
@Api(value = "batch", description = "Api for interaction with batches")
@RequestMapping("/api/batch")
public class BatchApi {

    private BatchService batchService;
    private CoordinatorService coordinatorService;
    private UserDetails userDetails;

    private static final String PRODUCER = "producerId";
    private static final String USER = "userId";

    @ApiOperation(value = "Starts processing of SIPs stored in the specified folder. Roles.ARCHIVIST, Roles.SUPER_ADMIN")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response"),
            @ApiResponse(code = 400, message = "Workflow config is empty")})
    @RolesAllowed({Roles.ARCHIVIST, Roles.SUPER_ADMIN})
    @RequestMapping(value = "/start", method = RequestMethod.POST)
    public String start(@ApiParam(value = "External id of the producer profile", required = true)
                        @RequestParam("producerProfileExternalId") String producerProfileExternalId,
                        @ApiParam(value = "JSON configuration of the ingest workflow", required = true)
                        @RequestParam("workflowConfig") String workflowConfig,
                        @ApiParam(value = "Transfer area path")
                        @RequestParam(value = "transferAreaPath", required = false) String transferAreaPath,
                        @ApiParam(value = "User id")
                        @RequestParam(value = "userId", required = false) String userId) {
        if (userId == null) {
            if (userDetails == null) throw new GeneralException("No user specified while trying to start processing of SIPs.");
            userId = userDetails.getId();
        }
        return coordinatorService.processBatchOfSips(producerProfileExternalId, workflowConfig, transferAreaPath, userId,null);
    }

    @ApiOperation(value = "Starts processing of a SIP from the provided SIP content. Roles.ARCHIVIST, Roles.SUPER_ADMIN")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response"),
            @ApiResponse(code = 400, message = "Content of the SIP package is not in the zip format, " +
                    "or some of the required attributes is empty")})
    @RolesAllowed({Roles.ARCHIVIST, Roles.SUPER_ADMIN})
    @RequestMapping(value = "/process_one", method = RequestMethod.POST)
    public String processSip(@ApiParam(value = "Content of SIP package", required = true)
                             @RequestParam("sipContent") MultipartFile sipContent,
                             @ApiParam(value = "Hash of SIP package", required = true)
                             @ModelAttribute("sipHash") Hash hash,
                             @ApiParam(value = "External id of producer profile", required = true)
                             @RequestParam("producerProfileExternalId") String producerProfileExternalId,
                             @ApiParam(value = "JSON configuration of ingest workflow", required = true)
                             @RequestParam("workflowConfig") String workflowConfig,
                             @ApiParam(value = "Transfer area path")
                             @RequestParam(value = "transferAreaPath", required = false) String transferAreaPath) throws IOException, BadRequestException {
        if (!("application/x-zip-compressed".equals(sipContent.getContentType()) ||
                ("application/zip".equalsIgnoreCase(sipContent.getContentType())))) {
            throw new BadRequestException();
        }
        return coordinatorService.processSip(sipContent.getInputStream(), hash, producerProfileExternalId, workflowConfig,
                sipContent.getOriginalFilename(), transferAreaPath);
    }

    @ApiOperation(value = "Suspends processing of batch. Roles.ARCHIVIST, Roles.SUPER_ADMIN")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response"),
            @ApiResponse(code = 404, message = "Instance does not exist")})
    @RolesAllowed({Roles.ARCHIVIST, Roles.SUPER_ADMIN})
    @RequestMapping(value = "/{batchId}/suspend", method = RequestMethod.POST)
    public void suspend(@ApiParam(value = "Id of the batch to suspend", required = true)
                        @PathVariable("batchId") String batchId) {
        coordinatorService.suspendBatch(batchId);
    }

    @ApiOperation(value = "Cancels processing of batch. Roles.ARCHIVIST, Roles.SUPER_ADMIN")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response"),
            @ApiResponse(code = 404, message = "Instance does not exist")})
    @RolesAllowed({Roles.ARCHIVIST, Roles.SUPER_ADMIN})
    @RequestMapping(value = "/{batchId}/cancel", method = RequestMethod.POST)
    public void cancel(@ApiParam(value = "Id of the batch to cancel", required = true)
                       @PathVariable("batchId") String batchId) {
        coordinatorService.cancelBatch(new JmsDto(batchId, userDetails.getId()));
    }

    @ApiOperation(value = "Resumes processing of batch. Roles.ARCHIVIST, Roles.SUPER_ADMIN", response = Boolean.class,
            notes = "returns true, if the batch has succeeded to resume, false otherwise")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response"),
            @ApiResponse(code = 404, message = "Instance does not exist")})
    @RolesAllowed({Roles.ARCHIVIST, Roles.SUPER_ADMIN})
    @RequestMapping(value = "/{batchId}/resume", method = RequestMethod.POST)
    public boolean resume(@ApiParam(value = "Id of the batch to resume", required = true)
                          @PathVariable("batchId") String batchId) {
        return coordinatorService.resumeBatch(batchId);
    }

    @ApiOperation(value = "Gets batches that respect the selected parameters.",
            notes = "Filter/Sort fields = id, created, updated, config, producerId, producerName, producerProfileId," +
                    " producerProfileName, state (PROCESSING,SUSPENDED,CANCELED,PROCESSED), userId.." +
                    "SUPER_ADMIN can see all batches, ADMIN can see all batches of its producer and others see their batches " +
                    "i.e. batches they started manually or batches created by their routine", response = Result.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = Result.class)})
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    public Result<Batch> list(@ApiParam(value = "Parameters to comply with", required = true)
                              @ModelAttribute Params params) {
        if (!hasRole(userDetails, Roles.SUPER_ADMIN)) {
            if (hasRole(userDetails, Roles.ADMIN)) {
                addPrefilter(params, new Filter(PRODUCER, FilterOperation.EQ, userDetails.getProducerId(), null));
            } else {
                addPrefilter(params, new Filter(USER, FilterOperation.EQ, userDetails.getId(), null));
            }
        }
        return batchService.getBatches(params);
    }

    @ApiOperation(value = "Gets DTOs of batches that respect the selected parameters.",
            notes = "Filter/Sort fields = id, created, updated, config, producerId, producerName, producerProfileId," +
                    " producerProfileName, state (PROCESSING,SUSPENDED,CANCELED,PROCESSED), userId.." +
                    "SUPER_ADMIN can see all batches, ADMIN can see all batches of its producer and others see their batches " +
                    "i.e. batches they started manually or batches created by their routine", response = Result.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = Result.class)})
    @RequestMapping(value = "/list_dtos", method = RequestMethod.GET)
    public Result<BatchDto> listDtos(@ApiParam(value = "Parameters to comply with", required = true)
                              @ModelAttribute Params params) {
        if (!hasRole(userDetails, Roles.SUPER_ADMIN)) {
            if (hasRole(userDetails, Roles.ADMIN)) {
                addPrefilter(params, new Filter(PRODUCER, FilterOperation.EQ, userDetails.getProducerId(), null));
            } else {
                addPrefilter(params, new Filter(USER, FilterOperation.EQ, userDetails.getId(), null));
            }
        }
        return batchService.listBatchDtos(params);
    }

    @ApiOperation(value = "Gets batch by id.", response = Batch.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = Batch.class),
            @ApiResponse(code = 404, message = "Instance does not exist")})
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public Batch get(@ApiParam(value = "Id of the batch", required = true)
                     @PathVariable("id") String id) {
        return batchService.get(id);
    }

    @Inject
    public void setBatchService(BatchService batchService) {
        this.batchService = batchService;
    }

    @Inject
    public void setCoordinatorService(CoordinatorService coordinatorService) {
        this.coordinatorService = coordinatorService;
    }

    @Inject
    public void setUserDetails(UserDetails userDetails) {
        this.userDetails = userDetails;
    }
}
