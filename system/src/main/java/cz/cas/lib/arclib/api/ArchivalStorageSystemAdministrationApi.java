package cz.cas.lib.arclib.api;

import cz.cas.lib.arclib.security.authorization.permission.Permissions;
import cz.cas.lib.arclib.service.archivalStorage.ArchivalStoragePipe;
import io.swagger.annotations.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RestController
@Api(value = "administration of the Archival Storage system",
        description = "These endpoints are identical to the endpoints of the Archival Storage + adds authorization." +
                " See documentation of Archival Storage for documentation of input and output data of these endpoints.")
@RequestMapping("/api/arcstorage/administration")
@Slf4j
public class ArchivalStorageSystemAdministrationApi extends ArchivalStoragePipe {

    @ApiOperation(value = "Creates/updates configuration of the Archival Storage [Perm.STORAGE_ADMINISTRATION_WRITE]")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "successful response"),
            @ApiResponse(code = 404, message = "config with the id is missing"),
            @ApiResponse(code = 400, message = "the config breaks the basic LTP policy (e.g. count of storages)"),
            @ApiResponse(code = 409, message = "provided config has different id than that stored in DB (only one config object is allowed)")
    })
    @ApiImplicitParams({
            @ApiImplicitParam(name = "body", required = true, paramType = "body")
    })
    @PreAuthorize("hasAuthority('" + Permissions.STORAGE_ADMINISTRATION_WRITE + "')")
    @RequestMapping(value = "/config", method = RequestMethod.POST)
    public void saveConfig(HttpServletResponse response, HttpServletRequest request) {
        passToArchivalStorage(response, request, "/administration/config", HttpMethod.POST, "update archival storage system configuration", AccessTokenType.ADMIN);
    }


    @ApiOperation(value = "Returns configuration [Perm.STORAGE_ADMINISTRATION_READ]")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "successful response"),
            @ApiResponse(code = 404, message = "config missing")
    })
    @PreAuthorize("hasAuthority('" + Permissions.STORAGE_ADMINISTRATION_READ + "')")
    @RequestMapping(value = "/config", method = RequestMethod.GET)
    public void getConfig(HttpServletResponse response, HttpServletRequest request) {
        passToArchivalStorage(response, request, "/administration/config", HttpMethod.GET, "retrieve archival storage system configuration", AccessTokenType.ADMIN);
    }

    @ApiOperation(value = "Cleans storage. In order to succeed, all storages must be reachable. [Perm.STORAGE_ADMINISTRATION_WRITE]",
            notes = "By default only failed package are cleaned (i.e. rolled back/deleted from all storages). If 'all' is set to true, also currently processing/stucked packages and tmp folder is cleaned.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "successful response"),
    })
    @PreAuthorize("hasAuthority('" + Permissions.STORAGE_ADMINISTRATION_WRITE + "')")
    @RequestMapping(value = "/cleanup", method = RequestMethod.POST)
    public void cleanup(
            @ApiParam(value = "all") @RequestParam(value = "all", defaultValue = "false") boolean all,
            HttpServletResponse response, HttpServletRequest request) {
        passToArchivalStorage(response, request, "/administration/cleanup?all=" + all, HttpMethod.POST, "clean up the storage, " + (all ? "including" : "without") + " workspace and processing packages", AccessTokenType.ADMIN);
    }

}