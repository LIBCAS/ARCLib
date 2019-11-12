package cz.cas.lib.core.security.preauth;

import cz.cas.lib.arclib.domainbase.audit.AuditLogger;
import cz.cas.lib.core.security.audit.LoginEvent;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedCredentialsNotFoundException;
import org.springframework.security.web.authentication.preauth.RequestHeaderAuthenticationFilter;

import javax.servlet.http.HttpServletRequest;
import java.time.Instant;

/**
 * Pre-authentication filter with logging of every attempt to login.
 * <p>
 * The value of the header is considered to be the username. If it is not the case, one should override
 * the extractUsername method to return sensible information from the header value.
 */
public class PreauthFilter extends RequestHeaderAuthenticationFilter {
    private Object credentialsConstant = null;

    public PreauthFilter(AuthenticationManager authenticationManager, AuditLogger logger) {
        super();

        this.setAuthenticationManager(authenticationManager);

        this.setAuthenticationSuccessHandler((request, response, authentication) -> logger.logEvent(new LoginEvent(Instant.now(), extractUsername(request), true)));
        this.setAuthenticationFailureHandler((request, response, exception) -> logger.logEvent(new LoginEvent(Instant.now(), extractUsername(request), false)));
    }

    protected String extractUsername(HttpServletRequest request) {
        try {
            return (String) getPreAuthenticatedPrincipal(request);
        } catch (PreAuthenticatedCredentialsNotFoundException ex) {
            return null;
        }
    }

    /**
     * Returns specified constant to be able to distinguish between tokens
     *
     * @param request Http request
     * @return Specific constant or value from request or "N/A"
     */
    protected Object getPreAuthenticatedCredentials(HttpServletRequest request) {
        if (credentialsConstant != null) {
            return credentialsConstant;
        } else {
            return super.getPreAuthenticatedCredentials(request);
        }
    }

    public Object getCredentialsConstant() {
        return credentialsConstant;
    }

    public void setCredentialsConstant(Object credentialsConstant) {
        this.credentialsConstant = credentialsConstant;
    }
}
