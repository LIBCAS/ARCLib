package cz.cas.lib.arclib.api;

import cz.cas.lib.arclib.index.solr.arclibxml.IndexedArclibXmlDocument;
import cz.cas.lib.arclib.security.authorization.permission.Permissions;
import cz.cas.lib.arclib.security.user.UserDetails;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@Tag(name = "favorites", description = "Api for list of favorites documents.")
@RequestMapping("/api/favorites")
@Slf4j
public class FavoritesBucketApi {

    private FavoritesBucketService favoritesBucketService;
    private UserDetails userDetails;

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

    @Autowired
    public void setFavoritesBucketService(FavoritesBucketService favoritesBucketService) {
        this.favoritesBucketService = favoritesBucketService;
    }

    @Autowired
    public void setUserDetails(UserDetails userDetails) {
        this.userDetails = userDetails;
    }
}
