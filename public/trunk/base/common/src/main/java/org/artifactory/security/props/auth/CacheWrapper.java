package org.artifactory.security.props.auth;

import java.util.concurrent.TimeUnit;

/**
 * <p>Created on 04/05/16
 *
 * @author Yinon Avraham
 */
public interface CacheWrapper<K, V> {

    void put(K key, V value);

    V get(K key);

    class CacheConfig {
        private final Long duration;
        private final TimeUnit timeUnit;

        private CacheConfig(Long duration, TimeUnit timeUnit) {
            this.duration = duration;
            this.timeUnit = timeUnit;
        }

        static CacheConfigBuilder newConfig() {
            return new CacheConfigBuilder();
        }

        public long getExpirationDuration() {
            assert hasExpiration();
            return duration;
        }

        public TimeUnit getExpirationTimeUnit() {
            assert hasExpiration();
            return timeUnit;
        }

        public boolean hasExpiration() {
            return duration != null && timeUnit != null;
        }
    }

    class CacheConfigBuilder {
        private Long duration = null;
        private TimeUnit timeUnit = null;

        public CacheConfigBuilder expireAfterWrite(long duration, TimeUnit timeUnit) {
            this.duration = duration;
            this.timeUnit = timeUnit;
            return this;
        }

        public CacheConfigBuilder noExpiration() {
            this.duration = null;
            this.timeUnit = null;
            return this;
        }

        public CacheConfig build() {
            return new CacheConfig(duration, timeUnit);
        }
    }

}
