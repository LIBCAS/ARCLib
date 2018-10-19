package cz.cas.lib.arclib.security.ldap;

import cz.cas.lib.arclib.domain.User;
import cz.cas.lib.arclib.mail.ArclibMailCenter;
import cz.cas.lib.arclib.security.authorization.assign.AssignedRoleService;
import cz.cas.lib.arclib.security.user.UserDelegate;
import cz.cas.lib.arclib.security.user.UserDetails;
import cz.cas.lib.arclib.store.UserStore;
import cz.cas.lib.core.security.UserDetailsService;
import cz.cas.lib.core.store.Transactional;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Updater of User data from external source
 */
@Service
public class ArclibUserDetailsService implements UserDetailsService {
    private UserStore store;

    private AssignedRoleService assignedRoleService;
    private ArclibMailCenter arclibMailCenter;

    @Transactional
    public User updateFromExternal(User external) {
        User user = store.findUserByUsername(external.getUsername());

        if (user == null || user.getDeleted() != null) {
            String id = user == null ? UUID.randomUUID().toString() : user.getId();
            user = new User(id);
            arclibMailCenter.sendNewUserRegisteredNotification(external.getUsername(), Instant.now());
        }

        user.setFirstName(external.getFirstName());
        user.setLastName(external.getLastName());
        user.setUsername(external.getUsername());
        user.setLdapDn(external.getLdapDn());
        user.setEmail(external.getEmail());
        user.setLdapDn(user.getLdapDn());

        store.save(user);
        return user;
    }

    @Override
    public List<UserDetails> loadUsersWithRole(String roleName) {
        return assignedRoleService.
                getIdsOfUsersWithRole(roleName)
                .stream()
                .map(this::loadUserById)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public UserDelegate loadUserById(String id) {
        User user = store.find(id);
        if (user != null) {
            return new UserDelegate(user, null);
        } else {
            return null;
        }
    }

    @Override
    public UserDelegate loadUserByUsername(String username) throws UsernameNotFoundException {
        throw new UnsupportedOperationException();
    }

    @Inject
    public void setStore(UserStore store) {
        this.store = store;
    }

    @Inject
    public void setAssignedRoleService(AssignedRoleService assignedRoleService) {
        this.assignedRoleService = assignedRoleService;
    }

    @Inject
    public void setArclibMailCenter(ArclibMailCenter arclibMailCenter) {
        this.arclibMailCenter = arclibMailCenter;
    }
}
