package cz.cas.lib.arclib.security.authorization.deprecated.assign;

import com.google.common.collect.Sets;
import cz.cas.lib.arclib.domainbase.audit.AuditLogger;
import cz.cas.lib.arclib.security.authorization.deprecated.Role;
import cz.cas.lib.arclib.security.authorization.deprecated.assign.audit.RoleAddEvent;
import cz.cas.lib.arclib.security.authorization.deprecated.assign.audit.RoleDelEvent;
import cz.cas.lib.core.store.Transactional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

// Fixme: change tests from roles to perms
@ConditionalOnProperty(prefix = "security.roles.internal", name = "enabled", havingValue = "true")
@Service
public class AssignedRoleService {
    private AssignedRoleStore store;
    private AuditLogger logger;

    @Transactional
    public Set<Role> getAssignedRoles(String userId) {
        return store.findAssignedRoles(userId);
    }

    @Transactional
    public void saveAssignedRoles(String userId, Set<Role> newRoles) {
        Set<Role> oldRoles = store.findAssignedRoles(userId);

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

    @Transactional
    public Set<GrantedAuthority> getAuthorities(String userId) {
        Set<Role> roles = store.findAssignedRoles(userId);

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

}
