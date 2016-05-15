package org.artifactory.security.props.auth;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * <p>Created on 04/05/16
 *
 * @author Yinon Avraham
 */
public class SimpleCacheWrapper<K, V> implements CacheWrapper<K, V> {

    private final Cache<K, V> cache;

    public SimpleCacheWrapper(CacheConfig cacheConfig) {
        cache = buildCache(cacheConfig);
    }

    private static <K, V> Cache<K, V> buildCache(CacheConfig cacheConfig) {
        CacheBuilder<Object, Object> builder = CacheBuilder.<K, V>newBuilder();
        if (cacheConfig.hasExpiration()) {
            builder.expireAfterWrite(cacheConfig.getExpirationDuration(), cacheConfig.getExpirationTimeUnit());
        }
        return builder.build();
    }

    @Override
    public void put(K key, V value) {
        cache.put(key, value);
    }

    @Override
    public V get(K key) {
        return cache.getIfPresent(key);
    }
}
