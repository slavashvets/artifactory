package org.artifactory.security.providermgr;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.artifactory.common.ConstantValues;
import org.artifactory.security.props.auth.model.OauthDockerErrorModel;
import org.artifactory.security.props.auth.model.OauthErrorModel;
import org.artifactory.security.props.auth.model.OauthModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * artifactory specific token cache implementation ,
 * The tokens are cached in-memory and auto expire according to the
 *
 * @author Chen Keinan
 */
@Component
public class ArtifactoryTokenProvider {
    private static final Logger log = LoggerFactory.getLogger(ArtifactoryTokenProvider.class);

    private LoadingCache<ArtifactoryCacheKey, OauthModel> tokens;

    @PostConstruct
    public void initTokensCache() {
        tokens = CacheBuilder.newBuilder()
                .initialCapacity(1000)
                .expireAfterWrite(ConstantValues.genericTokensCacheIdleTimeSecs.getLong(), TimeUnit.SECONDS)
                .build(new CacheLoader<ArtifactoryCacheKey, OauthModel>() {
                    @Override
                    public OauthModel load(@Nonnull ArtifactoryCacheKey key) throws Exception {
                        return fetchNewToken(key);
                    }
                });
    }

    public OauthModel getToken(ArtifactoryCacheKey artifactoryCacheKey) {
        String userName = artifactoryCacheKey.getUser();
        OauthModel oauthModel = null;
        try {
            log.trace("Getting token for " + userName);
            oauthModel = tokens.get(artifactoryCacheKey);
            return oauthModel;
        } catch (ExecutionException e) {
            throw new RuntimeException("Could not get token from cache for " + userName, e);
        } finally {
            if (oauthModel != null) {
                if (oauthModel instanceof OauthErrorModel || oauthModel instanceof OauthDockerErrorModel) {
                    tokens.invalidate(artifactoryCacheKey);
                }
            }
        }
    }

    /**
     * Used when expiring user credentials or revoking api keys - will remove the user's tokens from the cache
     * to force re-authentication.
     * @param userName  user to invalidate cache entries for
     */
    public void invalidateUserCacheEntries(String userName) {
        List<ArtifactoryCacheKey> toInvalidate = tokens.asMap().keySet().stream()
                .filter(cacheKey -> cacheKey.getUser().equalsIgnoreCase(userName))
                .collect(Collectors.toList());
        tokens.invalidateAll(toInvalidate);
    }

    public void invalidateCacheEntriesForAllUsers() {
        tokens.invalidateAll();
    }

    private OauthModel fetchNewToken(ArtifactoryCacheKey artifactoryCacheKey) {
        return artifactoryCacheKey.getProviderMgr().fetchTokenFromProvider();
    }
}
