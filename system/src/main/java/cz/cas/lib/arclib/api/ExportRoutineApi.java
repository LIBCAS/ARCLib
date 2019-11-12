package cz.cas.lib.arclib.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import cz.cas.lib.arclib.domain.ExportRoutine;
import cz.cas.lib.arclib.security.authorization.Roles;
import cz.cas.lib.arclib.service.ExportRoutineService;
import cz.cas.lib.arclib.domainbase.exception.BadArgument;
import cz.cas.lib.core.store.Transactional;
import io.swagger.annotations.*;
import lombok.Getter;
import org.springframework.web.bind.annotation.*;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import java.util.Collection;

import static cz.cas.lib.core.util.Utils.eq;

@RestController
@Api(value = "export routine", description = "Api for interaction with export routines")
@RequestMapping("/api/export_routine")
public class ExportRoutineApi {

    @Getter
    private ExportRoutineService exportRoutineService;

    @ApiOperation(value = "Saves an instance", notes = "Returns single instance (possibly with computed attributes). Roles.ADMIN, Roles.SUPER_ADMIN",
            response = ExportRoutine.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = ExportRoutine.class),
            @ApiResponse(code = 400, message = "Specified id does not correspond to the id of the instance")})
    @RolesAllowed({Roles.ADMIN, Roles.SUPER_ADMIN})
    @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
    @Transactional
    public ExportRoutine save(@ApiParam(value = "Id of the instance", required = true)
                              @PathVariable("id") String id,
                              @ApiParam(value = "Single instance", required = true)
                              @RequestBody ExportRoutine exportRoutine) throws JsonProcessingException {
        eq(id, exportRoutine.getId(), () -> new BadArgument("id"));

        return exportRoutineService.save(exportRoutine);
    }

    @ApiOperation(value = "Deletes an instance. Roles.ADMIN, Roles.SUPER_ADMIN")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response"),
            @ApiResponse(code = 404, message = "Instance does not exist")})
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    @Transactional
    @RolesAllowed({Roles.ADMIN, Roles.SUPER_ADMIN})
    public void delete(@ApiParam(value = "Id of the instance", required = true)
                       @PathVariable("id") String id) {
        exportRoutineService.delete(id);
    }

    @ApiOperation(value = "Gets one instance specified by id", response = ExportRoutine.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = ExportRoutine.class),
            @ApiResponse(code = 404, message = "Instance does not exist")})
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    @Transactional
    public ExportRoutine get(@ApiParam(value = "Id of the instance", required = true)
                             @PathVariable("id") String id) {
        return exportRoutineService.find(id);
    }

    @ApiOperation(value = "Gets one instance of export routine by id of the respective aip query", response = ExportRoutine.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = ExportRoutine.class),
            @ApiResponse(code = 404, message = "Instance does not exist")})
    @RequestMapping(value = "aip_query/{aipQueryId}", method = RequestMethod.GET)
    @Transactional
    public ExportRoutine findByAipQueryId(@ApiParam(value = "Id of the aip query", required = true)
                             @PathVariable("aipQueryId") String aipQueryId) {
        return exportRoutineService.findByAipQueryId(aipQueryId);
    }

    @ApiOperation(value = "Gets all instances", response = Collection.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = Collection.class)})
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
