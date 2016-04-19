/*
* Artifactory is a binaries repository manager.
* Copyright (C) 2012 JFrog Ltd.
*
* Artifactory is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* Artifactory is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
*/

package org.artifactory.repo.http.kerberos;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.jaas.memory.InMemoryConfiguration;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import org.apache.http.client.methods.HttpRequestBase;

import java.io.IOException;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;


/**
 * Provides kerberos auth capabilities
 *
 * @author Michael Pasternak
 */
public class KerberosAuthProvider {
    private final CloseableHttpClient closeableHttpClient;
    private static final Logger log = LoggerFactory.getLogger(KerberosAuthProvider.class);

    public KerberosAuthProvider(CloseableHttpClient closeableHttpClient) {
        this.closeableHttpClient = closeableHttpClient;
    }

    /**
     * Executes request using kerberos credentials authentication
     *
     * @param principal a user principal
     * @param password a principal password
     * @param request a request to execute
     *
     * @return {@link CloseableHttpResponse}
     */
    public CloseableHttpResponse executeKerberos(String principal, char[] password, HttpRequestBase request) {
        LoginContext loginCOntext = null;
        CloseableHttpResponse response = null;
        try {
            loginCOntext = new LoginContext("KrbLogin", new Subject(),
                    new KerberosCallBackHandler(principal, password),
                    new InMemoryConfiguration(new HashMap<String, AppConfigurationEntry[]>(){{
                        put("KrbLogin", getPasswordConfigurationEntry());
                    }}));

            log.trace("Obtaining password subject for principal '{}'", principal);
            loginCOntext.login();
            // TODO: [mp] reuse loged-in state rather than re-authenticating

            PrivilegedAction sendAction = new PrivilegedAction() {
                CloseableHttpResponse res = null;
                @Override
                public Object run() {
                    try {
                        log.trace("Executing PrivilegedAction callback");
                        res = closeableHttpClient.execute(request);
                    } catch (IOException e) {
                        log.error("Error during privileged action execution, " + e.getMessage());
                    }
                    return res;
                }
            };
            log.trace("Executing PrivilegedAction using password subject principals '{}'", loginCOntext.getSubject().getPrincipals());
            response = (CloseableHttpResponse) Subject.doAs(loginCOntext.getSubject(), sendAction);
            return response;
        } catch (LoginException le) {
            log.error("Kerberos login has failed, " + le.getMessage());
            log.debug("Cause: {}", le);
        } finally {
            logoutKerberos(loginCOntext);
        }
        return null;
    }

    /**
     * Explicit logout for kerberos auth
     *
     * @param loginCOntext {@link LoginContext} to be used
     */
    private void logoutKerberos(LoginContext loginCOntext) {
        if(loginCOntext != null) {
            try {
                log.trace("Log out of principals {}", loginCOntext.getSubject().getPrincipals());
                loginCOntext.logout();
            } catch (LoginException e) {
                log.debug("Logout has filed: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Executes request using kerberos keytab authentication
     *
     * @param userPrincipal a principal to be used
     * @param keyTabLocation a keytab location
     * @param request a request to execute
     *
     * @return {@link CloseableHttpResponse}
     */
    public CloseableHttpResponse executeKerberos(String userPrincipal, String keyTabLocation, HttpRequestBase request) {
        LoginContext loginCOntext = null;
        CloseableHttpResponse response = null;
        try {
            loginCOntext = new LoginContext("KrbLogin", new Subject(),
                    new KerberosCallBackHandler(userPrincipal),
                    new InMemoryConfiguration(new HashMap<String, AppConfigurationEntry[]>(){{
                        put("KrbLogin", getKeyTabConfigurationEntry(keyTabLocation, userPrincipal));
                    }}));

            log.trace("Obtaining keytab subject for principal '{}'", userPrincipal);
            loginCOntext.login();
            // TODO: [mp] reuse loged-in state rather than re-authenticating

            PrivilegedAction sendAction = new PrivilegedAction() {
                CloseableHttpResponse res = null;
                @Override
                public Object run() {
                    try {
                        log.trace("Executing PrivilegedAction callback");
                        res = closeableHttpClient.execute(request);
                    } catch (IOException e) {
                        log.error("Error during privileged action execution, " + e.getMessage());
                    }
                    return res;
                }
            };
            log.trace("Executing PrivilegedAction using keytab subject principals '{}'", loginCOntext.getSubject().getPrincipals());
            response = (CloseableHttpResponse) Subject.doAs(loginCOntext.getSubject(), sendAction);
            return response;
        } catch (LoginException le) {
            log.error("Kerberos login has failed, " + le.getMessage());
        } finally {
            logoutKerberos(loginCOntext);
        }
        return null;
    }

    /**
     * Configures keytab centric {@link AppConfigurationEntry}s
     *
     * @param keyTabLocation keytab location
     * @param userPrincipal the principal to be used
     *
     * @return and array of {@link AppConfigurationEntry}
     */
    private AppConfigurationEntry[] getKeyTabConfigurationEntry(String keyTabLocation, String userPrincipal) {

        Map<String, Object> options = new HashMap<String, Object>();

        options.put("useKeyTab", "true");
        options.put("storeKey", "true");
        options.put("keyTab", keyTabLocation);
        options.put("principal", userPrincipal);
        options.put("isInitiator", "true");
        options.put("debug", Boolean.toString(log.isDebugEnabled()));

        return new AppConfigurationEntry[] { new AppConfigurationEntry(
                "com.sun.security.auth.module.Krb5LoginModule",
                AppConfigurationEntry.LoginModuleControlFlag.REQUIRED, options) };
    }

    /**
     * Configures password centric {@link AppConfigurationEntry}s
     *
     * @return and array of {@link AppConfigurationEntry}
     */
    private AppConfigurationEntry[] getPasswordConfigurationEntry() {

        Map<String, Object> options = new HashMap<String, Object>();

        options.put("useSubjectCredsOnly", "false");
        options.put("doNotPrompt", "false");
        options.put("useTicketCache", "true");

        return new AppConfigurationEntry[] { new AppConfigurationEntry(
                "com.sun.security.auth.module.Krb5LoginModule",
                AppConfigurationEntry.LoginModuleControlFlag.REQUIRED, options) };
    }

    /**
     * A default KerberosCallBackHandler
     */
    class KerberosCallBackHandler implements CallbackHandler {

        private final String principal;
        private final char[] password;

        /**
         * @param principal
         * @param password
         */
        public KerberosCallBackHandler(String principal, char[] password) {
            this.principal = principal;
            this.password = password;
        }

        public KerberosCallBackHandler(String principal) {
            this.principal = principal;
            this.password = null;
        }

        /**
         * Handles callbacks
         *
         * @param callbacks
         * @throws IOException
         * @throws UnsupportedCallbackException
         */
        public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
            log.trace("KerberosCallBackHandler is invoked");
            for (Callback callback : callbacks) {
                log.trace("processing callback {}", callback.getClass().getSimpleName());
                if (callback instanceof NameCallback) {
                    NameCallback nc = (NameCallback) callback;
                    nc.setName(principal);
                } else if (callback instanceof PasswordCallback) {
                    PasswordCallback pc = (PasswordCallback) callback;
                    pc.setPassword(password);
                } else {
                    throw new UnsupportedCallbackException(callback, "Unknown Callback");
                }
            }
        }
    }
}
