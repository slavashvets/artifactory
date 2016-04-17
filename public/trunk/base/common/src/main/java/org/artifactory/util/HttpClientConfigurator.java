/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2014 JFrog Ltd.
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

package org.artifactory.util;

import org.apache.commons.lang.StringUtils;
import org.apache.http.*;
import org.apache.http.auth.*;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.auth.BasicSchemeFactory;
import org.apache.http.impl.auth.SPNegoSchemeFactory;
import org.apache.http.impl.client.*;
import org.apache.http.impl.conn.DefaultRoutePlanner;
import org.apache.http.impl.conn.DefaultSchemePortResolver;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContexts;
import org.artifactory.common.ConstantValues;
import org.artifactory.descriptor.repo.ProxyDescriptor;
import org.artifactory.repo.http.CloseableHttpClientDecorator;
import org.artifactory.security.crypto.CryptoHelper;
import org.artifactory.util.bearer.BearerSchemeFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.net.ssl.SSLContext;
import java.io.File;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.Principal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Builder for HTTP client.
 *
 * @author Yossi Shaul
 */
public class HttpClientConfigurator {
    private static final Logger log = LoggerFactory.getLogger(HttpClientConfigurator.class);

    // in httpclient 4.4 handling of stale connections was changed,
    // previously, the code would check every connection by default before re-using it.
    // The code now only checks connection if the elapsed time since the last use of
    // the connection exceeds the timeout that has been set
    private static final int INACTIVITY_TIMEOUT = 500;
    private static final int DEFAULT_PORT = 80;
    // max connections for localhost:80
    private static final String LOCALHOST = "localhost";
    private static final int MAX_CONNECTIONS_PER_HOST = 50;


    private HttpClientBuilder builder = HttpClients.custom();
    private RequestConfig.Builder config = RequestConfig.custom();
    private String host;
    private BasicCredentialsProvider credsProvider;
    private boolean explicitCookieSupport;

    private String keyStoreLocation;
    private char[] keyStorePassword;
    private boolean trustSelfSignCert;
    private boolean noHostVerification;
    private boolean allowAnyHostAuth;
    private String proxyHost;
    private String repoKey;
    private AuthScheme chosenAuthScheme = AuthScheme.BASIC; //Signifies what auth scheme will be used by the client

    public HttpClientConfigurator() {
        builder.setUserAgent(HttpUtils.getArtifactoryUserAgent());
        credsProvider = new BasicCredentialsProvider();
        handleGzipResponse(ConstantValues.httpAcceptEncodingGzip.getBoolean());
        config.setMaxRedirects(20);
        config.setCircularRedirectsAllowed(true);
    }

    public CloseableHttpClient getClient() {
        if (!explicitCookieSupport && !ConstantValues.enableCookieManagement.getBoolean()) {
            builder.disableCookieManagement();
        }
        additionalConfigByAuthScheme();
        if (hasCredentials()) {
            builder.setDefaultCredentialsProvider(credsProvider);
        }
        builder.setDefaultRequestConfig(config.build());

        /**
         * Connection management
         */
        builder.setKeepAliveStrategy(getConnectionKeepAliveStrategy());
        PoolingHttpClientConnectionManager connectionMgr = createConnectionMgr();
        builder.setConnectionManager(connectionMgr);
        return new CloseableHttpClientDecorator(builder.build(), connectionMgr, chosenAuthScheme == AuthScheme.SPNEGO);
    }

