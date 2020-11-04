package cz.cas.lib.arclib.api;

import cz.cas.lib.arclib.dto.IngestWorkflowDto;
import cz.cas.lib.arclib.security.authorization.data.Permissions;
import cz.cas.lib.arclib.service.IngestWorkflowService;
import io.swagger.annotations.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Inject;

@RestController
@Api(value = "ingest workflow", description = "Api for interaction with retrieving ingest workflows")
@RequestMapping("/api/ingest_workflow")
public class IngestWorkflowApi {

    private IngestWorkflowService ingestWorkflowService;

    @ApiOperation(value = "Gets ingest workflow with details. [Perm.INGEST_WORKFLOWS_READ]", response = IngestWorkflowDto.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = IngestWorkflowDto.class)})
    @PreAuthorize("hasAuthority('" + Permissions.INGEST_WORKFLOWS_READ + "')")
    @RequestMapping(value = "/{externalId}", method = RequestMethod.GET)
    public IngestWorkflowDto get(@ApiParam(value = "External ID of the ingest workflow", required = true)
                                 @PathVariable("externalId") String externalId) {
        return ingestWorkflowService.getInfo(externalId);
    }

    @Inject
    public void setIngestWorkflowService(IngestWorkflowService ingestWorkflowService) {
        this.ingestWorkflowService = ingestWorkflowService;
    }
}
