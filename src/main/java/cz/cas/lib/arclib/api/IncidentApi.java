package cz.cas.lib.arclib.api;

import cz.cas.lib.arclib.dto.IncidentCancellationDto;
import cz.cas.lib.arclib.dto.IncidentInfoDto;
import cz.cas.lib.arclib.dto.IncidentSolutionDto;
import cz.cas.lib.arclib.security.authorization.Roles;
import cz.cas.lib.arclib.service.incident.IncidentService;
import cz.cas.lib.arclib.service.incident.IncidentSortField;
import cz.cas.lib.core.index.dto.Order;
import io.swagger.annotations.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

import javax.annotation.security.RolesAllowed;
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

    @ApiOperation(value = "Gets incidents of active processes instances of specified batch.",
            notes = "Returns sorted list of active incidents.", response = IncidentInfoDto.class, responseContainer = "list")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response")})
    @RequestMapping(value = "/batch/{batchId}", method = RequestMethod.GET)
    public Collection<IncidentInfoDto> getIncidents(
            @ApiParam(value = "Id of batch", required = true) @PathVariable("batchId") String batchId,
            @ApiParam(value = "Sort field") @RequestParam(value = "sort", required = false) IncidentSortField sort,
            @ApiParam(value = "Sort order") @RequestParam(value = "order", required = false, defaultValue = "DESC") Order order
    ) {
        sort = sort == null ? IncidentSortField.TIMESTAMP : sort;
        return incidentService.getIncidentsOfBatch(batchId, sort, order);
    }

    @ApiOperation(value = "Solves incidents by changing config and executing jobs again. Roles.ANALYST, Roles.SUPER_ADMIN")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response"),
            @ApiResponse(code = 400, message = "User not logged in or config not valid json"),
            @ApiResponse(code = 404, message = "Some incident does not exist")
    })
    @RolesAllowed({Roles.ANALYST, Roles.SUPER_ADMIN})
    @RequestMapping(value = "/solve", method = RequestMethod.POST, consumes = "application/json")
    public void solveIncidents(
            @ApiParam(value = "solution data", required = true) @Valid @RequestBody IncidentSolutionDto solution
    ) throws IOException {
        incidentService.solveIncidents(solution.getIds(), solution.getConfig());
    }

    @ApiOperation(value = "Cancels incidents. Roles.ANALYST, Roles.SUPER_ADMIN")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response"),
            @ApiResponse(code = 404, message = "Some incident does not exist")
    })
    @RolesAllowed({Roles.ANALYST, Roles.SUPER_ADMIN})
    @RequestMapping(value = "/cancel", method = RequestMethod.POST, consumes = "application/json")
    public void cancelIncidents(
            @ApiParam(value = "cancellation data", required = true) @RequestBody @Valid IncidentCancellationDto cancellation
    ) throws IOException {
        incidentService.cancelIncidents(cancellation.getIds(), cancellation.getReason());
    }

    @ApiOperation(value = "Gets all incidents of active processes instances",
            notes = "Returns sorted list of active incidents.", response = IncidentInfoDto.class, responseContainer = "list")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response")})
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
