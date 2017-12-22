package cz.inqool.uas.util.httpclient;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.springframework.core.io.Resource;

/**
 * @author Lukas Jane (inQool) 12.12.2017.
 */
public class HttpClientCooker {
    private HttpClientBuilder builder = HttpClients.custom();

    public static HttpClientCooker gimmeNew() {
        return new HttpClientCooker();
    }

    public HttpClientCooker withSslContext(Resource keyStore, String keyStorePswd, Resource trustStore, String trustStorePswd) {
        SSLHelperMK2.setSslContext(builder, keyStore, keyStorePswd, trustStore, trustStorePswd);
        return this;
    }
    public HttpClientCooker withSpnego() {
        SpnegoHelper.addSpnego(builder);
        return this;
    }
    public HttpClientCooker withNoCookies() {
        builder.disableCookieManagement();
        return this;
    }
    public CloseableHttpClient shipIt() {
        return builder.build();
    }
}
