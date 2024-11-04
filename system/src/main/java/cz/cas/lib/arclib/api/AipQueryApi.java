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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

import static cz.cas.lib.arclib.utils.ArclibUtils.hasRole;
import static cz.cas.lib.core.util.Utils.eq;
import static cz.cas.lib.core.util.Utils.notNull;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@Tag(name = "aip", description = "Api for saved AIP queries.")
@RequestMapping("/api/saved_query")
@Slf4j
public class AipQueryApi {

    private AipQueryService aipQueryService;
    private UserDetails userDetails;
    private ExportRoutineService exportRoutineService;

    @Operation(summary = "Gets saved query by ID [Perm.AIP_QUERY_RECORDS_READ]", responses = {@ApiResponse(content = @Content(schema = @Schema(implementation = AipQuery.class)))})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response")})
    @PreAuthorize("hasAuthority('" + Permissions.AIP_QUERY_RECORDS_READ + "')")
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public AipQueryDetailDto getSavedQuery(@Parameter(description = "Id of the instance", required = true)
                                           @PathVariable("id") String id) {
        return aipQueryService.find(id);
    }

    @Operation(summary = "Gets DTOs of all saved queries of the user [Perm.AIP_QUERY_RECORDS_READ]",
            responses = {@ApiResponse(content = @Content(array = @ArraySchema(schema = @Schema(implementation = AipQueryDto.class))))})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response")})
    @PreAuthorize("hasAuthority('" + Permissions.AIP_QUERY_RECORDS_READ + "')")
    @RequestMapping(method = RequestMethod.GET)
    public List<AipQueryDto> getSavedQueryDtos() {
        return aipQueryService.listSavedQueryDtos(userDetails.getId());
    }

    @Operation(summary = "Deletes saved query. [Perm.AIP_QUERY_RECORDS_WRITE]",
            description = "If the calling user is not Roles.SUPER_ADMIN, the users producer must match the producer of the saved query.")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Successful response")})
    @PreAuthorize("hasAuthority('" + Permissions.AIP_QUERY_RECORDS_WRITE + "')")
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public void deleteSavedQuery(@Parameter(description = "Id of the instance", required = true)
                                 @PathVariable("id") String id) {
        AipQuery query = aipQueryService.findWithUserInitialized(id);
        notNull(query, () -> new MissingObject(aipQueryService.getClass(), id));
        if (!hasRole(userDetails, Permissions.SUPER_ADMIN_PRIVILEGE) &&
                !userDetails.getProducerId().equals(query.getUser().getProducer().getId()))
            throw new ForbiddenObject(AipQuery.class, id);
        aipQueryService.delete(query);
        log.debug("Aip query: " + id + " has been deleted.");
    }

    @Operation(summary = "Deletes an instance. [Perm.EXPORT_ROUTINE_WRITE]")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response"),
            @ApiResponse(responseCode = "404", description = "Instance does not exist")})
    @RequestMapping(value = "/export_routine/{id}", method = RequestMethod.DELETE)
    @PreAuthorize("hasAuthority('" + Permissions.EXPORT_ROUTINE_WRITE + "')")
    public void deleteExportRoutine(@Parameter(description = "Id of the instance", required = true)
                                    @PathVariable("id") String id) {
        exportRoutineService.delete(id);
    }

    @Operation(summary = "Saves an instance  [Perm.EXPORT_ROUTINE_WRITE]",
            description = "Returns single instance (possibly with computed attributes).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response", content = @Content(schema = @Schema(implementation = ExportRoutine.class))),
            @ApiResponse(responseCode = "400", description = "Specified id does not correspond to the id of the instance")})
    @PreAuthorize("hasAuthority('" + Permissions.EXPORT_ROUTINE_WRITE + "')")
    @RequestMapping(value = "/export_routine/{id}", method = RequestMethod.PUT)
    public ExportRoutine save(@Parameter(description = "Id of the instance", required = true) @PathVariable("id") String id,
                              @Parameter(description = "Single instance", required = true) @RequestBody @Valid ExportRoutine exportRoutine) throws JsonProcessingException {
        eq(id, exportRoutine.getId(), () -> new BadArgument("id"));
        return exportRoutineService.save(exportRoutine);
    }

    @Operation(summary = "Starts download of requested data to client PC. [Perm.EXPORT_FILES]")
    @RequestMapping(value = "/{id}/download", method = RequestMethod.POST)
    @PreAuthorize("hasAuthority('" + Permissions.EXPORT_FILES + "')")
    public void downloadData(@Parameter(description = "Id of the saved query", required = true)
                             @PathVariable("id") String id,
                             @Parameter(description = "Export config", required = true) @RequestBody @Valid ExportConfig exportConfig,
                             HttpServletResponse response) throws IOException {
        aipQueryService.downloadQueryResult(id, exportConfig, response);
    }

    @Operation(summary = "Starts export of requested data to client workspace. [Perm.EXPORT_FILES]")
    @RequestMapping(value = "/{id}/export", method = RequestMethod.POST, produces = APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('" + Permissions.EXPORT_FILES + "')")
    public String exportData(@Parameter(description = "Id of the saved query", required = true)
                             @PathVariable("id") String id,
                             @Parameter(description = "Export config", required = true) @RequestBody @Valid ExportConfig exportConfig) throws IOException {
        return aipQueryService.exportQueryResult(id, exportConfig, true).toString();
    }

    @Autowired
    public void setExportRoutineService(ExportRoutineService exportRoutineService) {
        this.exportRoutineService = exportRoutineService;
    }

    @Autowired
    public void setAipQueryService(AipQueryService aipQueryService) {
        this.aipQueryService = aipQueryService;
    }

    @Autowired
    public void setUserDetails(UserDetails userDetails) {
        this.userDetails = userDetails;
    }
}