    /**
     * Creates custom Http Client connection pool to be used by Http Client
     *
     * @return {@link PoolingHttpClientConnectionManager}
     */
    private PoolingHttpClientConnectionManager createConnectionMgr() {

        PoolingHttpClientConnectionManager connectionMgr;

        // prepare SSLContext
        if (!Strings.isNullOrEmpty(keyStoreLocation) && keyStorePassword != null) {
            ConnectionSocketFactory plainsf = PlainConnectionSocketFactory.getSocketFactory();
            SSLContext sslContext = null;
            if (trustSelfSignCert) {
                // Self signed cert. support
                try {
                    sslContext = SSLContexts.custom()
                            .loadTrustMaterial(TrustSelfSignedStrategy.INSTANCE)
                            .loadTrustMaterial(new File(keyStoreLocation), keyStorePassword)
                            .build();
                } catch (Exception e) {
                    log.error("SSLContexts initiation has failed, " + e.getMessage());
                }
            } else {
                try {
                    sslContext = SSLContexts.custom()
                            .loadTrustMaterial(new File(this.keyStoreLocation), keyStorePassword)
                            .build();
                } catch (Exception e) {
                    log.error("SSLContexts initiation has failed, " + e.getMessage());
                }
            }

            // we allow to disable host name verification against CA certificate,
            // notice: in general this is insecure and should be avoided in production,
            // (this type of configuration is useful for development purposes)
            LayeredConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
                    sslContext,
                    noHostVerification ? NoopHostnameVerifier.INSTANCE : new DefaultHostnameVerifier()
                    // todo: michaelp implement PublicSuffixMatcherLoader support (if needed)
            );

            Registry<ConnectionSocketFactory> r = RegistryBuilder.<ConnectionSocketFactory>create()
                    .register("http", plainsf)
                    .register("https", sslsf)
                    .build();
            connectionMgr = new PoolingHttpClientConnectionManager(r);
            // todo: support INACTIVITY_TIMEOUT & TimeUnit.MILLISECONDS
        } else {
            connectionMgr = new PoolingHttpClientConnectionManager(INACTIVITY_TIMEOUT, TimeUnit.MILLISECONDS);
        }

