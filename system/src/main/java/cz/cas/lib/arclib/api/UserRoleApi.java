package cz.cas.lib.arclib.api;

import cz.cas.lib.arclib.security.authorization.permission.Permissions;
import cz.cas.lib.arclib.security.authorization.role.CreateRoleDto;
import cz.cas.lib.arclib.security.authorization.role.UpdateRoleDto;
import cz.cas.lib.arclib.security.authorization.role.UserRole;
import cz.cas.lib.arclib.security.authorization.role.UserRoleService;
import cz.cas.lib.core.index.dto.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Assigning roles to user can be done through {@link UserApi#save}
 */
@Tag(name = "User roles API")
@RequestMapping("/api/role")
@RestController
public class UserRoleApi {

    private UserRoleService service;

    @Operation(summary = "Find Role [Perm.USER_ROLE_RECORDS_READ]")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = UserRole.class))),
            @ApiResponse(responseCode = "404", description = "Entity not found for given ID")
    })
    @PreAuthorize("hasAuthority('" + Permissions.USER_ROLE_RECORDS_READ + "')")
    @GetMapping(value = "/{id}", produces = APPLICATION_JSON_VALUE)
    public UserRole find(@Parameter(description = "Id of the instance", required = true) @PathVariable("id") String id) {
        return service.find(id);
    }

    @Operation(summary = "Create Role with permissions [Perm.USER_ROLE_RECORDS_WRITE]")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = UserRole.class))),
    })
    @PreAuthorize("hasAuthority('" + Permissions.USER_ROLE_RECORDS_WRITE + "')")
    @PostMapping(produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
    public UserRole create(@Parameter(description = "Create DTO", required = true) @Valid @RequestBody CreateRoleDto dto) {
        return service.create(dto);
    }

    @Operation(summary = "Update Role with permissions [Perm.USER_ROLE_RECORDS_WRITE]")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = UserRole.class))),
            @ApiResponse(responseCode = "404", description = "Entity not found for given ID")
    })
    @PreAuthorize("hasAuthority('" + Permissions.USER_ROLE_RECORDS_WRITE + "')")
    @PutMapping(produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
    public UserRole update(@Parameter(description = "Update DTO", required = true) @Valid @RequestBody UpdateRoleDto dto) {
        return service.update(dto);
    }

    @Operation(summary = "Delete Role [Perm.USER_ROLE_RECORDS_WRITE]")
    @PreAuthorize("hasAuthority('" + Permissions.USER_ROLE_RECORDS_WRITE + "')")
    @DeleteMapping(value = "/{id}")
    public void delete(@Parameter(description = "Id of the instance", required = true) @PathVariable("id") String id) {
        service.delete(id);
    }

    @Operation(summary = "Gets all roles [Perm.USER_ROLE_RECORDS_READ]",
            responses = {@ApiResponse(content = @Content(array = @ArraySchema(schema = @Schema(implementation = UserRole.class))))})
    @PreAuthorize("hasAuthority('" + Permissions.USER_ROLE_RECORDS_READ + "')")
    @PostMapping(value = "/find-all", produces = APPLICATION_JSON_VALUE)
    public Collection<UserRole> findAll() {
        return service.findAll();
    }

    @Operation(summary = "Gets all assignable permissions [Perm.USER_ROLE_RECORDS_READ]",
            responses = {@ApiResponse(content = @Content(array = @ArraySchema(schema = @Schema(implementation = String.class))))})
    @PreAuthorize("hasAuthority('" + Permissions.USER_ROLE_RECORDS_READ + "')")
    @PostMapping(value = "/permissions", produces = APPLICATION_JSON_VALUE)
    public Result<String> findAllPermissions() {
        return service.findAllAssignablePermissions();
    }


    @Autowired
    public void setService(UserRoleService service) {
        this.service = service;
    }
}
