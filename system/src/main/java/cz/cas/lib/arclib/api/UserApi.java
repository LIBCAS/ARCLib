package cz.cas.lib.arclib.api;

import cz.cas.lib.arclib.domain.User;
import cz.cas.lib.arclib.domainbase.exception.ConflictException;
import cz.cas.lib.arclib.domainbase.exception.MissingObject;
import cz.cas.lib.arclib.dto.UserCreateOrUpdateDto;
import cz.cas.lib.arclib.dto.UserFullnameDto;
import cz.cas.lib.arclib.security.authorization.data.Permissions;
import cz.cas.lib.arclib.security.user.UserDetails;
import cz.cas.lib.arclib.service.UserService;
import cz.cas.lib.core.index.dto.Filter;
import cz.cas.lib.core.index.dto.FilterOperation;
import cz.cas.lib.core.index.dto.Params;
import cz.cas.lib.core.index.dto.Result;
import cz.cas.lib.core.store.Transactional;
import cz.cas.lib.core.util.Utils;
import io.swagger.annotations.*;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.Email;
import java.util.List;

import static cz.cas.lib.arclib.utils.ArclibUtils.hasRole;
import static cz.cas.lib.core.util.Utils.addPrefilter;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@Api(value = "user", description = "Api for admins to manage users and their roles")
@RequestMapping("/api/user")
public class UserApi {

    private UserDetails userDetails;
    private UserService userService;

    /**
     * this endpoint is handled by springboot security
     */
    @RequestMapping(value = "/login", method = RequestMethod.POST)
    public void login() {
    }


    @ApiOperation(value = "Creates or updates the user. [Perm.USER_RECORDS_WRITE]",
            notes = "if the calling user is Roles.SUPER_ADMIN producer has to be specified, otherwise producer is automatically set to the producer of calling user if not specified",
            response = User.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = User.class),
            @ApiResponse(code = 400, message = "Specified id does not correspond to the id of the user or user is null or user's username is null or the calling user is superadmin but the user to be created does not have producer assigned")})
    @PreAuthorize("hasAuthority('" + Permissions.USER_RECORDS_WRITE + "')")
    @PutMapping(value = "/{id}", produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
    public User save(@ApiParam(value = "Id of the user", required = true) @PathVariable("id") String id,
                     @ApiParam(value = "User", required = true) @RequestBody UserCreateOrUpdateDto user) throws ConflictException {
        return userService.createOrUpdate(id, user);
    }

    @PreAuthorize("isAuthenticated()")
    @RequestMapping(value = "/me/update", method = RequestMethod.POST)
    @Transactional
    public User updateMyAccount(@ApiParam(value = "DTO", required = true) @RequestBody @Valid AccountUpdateDto accountUpdateDto) {
        User user = userService.find(userDetails.getId());
        user.setEmail(accountUpdateDto.getEmail());
        return userService.save(user);
    }

    @PreAuthorize("isAuthenticated()")
    @RequestMapping(value = "/me", method = RequestMethod.GET)
    public User getAccountInfo() {
        return userService.find(userDetails.getId());
    }

    @PreAuthorize("isAuthenticated()")
    @RequestMapping(value = "/list_names", method = RequestMethod.GET)
    public List<UserFullnameDto> listUserNames() {
        return userService.listUserNames();
    }


    @ApiOperation(value = "Deletes an user. [Perm.USER_RECORDS_WRITE]")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response"),
            @ApiResponse(code = 404, message = "User does not exist"),
            @ApiResponse(code = 403, message = "Calling user is not Roles.SUPER_ADMIN but tries to delete a Roles.SUPER_ADMIN user")})
    @PreAuthorize("hasAuthority('" + Permissions.USER_RECORDS_WRITE + "')")
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    @Transactional
    public void delete(@ApiParam(value = "Id of the user", required = true) @PathVariable("id") String id) {
        User entity = userService.find(id);
        Utils.notNull(entity, () -> new MissingObject(User.class, id));
        userService.delete(entity);
    }


    @ApiOperation(value = "Gets user [Perm.USER_RECORDS_READ]", response = User.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = User.class),
            @ApiResponse(code = 404, message = "User does not exist")})
    @PreAuthorize("hasAuthority('" + Permissions.USER_RECORDS_READ + "')")
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public User get(@ApiParam(value = "Id of the user", required = true) @PathVariable("id") String id) {
        User entity = userService.find(id);
        Utils.notNull(entity, () -> new MissingObject(User.class, id));
        return entity;
    }

    @ApiOperation(value = "Gets filtered users. [Perm.USER_RECORDS_READ]",
            notes = "if the calling user is not Roles.SUPER_ADMIN only users assigned to the admin's producer are returned.. " +
                    "sort/filter fields: username, firstName, lastName, email, ldapDn, producerId, producerName", response = Result.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = Result.class)})
    @RequestMapping(method = RequestMethod.GET)
    @PreAuthorize("hasAuthority('" + Permissions.USER_RECORDS_READ + "')")
    public Result<User> list(@ApiParam(value = "Parameters to comply with", required = true)
                             @ModelAttribute Params params) {
        if (!hasRole(userDetails, Permissions.SUPER_ADMIN_PRIVILEGE))
            addPrefilter(params, new Filter("producerId", FilterOperation.EQ, userDetails.getProducerId(), null));
        return userService.findAll(params);
    }


    @Getter
    @Setter
    public static final class AccountUpdateDto {
        @Email
        @NonNull
        private String email;
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
