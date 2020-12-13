package cz.cas.lib.arclib.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import cz.cas.lib.arclib.domain.ExportRoutine;
import cz.cas.lib.arclib.domainbase.exception.BadArgument;
import cz.cas.lib.arclib.security.authorization.permission.Permissions;
import cz.cas.lib.arclib.service.ExportRoutineService;
import cz.cas.lib.core.store.Transactional;
import io.swagger.annotations.*;
import lombok.Getter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import java.util.Collection;

import static cz.cas.lib.core.util.Utils.eq;

@RestController
@Api(value = "export routine", description = "Api for interaction with export routines")
@RequestMapping("/api/export_routine")
public class ExportRoutineApi {

    @Getter
    private ExportRoutineService exportRoutineService;

    @ApiOperation(value = "Saves an instance  [Perm.EXPORT_ROUTINE_WRITE]",
            notes = "Returns single instance (possibly with computed attributes).", response = ExportRoutine.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = ExportRoutine.class),
            @ApiResponse(code = 400, message = "Specified id does not correspond to the id of the instance")})
    @PreAuthorize("hasAuthority('" + Permissions.EXPORT_ROUTINE_WRITE + "')")
    @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
    @Transactional
    public ExportRoutine save(@ApiParam(value = "Id of the instance", required = true)
                              @PathVariable("id") String id,
                              @ApiParam(value = "Single instance", required = true)
                              @RequestBody ExportRoutine exportRoutine) throws JsonProcessingException {
        eq(id, exportRoutine.getId(), () -> new BadArgument("id"));

        return exportRoutineService.save(exportRoutine);
    }

    @ApiOperation(value = "Deletes an instance. [Perm.EXPORT_ROUTINE_WRITE]")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response"),
            @ApiResponse(code = 404, message = "Instance does not exist")})
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    @PreAuthorize("hasAuthority('" + Permissions.EXPORT_ROUTINE_WRITE + "')")
    @Transactional
    public void delete(@ApiParam(value = "Id of the instance", required = true)
                       @PathVariable("id") String id) {
        exportRoutineService.delete(id);
    }

    @ApiOperation(value = "Gets one instance specified by id [Perm.EXPORT_ROUTINE_READ]", response = ExportRoutine.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = ExportRoutine.class),
            @ApiResponse(code = 404, message = "Instance does not exist")})
    @PreAuthorize("hasAuthority('" + Permissions.EXPORT_ROUTINE_READ + "')")
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    @Transactional
    public ExportRoutine get(@ApiParam(value = "Id of the instance", required = true)
                             @PathVariable("id") String id) {
        return exportRoutineService.find(id);
    }

    @ApiOperation(value = "Gets all instances [Perm.EXPORT_ROUTINE_READ]", response = ExportRoutine.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = ExportRoutine.class)})
    @PreAuthorize("hasAuthority('" + Permissions.EXPORT_ROUTINE_READ + "')")
    @RequestMapping(method = RequestMethod.GET)
    @Transactional
    public Collection<ExportRoutine> list() {
        return exportRoutineService.find();
    }

    @Inject
    public void setExportRoutineService(ExportRoutineService exportRoutineService) {
        this.exportRoutineService = exportRoutineService;
    }
}
