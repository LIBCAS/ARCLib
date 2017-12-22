package cz.inqool.uas.security.ldap.ssl;

import lombok.extern.slf4j.Slf4j;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

@Slf4j
public class DummySSLSocketFactory extends SSLSocketFactory {
    private SSLSocketFactory socketFactory;

    public DummySSLSocketFactory() {
        try {
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(null, new TrustManager[] { new DummyTrustmanager() }, new SecureRandom());
            socketFactory = ctx.getSocketFactory();
        } catch (Exception ex) {
            log.error("Failed to create SSL context.", ex);
        }
    }

    public static SocketFactory getDefault() {
        return new DummySSLSocketFactory();
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return socketFactory.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return socketFactory.getSupportedCipherSuites();
    }

    @Override
    public Socket createSocket(Socket socket, String string, int i, boolean bln) throws IOException {
        return socketFactory.createSocket(socket, string, i, bln);
    }

    @Override
    public Socket createSocket(String string, int i) throws IOException, UnknownHostException {
        return socketFactory.createSocket(string, i);
    }

    @Override
    public Socket createSocket(String string, int i, InetAddress ia, int i1) throws IOException, UnknownHostException {
        return socketFactory.createSocket(string, i, ia, i1);
    }

    @Override
    public Socket createSocket(InetAddress ia, int i) throws IOException {
        return socketFactory.createSocket(ia, i);
    }

    @Override
    public Socket createSocket(InetAddress ia, int i, InetAddress ia1, int i1) throws IOException {
        return socketFactory.createSocket(ia, i, ia1, i1);
    }

    public static class DummyTrustmanager implements X509TrustManager {
        public void checkClientTrusted(X509Certificate[] xcs, String string) throws CertificateException {
            // do nothing
        }

        public void checkServerTrusted(X509Certificate[] xcs, String string) throws CertificateException {
            // do nothing
        }

        public X509Certificate[] getAcceptedIssuers() {
            return new java.security.cert.X509Certificate[0];
        }
    }
}