package cz.inqool.uas.util.httpclient;

import cz.inqool.uas.exception.GeneralException;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContextBuilder;
import org.springframework.core.io.Resource;
import org.springframework.ws.transport.http.HttpComponentsMessageSender;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.cert.CertificateException;

/**
 * Helper for setting up SSL communication.
 */
public class SSLHelperMK2 {

    public static void setSslContext(HttpClientBuilder clientBuilder, Resource keyStore, String keyStorePswd, Resource trustStore, String trustStorePswd) {
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

            clientBuilder
                    .addInterceptorFirst(new HttpComponentsMessageSender.RemoveSoapHeadersInterceptor())
                    .setSSLContext(sslContext)
                    .setSSLHostnameVerifier(new DefaultHostnameVerifier());
        } catch (CertificateException | NoSuchAlgorithmException | KeyStoreException | IOException | UnrecoverableKeyException | KeyManagementException ex) {
            throw new GeneralException(ex);
        }
    }

    private static KeyStore getKeyStore(Resource resource, String password) throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException {

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
