package cz.cas.lib.core.security;

import cz.cas.lib.arclib.security.user.UserDetails;

import java.util.List;

public interface UserDetailsService extends org.springframework.security.core.userdetails.UserDetailsService {
    UserDetails loadUserById(String id);

    default List<UserDetails> loadUsersWithRole(String permission) {
        throw new UnsupportedOperationException();
    }
}
