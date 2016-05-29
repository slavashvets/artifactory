package org.artifactory.repo.distribution.auth;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.apache.commons.lang.StringUtils;
import org.artifactory.api.bintray.distribution.DistributionService;
import org.artifactory.common.ConstantValues;
import org.artifactory.util.bearer.TokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Bintray specific implementation of a {@link TokenProvider}.
 * The tokens are cached in-memory and auto expire according to the
 * {@code ConstantValues.bintrayOAuthTokenExpirySeconds} system property
 *
 * @author Dan Feldman
 */
@Service
public class BintrayTokenProvider implements TokenProvider {
    private static final Logger log = LoggerFactory.getLogger(BintrayTokenProvider.class);

    @Autowired
    private DistributionService distService;

    private LoadingCache<String, String> tokens;

    @PostConstruct
    public void initTokensCache() {
        tokens = CacheBuilder.newBuilder()
                .initialCapacity(100)
                .expireAfterWrite(ConstantValues.bintrayOAuthTokenExpirySeconds.getLong(), TimeUnit.SECONDS)
                .build(new CacheLoader<String, String>() {
                    @Override
                    public String load(@Nonnull String repoKey) throws Exception {
                        String token = fetchNewToken(repoKey);
                        if (StringUtils.isBlank(token)) {
                            throw new Exception("Can't fetch Bintray OAuth token for repo: " + repoKey);
                        }
                        return token;
                    }
                });
    }

    @Override
    public String getToken(Map<String, String> challengeParams, String method, String uri, String repoKey) {
        try {
            log.trace("Getting Bintray OAuth token for {}, that has expiry of {}", repoKey,
                    ConstantValues.bintrayOAuthTokenExpirySeconds.getLong());
            return tokens.get(repoKey);
        } catch (ExecutionException e) {
            throw new RuntimeException("Could not get Bintray OAuth token from cache for " + repoKey, e);
        }
    }

    private String fetchNewToken(String repoKey) throws Exception {
        log.trace("Fetching new OAuth token from Bintray for repo '{}'", repoKey);
        //TODO [by dan]: handle errors?
        return distService.refreshBintrayOAuthAppToken(repoKey);
    }
}
