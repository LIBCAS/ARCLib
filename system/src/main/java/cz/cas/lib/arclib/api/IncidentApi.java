package cz.cas.lib.arclib.api;

import cz.cas.lib.arclib.dto.IncidentCancellationDto;
import cz.cas.lib.arclib.dto.IncidentInfoDto;
import cz.cas.lib.arclib.dto.IncidentSolutionDto;
import cz.cas.lib.arclib.security.authorization.permission.Permissions;
import cz.cas.lib.arclib.service.incident.IncidentService;
import cz.cas.lib.arclib.service.incident.IncidentSortField;
import cz.cas.lib.core.index.dto.Order;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Collection;

@ConditionalOnProperty(prefix = "bpm", name = "enabled", havingValue = "true")
@RestController
@Tag(name = "incident", description = "Api for interaction with incidents")
@RequestMapping("/api/incident")
public class IncidentApi {

    private IncidentService incidentService;

    @Operation(summary = "Gets incidents of active processes instances of specified batch. [Perm.INCIDENT_RECORDS_READ]",
            description = "Returns sorted list of active incidents.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response", content = @Content(array = @ArraySchema(schema = @Schema(implementation = IncidentInfoDto.class))))})
    @PreAuthorize("hasAuthority('" + Permissions.INCIDENT_RECORDS_READ + "')")
    @RequestMapping(value = "/batch/{batchId}", method = RequestMethod.GET)
    public Collection<IncidentInfoDto> getIncidents(
            @Parameter(description = "Id of batch", required = true) @PathVariable("batchId") String batchId,
            @Parameter(description = "Sort field") @RequestParam(value = "sort", required = false) IncidentSortField sort,
            @Parameter(description = "Sort order") @RequestParam(value = "order", required = false, defaultValue = "DESC") Order order
    ) {
        sort = sort == null ? IncidentSortField.TIMESTAMP : sort;
        return incidentService.getIncidentsOfBatch(batchId, sort, order);
    }

    @Operation(summary = "Solves incidents by changing config and executing jobs again. [Perm.INCIDENT_RECORDS_WRITE]")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response"),
            @ApiResponse(responseCode = "400", description = "User not logged in or config not valid json"),
            @ApiResponse(responseCode = "404", description = "Some incident does not exist")
    })
    @PreAuthorize("hasAuthority('" + Permissions.INCIDENT_RECORDS_WRITE + "')")
    @RequestMapping(value = "/solve", method = RequestMethod.POST, consumes = "application/json")
    public void solveIncidents(
            @Parameter(description = "solution data", required = true) @Valid @RequestBody IncidentSolutionDto solution) {
        incidentService.solveIncidents(solution.getIds(), solution.getConfig());
    }

    @Operation(summary = "Cancels incidents. [Perm.INCIDENT_RECORDS_WRITE]")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response"),
            @ApiResponse(responseCode = "404", description = "Some incident does not exist")
    })
    @PreAuthorize("hasAuthority('" + Permissions.INCIDENT_RECORDS_WRITE + "')")
    @RequestMapping(value = "/cancel", method = RequestMethod.POST, consumes = "application/json")
    public void cancelIncidents(
            @Parameter(description = "cancellation data", required = true) @RequestBody @Valid IncidentCancellationDto cancellation
    ) throws IOException {
        incidentService.cancelIncidents(cancellation.getIds(), cancellation.getReason());
    }

    @Operation(summary = "Gets all incidents of active processes instances [Perm.INCIDENT_RECORDS_READ]",
            description = "Returns sorted list of active incidents.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response", content = @Content(array = @ArraySchema(schema = @Schema(implementation = IncidentInfoDto.class))))})
    @PreAuthorize("hasAuthority('" + Permissions.INCIDENT_RECORDS_READ + "')")
    @RequestMapping(value = "/all", method = RequestMethod.GET)
    public Collection<IncidentInfoDto> getAllIncidents(
            @Parameter(description = "Sort field") @RequestParam(value = "sort", required = false) IncidentSortField sort,
            @Parameter(description = "Sort order") @RequestParam(value = "order", required = false, defaultValue = "DESC") Order order
    ) {
        sort = sort == null ? IncidentSortField.TIMESTAMP : sort;
        return incidentService.getIncidentsOfBatch(null, sort, order);
    }

    @Autowired
    public void setIncidentService(IncidentService incidentService) {
        this.incidentService = incidentService;
    }
}
