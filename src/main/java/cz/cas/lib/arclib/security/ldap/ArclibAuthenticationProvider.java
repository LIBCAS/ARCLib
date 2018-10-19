package cz.cas.lib.arclib.security.ldap;

import cz.cas.lib.arclib.domain.User;
import cz.cas.lib.arclib.security.authorization.assign.AssignedRoleService;
import cz.cas.lib.arclib.security.user.UserDelegate;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.authentication.LdapAuthenticator;
import org.springframework.security.ldap.userdetails.UserDetailsContextMapper;

import java.util.Collection;
import java.util.Set;

public class ArclibAuthenticationProvider extends LdapAuthenticationProvider implements UserDetailsContextMapper {
    private ArclibUserDetailsService updater;
    private AssignedRoleService roleService;

    public ArclibAuthenticationProvider(LdapAuthenticator authenticator, AssignedRoleService roleService, ArclibUserDetailsService updater) {
        super(authenticator);
        this.setUserDetailsContextMapper(this);
        this.updater = updater;
        this.roleService = roleService;
    }

    @Override
    protected Authentication createSuccessfulAuthentication(UsernamePasswordAuthenticationToken authentication, UserDetails details) {
        if (details instanceof UserDelegate) {
            UserDelegate delegate = (UserDelegate) details;
            User external = delegate.getUser();

            User user = updater.updateFromExternal(external);
            delegate.setUser(user);

            Set<GrantedAuthority> authorities = roleService.getAuthorities(user.getId());
            delegate.setAuthorities(authorities);

            return super.createSuccessfulAuthentication(authentication, delegate);
        } else {
            throw new BadCredentialsException("Wrong type of UserDetails detected");
        }
    }

    @Override
    public UserDetails mapUserFromContext(DirContextOperations ctx, String username, Collection<? extends GrantedAuthority> authorities) {

        User user = new User();
        user.setFirstName(ctx.getStringAttribute("givenName"));
        user.setLastName(ctx.getStringAttribute("sn"));
        user.setEmail(ctx.getStringAttribute("mail"));
        user.setLdapDn(ctx.getDn().toString());
        user.setUsername(username);

        return new UserDelegate(user, authorities);
    }

    @Override
    public void mapUserToContext(UserDetails user, DirContextAdapter ctx) {
        throw new UnsupportedOperationException();
    }
}
