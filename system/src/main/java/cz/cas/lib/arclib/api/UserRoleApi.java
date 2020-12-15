package cz.cas.lib.arclib.api;

import cz.cas.lib.arclib.security.authorization.permission.Permissions;
import cz.cas.lib.arclib.security.authorization.role.CreateRoleDto;
import cz.cas.lib.arclib.security.authorization.role.UpdateRoleDto;
import cz.cas.lib.arclib.security.authorization.role.UserRole;
import cz.cas.lib.arclib.security.authorization.role.UserRoleService;
import cz.cas.lib.core.index.dto.Result;
import io.swagger.annotations.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import javax.validation.Valid;
import java.util.Collection;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Assigning roles to user can be done through {@link UserApi#save}
 */
@Api(value = "User roles API")
@RequestMapping("/api/role")
@RestController
public class UserRoleApi {

    private UserRoleService service;

    @ApiOperation(value = "Find Role [Perm.USER_ROLE_RECORDS_READ]")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK", response = UserRole.class),
            @ApiResponse(code = 404, message = "Entity not found for given ID")
    })
    @PreAuthorize("hasAuthority('" + Permissions.USER_ROLE_RECORDS_READ + "')")
    @GetMapping(value = "/{id}", produces = APPLICATION_JSON_VALUE)
    public UserRole find(@ApiParam(value = "Id of the instance", required = true) @PathVariable("id") String id) {
        return service.find(id);
    }

    @ApiOperation(value = "Create Role with permissions [Perm.USER_ROLE_RECORDS_WRITE]")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK", response = UserRole.class),
    })
    @PreAuthorize("hasAuthority('" + Permissions.USER_ROLE_RECORDS_WRITE + "')")
    @PostMapping(produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
    public UserRole create(@ApiParam(value = "Create DTO", required = true) @Valid @RequestBody CreateRoleDto dto) {
        return service.create(dto);
    }

    @ApiOperation(value = "Update Role with permissions [Perm.USER_ROLE_RECORDS_WRITE]")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK", response = UserRole.class),
            @ApiResponse(code = 404, message = "Entity not found for given ID")
    })
    @PreAuthorize("hasAuthority('" + Permissions.USER_ROLE_RECORDS_WRITE + "')")
    @PutMapping(produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
    public UserRole update(@ApiParam(value = "Update DTO", required = true) @Valid @RequestBody UpdateRoleDto dto) {
        return service.update(dto);
    }

    @ApiOperation(value = "Delete Role [Perm.USER_ROLE_RECORDS_WRITE]")
    @PreAuthorize("hasAuthority('" + Permissions.USER_ROLE_RECORDS_WRITE + "')")
    @DeleteMapping(value = "/{id}")
    public void delete(@ApiParam(value = "Id of the instance", required = true) @PathVariable("id") String id) {
        service.delete(id);
    }

    @ApiOperation(value = "Gets all roles [Perm.USER_ROLE_RECORDS_READ]",
            response = UserRole.class, responseContainer = "List")
    @PreAuthorize("hasAuthority('" + Permissions.USER_ROLE_RECORDS_READ + "')")
    @PostMapping(value = "/find-all", produces = APPLICATION_JSON_VALUE)
    public Collection<UserRole> findAll() {
        return service.findAll();
    }

    @ApiOperation(value = "Gets all assignable permissions [Perm.USER_ROLE_RECORDS_READ]",
            response = String.class, responseContainer = "List")
    @PreAuthorize("hasAuthority('" + Permissions.USER_ROLE_RECORDS_READ + "')")
    @PostMapping(value = "/permissions", produces = APPLICATION_JSON_VALUE)
    public Result<String> findAllPermissions() {
        return service.findAllAssignablePermissions();
    }


    @Inject
    public void setService(UserRoleService service) {
        this.service = service;
    }
}
