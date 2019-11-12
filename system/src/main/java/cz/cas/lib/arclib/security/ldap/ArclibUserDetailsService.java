package cz.cas.lib.arclib.security.ldap;

import cz.cas.lib.arclib.domain.User;
import cz.cas.lib.arclib.mail.ArclibMailCenter;
import cz.cas.lib.arclib.security.authorization.assign.AssignedRoleService;
import cz.cas.lib.arclib.security.user.UserDelegate;
import cz.cas.lib.arclib.security.user.UserDetails;
import cz.cas.lib.arclib.service.UserService;
import cz.cas.lib.core.security.UserDetailsService;
import cz.cas.lib.core.store.Transactional;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@Service
public class ArclibUserDetailsService implements UserDetailsService {
    private UserService service;

    private AssignedRoleService assignedRoleService;
    private ArclibMailCenter arclibMailCenter;

    @Transactional
    public User updateFromExternal(User external) {
        User user = service.findUserByUsername(external.getUsername());

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

        service.save(user);
        log.debug("User " + user.getId() + " successfully updated from external.");

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
        User user = service.find(id);
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
    public void setStore(UserService service) {
        this.service = service;
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
