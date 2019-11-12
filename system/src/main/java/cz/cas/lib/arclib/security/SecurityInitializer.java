package cz.cas.lib.arclib.security;

import cz.cas.lib.arclib.domainbase.audit.AuditLogger;
import cz.cas.lib.arclib.security.authorization.assign.AssignedRoleService;
import cz.cas.lib.arclib.security.ldap.ArclibAuthenticationProvider;
import cz.cas.lib.arclib.security.ldap.ArclibUserDetailsService;
import cz.cas.lib.core.security.BaseSecurityInitializer;
import cz.cas.lib.core.security.basic.PathBasicAuthFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.search.LdapUserSearch;

import javax.inject.Inject;
import javax.servlet.Filter;

import static cz.cas.lib.core.util.Utils.asArray;

@Configuration
public class SecurityInitializer extends BaseSecurityInitializer {

    private AuditLogger auditLogger;

    private AssignedRoleService roleService;

    private LdapContextSource contextSource;

    private LdapUserSearch userSearch;

    private ArclibUserDetailsService userDetailsUpdater;

    private String authQuery;

    @Override
    protected AuthenticationProvider[] primaryAuthProviders() {
        BindAuthenticator authenticator = new BindAuthenticator(contextSource);
        authenticator.setUserSearch(userSearch);

        ArclibAuthenticationProvider provider = new ArclibAuthenticationProvider(authenticator, roleService, userDetailsUpdater);

        return asArray(provider);
    }

    @Override
    protected Filter[] primarySchemeFilters() throws Exception {
        PathBasicAuthFilter filter = new PathBasicAuthFilter(authenticationManager(), auditLogger, authQuery);
        return asArray(filter);
    }

    @Inject
    public void setAuditLogger(AuditLogger auditLogger) {
        this.auditLogger = auditLogger;
    }

    @Inject
    public void setContextSource(LdapContextSource contextSource) {
        this.contextSource = contextSource;
    }

    @Inject
    public void setAuthQuery(@Value("${security.basic.authQuery}") String authQuery) {
        this.authQuery = authQuery;
    }

    @Inject
    public void setUserSearch(LdapUserSearch userSearch) {
        this.userSearch = userSearch;
    }

    @Inject
    public void setUserDetailsUpdater(ArclibUserDetailsService userDetailsUpdater) {
        this.userDetailsUpdater = userDetailsUpdater;
    }

    @Inject
    public void setRoleService(AssignedRoleService roleService) {
        this.roleService = roleService;
    }
}
