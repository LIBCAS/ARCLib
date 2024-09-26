package cz.cas.lib.arclib.api;

import cz.cas.lib.arclib.dto.IngestWorkflowDto;
import cz.cas.lib.arclib.security.authorization.permission.Permissions;
import cz.cas.lib.arclib.service.IngestWorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

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

    @Autowired
    public void setIngestWorkflowService(IngestWorkflowService ingestWorkflowService) {
        this.ingestWorkflowService = ingestWorkflowService;
    }
}
