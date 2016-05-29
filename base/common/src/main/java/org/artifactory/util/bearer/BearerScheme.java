package org.artifactory.util.bearer;

import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.auth.AUTH;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.Credentials;
import org.apache.http.impl.auth.RFC2617Scheme;
import org.apache.http.message.BufferedHeader;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.CharArrayBuffer;
import org.artifactory.addon.docker.rest.DockerRemoteTokenProvider;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.descriptor.repo.RepoType;
import org.artifactory.repo.distribution.auth.BintrayTokenProvider;

import java.util.Optional;

/**
 * Bearer authentication scheme as defined in RFC 2617
 *
 * @author Shay Yaakov
 */
public class BearerScheme extends RFC2617Scheme {
    private String repoKey;
    private TokenProvider tokenProvider;

    public BearerScheme(String repoKey) {
        this.repoKey = repoKey;
        tokenProvider = getTokenProviderByRepoType(repoKey);
    }

    @Override
    public String getSchemeName() {
        return "bearer";
    }

    @Override
    public boolean isConnectionBased() {
        return false;
    }

    @Override
    public boolean isComplete() {
        return true;
    }

    @Override
    public Header authenticate(Credentials dummyCredentials, HttpRequest request) throws AuthenticationException {
        return authenticate(dummyCredentials, request, new BasicHttpContext());
    }

    @Override
    public Header authenticate(Credentials dummyCredentials, HttpRequest request, HttpContext context)
            throws AuthenticationException {
        String token = tokenProvider.getToken(getParameters(),
                request.getRequestLine().getMethod(),
                request.getRequestLine().getUri(),
                repoKey);
        final CharArrayBuffer buffer = new CharArrayBuffer(32);
        buffer.append(AUTH.WWW_AUTH_RESP);
        buffer.append(": Bearer ");
        buffer.append(token);
        return new BufferedHeader(buffer);
    }

    private TokenProvider getTokenProviderByRepoType(String repoKey) {
        TokenProvider provider;
        RepoType type = Optional.ofNullable(ContextHelper.get().beanForType(RepositoryService.class)
                .repoDescriptorByKey(repoKey))
                .orElseThrow(() -> new RuntimeException("No such repository " + repoKey))
                .getType();
        switch (type) {
            case Distribution:
                provider = ContextHelper.get().beanForType(BintrayTokenProvider.class);
                break;
            case Docker:
                provider = ContextHelper.get().beanForType(DockerRemoteTokenProvider.class);
                break;
            default:
                throw new IllegalArgumentException("Token Authentication is not available for repositories of type "
                        + type);
        }
        return provider;
    }
}
