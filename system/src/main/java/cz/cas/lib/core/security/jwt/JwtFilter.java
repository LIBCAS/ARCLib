package cz.cas.lib.core.security.jwt;

import cz.cas.lib.arclib.security.user.UserDetails;
import cz.cas.lib.core.security.jwt.spi.JwtHandler;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

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

    public JwtFilter(String secret, JwtHandler handler) {
        super();
        this.secret = secret;
        this.handler = handler;
    }

    private String secret;

    private JwtHandler handler;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(AUTHENTICATION_SCHEME_NAME)) {
            String token = extractTokenFromRequest(header);
            Claims claims = (Claims) Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret)))
                    .build()
                    .parse(token)
                    .getPayload();
            UserDetails user = handler.parseClaims(claims); // fully load user with roles and permissions from DB
            SecurityContextHolder.getContext().setAuthentication(new JwtToken(user, claims));
        }

        filterChain.doFilter(request, response);
    }

    private String extractTokenFromRequest(String authorizationHeader) {
        return authorizationHeader.substring(AUTHENTICATION_SCHEME_NAME.length() + 1);
    }
}
