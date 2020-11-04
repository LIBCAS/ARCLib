package cz.cas.lib.arclib.security.user;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@Configuration
public class UserProducer {
    @Bean
    @Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
    public UserDetails userDetails() {
        Object principal = null;

        SecurityContext context = SecurityContextHolder.getContext();
        if (context != null) {
            Authentication authentication = context.getAuthentication();

            if (authentication != null) {
                principal = authentication.getPrincipal();
            }
        }

        if (principal instanceof UserDetailsImpl) {
            return (UserDetails) principal;
        } else
            return null;
    }
}