        connectionMgr.setMaxTotal(ConstantValues.httpClientMaxTotalConnections.getInt());
        connectionMgr.setDefaultMaxPerRoute(MAX_CONNECTIONS_PER_HOST);
        HttpHost localhost = new HttpHost(LOCALHOST, DEFAULT_PORT);
        connectionMgr.setMaxPerRoute(new HttpRoute(localhost), ConstantValues.httpClientMaxConnectionsPerRoute.getInt());
        return connectionMgr;
    }

    /**
     * Produces a {@link ConnectionKeepAliveStrategy}
     *
     * @return keep-alive strategy to be used for connection pool
     */
    private ConnectionKeepAliveStrategy getConnectionKeepAliveStrategy() {
        return new ConnectionKeepAliveStrategy() {
            @Override
            public long getKeepAliveDuration(HttpResponse response, HttpContext context) {
                // Honor 'keep-alive' header
                HeaderElementIterator it = new BasicHeaderElementIterator(
                        response.headerIterator(HTTP.CONN_KEEP_ALIVE));
                while (it.hasNext()) {
                    HeaderElement he = it.nextElement();
                    String param = he.getName();
                    String value = he.getValue();
                    if (value != null && param.equalsIgnoreCase("timeout")) {
                        try {
                            return Long.parseLong(value) * 1000;
                        } catch (NumberFormatException ignore) {
                        }
                    }
                }
                return 30 * 1000;
            }
        };
    }

    /**
     * Disable the automatic gzip compression on read.
     * Once disabled cannot be activated.
     */
    public HttpClientConfigurator handleGzipResponse(boolean handleGzipResponse) {
        if (!handleGzipResponse) {
            builder.disableContentCompression();
        }
        return this;
    }

    /**
     * May throw a runtime exception when the given URL is invalid.
     */
    public HttpClientConfigurator hostFromUrl(String urlStr) {
        if (StringUtils.isNotBlank(urlStr)) {
            try {
                URL url = new URL(urlStr);
                host(url.getHost());
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException("Cannot parse the url " + urlStr, e);
            }
        }
        return this;
    }

    /**
     * Ignores blank values
     */
    public HttpClientConfigurator host(String host) {
        if (StringUtils.isNotBlank(host)) {
            this.host = host;
            builder.setRoutePlanner(new DefaultHostRoutePlanner(host));
        }
        return this;
    }

    public HttpClientConfigurator defaultMaxConnectionsPerHost(int maxConnectionsPerHost) {
        builder.setMaxConnPerRoute(maxConnectionsPerHost);
        return this;
    }

    public HttpClientConfigurator maxTotalConnections(int maxTotalConnections) {
        builder.setMaxConnTotal(maxTotalConnections);
        return this;
    }

    public HttpClientConfigurator connectionTimeout(int connectionTimeout) {
        config.setConnectTimeout(connectionTimeout);
        return this;
    }

    public HttpClientConfigurator soTimeout(int soTimeout) {
        config.setSocketTimeout(soTimeout);
        return this;
    }

    /**
     * see {@link org.apache.http.client.config.RequestConfig#isStaleConnectionCheckEnabled()}
     */
    public HttpClientConfigurator staleCheckingEnabled(boolean staleCheckingEnabled) {
        config.setStaleConnectionCheckEnabled(staleCheckingEnabled);
        return this;
    }

    /**
     * Disable request retries on service unavailability.
     */
    public HttpClientConfigurator noRetry() {
        return retry(0, false);
    }

    /**
     * Number of retry attempts. Default is 3 retries.
     *
     * @param retryCount Number of retry attempts. 0 means no retries.
     */
    public HttpClientConfigurator retry(int retryCount, boolean requestSentRetryEnabled) {
        if (retryCount == 0) {
            builder.disableAutomaticRetries();
        } else {
            builder.setRetryHandler(new DefaultHttpRequestRetryHandler(retryCount, requestSentRetryEnabled));
        }
        return this;
    }

    /**
     * Ignores blank or invalid input
     */
    public HttpClientConfigurator localAddress(String localAddress) {
        if (StringUtils.isNotBlank(localAddress)) {
            try {
                InetAddress address = InetAddress.getByName(localAddress);
                config.setLocalAddress(address);
            } catch (UnknownHostException e) {
                throw new IllegalArgumentException("Invalid local address: " + localAddress, e);
            }
        }
        return this;
    }

    /**
     * Ignores null credentials
     */
    public HttpClientConfigurator authentication(UsernamePasswordCredentials creds) {
        if (creds != null) {
            authentication(creds.getUserName(), creds.getPassword());
        }

        return this;
    }

    /**
     * Configures preemptive authentication on this client. Ignores blank username input.
     */
    public HttpClientConfigurator authentication(String username, String password) {
        return authentication(username, password, false);
    }

    /**
     * Configures preemptive authentication on this client. Ignores blank username input.
     */
    public HttpClientConfigurator authentication(String username, String password, boolean allowAnyHost) {
        if (StringUtils.isNotBlank(username)) {
            if (StringUtils.isBlank(host)) {
                throw new IllegalStateException("Cannot configure authentication when host is not set.");
            }
            this.allowAnyHostAuth = allowAnyHost;
            AuthScope authscope = allowAnyHost ?
                    new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT, AuthScope.ANY_REALM) :
                    new AuthScope(host, AuthScope.ANY_PORT, AuthScope.ANY_REALM);
            credsProvider.setCredentials(authscope, new UsernamePasswordCredentials(username, password));
        }
        return this;
    }

    /**
     * Enable cookie management for this client.
     */
    public HttpClientConfigurator enableCookieManagement(boolean enableCookieManagement) {
        if (enableCookieManagement) {
            config.setCookieSpec(CookieSpecs.BROWSER_COMPATIBILITY);
        } else {
            config.setCookieSpec(null);
        }
        explicitCookieSupport = enableCookieManagement;
        return this;
    }

    public HttpClientConfigurator setConnectionManagerShared(boolean connectionManagerShared) {
        builder.setConnectionManagerShared(true);
        return this;
    }

    public HttpClientConfigurator enableTokenAuthentication(boolean enableTokenAuthentication, String repoKey) {
        if (enableTokenAuthentication) {
            if (StringUtils.isBlank(host)) {
                throw new IllegalStateException("Cannot configure authentication when host is not set.");
            }
            this.repoKey = repoKey;
            config.setTargetPreferredAuthSchemes(Collections.singletonList("Bearer"));
            // The repository key is passed to the Bearer to reuse it's http client
            Registry<AuthSchemeProvider> bearerRegistry = RegistryBuilder.<AuthSchemeProvider>create()
                    .register("Bearer", new BearerSchemeFactory(repoKey))
                    .build();
            builder.setDefaultAuthSchemeRegistry(bearerRegistry);
            chosenAuthScheme = AuthScheme.BEARER;
        }
        return this;
    }

    public HttpClientConfigurator proxy(@Nullable ProxyDescriptor proxyDescriptor) {
        configureProxy(proxyDescriptor);
        return this;
    }

    /**
     * @param keyStoreLocation ssl keystore location
     * @return {@link HttpClientConfigurator}
     */
    public HttpClientConfigurator keyStoreLocation(String keyStoreLocation) {
        this.keyStoreLocation = keyStoreLocation;
        return this;
    }

    /**
     * @param keyStorePassword ssl keystore password
     * @return {@link HttpClientConfigurator}
     */
    public HttpClientConfigurator keyStorePassword(char[] keyStorePassword) {
        this.keyStorePassword = keyStorePassword;
        return this;
    }

    /**
     * Set SPNEGO scheme for kerberos auth.
     *
     * @param useKerberos to activate kerberos executions
     * @return {@link HttpClientConfigurator}
     */
    public HttpClientConfigurator useKerberos(boolean useKerberos) {
        if (useKerberos) {
            Credentials use_jaas_creds = new Credentials() {
                public String getPassword() {
                    return null;
                }

                public Principal getUserPrincipal() {
                    return null;
                }
            };
            CredentialsProvider credsProvider = new BasicCredentialsProvider();
            credsProvider.setCredentials(new AuthScope(null, -1, null), use_jaas_creds);
            Registry<AuthSchemeProvider> spnegoScheme = RegistryBuilder.<AuthSchemeProvider>create()
                    .register(AuthSchemes.SPNEGO, new SPNegoSchemeFactory(true))
                    .build();
            builder.setDefaultAuthSchemeRegistry(spnegoScheme).setDefaultCredentialsProvider(credsProvider);
            chosenAuthScheme = AuthScheme.SPNEGO;
        }
        return this;
    }

    /**
     * @param trustSelfSignCert Trust self signed certificates on SSL handshake
     * @return {@link HttpClientConfigurator}
     */
    public HttpClientConfigurator trustSelfSignCert(boolean trustSelfSignCert) {
        this.trustSelfSignCert = trustSelfSignCert;
        return this;
    }

    /**
     * @param noHostVerification whether host name verification against CA certificate is disabled on SSL handshake
     * @return {@link HttpClientConfigurator}
     */
    public HttpClientConfigurator noHostVerification(boolean noHostVerification) {
        this.noHostVerification = noHostVerification;
        return this;
    }

    private void configureProxy(ProxyDescriptor proxy) {
        if (proxy != null) {
            config.setProxy(new HttpHost(proxy.getHost(), proxy.getPort()));
            this.proxyHost = proxy.getHost();
            if (StringUtils.isNotBlank(proxy.getUsername())) {
                Credentials creds = null;
                if (proxy.getDomain() == null) {
                    creds = new UsernamePasswordCredentials(proxy.getUsername(),
                            CryptoHelper.decryptIfNeeded(proxy.getPassword()));
                    //This will demote the NTLM authentication scheme so that the proxy won't barf
                    //when we try to give it traditional credentials. If the proxy doesn't do NTLM
                    //then this won't hurt it (jcej at tragus dot org)
                    List<String> authPrefs = Arrays.asList(AuthSchemes.DIGEST, AuthSchemes.BASIC, AuthSchemes.NTLM);
                    config.setProxyPreferredAuthSchemes(authPrefs);
                    // preemptive proxy authentication
                    builder.addInterceptorFirst(new ProxyPreemptiveAuthInterceptor());
                } else {
                    try {
                        String ntHost =
                                StringUtils.isBlank(proxy.getNtHost()) ? InetAddress.getLocalHost().getHostName() :
                                        proxy.getNtHost();
                        creds = new NTCredentials(proxy.getUsername(),
                                CryptoHelper.decryptIfNeeded(proxy.getPassword()), ntHost, proxy.getDomain());
                    } catch (UnknownHostException e) {
                        log.error("Failed to determine required local hostname for NTLM credentials.", e);
                    }
                }
                if (creds != null) {
                    credsProvider.setCredentials(
                            new AuthScope(proxy.getHost(), proxy.getPort(), AuthScope.ANY_REALM), creds);
                    if (proxy.getRedirectedToHostsList() != null) {
                        for (String hostName : proxy.getRedirectedToHostsList()) {
                            credsProvider.setCredentials(
                                    new AuthScope(hostName, AuthScope.ANY_PORT, AuthScope.ANY_REALM), creds);
                        }
                    }
                }
            }
        }
    }

    private boolean hasCredentials() {
        return credsProvider.getCredentials(AuthScope.ANY) != null;
    }

    static class DefaultHostRoutePlanner extends DefaultRoutePlanner {

        private final HttpHost defaultHost;

        public DefaultHostRoutePlanner(String defaultHost) {
            super(DefaultSchemePortResolver.INSTANCE);
            this.defaultHost = new HttpHost(defaultHost);
        }

        @Override
        public HttpRoute determineRoute(HttpHost host, HttpRequest request, HttpContext context) throws HttpException {
            if (host == null) {
                host = defaultHost;
            }
            return super.determineRoute(host, request, context);
        }

        public HttpHost getDefaultHost() {
            return defaultHost;
        }
    }

    /**
     * Sets required configuration based on the final chosen config for this client so that configurations
     * don't interfere with each other based on when they were created in the chain (i.e. auth before token etc.)
     */
    private void additionalConfigByAuthScheme() {
        switch (chosenAuthScheme) {
            case BASIC:
                builder.addInterceptorFirst(new PreemptiveAuthInterceptor());
                break;
            case BEARER:
                if (shouldConfigureBearerDummyCredentials()) {
                    // We need dummy credentials hack to enforce httpClient behavior, otherwise we won't respond to a
                    //challenge properly... Dummy:dummy is the specification for forcing token authentication
                    credsProvider.setCredentials(new AuthScope(host, AuthScope.ANY_PORT, AuthScope.ANY_REALM),
                            new UsernamePasswordCredentials("dummy", "dummy"));
                } else {
                    //Valid credentials exist for target host - set basic auth preference and register additional scheme
                    //so we can respond to basic challenges from target as required
                    List<String> authPrefs = Arrays.asList("Bearer", AuthSchemes.BASIC);
                    config.setTargetPreferredAuthSchemes(authPrefs);
                    Registry<AuthSchemeProvider> bearerRegistry = RegistryBuilder.<AuthSchemeProvider>create()
                            .register("Bearer", new BearerSchemeFactory(repoKey))
                            .register(AuthSchemes.BASIC, new BasicSchemeFactory())
                            .build();
                    builder.setDefaultAuthSchemeRegistry(bearerRegistry);
                }
                break;
        }
    }

    /**
     * @return false if credentials were configured for this client's host (or any host if lenient) -> also
     * verifies the credential set is not the one configured for the proxy (proxy credentials are not considered
     * host credentials), true if dummy credentials should be configured for Bearer auth
     */
    private boolean shouldConfigureBearerDummyCredentials() {
        boolean shouldSetDummy = false;
        Credentials hostCreds = credsProvider.getCredentials(
                new AuthScope(host, AuthScope.ANY_PORT, AuthScope.ANY_REALM));
        Credentials anyHostCreds = credsProvider.getCredentials(
                new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT, AuthScope.ANY_REALM));
        Credentials proxyCreds = null;
        if (StringUtils.isNotBlank(proxyHost)) {
            proxyCreds = credsProvider.getCredentials(new AuthScope(proxyHost, AuthScope.ANY_PORT, AuthScope.ANY_REALM));
        }

        //Any host allowed - make sure credentials were set and that the credsProvider didn't just return
        //the proxy's credentials for the ANY_HOST scope.
        if (allowAnyHostAuth && anyHostCreds != null
                && (proxyCreds == null || (!proxyCreds.getUserPrincipal().equals(anyHostCreds.getUserPrincipal())))) {
            shouldSetDummy = true;
        } else if (hostCreds == null) {
            shouldSetDummy = true;
        }
        return shouldSetDummy;
    }

    private enum AuthScheme {
        BASIC, SPNEGO, BEARER
    }
}
