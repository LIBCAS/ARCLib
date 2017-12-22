package cz.inqool.uas.util.httpclient;

import org.apache.http.auth.AuthSchemeProvider;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.config.Lookup;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.impl.auth.SPNegoSchemeFactory;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;

import java.security.Principal;

/**
 * Helper for adding spnego to apache client
 */
public class SpnegoHelper {
    public static void addSpnego(HttpClientBuilder clientBuilder) {
        //Add spnego http header processor
        Lookup<AuthSchemeProvider> authSchemeRegistry = RegistryBuilder.<AuthSchemeProvider> create()
                .register(AuthSchemes.SPNEGO, new SPNegoSchemeFactory(true)).build();
        clientBuilder.setDefaultAuthSchemeRegistry(authSchemeRegistry);
        //There has to be at least this dummy credentials provider or apache http client gives up
        BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(new AuthScope(null, -1, null), new NullCredentials());
        clientBuilder.setDefaultCredentialsProvider(credentialsProvider);
    }

    private static class NullCredentials implements Credentials {
        @Override
        public Principal getUserPrincipal() {
            return null;
        }

        @Override
        public String getPassword() {
            return null;
        }
    }
}
