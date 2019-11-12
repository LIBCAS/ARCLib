package cz.cas.lib.core.security.certificate;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;

/**
 * Decodes certificate from Base64 encoded string
 */
@Slf4j
@Service
public class CertificateDecoder {
    private CertificateFactory factory;

    public CertificateDecoder() throws CertificateException {
        factory = CertificateFactory.getInstance("X.509");
    }

    public X509Certificate decode(String certStr) {
        try {
            byte[] decoded = Base64.getDecoder().decode(certStr);

            return (X509Certificate) factory.generateCertificate(new ByteArrayInputStream(decoded));
        } catch (IllegalArgumentException | CertificateException e) {
            log.warn("Failed to decode certificate {}.", certStr);
            return null;
        }
    }
}
