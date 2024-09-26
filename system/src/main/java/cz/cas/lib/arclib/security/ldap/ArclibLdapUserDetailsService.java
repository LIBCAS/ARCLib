package cz.cas.lib.arclib.security.ldap;

import cz.cas.lib.arclib.domain.User;
import cz.cas.lib.arclib.mail.ArclibMailCenter;
import cz.cas.lib.arclib.security.user.UserDetailsImpl;
import cz.cas.lib.arclib.service.UserService;
import cz.cas.lib.core.security.UserDetailsService;
import cz.cas.lib.core.store.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Updater of User data from external source
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "security.ldap", name = "enabled", havingValue = "true")
public class ArclibLdapUserDetailsService implements UserDetailsService {

    private UserService service;
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
        user.setInstitution(external.getInstitution());
        if (user.getEmail() == null || user.getEmail().isBlank())
            user.setEmail(external.getEmail());

        service.save(user);
        log.debug("User " + user.getId() + " successfully updated from external.");

        return user;
    }

    @Override
    public UserDetailsImpl loadUserById(String id) {
        User user = service.find(id);
        if (user != null) {
            return new UserDetailsImpl(user);
        } else {
            return null;
        }
    }

    @Override
    public UserDetailsImpl loadUserByUsername(String username) throws UsernameNotFoundException {
        throw new UnsupportedOperationException();
    }


    @Autowired
    public void setStore(UserService service) {
        this.service = service;
    }

    @Autowired
    public void setArclibMailCenter(ArclibMailCenter arclibMailCenter) {
        this.arclibMailCenter = arclibMailCenter;
    }
}
