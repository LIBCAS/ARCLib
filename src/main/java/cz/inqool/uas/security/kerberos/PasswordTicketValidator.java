/*
 * Copyright 2009-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cz.inqool.uas.security.kerberos;

import lombok.extern.slf4j.Slf4j;
import org.ietf.jgss.*;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.kerberos.authentication.KerberosTicketValidation;
import org.springframework.security.kerberos.authentication.KerberosTicketValidator;
import org.springframework.util.Assert;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import java.security.Principal;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Implementation of {@link KerberosTicketValidator} which uses the SUN JAAS
 * login module, which is included in the SUN JRE, it will not work with an IBM JRE.
 * The whole configuration is done in this class, no additional JAAS configuration
 * is needed.
 *
 * @author Mike Wiesner
 * @author Jeremy Stone
 * @author Matus Zamborsky
 * @since 1.0
 */
@Slf4j
public class PasswordTicketValidator implements KerberosTicketValidator, InitializingBean {

    private String servicePrincipal;
    private String password;
    private Subject serviceSubject;
    private boolean holdOnToGSSContext;
    private boolean debug = true;
    private boolean isInitiator = true;

    @Override
    public KerberosTicketValidation validateTicket(byte[] token) {
        try {
            return Subject.doAs(this.serviceSubject, new KerberosValidateAction(token));
        }
        catch (PrivilegedActionException e) {
            throw new BadCredentialsException("Kerberos validation not successful", e);
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(this.servicePrincipal, "servicePrincipal must be specified");
        LoginConfig loginConfig = new LoginConfig(this.servicePrincipal, this.debug, isInitiator);
        Set<Principal> princ = new HashSet<>(1);
        princ.add(new KerberosPrincipal(this.servicePrincipal));
        Subject sub = new Subject(false, princ, new HashSet<>(), new HashSet<>());

        final CallbackHandler handler = getUsernamePasswordHandler(servicePrincipal, password);

        LoginContext lc = new LoginContext("", sub, handler, loginConfig);
        lc.login();
        this.serviceSubject = lc.getSubject();
    }

    /**
     * The service principal of the application.
     * For web apps this is <code>HTTP/full-qualified-domain-name@DOMAIN</code>.
     * The keytab must contain the key for this principal.
     *
     * @param servicePrincipal service principal to use
     */
    public void setServicePrincipal(String servicePrincipal) {
        this.servicePrincipal = servicePrincipal;
    }

    /**
     * Enables the debug mode of the JAAS Kerberos login module.
     *
     * @param debug default is false
     */
    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public void setIsInitiator(boolean isInitiator) {
        this.isInitiator = isInitiator;
    }

    /**
     * Determines whether to hold on to the {@link GSSContext GSS security context} or
     * otherwise {@link GSSContext#dispose() dispose} of it immediately (the default behaviour).
     * <p>Holding on to the GSS context allows decrypt and encrypt operations for subsequent
     * interactions with the principal.
     *
     * @param holdOnToGSSContext true if should hold on to context
     */
    public void setHoldOnToGSSContext(boolean holdOnToGSSContext) {
        this.holdOnToGSSContext = holdOnToGSSContext;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * This class is needed, because the validation must run with previously generated JAAS subject
     * which belongs to the service principal and was loaded out of the keytab during startup.
     */
    private class KerberosValidateAction implements PrivilegedExceptionAction<KerberosTicketValidation> {
        byte[] kerberosTicket;

        public KerberosValidateAction(byte[] kerberosTicket) {
            this.kerberosTicket = kerberosTicket;
        }

        @Override
        public KerberosTicketValidation run() throws Exception {
            byte[] responseToken = new byte[0];
            GSSName gssName = null;
            GSSContext context = GSSManager.getInstance().createContext((GSSCredential) null);
            boolean first = true;
            while (!context.isEstablished()) {
                if (first) {
                    kerberosTicket = tweakJdkRegression(kerberosTicket);
                }
                responseToken = context.acceptSecContext(kerberosTicket, 0, kerberosTicket.length);
                gssName = context.getSrcName();
                if (gssName == null) {
                    throw new BadCredentialsException("GSSContext name of the context initiator is null");
                }
                first = false;
            }
            if (!holdOnToGSSContext) {
                context.dispose();
            }
            return new KerberosTicketValidation(gssName.toString(), servicePrincipal, responseToken, context);
        }
    }

    /**
     * Normally you need a JAAS config file in order to use the JAAS Kerberos Login Module,
     * with this class it is not needed and you can have different configurations in one JVM.
     */
    private static class LoginConfig extends Configuration {
        private String servicePrincipalName;
        private boolean debug;
        private boolean isInitiator;

        public LoginConfig(String servicePrincipalName, boolean debug, boolean isInitiator) {
            this.servicePrincipalName = servicePrincipalName;
            this.debug = debug;
            this.isInitiator = isInitiator;
        }

        @Override
        public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
            HashMap<String, String> options = new HashMap<>();
            options.put("principal", this.servicePrincipalName);
            options.put("storeKey", "true");
            options.put("debug", String.valueOf(debug));
            options.put("isInitiator", String.valueOf(isInitiator));

            return new AppConfigurationEntry[] { new AppConfigurationEntry("com.sun.security.auth.module.Krb5LoginModule",
                    AppConfigurationEntry.LoginModuleControlFlag.REQUIRED, options), };
        }

    }

    private static byte[] tweakJdkRegression(byte[] token) throws GSSException {

//    	Due to regression in 8u40/8u45 described in
//    	https://bugs.openjdk.java.net/browse/JDK-8078439
//    	try to tweak token package if it looks like it has
//    	OID's in wrong order
//
//      0000: 60 82 06 5C 06 06 2B 06   01 05 05 02 A0 82 06 50
//      0010: 30 82 06 4C A0 30 30 2E  |06 09 2A 86 48 82 F7 12
//      0020: 01 02 02|06 09 2A 86 48   86 F7 12 01 02 02 06|0A
//      0030: 2B 06 01 04 01 82 37 02   02 1E 06 0A 2B 06 01 04
//      0040: 01 82 37 02 02 0A A2 82   06 16 04 82 06 12 60 82
//
//    	In above package first token is in position 24 and second
//    	in 35 with both having size 11.
//
//    	We simple check if we have these two in this order and swap
//
//    	Below code would create two arrays, lets just create that
//    	manually because it doesn't change
//      Oid GSS_KRB5_MECH_OID = new Oid("1.2.840.113554.1.2.2");
//      Oid MS_KRB5_MECH_OID = new Oid("1.2.840.48018.1.2.2");
//		byte[] der1 = GSS_KRB5_MECH_OID.getDER();
//		byte[] der2 = MS_KRB5_MECH_OID.getDER();

//		0000: 06 09 2A 86 48 86 F7 12   01 02 02
//		0000: 06 09 2A 86 48 82 F7 12   01 02 02

        if (token == null || token.length < 48) {
            return token;
        }

        int[] toCheck = new int[] { 0x06, 0x09, 0x2A, 0x86, 0x48, 0x82, 0xF7, 0x12, 0x01, 0x02, 0x02, 0x06, 0x09, 0x2A,
                0x86, 0x48, 0x86, 0xF7, 0x12, 0x01, 0x02, 0x02 };

        for (int i = 0; i < 22; i++) {
            if ((byte) toCheck[i] != token[i + 24]) {
                return token;
            }
        }

        byte[] nt = new byte[token.length];
        System.arraycopy(token, 0, nt, 0, 24);
        System.arraycopy(token, 35, nt, 24, 11);
        System.arraycopy(token, 24, nt, 35, 11);
        System.arraycopy(token, 46, nt, 46, token.length - 24 - 11 - 11);
        return nt;
    }

    private static CallbackHandler getUsernamePasswordHandler(final String username, final String password) {
        return callbacks -> {
            for (Callback callback : callbacks) {
                if (callback instanceof NameCallback) {
                    final NameCallback nameCallback = (NameCallback) callback;
                    nameCallback.setName(username);
                } else if (callback instanceof PasswordCallback) {
                    final PasswordCallback passCallback = (PasswordCallback) callback;
                    passCallback.setPassword(password.toCharArray());
                } else {
                    System.err.println("Unsupported Callback: " + callback.getClass().getName());
                }
            }
        };
    }
}
