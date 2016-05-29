package org.artifactory.util.bearer;

import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.auth.AuthState;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Interceptor that preemptively acquires the Bintray OAuth token and attaches it to the request
 * Needed because Bintray's default www-authenticate response is Basic and therefore the client can't
 * authenticate with Bearer auth automatically.
 *
 * @author Dan Feldman
 */
public class BintrayBearerPreemptiveAuthInterceptor implements HttpRequestInterceptor {
    private static final Logger log = LoggerFactory.getLogger(BintrayBearerPreemptiveAuthInterceptor.class);

    private String repoKey;

    public BintrayBearerPreemptiveAuthInterceptor(String repoKey) {
        this.repoKey = repoKey;
    }

    @Override
    public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {
        HttpClientContext clientContext = HttpClientContext.adapt(context);
        AuthState authState = clientContext.getTargetAuthState();

        // If there's no auth scheme available yet, try to initialize it preemptively
        if (authState.getAuthScheme() == null) {
            HttpHost targetHost = clientContext.getTargetHost();
            log.debug("Updating bearer credentials for Bintray host " + targetHost);
            //Bintray token provider only needs repo key to work so dummy is ok here
            authState.update(new BearerScheme(repoKey), new UsernamePasswordCredentials("dummy", "dummy"));
        }
    }
}
