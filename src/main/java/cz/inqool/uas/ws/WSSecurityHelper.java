package cz.inqool.uas.ws;

import cz.inqool.uas.exception.GeneralException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.wss4j.common.crypto.Crypto;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.ws.client.core.support.WebServiceGatewaySupport;
import org.springframework.ws.client.support.interceptor.ClientInterceptor;
import org.springframework.ws.soap.security.wss4j2.Wss4jSecurityInterceptor;
import org.springframework.ws.soap.security.wss4j2.support.CryptoFactoryBean;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Helper for setting up WS-Security Signature creation and validation.
 */
@Slf4j
@Service
public class WSSecurityHelper {
    /**
     * Applies WS-Security signature creation, validation or both configuration to implementation of {@link WebServiceGatewaySupport}.
     *
     * @param ws Webservice client implementation
     * @param keyStore KeyStore
     * @param keyStorePswd Password for KeyStore
     * @param trustStore TrustStore
     * @param trustStorePswd Password for TrustStore
     */
    public void apply(WebServiceGatewaySupport ws, Resource keyStore, String keyStorePswd,
                      String keyAlias, String keyPassword,
                      Resource trustStore, String trustStorePswd) {

        ClientInterceptor[] interceptors = new ClientInterceptor[] {createInterceptor(keyStore, keyStorePswd,
                keyAlias, keyPassword, trustStore, trustStorePswd)};

        ws.setInterceptors(interceptors);
    }

    public Wss4jSecurityInterceptor createInterceptor(Resource keyStore, String keyStorePswd,
                                                      String keyAlias, String keyPassword,
                                                      Resource trustStore, String trustStorePswd) {

        try {
            Wss4jSecurityInterceptor interceptor = new Wss4jSecurityInterceptor();

            if (keyStore != null) {
                interceptor.setSecurementActions("Signature");
                interceptor.setSecurementUsername(keyAlias);
                interceptor.setSecurementPassword(keyPassword);
                interceptor.setSecurementEncryptionCrypto(getCrypto(keyStore, keyStorePswd));
            }

            if (trustStore != null) {
                interceptor.setValidationActions("Signature");
                interceptor.setSecurementEncryptionCrypto(getCrypto(trustStore, trustStorePswd));
            }

            return interceptor;
        } catch (Exception ex) {
            throw new GeneralException(ex);
        }
    }

    private Crypto getCrypto(Resource resource, String password) throws Exception {
        CryptoFactoryBean factory = new CryptoFactoryBean();

        File file = createTempFile(resource);

        try {
            factory.setKeyStoreLocation(new FileSystemResource(file));

            if (password != null) {
                factory.setKeyStorePassword(password);
            }

            String filename = resource.getFilename().toLowerCase();
            if (filename.endsWith("pfx") || filename.endsWith("p12")) {
                factory.setKeyStoreType("PKCS12");
            } else {
                factory.setKeyStoreType("JKS");
            }

            factory.afterPropertiesSet();
            return factory.getObject();
        } finally {
            if (!file.delete()) {
                log.warn("Failed to delete temporal keystore");
            }
        }
    }

    private File createTempFile(Resource resource) throws IOException {
        File temp = File.createTempFile("temp", "tmp");

        try (InputStream in = resource.getInputStream()){
            FileUtils.copyInputStreamToFile(in, temp);
        }

        return temp;
    }
}
