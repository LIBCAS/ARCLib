package cz.cas.lib.arclib.api;

import cz.cas.lib.arclib.domain.User;
import cz.cas.lib.arclib.exception.BadRequestException;
import cz.cas.lib.arclib.security.authorization.Roles;
import cz.cas.lib.arclib.security.authorization.assign.AssignedRoleService;
import cz.cas.lib.arclib.security.authorization.role.Role;
import cz.cas.lib.arclib.security.user.UserDetails;
import cz.cas.lib.arclib.service.UserService;
import cz.cas.lib.core.exception.BadArgument;
import cz.cas.lib.core.exception.MissingObject;
import cz.cas.lib.core.index.dto.Filter;
import cz.cas.lib.core.index.dto.FilterOperation;
import cz.cas.lib.core.index.dto.Params;
import cz.cas.lib.core.index.dto.Result;
import cz.cas.lib.core.store.Transactional;
import cz.cas.lib.core.util.Utils;
import io.swagger.annotations.*;
import org.springframework.web.bind.annotation.*;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import java.util.Set;

import static cz.cas.lib.arclib.utils.ArclibUtils.hasRole;
import static cz.cas.lib.core.util.Utils.addPrefilter;
import static cz.cas.lib.core.util.Utils.notNull;

@RestController
@Api(value = "user", description = "Api for admins to manage users and their roles")
@RequestMapping("/api/user")
public class UserApi {
    private UserDetails userDetails;
    private UserService userService;
    private AssignedRoleService assignedRoleService;

    /**
     * this endpoint is handled by springboot security
     */
    @RequestMapping(value = "/login", method = RequestMethod.POST)
    public void login() {
    }

    @ApiOperation(value = "Saves an user. Roles.ADMIN, Roles.SUPER_ADMIN", notes =
            "if the calling user is Roles.SUPER_ADMIN producer has to be specified," +
                    "otherwise producer is automatically set to the producer of calling user if not specified",
            response = User.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = User.class),
            @ApiResponse(code = 400, message = "Specified id does not correspond to the id of the user or user is null or user's username is null or the calling user is superadmin but the user to be created does not have producer assigned")})
    @RolesAllowed({Roles.ADMIN, Roles.SUPER_ADMIN})
    @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
    @Transactional
    public User save(@ApiParam(value = "Id of the user", required = true) @PathVariable("id") String id,
                     @ApiParam(value = "User", required = true) @RequestBody User user) {
        Utils.notNull(user, () -> new BadArgument("user is null"));
        Utils.eq(id, user.getId(), () -> new BadArgument("id"));
        Utils.notNull(user.getUsername(), () -> new BadArgument("missing username"));
        if (!hasRole(userDetails, Roles.SUPER_ADMIN))
            user.setProducer(userDetails.getProducerId());
        else
            notNull(user.getProducer(), () -> new BadRequestException("user has to have producer assigned"));
        return userService.save(user);
    }

    @ApiOperation(value = "Deletes an user. Roles.ADMIN, Roles.SUPER_ADMIN")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response"),
            @ApiResponse(code = 404, message = "User does not exist"),
            @ApiResponse(code = 403, message = "Calling user is not Roles.SUPER_ADMIN but tries to delete a Roles.SUPER_ADMIN user")})
    @RolesAllowed({Roles.ADMIN, Roles.SUPER_ADMIN})
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    @Transactional
    public void delete(@ApiParam(value = "Id of the user", required = true) @PathVariable("id") String id) {
        User entity = userService.find(id);
        Utils.notNull(entity, () -> new MissingObject(User.class, id));
        userService.delete(entity);
    }


    @ApiOperation(value = "Gets user", response = User.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = User.class),
            @ApiResponse(code = 404, message = "User does not exist")})
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    @Transactional
    public User get(@ApiParam(value = "Id of the user", required = true) @PathVariable("id") String id) {
        User entity = userService.find(id);
        Utils.notNull(entity, () -> new MissingObject(User.class, id));
        return entity;
    }

    @ApiOperation(value = "Gets filtered users. Roles.ADMIN, Roles.SUPER_ADMIN",
            notes = "if the calling user is not Roles.SUPER_ADMIN only users assigned to the admin's producer are returned.. " +
                    "sort/filter fields: username, firstName, lastName, email, ldapDn, producerId, producerName", response = Result.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = Result.class)})
    @RequestMapping(method = RequestMethod.GET)
    @RolesAllowed({Roles.ADMIN, Roles.SUPER_ADMIN})
    @Transactional
    public Result<User> list(@ApiParam(value = "Parameters to comply with", required = true)
                             @ModelAttribute Params params) {
        if (!hasRole(userDetails, Roles.SUPER_ADMIN))
            addPrefilter(params, new Filter("producerId", FilterOperation.EQ, userDetails.getProducerId(), null));
        return userService.findAll(params);
    }

    @ApiOperation(value = "Gets all roles assigned to the specified user.", response = Set.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = Set.class)})
    @RequestMapping(value = "/{userId}/roles", method = RequestMethod.GET)
    @Transactional
    public Set<Role> getRoles(@ApiParam(value = "User id", required = true)
                              @PathVariable("userId") String userId) {
        return assignedRoleService.getAssignedRoles(userId);
    }

    @ApiOperation(value = "Saves assigned roles to the given user. Roles.ADMIN, Roles.SUPER_ADMIN")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response"),
            @ApiResponse(code = 400, message = "Roles null or user does not have producer assigned and has other roles than SUPER_ADMIN"),
            @ApiResponse(code = 403, message = "Calling user is not Roles.SUPER_ADMIN but tries to assign Roles.SUPER_ADMIN role")})
    @RolesAllowed({Roles.ADMIN, Roles.SUPER_ADMIN})
    @RequestMapping(value = "/{userId}/roles", method = RequestMethod.PUT)
    @Transactional
    public void saveRoles(@ApiParam(value = "User id", required = true)
                          @PathVariable("userId") String userId,
                          @ApiParam(value = "Set of roles to set", required = true)
                          @RequestBody Set<Role> roles) {
        notNull(roles, () -> new BadRequestException("roles cant be null"));
        userService.saveRoles(userId, roles);
    }

    @Inject
    public void setAssignedRoleService(AssignedRoleService assignedRoleService) {
        this.assignedRoleService = assignedRoleService;
    }

    @Inject
    public void setUserDetails(UserDetails userDetails) {
        this.userDetails = userDetails;
    }

    @Inject
    public void setUserService(UserService userService) {
        this.userService = userService;
    }
}
