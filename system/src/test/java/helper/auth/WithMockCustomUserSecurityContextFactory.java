package helper.auth;

import cz.cas.lib.arclib.domain.User;
import cz.cas.lib.arclib.security.authorization.assign.AssignedRole;
import cz.cas.lib.arclib.security.authorization.role.Role;
import cz.cas.lib.arclib.security.user.UserDelegate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.util.HashSet;
import java.util.Set;

public class WithMockCustomUserSecurityContextFactory
        implements WithSecurityContextFactory<WithMockCustomUser> {
    @Override
    public SecurityContext createSecurityContext(WithMockCustomUser customUser) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();

        User u = new User();
        u.setId(customUser.id());
        u.setUsername(customUser.username());
        Role r = new Role();
        r.setName(customUser.role());
        AssignedRole assignedRole = new AssignedRole();
        assignedRole.setRole(r);
        assignedRole.setUserId(u.getId());
        Set<GrantedAuthority> authoritySet = new HashSet<>();
        authoritySet.add(new SimpleGrantedAuthority(r.getName()));
        UserDelegate principal = new UserDelegate(u, authoritySet);
        Authentication auth =
                new UsernamePasswordAuthenticationToken(principal, "password", principal.getAuthorities());
        context.setAuthentication(auth);
        return context;
    }
}
