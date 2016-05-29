package org.artifactory.security.props.auth;

import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.ha.HaCommonAddon;
import org.artifactory.security.props.auth.CacheWrapper.CacheConfig;
import org.artifactory.security.props.auth.model.TokenKeyValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * <p>Created on 04/05/16
 *
 * @author Yinon Avraham
 */
@Component
public class PropsTokenCacheImpl implements PropsTokenCache {

    private static final String PROPS_TOKEN_CACHE_NAME = PropsTokenCacheImpl.class.getName();

    @Autowired
    private AddonsManager addonsManager;

    private CacheWrapper<TokenKeyValue, UserDetails> cache;
    private final ReentrantLock cacheLock = new ReentrantLock();

    private void init() {
        if (cache == null) {
            cacheLock.lock();
            if (cache == null) {
                try {
                    initCache();
                } finally {
                    cacheLock.unlock();
                }
            }
        }
    }

    private void initCache() {
        CacheConfig cacheConfig = CacheConfig.newConfig()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .build();
        HaCommonAddon haAddon = addonsManager.addonByType(HaCommonAddon.class);
        this.cache = haAddon.getCache(PROPS_TOKEN_CACHE_NAME, cacheConfig);
    }

    @Override
    public void put(TokenKeyValue tokenKeyValue, UserDetails principal) {
        init();
        cache.put(tokenKeyValue, principal);
    }

    @Override
    public UserDetails get(TokenKeyValue tokenKeyValue) {
        init();
        return cache.get(tokenKeyValue);
    }
}
