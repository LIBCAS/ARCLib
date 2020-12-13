package cz.cas.lib.arclib.api;

import cz.cas.lib.arclib.dto.IncidentCancellationDto;
import cz.cas.lib.arclib.dto.IncidentInfoDto;
import cz.cas.lib.arclib.dto.IncidentSolutionDto;
import cz.cas.lib.arclib.security.authorization.permission.Permissions;
import cz.cas.lib.arclib.service.incident.IncidentService;
import cz.cas.lib.arclib.service.incident.IncidentSortField;
import cz.cas.lib.core.index.dto.Order;
import io.swagger.annotations.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import javax.validation.Valid;
import java.io.IOException;
import java.util.Collection;

@ConditionalOnProperty(prefix = "bpm", name = "enabled", havingValue = "true")
@RestController
@Api(value = "incident", description = "Api for interaction with incidents")
@RequestMapping("/api/incident")
public class IncidentApi {

    private IncidentService incidentService;

    @ApiOperation(value = "Gets incidents of active processes instances of specified batch. [Perm.INCIDENT_RECORDS_READ]",
            notes = "Returns sorted list of active incidents.", response = IncidentInfoDto.class, responseContainer = "list")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response")})
    @PreAuthorize("hasAuthority('" + Permissions.INCIDENT_RECORDS_READ + "')")
    @RequestMapping(value = "/batch/{batchId}", method = RequestMethod.GET)
    public Collection<IncidentInfoDto> getIncidents(
            @ApiParam(value = "Id of batch", required = true) @PathVariable("batchId") String batchId,
            @ApiParam(value = "Sort field") @RequestParam(value = "sort", required = false) IncidentSortField sort,
            @ApiParam(value = "Sort order") @RequestParam(value = "order", required = false, defaultValue = "DESC") Order order
    ) {
        sort = sort == null ? IncidentSortField.TIMESTAMP : sort;
        return incidentService.getIncidentsOfBatch(batchId, sort, order);
    }

    @ApiOperation(value = "Solves incidents by changing config and executing jobs again. [Perm.INCIDENT_RECORDS_WRITE]")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response"),
            @ApiResponse(code = 400, message = "User not logged in or config not valid json"),
            @ApiResponse(code = 404, message = "Some incident does not exist")
    })
    @PreAuthorize("hasAuthority('" + Permissions.INCIDENT_RECORDS_WRITE + "')")
    @RequestMapping(value = "/solve", method = RequestMethod.POST, consumes = "application/json")
    public void solveIncidents(
            @ApiParam(value = "solution data", required = true) @Valid @RequestBody IncidentSolutionDto solution) {
        incidentService.solveIncidents(solution.getIds(), solution.getConfig());
    }

    @ApiOperation(value = "Cancels incidents. [Perm.INCIDENT_RECORDS_WRITE]")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response"),
            @ApiResponse(code = 404, message = "Some incident does not exist")
    })
    @PreAuthorize("hasAuthority('" + Permissions.INCIDENT_RECORDS_WRITE + "')")
    @RequestMapping(value = "/cancel", method = RequestMethod.POST, consumes = "application/json")
    public void cancelIncidents(
            @ApiParam(value = "cancellation data", required = true) @RequestBody @Valid IncidentCancellationDto cancellation
    ) throws IOException {
        incidentService.cancelIncidents(cancellation.getIds(), cancellation.getReason());
    }

    @ApiOperation(value = "Gets all incidents of active processes instances [Perm.INCIDENT_RECORDS_READ]",
            notes = "Returns sorted list of active incidents.", response = IncidentInfoDto.class, responseContainer = "list")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response")})
    @PreAuthorize("hasAuthority('" + Permissions.INCIDENT_RECORDS_READ + "')")
    @RequestMapping(value = "/all", method = RequestMethod.GET)
    public Collection<IncidentInfoDto> getAllIncidents(
            @ApiParam(value = "Sort field") @RequestParam(value = "sort", required = false) IncidentSortField sort,
            @ApiParam(value = "Sort order") @RequestParam(value = "order", required = false, defaultValue = "DESC") Order order
    ) {
        sort = sort == null ? IncidentSortField.TIMESTAMP : sort;
        return incidentService.getIncidentsOfBatch(null, sort, order);
    }

    @Inject
    public void setIncidentService(IncidentService incidentService) {
        this.incidentService = incidentService;
    }
}
