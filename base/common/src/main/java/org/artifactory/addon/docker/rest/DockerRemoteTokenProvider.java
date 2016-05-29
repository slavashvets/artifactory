package org.artifactory.addon.docker.rest;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.apache.commons.lang.StringUtils;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.docker.DockerAddon;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.common.ConstantValues;
import org.artifactory.util.bearer.TokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Docker specific implementation of a {@link TokenProvider}.
 * The tokens are cached in-memory and auto expire according to the
 * {@code artifactory.docker.tokens.cache.idleTimeSecs} system property
 *
 * @author Shay Yaakov
 */
@Service
public class DockerRemoteTokenProvider implements TokenProvider {
    private static final Logger log = LoggerFactory.getLogger(DockerRemoteTokenProvider.class);

    private LoadingCache<DockerTokenCacheKey, String> tokens;

    @PostConstruct
    public void initTokensCache() {
        tokens = CacheBuilder.newBuilder()
                .initialCapacity(1000)
                .expireAfterWrite(ConstantValues.dockerTokensCacheIdleTimeSecs.getLong(), TimeUnit.SECONDS)
                .build(new CacheLoader<DockerTokenCacheKey, String>() {
                    @Override
                    public String load(@Nonnull DockerTokenCacheKey key) throws Exception {
                        String token = fetchNewToken(key);
                        if (StringUtils.isBlank(token)) {
                            throw new Exception("Can't fetch token for repo: " + key.getRepoKey() + " realm: "
                                    + key.getRealm() + " scope:" + key.getScope());
                        }
                        return token;
                    }
                });
    }

    @Override
    public String getToken(Map<String, String> challengeParams, String method, String uri, String repoKey) {
        try {
            populateParamsIfNeeded(challengeParams, method, uri);
            log.trace("Getting token for " + challengeParams);
            return tokens.get(new DockerTokenCacheKey(challengeParams, repoKey));
        } catch (ExecutionException e) {
            throw new RuntimeException("Could not get token from cache for " + challengeParams, e);
        }
    }

    private void populateParamsIfNeeded(Map<String, String> challengeParams, String method, String uri) {
        if (!challengeParams.containsKey("service")) {
            String realm = challengeParams.get("realm");
            if (StringUtils.isNotBlank(realm) && realm.startsWith("http")) {
                try {
                    challengeParams.put("service", new URL(realm).getHost());
                } catch (MalformedURLException e) {
                    log.error("Realm of token cannot be parsed due to: " + e.getMessage(), e);
                }
            }
        }
        if (!challengeParams.containsKey("scope")) {
            if (uri.startsWith("/v2/")) {
                String newScope = null;
                StringBuilder scopeBuilder = new StringBuilder("repository:");
                String[] split = uri.split("/"); // The first / ends up with empty string at pos 0
                if (split.length < 3) {
                    // Simple ping => empty scope
                } else {
                    scopeBuilder.append(split[2]);
                }
                if (split.length > 3) {
                    scopeBuilder.append("/");
                    scopeBuilder.append(split[3]);
                }
                scopeBuilder.append(":");
                if ("GET".equalsIgnoreCase(method)) {
                    scopeBuilder.append("pull");
                    newScope = scopeBuilder.toString();
                } else if ("PUT".equalsIgnoreCase(method)) {
                    scopeBuilder.append("pull");
                    newScope = scopeBuilder.toString();
                } else {
                    log.warn("Docker Bearer challenge has no scope and method is not GET or PUT '" + method + "'");
                }
                if (newScope != null) {
                    log.debug("Docker Bearer challenge new scope set to '" + newScope + "'");
                    challengeParams.put("scope", newScope);
                }
            } else {
                log.warn("Docker Bearer challenge has no scope and URI does not starts with /v2 but '" + uri + "'");
            }
        }
    }

    private String fetchNewToken(DockerTokenCacheKey tokenCacheKey) {
        log.trace("Fetching new token for '{}'", tokenCacheKey);
        return ContextHelper.get().beanForType(AddonsManager.class).addonByType(DockerAddon.class)
                .fetchDockerAuthToken(tokenCacheKey);
    }
}
