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
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = "administration of the Archival Storage system",
        description = "These endpoints are identical to the endpoints of the Archival Storage + adds authorization." +
                " See documentation of Archival Storage for documentation of input and output data of these endpoints.")
@RequestMapping("/api/arcstorage/administration")
@Slf4j
public class ArchivalStorageSystemAdministrationApi extends ArchivalStoragePipe {

    @Operation(summary = "Creates/updates configuration of the Archival Storage [Perm.STORAGE_ADMINISTRATION_WRITE]")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "successful response"),
            @ApiResponse(responseCode = "404", description = "config with the id is missing"),
            @ApiResponse(responseCode = "400", description = "the config breaks the basic LTP policy (e.g. count of storages)"),
            @ApiResponse(responseCode = "409", description = "provided config has different id than that stored in DB (only one config object is allowed)")
    })
    //todo swagger required body
    @PreAuthorize("hasAuthority('" + Permissions.STORAGE_ADMINISTRATION_WRITE + "')")
    @RequestMapping(value = "/config", method = RequestMethod.POST)
    public void saveConfig(HttpServletResponse response, HttpServletRequest request) {
        passToArchivalStorage(response, request, "/administration/config", HttpMethod.POST, "update archival storage system configuration", AccessTokenType.ADMIN);
    }


    @Operation(summary = "Returns configuration [Perm.STORAGE_ADMINISTRATION_READ]")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "successful response"),
            @ApiResponse(responseCode = "404", description = "config missing")
    })
    @PreAuthorize("hasAuthority('" + Permissions.STORAGE_ADMINISTRATION_READ + "')")
    @RequestMapping(value = "/config", method = RequestMethod.GET)
    public void getConfig(HttpServletResponse response, HttpServletRequest request) {
        passToArchivalStorage(response, request, "/administration/config", HttpMethod.GET, "retrieve archival storage system configuration", AccessTokenType.ADMIN);
    }

    @Operation(summary = "Cleans storage. In order to succeed, all storages must be reachable. [Perm.STORAGE_ADMINISTRATION_WRITE]",
            description = "By default only failed package are cleaned (i.e. rolled back/deleted from all storages). If 'all' is set to true, also currently processing/stucked packages and tmp folder is cleaned.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "successful response"),
    })
    @PreAuthorize("hasAuthority('" + Permissions.STORAGE_ADMINISTRATION_WRITE + "')")
    @RequestMapping(value = "/cleanup", method = RequestMethod.POST)
    public void cleanup(
            @Parameter(description = "all") @RequestParam(value = "all", defaultValue = "false") boolean all,
            HttpServletResponse response, HttpServletRequest request) {
        passToArchivalStorage(response, request, "/administration/cleanup?all=" + all, HttpMethod.POST, "clean up the storage, " + (all ? "including" : "without") + " workspace and processing packages", AccessTokenType.ADMIN);
    }

    @Operation(summary = "Switches primary storage.",
            description = "In order to succeed, all storage queues must be empty.")
    @PostMapping("/config/storage/{id}/primary")
    public void switchPrimaryStorage(
            HttpServletResponse response, HttpServletRequest request,
            @Parameter(description = "ID of new primary storage") @PathVariable(value = "id") String id) {
        passToArchivalStorage(response, request, "/administration/config/storage/" + id + "/primary", HttpMethod.POST, "switch primary storage to " + id, AccessTokenType.ADMIN);
    }

    @Operation(summary = "Sets system to read-only mode")
    @PostMapping("/config/read_only")
    public void setReadOnly(HttpServletResponse response, HttpServletRequest request) {
        passToArchivalStorage(response, request, "/administration/config/read_only", HttpMethod.POST, "set readonly mode", AccessTokenType.ADMIN);
    }

    @Operation(summary = "Sets system to read-write mode")
    @PostMapping("/config/read_write")
    public void setReadWrite(HttpServletResponse response, HttpServletRequest request) {
        passToArchivalStorage(response, request, "/administration/config/read_write", HttpMethod.POST, "set readwrite mode", AccessTokenType.ADMIN);
    }

}