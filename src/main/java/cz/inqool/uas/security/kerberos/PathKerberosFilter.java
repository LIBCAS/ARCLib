package cz.inqool.uas.security.kerberos;

import cz.inqool.uas.audit.AuditLogger;
import cz.inqool.uas.security.audit.LoginEvent;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.kerberos.web.authentication.SpnegoAuthenticationProcessingFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Set;

import static cz.inqool.uas.util.Utils.asSet;

public class PathKerberosFilter extends SpnegoAuthenticationProcessingFilter {
    private Set<String> authQueries;

    public PathKerberosFilter(AuthenticationManager authenticationManager, AuditLogger logger, String authQuery) {
        this(authenticationManager, logger, new String[]{authQuery});
    }

    public PathKerberosFilter(AuthenticationManager authenticationManager, AuditLogger logger, String[] authQueries) {
        this.authQueries = asSet(authQueries);

        this.setAuthenticationManager(authenticationManager);

        this.setSuccessHandler((request, response, authentication) -> logger.logEvent(new LoginEvent(Instant
                .now(), authentication.getName(), true)));
        this.setFailureHandler((request, response, exception) -> logger.logEvent(new LoginEvent(Instant.now(), null, false)));
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
            throw new ServletException("PathKerberosFilter just supports HTTP requests");
        }
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String query = httpRequest.getRequestURI();

        if (!authQueries.contains(query)) {
            chain.doFilter(request, response);
            return;
        }

        String header = httpRequest.getHeader("Authorization");

        if (header == null) {
            httpResponse.addHeader("WWW-Authenticate", "Negotiate");
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            httpResponse.flushBuffer();
        } else {
            super.doFilter(request, response, chain);
        }
    }
}
