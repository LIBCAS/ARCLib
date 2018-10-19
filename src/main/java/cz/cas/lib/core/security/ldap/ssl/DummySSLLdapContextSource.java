package cz.cas.lib.core.security.ldap.ssl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ldap.core.support.LdapContextSource;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.ldap.InitialLdapContext;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Hashtable;

@Slf4j
public class DummySSLLdapContextSource extends LdapContextSource {
    @Override
    protected DirContext getDirContextInstance(Hashtable<String, Object> env) throws NamingException {
        String url = (String) env.get(Context.PROVIDER_URL);
        try {
            if (new URI(url).getScheme().equalsIgnoreCase("ldaps")) {
                env.put("java.naming.ldap.factory.socket", "cz.cas.lib.core.security.ldap.ssl.DummySSLSocketFactory");
            }
        } catch (URISyntaxException e) {
            log.error("LDAP URL {} is wrong", url, e);
        }
        return new InitialLdapContext(env, null);
    }
}
