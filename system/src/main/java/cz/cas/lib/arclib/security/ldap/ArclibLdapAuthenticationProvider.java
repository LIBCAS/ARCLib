package cz.cas.lib.arclib.security.ldap;

import cz.cas.lib.arclib.domain.User;
import cz.cas.lib.arclib.security.user.UserDetailsImpl;
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

public class ArclibLdapAuthenticationProvider extends LdapAuthenticationProvider implements UserDetailsContextMapper {
    private ArclibLdapUserDetailsService updater;

    public ArclibLdapAuthenticationProvider(LdapAuthenticator authenticator, ArclibLdapUserDetailsService updater) {
        super(authenticator);
        this.setUserDetailsContextMapper(this);
        this.updater = updater;
    }

    @Override
    protected Authentication createSuccessfulAuthentication(UsernamePasswordAuthenticationToken authentication, UserDetails details) {
        if (details instanceof UserDetailsImpl) {
            UserDetailsImpl delegate = (UserDetailsImpl) details;
            User external = delegate.getUser(); // without roles

            User user = updater.updateFromExternal(external); // loads user from DB with roles and their permissions
            delegate.setUser(user);

            return super.createSuccessfulAuthentication(authentication, delegate);
        } else {
            throw new BadCredentialsException("Wrong type of UserDetails detected");
        }
    }

    @Override
    public UserDetails mapUserFromContext(DirContextOperations ctx, String username, Collection<? extends GrantedAuthority> ignored) {
        User user = new User();
        user.setFirstName(ctx.getStringAttribute("givenName"));
        user.setLastName(ctx.getStringAttribute("sn"));
        user.setEmail(ctx.getStringAttribute("mail"));
        user.setInstitution(ctx.getStringAttribute("o"));
        user.setLdapDn(ctx.getDn().toString());
        user.setUsername(username);

        return new UserDetailsImpl(user);
    }

    @Override
    public void mapUserToContext(UserDetails user, DirContextAdapter ctx) {
        throw new UnsupportedOperationException();
    }
}
