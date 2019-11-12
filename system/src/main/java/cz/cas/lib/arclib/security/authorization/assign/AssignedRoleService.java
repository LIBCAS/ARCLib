package cz.cas.lib.arclib.security.authorization.assign;

import com.google.common.collect.Sets;
import cz.cas.lib.arclib.domain.User;
import cz.cas.lib.arclib.domainbase.audit.AuditLogger;
import cz.cas.lib.arclib.security.authorization.assign.audit.RoleAddEvent;
import cz.cas.lib.arclib.security.authorization.assign.audit.RoleDelEvent;
import cz.cas.lib.arclib.security.authorization.role.Role;
import cz.cas.lib.arclib.security.user.UserDetails;
import cz.cas.lib.arclib.service.UserService;
import cz.cas.lib.core.store.Transactional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.time.Instant;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cz.cas.lib.core.util.Utils.unwrap;

@ConditionalOnProperty(prefix = "security.roles.internal", name = "enabled", havingValue = "true")
@Service
public class AssignedRoleService {
    private AssignedRoleStore store;

    private AuditLogger logger;

    private UserDetails userDetails;
    private UserService userService;

    @Transactional
    public Set<Role> getAssignedRoles(String userId) {
        return store.findAssignedRoles(userId);
    }

    @Transactional
    public Set<Role> getAssignedRolesMine() {
        UserDetails unwrapped = unwrap(userDetails);

        if (unwrapped != null) {
            return getAssignedRoles(unwrapped.getId());
        } else {
            return null;
        }
    }

    @Transactional
    public void saveAssignedRoles(String userId, Set<Role> newRoles) {
        Set<Role> oldRoles = getAssignedRoles(userId);

        Sets.SetView<Role> removedRoles = Sets.difference(oldRoles, newRoles);
        Sets.SetView<Role> addedRoles = Sets.difference(newRoles, oldRoles);

        removedRoles.forEach(role -> {
            store.deleteRole(userId, role);
            logger.logEvent(new RoleDelEvent(Instant.now(), userId, role.getId(), role.getName()));
        });

        addedRoles.forEach(role -> {
            store.addRole(userId, role);
            logger.logEvent(new RoleAddEvent(Instant.now(), userId, role.getId(), role.getName()));
        });
    }

    public Collection<String> getIdsOfUsersWithRole(String roleName) {
        return store.getUsersWithRole(roleName);
    }

    public Collection<User> getUsersWithRole(String roleName) {
        return userService.findAllInList(store.getUsersWithRole(roleName));
    }

    public Collection<String> getEmailsOfUsersWithRole(String roleName) {
        return userService.findAllInList(store.getUsersWithRole(roleName)).stream().map(User::getEmail).filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Transactional
    public Set<GrantedAuthority> getAuthorities(String userId) {
        Set<Role> roles = getAssignedRoles(userId);

        return roles.stream()
                .map(Role::getName)
                .distinct()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());
    }

    @Inject
    public void setStore(AssignedRoleStore store) {
        this.store = store;
    }

    @Inject
    public void setLogger(AuditLogger logger) {
        this.logger = logger;
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
