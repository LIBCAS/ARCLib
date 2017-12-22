package cz.inqool.uas.security.authorization.assign;

import com.google.common.collect.Sets;
import cz.inqool.uas.audit.AuditLogger;
import cz.inqool.uas.security.UserDetails;
import cz.inqool.uas.security.authorization.assign.audit.RoleAddEvent;
import cz.inqool.uas.security.authorization.assign.audit.RoleDelEvent;
import cz.inqool.uas.security.authorization.role.Role;
import cz.inqool.uas.store.Transactional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static cz.inqool.uas.util.Utils.unwrap;

@ConditionalOnProperty(prefix = "security.roles.internal", name = "enabled", havingValue = "true")
@Service
public class AssignedRoleService {
    private AssignedRoleStore store;

    private AuditLogger logger;

    private UserDetails userDetails;

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

    public Collection<String> getUsersWithRole(Role role) {
        return store.getUsersWithRole(role);
    }

    @Transactional
    public Set<GrantedAuthority> getAuthorities(String userId) {
        Set<Role> roles = getAssignedRoles(userId);

        return roles.stream()
                    .map(this::gatherPermissions)
                    .flatMap(Collection::stream)
                    .distinct()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toSet());
    }

    private Set<String> gatherPermissions(Role role) {
        Set<String> permissions = new HashSet<>(role.getPermissions());

        Role parent = role.getParent();
        if (parent != null) {
            permissions.addAll(gatherPermissions(parent));
        }

        return permissions;
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
}
