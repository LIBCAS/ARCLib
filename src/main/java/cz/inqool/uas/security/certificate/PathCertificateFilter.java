package cz.inqool.uas.security.certificate;

import cz.inqool.uas.audit.AuditLogger;
import cz.inqool.uas.security.preauth.PathPreauthFilter;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.web.authentication.preauth.x509.SubjectDnX509PrincipalExtractor;
import org.springframework.security.web.authentication.preauth.x509.X509PrincipalExtractor;

import javax.servlet.http.HttpServletRequest;
import java.security.cert.X509Certificate;

/**
 * Certificate pre-authentication filter for specific URLs.
 */
public class PathCertificateFilter extends PathPreauthFilter {
    private X509PrincipalExtractor principalExtractor = new SubjectDnX509PrincipalExtractor();

    public PathCertificateFilter(AuthenticationManager authenticationManager, AuditLogger logger, String authQuery) {
        super(authenticationManager, logger, authQuery);
    }

    public PathCertificateFilter(AuthenticationManager authenticationManager, AuditLogger logger, String[] authQueries) {
        super(authenticationManager, logger, authQueries);
    }

    @Override
    protected String extractUsername(HttpServletRequest request) {
        return (String) getPreAuthenticatedPrincipal(request);
    }

    protected Object getPreAuthenticatedPrincipal(HttpServletRequest request) {
        X509Certificate cert = extractClientCertificate(request);

        if (cert == null) {
            return null;
        }

        return principalExtractor.extractPrincipal(cert);
    }

    protected Object getPreAuthenticatedCredentials(HttpServletRequest request) {
        return extractClientCertificate(request);
    }

    private X509Certificate extractClientCertificate(HttpServletRequest request) {
        X509Certificate[] certs = (X509Certificate[]) request
                .getAttribute("javax.servlet.request.X509Certificate");

        if (certs != null && certs.length > 0) {
            if (logger.isDebugEnabled()) {
                logger.debug("X.509 client authentication certificate:" + certs[0]);
            }

            return certs[0];
        }

        if (logger.isDebugEnabled()) {
            logger.debug("No client certificate found in request.");
        }

        return null;
    }

    public void setPrincipalExtractor(X509PrincipalExtractor principalExtractor) {
        this.principalExtractor = principalExtractor;
    }
}
