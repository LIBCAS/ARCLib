package cz.inqool.uas.security.authorization.assign;

import cz.inqool.uas.security.authorization.role.Role;
import cz.inqool.uas.store.Transactional;
import io.swagger.annotations.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import java.util.Set;

@ConditionalOnProperty(prefix = "security.roles.internal", name = "enabled", havingValue = "true")
@RestController
@Api(value = "user", description = "Api for interaction with users")
@RequestMapping("/api/users")
public class AssignedRoleApi {
    private AssignedRoleService service;

    @ApiOperation(value = "Gets all roles assigned to me", response = Set.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = Set.class)})
    @RequestMapping(value = "/me/roles", method = RequestMethod.GET)
    @Transactional
    public Set<Role> getMine() {
        return service.getAssignedRolesMine();
    }

    @ApiOperation(value = "Gets all roles assigned to the specified user.", response = Set.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = Set.class)})
    @RequestMapping(value = "/{userId}/roles", method = RequestMethod.GET)
    @Transactional
    public Set<Role> get(@ApiParam(value = "User id", required = true)
                             @PathVariable("userId") String userId) {
        return service.getAssignedRoles(userId);
    }

    @ApiOperation(value = "Saves assigned roles to the given user.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response")})
    @RequestMapping(value = "/{userId}/roles", method = RequestMethod.PUT)
    @Transactional
    public void save(@ApiParam(value = "User id", required = true)
                         @PathVariable("userId") String userId,
                     @ApiParam(value = "Set of roles to set", required = true)
                         @RequestBody Set<Role> roles) {
        service.saveAssignedRoles(userId, roles);
    }

    @Inject
    public void setService(AssignedRoleService service) {
        this.service = service;
    }
}
