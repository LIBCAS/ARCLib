package cz.cas.lib.arclib.api;

import cz.cas.lib.arclib.domain.User;
import cz.cas.lib.arclib.domainbase.exception.ConflictException;
import cz.cas.lib.arclib.domainbase.exception.MissingObject;
import cz.cas.lib.arclib.dto.AccountUpdateDto;
import cz.cas.lib.arclib.dto.UserCreateOrUpdateDto;
import cz.cas.lib.arclib.dto.UserFullnameDto;
import cz.cas.lib.arclib.security.authorization.permission.Permissions;
import cz.cas.lib.arclib.security.user.UserDetails;
import cz.cas.lib.arclib.service.UserService;
import cz.cas.lib.core.index.dto.Filter;
import cz.cas.lib.core.index.dto.FilterOperation;
import cz.cas.lib.core.index.dto.Params;
import cz.cas.lib.core.index.dto.Result;
import cz.cas.lib.core.store.Transactional;
import cz.cas.lib.core.util.Utils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static cz.cas.lib.arclib.utils.ArclibUtils.hasRole;
import static cz.cas.lib.core.util.Utils.addPrefilter;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@Tag(name = "user", description = "Api for admins to manage users and their roles")
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


    @Operation(summary = "Creates or updates the user. [Perm.USER_RECORDS_WRITE]",
            description = "if the calling user is Roles.SUPER_ADMIN producer has to be specified, otherwise producer is automatically set to the producer of calling user if not specified")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response", content = @Content(schema = @Schema(implementation = User.class))),
            @ApiResponse(responseCode = "400", description = "Specified id does not correspond to the id of the user or user is null or user's username is null or the calling user is superadmin but the user to be created does not have producer assigned")})
    @PreAuthorize("hasAuthority('" + Permissions.USER_RECORDS_WRITE + "')")
    @PutMapping(value = "/{id}", produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
    public User save(@Parameter(description = "Id of the user", required = true) @PathVariable("id") String id,
                     @Parameter(description = "User", required = true) @RequestBody UserCreateOrUpdateDto user) throws ConflictException {
        return userService.createOrUpdate(id, user);
    }

    @PreAuthorize("isAuthenticated()")
    @RequestMapping(value = "/me/update", method = RequestMethod.POST)
    public User updateMyAccount(@Parameter(description = "DTO", required = true) @RequestBody @Valid AccountUpdateDto accountUpdateDto) {
        return userService.updateAccount(accountUpdateDto);
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


    @Operation(summary = "Deletes an user. [Perm.USER_RECORDS_WRITE]")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response"),
            @ApiResponse(responseCode = "404", description = "User does not exist"),
            @ApiResponse(responseCode = "403", description = "Calling user is not Roles.SUPER_ADMIN but tries to delete a Roles.SUPER_ADMIN user")})
    @PreAuthorize("hasAuthority('" + Permissions.USER_RECORDS_WRITE + "')")
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    @Transactional
    public void delete(@Parameter(description = "Id of the user", required = true) @PathVariable("id") String id) {
        User entity = userService.find(id);
        Utils.notNull(entity, () -> new MissingObject(User.class, id));
        userService.delete(entity);
    }


    @Operation(summary = "Gets user [Perm.USER_RECORDS_READ]")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response", content = @Content(schema = @Schema(implementation = User.class))),
            @ApiResponse(responseCode = "404", description = "User does not exist")})
    @PreAuthorize("hasAuthority('" + Permissions.USER_RECORDS_READ + "')")
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public User get(@Parameter(description = "Id of the user", required = true) @PathVariable("id") String id) {
        User entity = userService.find(id);
        Utils.notNull(entity, () -> new MissingObject(User.class, id));
        return entity;
    }

    @Operation(summary = "Gets filtered users. [Perm.USER_RECORDS_READ]",
            description = "if the calling user is not [Perm.SUPER_ADMIN_PRIVILEGE] only users assigned to the admin's producer are returned. " +
                    "sort/filter fields: username, firstName, lastName, email, ldapDn, producerId, producerName")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response", content = @Content(schema = @Schema(implementation = Result.class)))})
    @RequestMapping(method = RequestMethod.GET)
    @PreAuthorize("hasAuthority('" + Permissions.USER_RECORDS_READ + "')")
    public Result<User> list(@Parameter(description = "Parameters to comply with", required = true)
                             @ModelAttribute Params params) {
        if (!hasRole(userDetails, Permissions.SUPER_ADMIN_PRIVILEGE))
            addPrefilter(params, new Filter("producerId", FilterOperation.EQ, userDetails.getProducerId(), null));
        return userService.findAll(params);
    }


    @Autowired
    public void setUserDetails(UserDetails userDetails) {
        this.userDetails = userDetails;
    }

    @Autowired
    public void setUserService(UserService userService) {
        this.userService = userService;
    }
}
