package cz.cas.lib.arclib.service;

import cz.cas.lib.arclib.domain.User;
import cz.cas.lib.arclib.exception.BadRequestException;
import cz.cas.lib.arclib.exception.ForbiddenException;
import cz.cas.lib.arclib.security.authorization.Roles;
import cz.cas.lib.arclib.security.authorization.assign.AssignedRoleService;
import cz.cas.lib.arclib.security.authorization.role.Role;
import cz.cas.lib.arclib.security.authorization.role.RoleStore;
import cz.cas.lib.arclib.security.user.UserDetails;
import cz.cas.lib.arclib.store.UserStore;
import cz.cas.lib.core.exception.MissingObject;
import cz.cas.lib.core.rest.data.DelegateAdapter;
import cz.cas.lib.core.store.Transactional;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static cz.cas.lib.arclib.utils.ArclibUtils.hasRole;
import static cz.cas.lib.core.util.Utils.notNull;

@Slf4j
@Service
public class UserService implements DelegateAdapter<User> {

    @Getter
    private UserStore delegate;
    private UserDetails userDetails;
    private RoleStore roleStore;
    private AssignedRoleService assignedRoleService;
    private UserStore userStore;

    /**
     * Save assigned roles to user
     *
     * @param userId id of the user to be assigned with roles
     * @param roles  set of roles to assign
     */
    @Transactional
    public void saveRoles(String userId, Set<Role> roles) throws BadRequestException, ForbiddenException {
        log.debug("Saving assigned roles to user " + userId + ".");

        User user = userStore.find(userId);
        notNull(user, () -> new MissingObject(User.class, userId));
        //if user does not have producer and the roles contains at least one role different to SUPER_ADMIN
        if (user.getProducer() == null &&
                !(roles.isEmpty() || (roles.size() == 1 && Roles.SUPER_ADMIN.equals(roles.iterator().next().getName())))) {
            throw new BadRequestException(
                    "User has to have producer assigned. Only users without any role or only with one role equal to " +
                            Roles.SUPER_ADMIN + " does not have to be bound with producer.");
        }
        if (!hasRole(userDetails, Roles.SUPER_ADMIN)) {
            List<Role> rolesInDb = roleStore.findAllInList(roles.stream().map(Role::getId).collect(Collectors.toList()));
            if (rolesInDb.stream().anyMatch(r -> r.getName().equals(Roles.SUPER_ADMIN)))
                throw new ForbiddenException("you are not allowed to assign " + Roles.SUPER_ADMIN);
        }
        assignedRoleService.saveAssignedRoles(userId, roles);
    }

    @Override
    @Transactional
    public void delete(User u) throws ForbiddenException {
        if (!hasRole(userDetails, Roles.SUPER_ADMIN)) {
            Set<Role> userRoles = assignedRoleService.getAssignedRoles(u.getId());
            if (userRoles.stream().anyMatch(r -> r.getName().equals(Roles.SUPER_ADMIN)))
                throw new ForbiddenException("you are not allowed to delete a user with " + Roles.SUPER_ADMIN);
        }
        assignedRoleService.saveAssignedRoles(u.getId(),new HashSet<>());
        delegate.delete(u);
    }

    public User findUserByUsername(String username) {
        return userStore.findUserByUsername(username);
    }


    @Inject
    public void setAssignedRoleService(AssignedRoleService assignedRoleService) {
        this.assignedRoleService = assignedRoleService;
    }

    @Inject
    public void setRoleStore(RoleStore roleStore) {
        this.roleStore = roleStore;
    }

    @Inject
    public void setDelegate(UserStore delegate) {
        this.delegate = delegate;
    }

    @Inject
    public void setUserDetails(UserDetails userDetails) {
        this.userDetails = userDetails;
    }

    @Inject
    public void setUserStore(UserStore userStore) {
        this.userStore = userStore;
    }
}
