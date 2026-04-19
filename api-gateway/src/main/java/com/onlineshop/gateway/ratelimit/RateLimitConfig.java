package com.onlineshop.gateway.ratelimit;

import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

@Configuration
@ConditionalOnProperty(name = "gateway.ratelimit.enabled", havingValue = "true", matchIfMissing = true)
public class RateLimitConfig {

    @Bean
    public ProxyManager<String> bucket4jProxyManager(RedisConnectionFactory redisConnectionFactory) {
        LettuceConnectionFactory lettuceFactory = (LettuceConnectionFactory) redisConnectionFactory;

        RedisClient redisClient = RedisClient.create(
                String.format("redis://%s:%d",
                        lettuceFactory.getHostName(),
                        lettuceFactory.getPort())
        );

        StatefulRedisConnection<String, byte[]> connection = redisClient.connect(
                io.lettuce.core.codec.RedisCodec.of(
                        io.lettuce.core.codec.StringCodec.UTF8,
                        io.lettuce.core.codec.ByteArrayCodec.INSTANCE
                )
        );

        return LettuceBasedProxyManager.builderFor(connection)
                .build();
    }
}
