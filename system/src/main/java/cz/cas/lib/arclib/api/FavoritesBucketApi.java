package cz.cas.lib.arclib.api;

import cz.cas.lib.arclib.domain.export.ExportConfig;
import cz.cas.lib.arclib.dto.BucketExportRequestDto;
import cz.cas.lib.arclib.index.solr.arclibxml.IndexedArclibXmlDocument;
import cz.cas.lib.arclib.security.authorization.permission.Permissions;
import cz.cas.lib.arclib.security.user.UserDetails;
import cz.cas.lib.arclib.service.AipQueryService;
import cz.cas.lib.arclib.service.FavoritesBucketService;
import cz.cas.lib.core.index.dto.Result;
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
import java.util.Set;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@Tag(name = "favorites", description = "Api for list of favorites documents.")
@RequestMapping("/api/favorites")
@Slf4j
public class FavoritesBucketApi {

    private FavoritesBucketService favoritesBucketService;
    private UserDetails userDetails;
    private AipQueryService aipQueryService;

    @Operation(summary = "Gets IDS of favorites documents of the user [Perm.AIP_QUERY_RECORDS_READ]")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response", content = @Content(array = @ArraySchema(schema = @Schema(implementation = String.class))))})
    @PreAuthorize("hasAuthority('" + Permissions.AIP_QUERY_RECORDS_READ + "')")
    @RequestMapping(value = "/ids", method = RequestMethod.GET)
    public Set<String> getFavoritesIds() {
        return favoritesBucketService.findFavoriteIdsOfUser(userDetails.getId());
    }

    @Operation(summary = "Updates list of favorites documents. [Perm.AIP_QUERY_RECORDS_READ]")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response")
    })
    @PreAuthorize("hasAuthority('" + Permissions.AIP_QUERY_RECORDS_WRITE + "')")
    @RequestMapping(value = "/ids", method = RequestMethod.POST)
    public void save(@Parameter(description = "list of ids", required = true) @RequestBody Set<String> ids) {
        favoritesBucketService.saveFavoriteIdsOfUser(userDetails.getId(), ids);
    }

    @Operation(summary = "Gets IDS of favorites documents of the user [Perm.AIP_QUERY_RECORDS_READ]")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response", content = @Content(array = @ArraySchema(schema = @Schema(implementation = String.class))))})
    @PreAuthorize("hasAuthority('" + Permissions.AIP_QUERY_RECORDS_READ + "')")
    @RequestMapping(method = RequestMethod.GET)
    public Result<IndexedArclibXmlDocument> getFavorites(
            @Parameter(description = "pagination: ordinal number of page", required = true)
            @RequestParam(value = "page") Integer page,
            @Parameter(description = "pagination: size of page", required = true)
            @RequestParam(value = "pageSize") Integer pageSize
    ) {
        return favoritesBucketService.getFavorites(userDetails.getId(), page, pageSize);
    }

    @Operation(summary = "Starts download of requested data to client PC. [Perm.EXPORT_FILES]")
    @RequestMapping(value = "/download", method = RequestMethod.POST)
    @PreAuthorize("hasAuthority('" + Permissions.EXPORT_FILES + "')")
    public void downloadData(@Parameter(description = "Export config", required = true) @RequestBody @Valid BucketExportRequestDto exportConfig,
                             HttpServletResponse response) throws IOException {
        aipQueryService.downloadBucketResult(exportConfig, response);
    }

    @Operation(summary = "Starts export of requested data to client workspace. [Perm.EXPORT_FILES]")
    @RequestMapping(value = "/export", method = RequestMethod.POST, produces = APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('" + Permissions.EXPORT_FILES + "')")
    public String exportData(@Parameter(description = "Export config", required = true) @RequestBody @Valid BucketExportRequestDto exportConfig) throws IOException {
        return aipQueryService.exportBucketResult(exportConfig).toString();
    }

    @Autowired
    public void setFavoritesBucketService(FavoritesBucketService favoritesBucketService) {
        this.favoritesBucketService = favoritesBucketService;
    }

    @Autowired
    public void setUserDetails(UserDetails userDetails) {
        this.userDetails = userDetails;
    }

    @Autowired
    public void setAipQueryService(AipQueryService aipQueryService) {
        this.aipQueryService = aipQueryService;
    }
}
