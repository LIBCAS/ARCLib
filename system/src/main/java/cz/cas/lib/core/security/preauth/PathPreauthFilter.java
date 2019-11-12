package cz.cas.lib.core.security.preauth;

import cz.cas.lib.arclib.domainbase.audit.AuditLogger;
import org.springframework.security.authentication.AuthenticationManager;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Set;

import static cz.cas.lib.core.util.Utils.asSet;

/**
 * Pre-authentication filter for specific URLs.
 */
public class PathPreauthFilter extends PreauthFilter {
    private Set<String> authQueries;

    public PathPreauthFilter(AuthenticationManager authenticationManager, AuditLogger logger, String authQuery) {
        super(authenticationManager, logger);
        this.authQueries = asSet(authQuery);
    }

    public PathPreauthFilter(AuthenticationManager authenticationManager, AuditLogger logger, String[] authQueries) {
        super(authenticationManager, logger);
        this.authQueries = asSet(authQueries);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (request instanceof HttpServletRequest) {
            String query = ((HttpServletRequest) request).getRequestURI();

            if (!authQueries.contains(query)) {
                chain.doFilter(request, response);
                return;
            }

            super.doFilter(request, response, chain);
        } else {
            chain.doFilter(request, response);
        }
    }
}
