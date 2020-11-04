package cz.cas.lib.core.security;

import cz.cas.lib.arclib.security.user.UserDetails;

public interface UserDetailsService extends org.springframework.security.core.userdetails.UserDetailsService {

    UserDetails loadUserById(String id);

}
