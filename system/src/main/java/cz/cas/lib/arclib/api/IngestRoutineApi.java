package cz.cas.lib.arclib.api;

import cz.cas.lib.arclib.domain.IngestRoutine;
import cz.cas.lib.arclib.domainbase.exception.BadArgument;
import cz.cas.lib.arclib.dto.IngestRoutineDto;
import cz.cas.lib.arclib.security.authorization.permission.Permissions;
import cz.cas.lib.arclib.service.IngestRoutineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

import static cz.cas.lib.core.util.Utils.eq;

@Tag(name = "ingest routine", description = "Api for interaction with ingest routines")
@RequestMapping("/api/ingest_routine")
@RestController
public class IngestRoutineApi {

    private IngestRoutineService service;

    @Operation(summary = "Saves an instance [Perm.INGEST_ROUTINE_RECORDS_WRITE]",
            description = "Returns single instance (possibly with computed attributes).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response", content = @Content(schema = @Schema(implementation = IngestRoutine.class))),
            @ApiResponse(responseCode = "400", description = "Specified id does not correspond to the id of the instance"),
            @ApiResponse(responseCode = "403", description = "Property 'auto' is not editable")
    })
    @PreAuthorize("hasAuthority('" + Permissions.INGEST_ROUTINE_RECORDS_WRITE + "')")
    @PutMapping(value = "/{id}")
    public IngestRoutine save(@Parameter(description = "Id of the instance", required = true) @PathVariable("id") String id,
                              @Parameter(description = "Single instance", required = true) @RequestBody IngestRoutine ingestRoutine) {
        eq(id, ingestRoutine.getId(), () -> new BadArgument("id"));
        return service.save(ingestRoutine);
    }

    @Operation(summary = "Gets one instance specified by id [Perm.INGEST_ROUTINE_RECORDS_READ]")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response", content = @Content(schema = @Schema(implementation = IngestRoutine.class))),
            @ApiResponse(responseCode = "404", description = "Instance does not exist")})
    @PreAuthorize("hasAuthority('" + Permissions.INGEST_ROUTINE_RECORDS_READ + "')")
    @GetMapping(value = "/{id}")
    public IngestRoutine get(@Parameter(description = "Id of the instance", required = true) @PathVariable("id") String id) {
        return service.find(id);
    }

    @Operation(summary = "Deletes an instance. [Perm.INGEST_ROUTINE_RECORDS_WRITE]")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response"),
            @ApiResponse(responseCode = "404", description = "Instance does not exist")})
    @PreAuthorize("hasAuthority('" + Permissions.INGEST_ROUTINE_RECORDS_WRITE + "')")
    @DeleteMapping(value = "/{id}")
    public void delete(@Parameter(description = "Id of the instance", required = true) @PathVariable("id") String id) {
        service.delete(id);
    }

    @Operation(summary = "Gets DTOs of all instances [Perm.INGEST_ROUTINE_RECORDS_READ]")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK", content = @Content(array = @ArraySchema(schema = @Schema(implementation = IngestRoutineDto.class))))})
    @PreAuthorize("hasAuthority('" + Permissions.INGEST_ROUTINE_RECORDS_READ + "')")
    @GetMapping(value = "/list_dtos")
    public Collection<IngestRoutineDto> listDtos() {
        return service.listIngestRoutineDtos();
    }

    @Autowired
    public void setService(IngestRoutineService service) {
        this.service = service;
    }
}
