package cz.inqool.uas.security.jwt;

import cz.inqool.uas.security.UserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Spring security filter putting JWT token into http response header.
 *
 * <p>
 *     Detects if user is authenticated with primary authentication scheme (other than {@link JwtToken}) or if used
 *     {@link JwtToken} is past the fresh period (implemented in {@link JwtTokenProvider#refreshIfNeeded(JwtToken)}).
 *     In that case return newly generated {@link JwtToken} through http response header.
 * </p>
 */
public class JwtPostFilter extends OncePerRequestFilter {
    private static final String AUTHENTICATION_SCHEME_NAME = "Bearer";

    private JwtTokenProvider provider;

    public JwtPostFilter(JwtTokenProvider provider) {
        this.provider = provider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        putTokenToResponse(response);

        filterChain.doFilter(request, response);
    }

    private void putTokenToResponse(HttpServletResponse response) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof JwtToken) {
            String newToken = provider.refreshIfNeeded((JwtToken)authentication);

            if (newToken != null) {
                response.addHeader(AUTHENTICATION_SCHEME_NAME, newToken);
            }
        } else if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
            UserDetails user = (UserDetails) authentication.getPrincipal();

            String token = provider.generateToken(user);

            response.addHeader(AUTHENTICATION_SCHEME_NAME, token);
        }
    }
}
