package cz.cas.lib.arclib.api;

import cz.cas.lib.arclib.security.authorization.permission.Permissions;
import cz.cas.lib.arclib.service.archivalStorage.ArchivalStoragePipe;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import static cz.cas.lib.core.util.Utils.checkUUID;

@RestController
@Tag(name = "administration of the logical storages of the Archival Storage",
        description = "These endpoints are identical to the endpoints of the Archival Storage + adds authorization." +
                " See documentation of Archival Storage for documentation of input and output data of these endpoints.")
@RequestMapping("/api/arcstorage/administration/storage")
@Slf4j
public class ArchivalStorageStorageAdministrationApi extends ArchivalStoragePipe {

    @Operation(summary = "retrieve all storages from archival storage [Perm.STORAGE_ADMINISTRATION_READ]")
    @PreAuthorize("hasAuthority('" + Permissions.STORAGE_ADMINISTRATION_READ + "')")
    @RequestMapping(method = RequestMethod.GET)
    public void getAll(HttpServletResponse response, HttpServletRequest request) {
        passToArchivalStorage(response, request, "/administration/storage", HttpMethod.GET, "retrieve all storages", AccessTokenType.ADMIN);
    }

    @Operation(summary = "retrieve all storages as simple DTOs from archival storage [Perm.AIP_RECORDS_READ]")
    @PreAuthorize("hasAuthority('" + Permissions.AIP_RECORDS_READ + "')")
    @RequestMapping(value = "/basic", method = RequestMethod.GET)
    public void getAllAsDtos(HttpServletResponse response, HttpServletRequest request) {
        passToArchivalStorage(response, request, "/administration/storage/basic", HttpMethod.GET, "retrieve all storages as simple DTOs", AccessTokenType.ADMIN);
    }

    @Operation(summary = "[Perm.STORAGE_ADMINISTRATION_READ]")
    @PreAuthorize("hasAuthority('" + Permissions.STORAGE_ADMINISTRATION_READ + "')")
    @RequestMapping(method = RequestMethod.GET, value = "/{id}")
    public void getOne(@Parameter(description = "id of the logical storage", required = true) @PathVariable("id") String id,
                       HttpServletResponse response, HttpServletRequest request) {
        passToArchivalStorage(response, request, "/administration/storage/" + id, HttpMethod.GET, "retrieve data of storage with id: " + id, AccessTokenType.ADMIN);
    }

    @Operation(summary = "attaches now storage [Perm.STORAGE_ADMINISTRATION_WRITE]")
    //todo swagger required body
    @PreAuthorize("hasAuthority('" + Permissions.STORAGE_ADMINISTRATION_WRITE + "')")
    @RequestMapping(method = RequestMethod.POST)
    public void attachNewStorage(HttpServletResponse response, HttpServletRequest request) {
        passToArchivalStorage(response, request, "/administration/storage", HttpMethod.POST, "attach new storage", AccessTokenType.ADMIN);
    }

    @Operation(summary = "Attaches existing detached logical storage [Perm.STORAGE_ADMINISTRATION_WRITE]")
    //todo swagger required body
    @PreAuthorize("hasAuthority('" + Permissions.STORAGE_ADMINISTRATION_WRITE + "')")
    @RequestMapping(value = "/{id}/attach",method = RequestMethod.POST)
    public void attachExistingStorage(
            @Parameter(description = "id of the logical storage", required = true) @PathVariable("id") String id, HttpServletResponse response, HttpServletRequest request) {
        passToArchivalStorage(response, request, "/administration/storage/"+id+"/attach", HttpMethod.POST, "attach existing logical storage", AccessTokenType.ADMIN);
    }

    @Operation(summary = "Detaches logical storage [Perm.STORAGE_ADMINISTRATION_WRITE]")
    //todo swagger required body
    @PreAuthorize("hasAuthority('" + Permissions.STORAGE_ADMINISTRATION_WRITE + "')")
    @RequestMapping(value = "/{id}/detach",method = RequestMethod.POST)
    public void detachStorage(
            @Parameter(description = "id of the logical storage", required = true) @PathVariable("id") String id, HttpServletResponse response, HttpServletRequest request) {
        passToArchivalStorage(response, request, "/administration/storage/"+id+"/detach", HttpMethod.POST, "detach logical storage", AccessTokenType.ADMIN);
    }

    @Operation(summary = "[Perm.STORAGE_ADMINISTRATION_READ]")
    @PreAuthorize("hasAuthority('" + Permissions.STORAGE_ADMINISTRATION_READ + "')")
    @RequestMapping(value = "/sync/{id}", method = RequestMethod.GET)
    public void getSyncStatusOfStorage(
            @Parameter(description = "id of the storage", required = true) @PathVariable("id") String id,
            HttpServletResponse response, HttpServletRequest request) {
        passToArchivalStorage(response, request, "/administration/storage/sync/" + id, HttpMethod.GET, "retrieve synchronization status storage with id: " + id, AccessTokenType.ADMIN);
    }

    @Operation(summary = "[Perm.STORAGE_ADMINISTRATION_READ]")
    @PreAuthorize("hasAuthority('" + Permissions.STORAGE_ADMINISTRATION_READ + "')")
    @RequestMapping(value = "/check_reachability", method = RequestMethod.POST)
    public void checkReachability(
            HttpServletResponse response, HttpServletRequest request) {
        passToArchivalStorage(response, request, "/administration/storage/check_reachability", HttpMethod.POST, "manual check of reachability of storages", AccessTokenType.ADMIN);
    }

    @Operation(summary = "Returns state of logical storage. [Perm.STORAGE_ADMINISTRATION_READ]")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "successful response"),
            @ApiResponse(responseCode = "404", description = "storage with the id is missing")
    })
    @PreAuthorize("hasAuthority('" + Permissions.STORAGE_ADMINISTRATION_READ + "')")
    @RequestMapping(value = "/{id}/state", method = RequestMethod.GET)
    public void getStorageState(
            @Parameter(description = "id of the storage", required = true) @PathVariable("id") String id,
            HttpServletResponse response, HttpServletRequest request) {
        checkUUID(id);
        passToArchivalStorage(response, request, "/administration/storage/" + id + "/state", HttpMethod.GET, "retrieve state of storage with id: " + id, AccessTokenType.ADMIN);
    }

    @Operation(summary = "[Perm.STORAGE_ADMINISTRATION_WRITE]")
    //todo swagger required body
    @PreAuthorize("hasAuthority('" + Permissions.STORAGE_ADMINISTRATION_WRITE + "')")
    @RequestMapping(value = "/update", method = RequestMethod.POST)
    public void update(HttpServletResponse response, HttpServletRequest request) {
        passToArchivalStorage(response, request, "/administration/storage/update", HttpMethod.POST, "update storage", AccessTokenType.ADMIN);
    }

    @Operation(summary = "[Perm.STORAGE_ADMINISTRATION_WRITE]")
    @PreAuthorize("hasAuthority('" + Permissions.STORAGE_ADMINISTRATION_WRITE + "')")
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public void delete(
            @Parameter(description = "id of the logical storage", required = true) @PathVariable("id") String id,
            HttpServletResponse response, HttpServletRequest request) {
        passToArchivalStorage(response, request, "/administration/storage/" + id, HttpMethod.DELETE, "delete storage with id: " + id, AccessTokenType.ADMIN);
    }
}
