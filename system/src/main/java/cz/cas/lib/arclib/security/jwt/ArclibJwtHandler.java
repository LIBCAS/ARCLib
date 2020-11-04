package cz.cas.lib.arclib.security.jwt;

import cz.cas.lib.arclib.domain.User;
import cz.cas.lib.arclib.security.user.UserDetails;
import cz.cas.lib.arclib.security.user.UserDetailsImpl;
import cz.cas.lib.arclib.service.UserService;
import cz.cas.lib.core.security.jwt.spi.JwtHandler;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.LinkedHashMap;
import java.util.Map;

import static cz.cas.lib.arclib.security.SecurityConstants.Claims;
import static cz.cas.lib.core.util.Utils.notNull;

@Service
public class ArclibJwtHandler implements JwtHandler {

    private UserService userService;

    public UserDetails parseClaims(Map<String, Object> claims) {
        String userId = (String) claims.get(Claims.SUBJECT);
        User user = userService.find(userId); // fully load user with roles and permissions from DB
        notNull(user, () -> new BadCredentialsException("User not found."));

        return new UserDetailsImpl(user);
    }

    public Map<String, Object> createClaims(UserDetails userDetails) {
        User user = userDetails.getUser();

        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put(Claims.SUBJECT, user.getId());
        claims.put(Claims.AUTHORITIES, user.jointPermissions());
        claims.put(Claims.LDAP_DN, user.getLdapDn());
        claims.put(Claims.EMAIL, user.getEmail());
        claims.put(Claims.USERNAME, user.getUsername());
        claims.put(Claims.FIRST_NAME, user.getFirstName());
        claims.put(Claims.LAST_NAME, user.getLastName());
        claims.put(Claims.FULL_NAME, user.getFullName());

        return claims;
    }


    @Inject
    public void setUserService(UserService userService) {
        this.userService = userService;
    }
}
