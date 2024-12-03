package cz.cas.lib.arclib.api;

import cz.cas.lib.arclib.dto.IngestWorkflowDto;
import cz.cas.lib.arclib.report.ExportFormat;
import cz.cas.lib.arclib.security.authorization.permission.Permissions;
import cz.cas.lib.arclib.service.IngestWorkflowService;
import cz.cas.lib.arclib.service.tableexport.TableExportType;
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

import java.util.List;

@RestController
@Tag(name = "ingest workflow", description = "Api for interaction with retrieving ingest workflows")
@RequestMapping("/api/ingest_workflow")
public class IngestWorkflowApi {

    private IngestWorkflowService ingestWorkflowService;

    @Operation(summary = "Gets ingest workflow with details. [Perm.INGEST_WORKFLOWS_READ]")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response", content = @Content(schema = @Schema(implementation = IngestWorkflowDto.class)))})
    @PreAuthorize("hasAuthority('" + Permissions.INGEST_WORKFLOWS_READ + "')")
    @RequestMapping(value = "/{externalId}", method = RequestMethod.GET)
    public IngestWorkflowDto get(@Parameter(description = "External ID of the ingest workflow", required = true)
                                 @PathVariable("externalId") String externalId) {
        return ingestWorkflowService.getInfo(externalId);
    }

    @Operation(summary = "Exports events of ingest workflow into file. [Perm.INGEST_WORKFLOWS_READ]")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response")})
    @PreAuthorize("hasAuthority('" + Permissions.INGEST_WORKFLOWS_READ + "')")
    @RequestMapping(value = "/{externalId}/event/export", method = RequestMethod.GET)
    public void exportEvents(
            @Parameter(description = "External ID of the ingest workflow", required = true) @PathVariable("externalId") String externalId,
            @Parameter(description = "Export format", required = true) @RequestParam("format") TableExportType format,
            @Parameter(description = "Export name", required = true) @RequestParam("name") String name,
            @Parameter(description = "Ordered columns to export", required = true) @RequestParam("columns") List<String> columns,
            @Parameter(description = "Ordered values of first row (header)", required = true) @RequestParam("header") List<String> header,
            HttpServletResponse response
    ) {
        ingestWorkflowService.exportEvents(externalId, name, columns, header, format, response);
    }

    @Operation(summary = "Exports process variables of ingest workflow into file. [Perm.INGEST_WORKFLOWS_READ]")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response")})
    @PreAuthorize("hasAuthority('" + Permissions.INGEST_WORKFLOWS_READ + "')")
    @RequestMapping(value = "/{externalId}/process_variable/export", method = RequestMethod.GET)
    public void exportProcessVariables(
            @Parameter(description = "External ID of the ingest workflow", required = true) @PathVariable("externalId") String externalId,
            @Parameter(description = "Export format", required = true) @RequestParam("format") TableExportType format,
            @Parameter(description = "Export name", required = true) @RequestParam("name") String name,
            HttpServletResponse response
    ) {
        ingestWorkflowService.exportProcessVariables(externalId, name, format, response);
    }

    @Autowired
    public void setIngestWorkflowService(IngestWorkflowService ingestWorkflowService) {
        this.ingestWorkflowService = ingestWorkflowService;
    }
}
