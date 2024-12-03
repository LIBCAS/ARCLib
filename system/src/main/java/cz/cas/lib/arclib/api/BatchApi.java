package cz.cas.lib.arclib.api;

import cz.cas.lib.arclib.domain.Batch;
import cz.cas.lib.arclib.domain.Hash;
import cz.cas.lib.arclib.domainbase.exception.MissingObject;
import cz.cas.lib.arclib.dto.BatchDetailDto;
import cz.cas.lib.arclib.dto.BatchDto;
import cz.cas.lib.arclib.dto.JmsDto;
import cz.cas.lib.arclib.exception.BadRequestException;
import cz.cas.lib.arclib.report.ExportFormat;
import cz.cas.lib.arclib.security.authorization.permission.Permissions;
import cz.cas.lib.arclib.security.user.UserDetails;
import cz.cas.lib.arclib.service.BatchService;
import cz.cas.lib.arclib.service.CoordinatorService;
import cz.cas.lib.arclib.service.tableexport.TableExportType;
import cz.cas.lib.core.index.dto.Filter;
import cz.cas.lib.core.index.dto.FilterOperation;
import cz.cas.lib.core.index.dto.Params;
import cz.cas.lib.core.index.dto.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

import static cz.cas.lib.arclib.utils.ArclibUtils.hasRole;
import static cz.cas.lib.core.util.Utils.addPrefilter;
import static cz.cas.lib.core.util.Utils.notNull;

@RestController
@Tag(name = "batch", description = "Api for interaction with batches")
@RequestMapping("/api/batch")
public class BatchApi {

    private BatchService batchService;
    private CoordinatorService coordinatorService;
    private UserDetails userDetails;

    // Vykomentovane za suhlasu @Tomasek, ku dnu vykomentovania nebol endpoint pouzivany na FE
//    @Operation(summary = "Starts processing of SIPs stored in the specified folder. [Perm.BATCH_PROCESSING_WRITE]")
//    @ApiResponses(value = {
//            @ApiResponse(responseCode = "200", description = "Successful response"),
//            @ApiResponse(responseCode = "400", description = "Workflow config is empty")})
//    @PreAuthorize("hasAuthority('" + Permissions.BATCH_PROCESSING_WRITE + "')")
//    @RequestMapping(value = "/start", method = RequestMethod.POST)
//    public String start(@Parameter(description = "External id of the producer profile", required = true)
//                        @RequestParam("producerProfileExternalId") String producerProfileExternalId,
//                        @Parameter(description = "JSON configuration of the ingest workflow", required = true)
//                        @RequestParam("workflowConfig") String workflowConfig,
//                        @Parameter(description = "Transfer area path")
//                        @RequestParam(value = "transferAreaPath", required = false) String transferAreaPath,
//                        @Parameter(description = "User id")
//                        @RequestParam(value = "userId", required = false) String userId) throws IOException {
//        if (userId == null) {
//            if (userDetails == null)
//                throw new GeneralException("No user specified while trying to start processing of SIPs.");
//            userId = userDetails.getId();
//        }
//        return coordinatorService.processBatchOfSips(producerProfileExternalId, workflowConfig, transferAreaPath, userId, null);
//    }

