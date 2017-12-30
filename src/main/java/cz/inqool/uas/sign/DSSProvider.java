package cz.inqool.uas.sign;

import eu.europa.esig.dss.pades.signature.PAdESService;
import eu.europa.esig.dss.validation.CommonCertificateVerifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DSSProvider {
    @Bean
    public PAdESService pAdESService() {
        return new PAdESService(new CommonCertificateVerifier());
    }
}
