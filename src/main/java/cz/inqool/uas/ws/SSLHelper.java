package cz.inqool.uas.ws;

import cz.inqool.uas.exception.GeneralException;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.ws.client.core.support.WebServiceGatewaySupport;
import org.springframework.ws.transport.WebServiceMessageSender;
import org.springframework.ws.transport.http.HttpComponentsMessageSender;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.cert.CertificateException;

/**
 * Helper for setting up SSL communication.
 */
@Service
public class SSLHelper {

    /**
     * Applies one-way or two-way SSL configuration to implementation of {@link WebServiceGatewaySupport}.
     * Adds also RemoveSoapHeadersInterceptor, because otherwise there will be Content-Length already present exceptions
     *
     * @param ws Webservice client implementation
     * @param keyStore KeyStore
     * @param keyStorePswd Password for KeyStore
     * @param trustStore TrustStore
     * @param trustStorePswd Password for TrustStore
     */
    public void apply(WebServiceGatewaySupport ws, Resource keyStore,
                     String keyStorePswd, Resource trustStore, String trustStorePswd) {

        WebServiceMessageSender messageSender = getMessageSender(keyStore, keyStorePswd, trustStore, trustStorePswd);
        ws.setMessageSender(messageSender);
    }

    private WebServiceMessageSender getMessageSender(Resource keyStore, String keyStorePswd, Resource trustStore, String trustStorePswd) {
        try {
            SSLContextBuilder builder = SSLContextBuilder.create();
            if (keyStore != null) {
                KeyStore store = getKeyStore(keyStore, keyStorePswd);
                builder.loadKeyMaterial(store, keyStorePswd != null ? keyStorePswd.toCharArray() : null);
            }

            if (trustStore != null) {
                KeyStore store = getKeyStore(trustStore, trustStorePswd);
                builder.loadTrustMaterial(store, null);
            }

            SSLContext sslContext = builder.build();

            CloseableHttpClient client = HttpClients.custom()
                    .addInterceptorFirst(new HttpComponentsMessageSender.RemoveSoapHeadersInterceptor())
                    .setSSLContext(sslContext)
                    .setSSLHostnameVerifier(new DefaultHostnameVerifier())
                    .build();

            return new HttpComponentsMessageSender(client);
        } catch (CertificateException | NoSuchAlgorithmException | KeyStoreException | IOException | UnrecoverableKeyException | KeyManagementException ex) {
            throw new GeneralException(ex);
        }
    }

    private KeyStore getKeyStore(Resource resource, String password) throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException {

        try (InputStream stream = resource.getInputStream()) {
            KeyStore keyStore;

            String filename = resource.getFilename().toLowerCase();
            if (filename.endsWith("pfx") || filename.endsWith("p12")) {
                keyStore = KeyStore.getInstance("PKCS12");
            } else {
                keyStore = KeyStore.getInstance("JKS");
            }

            keyStore.load(stream,password.toCharArray());

            return keyStore;
        }
    }
}
