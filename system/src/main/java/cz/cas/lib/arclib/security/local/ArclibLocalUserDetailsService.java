package cz.cas.lib.arclib.security.local;

import cz.cas.lib.arclib.domain.User;
import cz.cas.lib.arclib.security.user.UserDetailsImpl;
import cz.cas.lib.arclib.service.UserService;
import cz.cas.lib.core.security.UserDetailsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@ConditionalOnProperty(prefix = "security.local", name = "enabled", havingValue = "true")
public class ArclibLocalUserDetailsService implements UserDetailsService {

    private UserService service;

    @Override
    public UserDetailsImpl loadUserById(String id) {
        User user = service.find(id);
        return user == null ? null : new UserDetailsImpl(user);
    }

    @Override
    public UserDetailsImpl loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = service.findUserByUsername(username);
        return user == null ? null : new UserDetailsImpl(user);
    }


    @Autowired
    public void setStore(UserService service) {
        this.service = service;
    }
}
