package cz.inqool.uas.security.preauth;

import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

public class PreauthProvider implements AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> {
    private UserDetailsService detailsService;

    public PreauthProvider(UserDetailsService detailsService) {
        this.detailsService = detailsService;
    }

    @Override
    public UserDetails loadUserDetails(PreAuthenticatedAuthenticationToken token) throws UsernameNotFoundException {
        String username = (String) token.getPrincipal();

        return detailsService.loadUserByUsername(username);
    }
}
