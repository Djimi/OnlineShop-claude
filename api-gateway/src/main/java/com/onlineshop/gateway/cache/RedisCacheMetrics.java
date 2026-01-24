package com.onlineshop.gateway.cache;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.cache.CacheMeterBinder;
import org.springframework.data.redis.cache.CacheStatistics;
import org.springframework.data.redis.cache.RedisCache;

/**
 * Micrometer cache metrics binder backed by Redis cache statistics.
 */
public class RedisCacheMetrics extends CacheMeterBinder<RedisCache> {

    public RedisCacheMetrics(RedisCache cache, String name, Iterable<Tag> tags) {
        super(cache, name, tags);
    }

    @Override
    protected Long size() {
        return null;
    }

    @Override
    protected long hitCount() {
        return stats().getHits();
    }

    @Override
    protected Long missCount() {
        return stats().getMisses();
    }

    @Override
    protected Long evictionCount() {
        return stats().getDeletes();
    }

    @Override
    protected long putCount() {
        return stats().getPuts();
    }

    @Override
    protected void bindImplementationSpecificMetrics(io.micrometer.core.instrument.MeterRegistry registry) {
        // No Redis-specific meters beyond standard cache metrics.
    }

    private CacheStatistics stats() {
        return getCache().getStatistics();
    }
}
