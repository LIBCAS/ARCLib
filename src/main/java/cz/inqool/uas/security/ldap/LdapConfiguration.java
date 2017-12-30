package cz.inqool.uas.security.ldap;

import cz.inqool.uas.security.ldap.authorities.UasAuthoritiesPopulator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.ldap.search.LdapUserSearch;

import javax.inject.Inject;

@ConditionalOnProperty(prefix = "security.ldap", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(LdapConfiguration.DefaultLdapProperties.class)
@Slf4j
@Configuration
public class LdapConfiguration {

    @ConfigurationProperties(prefix = "security.ldap")
    static class DefaultLdapProperties extends LdapProperties {}

    private DefaultLdapProperties properties;

    @Bean
    public LdapContextSource contextSource() {
        return LdapFactory.createContext(properties);
    }

    @ConditionalOnProperty(prefix = "security.ldap.user", name = "type")
    @Bean
    public LdapUserSearch userSearch() {
        return LdapFactory.createUserSearch(properties, contextSource());
    }

    @Bean
    @ConditionalOnProperty(prefix = "security.ldap.group", name = "type")
    public UasAuthoritiesPopulator authoritiesPopulator() {
        return LdapFactory.createAuthoritiesPopulator(properties, contextSource());
    }

    @Inject
    public void setProperties(DefaultLdapProperties properties) {
        this.properties = properties;
    }
}
