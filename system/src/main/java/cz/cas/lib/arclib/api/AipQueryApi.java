package cz.cas.lib.arclib.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import cz.cas.lib.arclib.domain.AipQuery;
import cz.cas.lib.arclib.domain.export.ExportConfig;
import cz.cas.lib.arclib.domain.export.ExportRoutine;
import cz.cas.lib.arclib.domainbase.exception.BadArgument;
import cz.cas.lib.arclib.domainbase.exception.ForbiddenObject;
import cz.cas.lib.arclib.domainbase.exception.MissingObject;
import cz.cas.lib.arclib.dto.AipQueryDetailDto;
import cz.cas.lib.arclib.dto.AipQueryDto;
import cz.cas.lib.arclib.security.authorization.permission.Permissions;
import cz.cas.lib.arclib.security.user.UserDetails;
import cz.cas.lib.arclib.service.AipQueryService;
import cz.cas.lib.arclib.service.ExportRoutineService;
import io.swagger.annotations.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.util.List;

import static cz.cas.lib.arclib.utils.ArclibUtils.hasRole;
import static cz.cas.lib.core.util.Utils.eq;
import static cz.cas.lib.core.util.Utils.notNull;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@Api(value = "aip", description = "Api for saved AIP queries.")
@RequestMapping("/api/saved_query")
@Slf4j
public class AipQueryApi {

    private AipQueryService aipQueryService;
    private UserDetails userDetails;
    private ExportRoutineService exportRoutineService;

    @ApiOperation(value = "Gets saved query by ID [Perm.AIP_QUERY_RECORDS_READ]", response = AipQuery.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response")})
    @PreAuthorize("hasAuthority('" + Permissions.AIP_QUERY_RECORDS_READ + "')")
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public AipQueryDetailDto getSavedQuery(@ApiParam(value = "Id of the instance", required = true)
                                           @PathVariable("id") String id) {
        return aipQueryService.find(id);
    }

    @ApiOperation(value = "Gets DTOs of all saved queries of the user [Perm.AIP_QUERY_RECORDS_READ]",
            response = AipQueryDto.class, responseContainer = "list")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response")})
    @PreAuthorize("hasAuthority('" + Permissions.AIP_QUERY_RECORDS_READ + "')")
    @RequestMapping(method = RequestMethod.GET)
    public List<AipQueryDto> getSavedQueryDtos() {
        return aipQueryService.listSavedQueryDtos(userDetails.getId());
    }

    @ApiOperation(value = "Deletes saved query. [Perm.AIP_QUERY_RECORDS_WRITE]",
            notes = "If the calling user is not Roles.SUPER_ADMIN, the users producer must match the producer of the saved query.")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful response")})
    @PreAuthorize("hasAuthority('" + Permissions.AIP_QUERY_RECORDS_WRITE + "')")
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public void deleteSavedQuery(@ApiParam(value = "Id of the instance", required = true)
                                 @PathVariable("id") String id) {
        AipQuery query = aipQueryService.findWithUserInitialized(id);
        notNull(query, () -> new MissingObject(aipQueryService.getClass(), id));
        if (!hasRole(userDetails, Permissions.SUPER_ADMIN_PRIVILEGE) &&
                !userDetails.getProducerId().equals(query.getUser().getProducer().getId()))
            throw new ForbiddenObject(AipQuery.class, id);
        aipQueryService.delete(query);
        log.debug("Aip query: " + id + " has been deleted.");
    }

    @ApiOperation(value = "Deletes an instance. [Perm.EXPORT_ROUTINE_WRITE]")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response"),
            @ApiResponse(code = 404, message = "Instance does not exist")})
    @RequestMapping(value = "/export_routine/{id}", method = RequestMethod.DELETE)
    @PreAuthorize("hasAuthority('" + Permissions.EXPORT_ROUTINE_WRITE + "')")
    public void deleteExportRoutine(@ApiParam(value = "Id of the instance", required = true)
                                    @PathVariable("id") String id) {
        exportRoutineService.delete(id);
    }

    @ApiOperation(value = "Saves an instance  [Perm.EXPORT_ROUTINE_WRITE]",
            notes = "Returns single instance (possibly with computed attributes).", response = ExportRoutine.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = ExportRoutine.class),
            @ApiResponse(code = 400, message = "Specified id does not correspond to the id of the instance")})
    @PreAuthorize("hasAuthority('" + Permissions.EXPORT_ROUTINE_WRITE + "')")
    @RequestMapping(value = "/export_routine/{id}", method = RequestMethod.PUT)
    public ExportRoutine save(@ApiParam(value = "Id of the instance", required = true) @PathVariable("id") String id,
                              @ApiParam(value = "Single instance", required = true) @RequestBody @Valid ExportRoutine exportRoutine) throws JsonProcessingException {
        eq(id, exportRoutine.getId(), () -> new BadArgument("id"));
        return exportRoutineService.save(exportRoutine);
    }

    @ApiOperation(value = "Starts download of requested data to client PC. [Perm.EXPORT_FILES]")
    @RequestMapping(value = "/{id}/download", method = RequestMethod.POST)
    @PreAuthorize("hasAuthority('" + Permissions.EXPORT_FILES + "')")
    public void downloadData(@ApiParam(value = "Id of the saved query OR 'bucket' to download data of users bucket", required = true)
                             @PathVariable("id") String id,
                             @ApiParam(value = "Export config", required = true) @RequestBody @Valid ExportConfig exportConfig,
                             HttpServletResponse response) throws IOException {
        aipQueryService.downloadResult(id, exportConfig, response);
    }

    @ApiOperation(value = "Starts export of requested data to client workspace. [Perm.EXPORT_FILES]")
    @RequestMapping(value = "/{id}/export", method = RequestMethod.POST, produces = APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('" + Permissions.EXPORT_FILES + "')")
    public String exportData(@ApiParam(value = "Id of the saved query OR 'bucket' to export data of users bucket", required = true)
                             @PathVariable("id") String id,
                             @ApiParam(value = "Export config", required = true) @RequestBody @Valid ExportConfig exportConfig) throws IOException {
        return aipQueryService.exportResult(id, exportConfig, true).toString();
    }

    @Inject
    public void setExportRoutineService(ExportRoutineService exportRoutineService) {
        this.exportRoutineService = exportRoutineService;
    }

    @Inject
    public void setAipQueryService(AipQueryService aipQueryService) {
        this.aipQueryService = aipQueryService;
    }

    @Inject
    public void setUserDetails(UserDetails userDetails) {
        this.userDetails = userDetails;
    }
}
