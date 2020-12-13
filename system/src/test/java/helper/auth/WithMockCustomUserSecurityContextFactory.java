package helper.auth;

import cz.cas.lib.arclib.domain.User;
import cz.cas.lib.arclib.security.authorization.role.UserRole;
import cz.cas.lib.arclib.security.user.UserDetailsImpl;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.util.Set;

public class WithMockCustomUserSecurityContextFactory implements WithSecurityContextFactory<WithMockCustomUser> {
    @Override
    public SecurityContext createSecurityContext(WithMockCustomUser customUser) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();

        User user = new User();
        user.setId(customUser.id());
        user.setUsername(customUser.username());

        UserRole role = new UserRole();
        role.setName(customUser.roleName());
        role.setDescription(customUser.roleDescription());
        role.setPermissions(Set.of(customUser.permissions()));

        user.setRoles(Set.of(role));

        UserDetailsImpl principal = new UserDetailsImpl(user);
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, "password", principal.getAuthorities());
        context.setAuthentication(auth);
        return context;
    }
}
