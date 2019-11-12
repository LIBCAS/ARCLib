package cz.cas.lib.arclib.api;

import cz.cas.lib.arclib.security.authorization.Roles;
import cz.cas.lib.arclib.service.archivalStorage.ArchivalStoragePipe;
import io.swagger.annotations.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static cz.cas.lib.core.util.Utils.checkUUID;

@RestController
@Api(value = "administration of the logical storages of the Archival Storage",
        description = "These endpoints are identical to the endpoints of the Archival Storage + adds authorization." +
                " See documentation of Archival Storage for documentation of input and output data of these endpoints.")
@RequestMapping("/api/arcstorage/administration/storage")
@RolesAllowed({Roles.SUPER_ADMIN})
@Slf4j
public class ArchivalStorageStorageAdministrationApi extends ArchivalStoragePipe {

    @RequestMapping(method = RequestMethod.GET)
    public void getAll(HttpServletResponse response, HttpServletRequest request) {
        passToArchivalStorage(response, request, "/administration/storage", HttpMethod.GET, "retrieve all storages", AccessTokenType.ADMIN);
    }

    @RequestMapping(method = RequestMethod.GET, value = "/{id}")
    public void getOne(@ApiParam(value = "id of the logical storage", required = true) @PathVariable("id") String id,
                       HttpServletResponse response, HttpServletRequest request) {
        passToArchivalStorage(response, request, "/administration/storage/" + id, HttpMethod.GET, "retrieve data of storage with id: " + id, AccessTokenType.ADMIN);
    }

    @RequestMapping(method = RequestMethod.POST)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "body", required = true, paramType = "body")
    })
    public void attachStorage(HttpServletResponse response, HttpServletRequest request) {
        passToArchivalStorage(response, request, "/administration/storage", HttpMethod.POST, "attach new storage", AccessTokenType.ADMIN);
    }

    @RequestMapping(value = "/sync/{id}", method = RequestMethod.POST)
    public void continueSync(
            @ApiParam(value = "id of the synchronization status entity", required = true) @PathVariable("id") String id,
            HttpServletResponse response, HttpServletRequest request) {
        passToArchivalStorage(response, request, "/administration/storage/sync/" + id, HttpMethod.POST, "continue with synchronization with id: " + id + " of storage", AccessTokenType.ADMIN);
    }

    @RequestMapping(value = "/sync/{id}", method = RequestMethod.GET)
    public void getSyncStatusOfStorage(
            @ApiParam(value = "id of the storage", required = true) @PathVariable("id") String id,
            HttpServletResponse response, HttpServletRequest request) {
        passToArchivalStorage(response, request, "/administration/storage/sync/" + id, HttpMethod.GET, "retrieve synchronization status storage with id: " + id, AccessTokenType.ADMIN);
    }

    @RequestMapping(value = "/check_reachability", method = RequestMethod.POST)
    public void checkReachability(
            HttpServletResponse response, HttpServletRequest request) {
        passToArchivalStorage(response, request, "/administration/storage/check_reachability", HttpMethod.POST, "manual check of reachability of storages", AccessTokenType.ADMIN);
    }

    @ApiOperation(value = "Returns state of logical storage.")
    @RequestMapping(value = "/{id}/state", method = RequestMethod.GET)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "successful response"),
            @ApiResponse(code = 404, message = "storage with the id is missing")
    })
    public void getStorageState(
            @ApiParam(value = "id of the storage", required = true) @PathVariable("id") String id,
            HttpServletResponse response, HttpServletRequest request) {
        checkUUID(id);
        passToArchivalStorage(response, request, "/administration/storage/" + id + "/state", HttpMethod.GET, "retrieve state of storage with id: " + id, AccessTokenType.ADMIN);
    }

    @ApiImplicitParams({
            @ApiImplicitParam(name = "body", required = true, paramType = "body")
    })
    @RequestMapping(value = "/update", method = RequestMethod.POST)
    public void update(HttpServletResponse response, HttpServletRequest request) {
        passToArchivalStorage(response, request, "/administration/storage/update", HttpMethod.POST, "update storage", AccessTokenType.ADMIN);
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public void delete(
            @ApiParam(value = "id of the logical storage", required = true) @PathVariable("id") String id,
            HttpServletResponse response, HttpServletRequest request) {
        passToArchivalStorage(response, request, "/administration/storage/" + id, HttpMethod.DELETE, "delete storage with id: " + id, AccessTokenType.ADMIN);
    }
}
