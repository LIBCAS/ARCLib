package cz.inqool.uas.security.kerberos;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.kerberos.authentication.KerberosTicketValidator;
import org.springframework.security.kerberos.authentication.sun.GlobalSunJaasKerberosConfig;

import javax.inject.Inject;

@ConditionalOnProperty(prefix = "security.kerberos", name = "enabled", havingValue = "true")
@Slf4j
@Configuration
public class KerberosConfiguration {
    private String krb5Location;

    private String servicePrincipal;

    private String servicePassword;

    private Boolean debug;

    private Boolean initiator;

    @Bean
    public KerberosTicketValidator kerberosTicketValidator() {
        PasswordTicketValidator validator = new PasswordTicketValidator();
        validator.setServicePrincipal(servicePrincipal);
        validator.setPassword(servicePassword);
        validator.setDebug(debug);
        validator.setIsInitiator(initiator);
        return validator;
    }

    @Bean
    public GlobalSunJaasKerberosConfig globalSunJaasKerberosConfig() {
        GlobalSunJaasKerberosConfig config = new GlobalSunJaasKerberosConfig();
        config.setDebug(debug);
        config.setKrbConfLocation(krb5Location);
        return config;
    }

    @Inject
    public void setKrb5Location(@Value("${security.kerberos.krb5-location}") String krb5Location) {
        this.krb5Location = krb5Location;
    }

    @Inject
    public void setServicePrincipal(@Value("${security.kerberos.service-principal}") String servicePrincipal) {
        this.servicePrincipal = servicePrincipal;
    }

    @Inject
    public void setServicePassword(@Value("${security.kerberos.service-password}") String servicePassword) {
        this.servicePassword = servicePassword;
    }

    @Inject
    public void setDebug(@Value("${security.kerberos.debug}") Boolean debug) {
        this.debug = debug;
    }

    @Inject
    public void setInitiator(@Value("${security.kerberos.initiator}") Boolean initiator) {
        this.initiator = initiator;
    }
}
