package cz.cas.lib.arclib.api;

import cz.cas.lib.arclib.domain.IngestRoutine;
import cz.cas.lib.arclib.domainbase.exception.BadArgument;
import cz.cas.lib.arclib.dto.IngestRoutineDto;
import cz.cas.lib.arclib.security.authorization.data.Permissions;
import cz.cas.lib.arclib.service.IngestRoutineService;
import cz.cas.lib.core.store.Transactional;
import io.swagger.annotations.*;
import lombok.Getter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import java.util.Collection;

import static cz.cas.lib.core.util.Utils.eq;

@RestController
@Api(value = "ingest routine", description = "Api for interaction with ingest routines")
@RequestMapping("/api/ingest_routine")
public class IngestRoutineApi {

    @Getter
    private IngestRoutineService ingestRoutineService;

    @ApiOperation(value = "Saves an instance [Perm.INGEST_ROUTINE_RECORDS_WRITE]", notes = "Returns single instance (possibly with computed attributes).",
            response = IngestRoutine.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = IngestRoutine.class),
            @ApiResponse(code = 400, message = "Specified id does not correspond to the id of the instance")})
    @PreAuthorize("hasAuthority('" + Permissions.INGEST_ROUTINE_RECORDS_WRITE + "')")
    @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
    public IngestRoutine save(@ApiParam(value = "Id of the instance", required = true)
                              @PathVariable("id") String id,
                              @ApiParam(value = "Single instance", required = true)
                              @RequestBody IngestRoutine ingestRoutine) {
        eq(id, ingestRoutine.getId(), () -> new BadArgument("id"));
        return ingestRoutineService.save(ingestRoutine);
    }

    @ApiOperation(value = "Deletes an instance. [Perm.INGEST_ROUTINE_RECORDS_WRITE]")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response"),
            @ApiResponse(code = 404, message = "Instance does not exist")})
    @PreAuthorize("hasAuthority('" + Permissions.INGEST_ROUTINE_RECORDS_WRITE + "')")
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public void delete(@ApiParam(value = "Id of the instance", required = true)
                       @PathVariable("id") String id) {
        ingestRoutineService.delete(id);
    }

    @ApiOperation(value = "Gets one instance specified by id [Perm.INGEST_ROUTINE_RECORDS_READ]", response = IngestRoutine.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = IngestRoutine.class),
            @ApiResponse(code = 404, message = "Instance does not exist")})
    @PreAuthorize("hasAuthority('" + Permissions.INGEST_ROUTINE_RECORDS_READ + "')")
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public IngestRoutine get(@ApiParam(value = "Id of the instance", required = true)
                             @PathVariable("id") String id) {
        return ingestRoutineService.find(id);
    }

    @ApiOperation(value = "Gets DTOs of all instances [Perm.INGEST_ROUTINE_RECORDS_READ]", response = Collection.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = Collection.class)})
    @PreAuthorize("hasAuthority('" + Permissions.INGEST_ROUTINE_RECORDS_READ + "')")
    @RequestMapping(value = "/list_dtos", method = RequestMethod.GET)
    @Transactional
    public Collection<IngestRoutineDto> listDtos() {
        return ingestRoutineService.listIngestRoutineDtos();
    }

    @Inject
    public void setIngestRoutineService(IngestRoutineService ingestRoutineService) {
        this.ingestRoutineService = ingestRoutineService;
    }
}
