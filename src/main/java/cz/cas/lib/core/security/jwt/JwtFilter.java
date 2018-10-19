package cz.cas.lib.core.security.jwt;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Spring security filter authorizing user by JWT token.
 *
 * <p>
 * Detects the presence of Authorization header in http request and tests if it is a Bearer scheme. If so,
 * than pass the {@link JwtToken} as {@link Authentication} down the Spring security pipeline.
 * </p>
 */
public class JwtFilter extends OncePerRequestFilter {
    private static final String AUTHENTICATION_SCHEME_NAME = "Bearer";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith(AUTHENTICATION_SCHEME_NAME)) {
            String token = extractTokenFromRequest(header);

            Authentication auth = new JwtToken(token);
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
    }

    private String extractTokenFromRequest(String authorizationHeader) {
        return authorizationHeader.substring(AUTHENTICATION_SCHEME_NAME.length() + 1);
    }
}
