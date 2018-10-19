package cz.cas.lib.arclib.security.jwt;

import cz.cas.lib.arclib.domain.User;
import cz.cas.lib.arclib.security.user.UserDelegate;
import cz.cas.lib.arclib.security.user.UserDetails;
import cz.cas.lib.arclib.store.UserStore;
import cz.cas.lib.core.security.jwt.spi.JwtHandler;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static cz.cas.lib.core.util.Utils.notNull;

@Service
public class ArclibJwtHandler implements JwtHandler {

    private UserStore userStore;

    public UserDetails parseClaims(Map<String, Object> claims) {
        String userId = (String) claims.get("sub");
        List<String> authorityNames = (List<String>) claims.get("authorities");
        Set<GrantedAuthority> authorities = authorityNames.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());
        User user = userStore.find(userId);
        notNull(user, () -> new BadCredentialsException("User not found."));
        return new UserDelegate(user, authorities);
    }

    public Map<String, Object> createClaims(UserDetails userDetails) {
        if (userDetails instanceof UserDelegate) {
            UserDelegate delegate = (UserDelegate) userDetails;
            User user = delegate.getUser();
            String[] roles = userDetails.getAuthorities()
                    .stream()
                    .map(GrantedAuthority::getAuthority)
                    .toArray(String[]::new);
            Map<String, Object> claims = new LinkedHashMap<>();
            claims.put("sub", user.getId());
            claims.put("authorities", roles);
            claims.put("firstName", user.getFirstName());
            claims.put("lastName", user.getLastName());
            claims.put("name", user.getFullName());
            claims.put("ldapDn", user.getLdapDn());
            claims.put("username", user.getUsername());
            claims.put("email", user.getEmail());
            return claims;
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Inject
    public void setUserStore(UserStore userStore) {
        this.userStore = userStore;
    }
}
