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

package org.artifactory.repo.http;

import com.google.common.collect.Lists;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.observers.CloseableObserver;
import org.artifactory.repo.http.kerberos.KerberosAuthProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Provides decoration capabilities for HttpClient,
 * where {@link CloseableObserver} can register to
 * onClose() event
 *
 * @author Michael Pasternak
 */
public class CloseableHttpClientDecorator extends CloseableHttpClient {

    private static final Logger log = LoggerFactory.getLogger(CloseableHttpClientDecorator.class);

    private final CloseableHttpClient closeableHttpClient;
    private final List<CloseableObserver> closeableObservers;
    private final KerberosAuthProvider kerberosAuthProvider;

    /**
     * @param closeableHttpClient     {@link CloseableHttpClient}
     * @param clientConnectionManager {@link PoolingHttpClientConnectionManager}
     */
    public CloseableHttpClientDecorator(CloseableHttpClient closeableHttpClient,
            PoolingHttpClientConnectionManager clientConnectionManager, boolean useKerberos) {
        assert closeableHttpClient != null : "closeableHttpClient cannot be empty";
        assert clientConnectionManager != null : "clientConnectionManager cannot be empty";
        this.closeableObservers = Lists.newArrayList();
        this.closeableHttpClient = closeableHttpClient;
        IdleConnectionMonitorService idleConnectionMonitorService =
                ContextHelper.get().beanForType(IdleConnectionMonitorService.class);
        idleConnectionMonitorService.add(this, clientConnectionManager);
        registerCloseableObserver((CloseableObserver) idleConnectionMonitorService);
        kerberosAuthProvider = useKerberos ? new KerberosAuthProvider(closeableHttpClient) : null;
    }

    /**
     * Release resources and unregister itself from {@link IdleConnectionMonitorService}
     *
     * @throws IOException
     */
    @Override
    public void close() throws IOException {
        // notify listeners
        onClose();
        // release resources
        closeableHttpClient.close();
    }

    @Override
    protected CloseableHttpResponse doExecute(HttpHost target, HttpRequest request, HttpContext context)
            throws IOException {
        return closeableHttpClient.execute(target, request, context);
    }

    @Deprecated
    @Override
    public HttpParams getParams() {
        return closeableHttpClient.getParams();
    }

    @Deprecated
    @Override
    public ClientConnectionManager getConnectionManager() {
        return closeableHttpClient.getConnectionManager();
    }

    /**
     * @return {@link CloseableHttpClient}
     */
    public final CloseableHttpClient getDecorated() {
        return closeableHttpClient;
    }

    /**
     * Registers {@link CloseableObserver}
     *
     * @param closeableObserver
     */
    public final void registerCloseableObserver(CloseableObserver closeableObserver) {
        closeableObservers.add(closeableObserver);
    }

    /**
     * Fired on close() event
     */
    private void onClose() {
        closeableObservers.stream().forEach(o -> o.onObservedClose(this));
    }

    /**
     * Executes request using kerberos credentials authentication
     *
     * @param principal a user principal
     * @param password  a principal password
     * @param request   a request to execute
     *
     * @return {@link CloseableHttpResponse}
     */
    public CloseableHttpResponse executeKerberos(String principal, char[] password, HttpRequestBase request) throws IOException {
        log.trace("Executing kerberos password based request principal='{}', method='{}', url={}",
                principal, request.getMethod(), request.getURI());
        return Optional.ofNullable(kerberosAuthProvider)
                .orElseThrow(() -> new IOException("Kerberos authentication not supported by this client"))
                .executeKerberos(principal, password, request);
    }

    /**
     * Executes request using kerberos keytab authentication
     *
     * @param principal  a principal to be used
     * @param keyTabLocation a keytab location
     * @param request        a request to execute
     *
     * @return {@link CloseableHttpResponse}
     */
    public CloseableHttpResponse executeKerberos(String principal, String keyTabLocation, HttpRequestBase request) throws IOException {
        log.trace("Executing kerberos keytab based request principal='{}', method='{}', url={}",
                principal, request.getMethod(), request.getURI());
        return Optional.ofNullable(kerberosAuthProvider)
                .orElseThrow(() -> new IOException("Kerberos authentication not supported by this client"))
                .executeKerberos(principal, keyTabLocation, request);
    }
}