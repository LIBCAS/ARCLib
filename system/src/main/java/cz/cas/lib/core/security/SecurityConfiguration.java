package cz.cas.lib.core.security;

import cz.cas.lib.arclib.domainbase.audit.AuditLogger;
import cz.cas.lib.arclib.security.authorization.business.CustomAccessDeniedHandler;
import cz.cas.lib.core.security.basic.PathBasicAuthFilter;
import cz.cas.lib.core.security.jwt.JwtFilter;
import cz.cas.lib.core.security.jwt.JwtPostFilter;
import cz.cas.lib.core.security.jwt.JwtTokenProvider;
import cz.cas.lib.core.security.jwt.spi.JwtHandler;
import jakarta.servlet.Filter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

import static cz.cas.lib.core.util.Utils.asArray;

/**
 * @author Lukas Jane (inQool), 2023.
 */
@Configuration
@EnableMethodSecurity(jsr250Enabled = true)
@EnableWebSecurity
public class SecurityConfiguration {

    private AuthenticationManager authenticationManager;
    private JwtTokenProvider tokenProvider;
    private AuditLogger auditLogger;
    private String authQuery;
    private String jwtSecret;
    private JwtHandler jwtHandler;
    private AuthenticationProvider authenticationProvider;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        HttpSecurity httpSecurity = http
                .authenticationProvider(authenticationProvider)
                .authenticationProvider(tokenProvider)
                .securityMatchers(c -> c.requestMatchers(urlPatterns()))
                .sessionManagement(c -> c.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(c -> c.accessDeniedHandler(accessDeniedHandler()))
                .headers(c -> {
                    c.cacheControl(Customizer.withDefaults());
                    c.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable);
                })
                .authorizeHttpRequests(c -> c.anyRequest().permitAll())
                .csrf(AbstractHttpConfigurer::disable);

        Filter[] filters = primarySchemeFilters();
        for (Filter filter : filters) {
            httpSecurity = httpSecurity.addFilterBefore(filter, AnonymousAuthenticationFilter.class);
        }

        httpSecurity = httpSecurity.addFilterBefore(new JwtFilter(jwtSecret, jwtHandler), AnonymousAuthenticationFilter.class);

        httpSecurity.addFilterAfter(new JwtPostFilter(tokenProvider), AuthorizationFilter.class);
        return http.build();
    }

    private String[] urlPatterns() {
        return new String[]{"/api/**"};
    }


    protected Filter[] primarySchemeFilters() throws Exception {
        PathBasicAuthFilter filter = new PathBasicAuthFilter(authenticationManager, auditLogger, authQuery);
        return asArray(filter);
    }

    @Autowired
    @Lazy
    public void setAuthenticationManager(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return new CustomAccessDeniedHandler();
    }

    @Autowired
    public void setTokenProvider(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Autowired
    public void setAuditLogger(AuditLogger auditLogger) {
        this.auditLogger = auditLogger;
    }

    @Autowired
    public void setAuthQuery(@Value("${security.basic.authQuery}") String authQuery) {
        this.authQuery = authQuery;
    }

    /**
     * Sets the secret used for token signing.
     *
     * @param secret Provided secret
     */
    @Autowired
    public void setJwtSecret(@Value("${security.jwt.secret}") String secret) {
        this.jwtSecret = secret;
    }

    @Autowired
    public void setJwtHandler(JwtHandler jwtHandler) {
        this.jwtHandler = jwtHandler;
    }

    @Autowired
    public void setAuthenticationProvider(AuthenticationProvider authenticationProvider) {
        this.authenticationProvider = authenticationProvider;
    }
}
