package cz.cas.lib.arclib.security.ldap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.search.LdapUserSearch;

@Configuration
@ConditionalOnProperty(prefix = "security.ldap", name = "enabled", havingValue = "true")
public class ArclibLdapAuthenticationConfiguration {

    private LdapContextSource contextSource;
    private LdapUserSearch userSearch;
    private ArclibLdapUserDetailsService userDetailsUpdater;

    @Bean
    public AuthenticationProvider authenticationProvider() {
        BindAuthenticator authenticator = new BindAuthenticator(contextSource);
        authenticator.setUserSearch(userSearch);
        return new ArclibLdapAuthenticationProvider(authenticator, userDetailsUpdater);
    }

    @Autowired
    public void setContextSource(LdapContextSource contextSource) {
        this.contextSource = contextSource;
    }

    @Autowired
    public void setUserSearch(LdapUserSearch userSearch) {
        this.userSearch = userSearch;
    }

    @Autowired
    public void setUserDetailsUpdater(ArclibLdapUserDetailsService userDetailsUpdater) {
        this.userDetailsUpdater = userDetailsUpdater;
    }
}
