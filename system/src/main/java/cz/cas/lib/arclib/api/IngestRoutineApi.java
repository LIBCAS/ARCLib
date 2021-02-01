package cz.cas.lib.arclib.api;

import cz.cas.lib.arclib.domain.IngestRoutine;
import cz.cas.lib.arclib.domainbase.exception.BadArgument;
import cz.cas.lib.arclib.dto.IngestRoutineDto;
import cz.cas.lib.arclib.security.authorization.permission.Permissions;
import cz.cas.lib.arclib.service.IngestRoutineService;
import io.swagger.annotations.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import java.util.Collection;

import static cz.cas.lib.core.util.Utils.eq;

@Api(value = "ingest routine", description = "Api for interaction with ingest routines")
@RequestMapping("/api/ingest_routine")
@RestController
public class IngestRoutineApi {

    private IngestRoutineService service;

    @ApiOperation(value = "Saves an instance [Perm.INGEST_ROUTINE_RECORDS_WRITE]",
            notes = "Returns single instance (possibly with computed attributes).")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = IngestRoutine.class),
            @ApiResponse(code = 400, message = "Specified id does not correspond to the id of the instance"),
            @ApiResponse(code = 403, message = "Property 'auto' is not editable")
    })
    @PreAuthorize("hasAuthority('" + Permissions.INGEST_ROUTINE_RECORDS_WRITE + "')")
    @PutMapping(value = "/{id}")
    public IngestRoutine save(@ApiParam(value = "Id of the instance", required = true) @PathVariable("id") String id,
                              @ApiParam(value = "Single instance", required = true) @RequestBody IngestRoutine ingestRoutine) {
        eq(id, ingestRoutine.getId(), () -> new BadArgument("id"));
        return service.save(ingestRoutine);
    }

    @ApiOperation(value = "Gets one instance specified by id [Perm.INGEST_ROUTINE_RECORDS_READ]")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = IngestRoutine.class),
            @ApiResponse(code = 404, message = "Instance does not exist")})
    @PreAuthorize("hasAuthority('" + Permissions.INGEST_ROUTINE_RECORDS_READ + "')")
    @GetMapping(value = "/{id}")
    public IngestRoutine get(@ApiParam(value = "Id of the instance", required = true) @PathVariable("id") String id) {
        return service.find(id);
    }

    @ApiOperation(value = "Deletes an instance. [Perm.INGEST_ROUTINE_RECORDS_WRITE]")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response"),
            @ApiResponse(code = 404, message = "Instance does not exist")})
    @PreAuthorize("hasAuthority('" + Permissions.INGEST_ROUTINE_RECORDS_WRITE + "')")
    @DeleteMapping(value = "/{id}")
    public void delete(@ApiParam(value = "Id of the instance", required = true) @PathVariable("id") String id) {
        service.delete(id);
    }

    @ApiOperation(value = "Gets DTOs of all instances [Perm.INGEST_ROUTINE_RECORDS_READ]")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "OK", response = IngestRoutineDto.class, responseContainer = "List")})
    @PreAuthorize("hasAuthority('" + Permissions.INGEST_ROUTINE_RECORDS_READ + "')")
    @GetMapping(value = "/list_dtos")
    public Collection<IngestRoutineDto> listDtos() {
        return service.listIngestRoutineDtos();
    }

    @Inject
    public void setService(IngestRoutineService service) {
        this.service = service;
    }
}