    @Operation(summary = "Starts processing of a SIP from the provided SIP content. [Perm.BATCH_PROCESSING_WRITE]")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response"),
            @ApiResponse(responseCode = "400", description = "Content of the SIP package is not in the zip format, " +
                    "or some of the required attributes is empty")})
    @PreAuthorize("hasAuthority('" + Permissions.BATCH_PROCESSING_WRITE + "')")
    @RequestMapping(value = "/process_one", method = RequestMethod.POST)
    public String processSip(@Parameter(description = "Content of SIP package", required = true)
                             @RequestParam("sipContent") MultipartFile sipContent,
                             @Parameter(description = "Hash of SIP package", required = true)
                             @ModelAttribute("sipHash") Hash hash,
                             @Parameter(description = "External id of producer profile", required = true)
                             @RequestParam("producerProfileExternalId") String producerProfileExternalId,
                             @Parameter(description = "JSON configuration of ingest workflow", required = true)
                             @RequestParam("workflowConfig") String workflowConfig,
                             @Parameter(description = "Transfer area path")
                             @RequestParam(value = "transferAreaPath", required = false) String transferAreaPath) throws IOException, BadRequestException {
        if (!("application/x-zip-compressed".equals(sipContent.getContentType()) ||
                ("application/zip".equalsIgnoreCase(sipContent.getContentType())))) {
            throw new BadRequestException();
        }
        return coordinatorService.processSip(sipContent.getInputStream(), hash, producerProfileExternalId, workflowConfig,
                sipContent.getOriginalFilename(), transferAreaPath);
    }

    @Operation(summary = "Suspends processing of batch. [Perm.BATCH_PROCESSING_WRITE]")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response"),
            @ApiResponse(responseCode = "404", description = "Instance does not exist")})
    @PreAuthorize("hasAuthority('" + Permissions.BATCH_PROCESSING_WRITE + "')")
    @RequestMapping(value = "/{batchId}/suspend", method = RequestMethod.POST)
    public void suspend(@Parameter(description = "Id of the batch to suspend", required = true) @PathVariable("batchId") String batchId) {
        coordinatorService.suspendBatch(batchId);
    }

    @Operation(summary = "Cancels processing of batch. [Perm.BATCH_PROCESSING_WRITE]")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response"),
            @ApiResponse(responseCode = "404", description = "Instance does not exist")})
    @PreAuthorize("hasAuthority('" + Permissions.BATCH_PROCESSING_WRITE + "')")
    @RequestMapping(value = "/{batchId}/cancel", method = RequestMethod.POST)
    public void cancel(@Parameter(description = "Id of the batch to cancel", required = true) @PathVariable("batchId") String batchId) {
        Batch batch = batchService.find(batchId);
        notNull(batch, () -> new MissingObject(Batch.class, batchId));
        notNull(batch.getProducerProfile(), () -> new IllegalArgumentException("producer profile of batch " + batchId + " is null"));
        notNull(batch.getProducerProfile().getProducer(), () -> new IllegalArgumentException("producer of batch " + batchId + " is null"));
        coordinatorService.verifyProducer(batch.getProducerProfile().getProducer(), "User cannot cancel batch that does not belong to his producer.");

        coordinatorService.cancelBatch(new JmsDto(batchId, userDetails.getId()));
    }

    @Operation(summary = "Resumes processing of batch. [Perm.BATCH_PROCESSING_WRITE]", responses = {@ApiResponse(content = @Content(schema = @Schema(implementation = Boolean.class)))},
            description = "returns true, if the batch has succeeded to resume, false otherwise")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response"),
            @ApiResponse(responseCode = "404", description = "Instance does not exist")})
    @PreAuthorize("hasAuthority('" + Permissions.BATCH_PROCESSING_WRITE + "')")
    @RequestMapping(value = "/{batchId}/resume", method = RequestMethod.POST)
    public boolean resume(@Parameter(description = "Id of the batch to resume", required = true)
                          @PathVariable("batchId") String batchId) {
        return coordinatorService.resumeBatch(batchId);
    }

    @Operation(summary = "Gets DTOs of batches that respect the selected parameters. [Perm.BATCH_PROCESSING_READ]",
            description = "Filter/Sort fields = id, created, updated, config, producerId, producerName, producerProfileId," +
                    " producerProfileName, state (PROCESSING,SUSPENDED,CANCELED,PROCESSED), userId.." +
                    "SUPER_ADMIN can see all batches, ADMIN can see all batches of its producer and others see their batches " +
                    "i.e. batches they started manually or batches created by their routine")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response", content = @Content(schema = @Schema(implementation = Result.class)))})
    @PreAuthorize("hasAuthority('" + Permissions.BATCH_PROCESSING_READ + "')")
    @RequestMapping(value = "/list_dtos", method = RequestMethod.GET)
    public Result<BatchDto> listDtos(@Parameter(description = "Parameters to comply with", required = true)
                                     @ModelAttribute Params params) {
        if (!hasRole(userDetails, Permissions.SUPER_ADMIN_PRIVILEGE)) {
            addPrefilter(params, new Filter("producerId", FilterOperation.EQ, userDetails.getProducerId(), null));
        }
        return batchService.listBatchDtos(params);
    }

    @Operation(summary = "Exports DTOs of batches that respect the selected parameters. [Perm.BATCH_PROCESSING_READ]")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response")})
    @PreAuthorize("hasAuthority('" + Permissions.BATCH_PROCESSING_READ + "')")
    @RequestMapping(value = "/list_dtos/export", method = RequestMethod.GET)
    public void exportDtos(@Parameter(description = "Parameters to comply with", required = true)
                           @ModelAttribute Params params,
                           @Parameter(description = "Ignore pagination - export all", required = true) @RequestParam("ignorePagination") boolean ignorePagination,
                           @Parameter(description = "Export format", required = true) @RequestParam("format") TableExportType format,
                           @Parameter(description = "Export name", required = true) @RequestParam("name") String name,
                           @Parameter(description = "Ordered columns to export", required = true) @RequestParam("columns") List<String> columns,
                           @Parameter(description = "Ordered values of first row (header)", required = true) @RequestParam("header") List<String> header,
                           HttpServletResponse response) {
        if (!hasRole(userDetails, Permissions.SUPER_ADMIN_PRIVILEGE)) {
            addPrefilter(params, new Filter("producerId", FilterOperation.EQ, userDetails.getProducerId(), null));
        }
        batchService.exportBatchDtos(params, ignorePagination, name, columns, header, format, response);
    }

    @Operation(summary = "Exports ingest workflows of batch into file. [Perm.BATCH_PROCESSING_READ]")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response")})
    @PreAuthorize("hasAuthority('" + Permissions.BATCH_PROCESSING_READ + "')")
    @RequestMapping(value = "/{id}/ingest_workflow/export", method = RequestMethod.GET)
    public void exportIngestWorkflows(
            @Parameter(description = "ID of the batch", required = true) @PathVariable("id") String id,
            @Parameter(description = "Export format", required = true) @RequestParam("format") TableExportType format,
            @Parameter(description = "Export name", required = true) @RequestParam("name") String name,
            @Parameter(description = "Ordered columns to export", required = true) @RequestParam("columns") List<String> columns,
            @Parameter(description = "Ordered values of first row (header)", required = true) @RequestParam("header") List<String> header,
            HttpServletResponse response
    ) {
        batchService.exportIngestWorkflows(id, name, columns, header, format, response);
    }

    @Operation(summary = "Gets batch by id. [Perm.BATCH_PROCESSING_READ]")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response", content = @Content(schema = @Schema(implementation = Batch.class))),
            @ApiResponse(responseCode = "404", description = "Instance does not exist")})
    @PreAuthorize("hasAuthority('" + Permissions.BATCH_PROCESSING_READ + "')")
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public BatchDetailDto get(@Parameter(description = "Id of the batch", required = true)
                              @PathVariable("id") String id) {
        return batchService.getDetailView(id);
    }

    @Autowired
    public void setBatchService(BatchService batchService) {
        this.batchService = batchService;
    }

    @Autowired
    public void setCoordinatorService(CoordinatorService coordinatorService) {
        this.coordinatorService = coordinatorService;
    }

    @Autowired
    public void setUserDetails(UserDetails userDetails) {
        this.userDetails = userDetails;
    }
}
