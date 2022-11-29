package cz.cas.lib.arclib.api;

import cz.cas.lib.arclib.index.solr.arclibxml.IndexedArclibXmlDocument;
import cz.cas.lib.arclib.security.authorization.permission.Permissions;
import cz.cas.lib.arclib.security.user.UserDetails;
import cz.cas.lib.arclib.service.FavoritesBucketService;
import cz.cas.lib.core.index.dto.Result;
import io.swagger.annotations.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import java.util.Set;

@RestController
@Api(value = "favorites", description = "Api for list of favorites documents.")
@RequestMapping("/api/favorites")
@Slf4j
public class FavoritesBucketApi {

    private FavoritesBucketService favoritesBucketService;
    private UserDetails userDetails;

    @ApiOperation(value = "Gets IDS of favorites documents of the user [Perm.AIP_QUERY_RECORDS_READ]", response = String.class, responseContainer = "list")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response")})
    @PreAuthorize("hasAuthority('" + Permissions.AIP_QUERY_RECORDS_READ + "')")
    @RequestMapping(value = "/ids", method = RequestMethod.GET)
    public Set<String> getFavoritesIds() {
        return favoritesBucketService.findFavoriteIdsOfUser(userDetails.getId());
    }

    @ApiOperation(value = "Updates list of favorites documents. [Perm.AIP_QUERY_RECORDS_READ]")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response")
    })
    @PreAuthorize("hasAuthority('" + Permissions.AIP_QUERY_RECORDS_WRITE + "')")
    @RequestMapping(value = "/ids", method = RequestMethod.POST)
    public void save(@ApiParam(value = "list of ids", required = true) @RequestBody Set<String> ids) {
        favoritesBucketService.saveFavoriteIdsOfUser(userDetails.getId(), ids);
    }

    @ApiOperation(value = "Gets IDS of favorites documents of the user [Perm.AIP_QUERY_RECORDS_READ]", response = String.class, responseContainer = "list")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response")})
    @PreAuthorize("hasAuthority('" + Permissions.AIP_QUERY_RECORDS_READ + "')")
    @RequestMapping(method = RequestMethod.GET)
    public Result<IndexedArclibXmlDocument> getFavorites(
            @ApiParam(value = "pagination: ordinal number of page", required = true)
            @RequestParam(value = "page") Integer page,
            @ApiParam(value = "pagination: size of page", required = true)
            @RequestParam(value = "pageSize") Integer pageSize
    ) {
        return favoritesBucketService.getFavorites(userDetails.getId(), page, pageSize);
    }

    @Inject
    public void setFavoritesBucketService(FavoritesBucketService favoritesBucketService) {
        this.favoritesBucketService = favoritesBucketService;
    }

    @Inject
    public void setUserDetails(UserDetails userDetails) {
        this.userDetails = userDetails;
    }
}
