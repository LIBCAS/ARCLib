package cz.cas.lib.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PasswordEncoderProducer {

    /**
     * Produces Spring {@link PasswordEncoder} used for hashing user passwords if applicable.
     *
     * <p>
     * In case the identity is assumed from external system, this encoder is not used.
     * </p>
     *
     * @return Produced {@link PasswordEncoder}
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
