package com.liftsync.config.cache;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;


@Configuration
@EnableCaching
@EnableConfigurationProperties(CacheTtlProperties.class)
@ConditionalOnProperty(name = "app.cache.enabled", havingValue = "true")
public class CacheConfig {

    private final CacheTtlProperties cacheTtlProperties;
    private final ObjectMapper cacheObjectMapper;
    private final String keyVersion;

    public CacheConfig(CacheTtlProperties cacheTtlProperties,
                       @Value("${app.cache.key-version:v1}") String keyVersion) {
        this.cacheTtlProperties = cacheTtlProperties;
        this.keyVersion = keyVersion;


        this.cacheObjectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(cacheConfiguration())
                .withInitialCacheConfigurations(buildPerCacheConfigurations())
                .build();
    }

    @Bean
    public RedisCacheConfiguration cacheConfiguration() {
        // Base configuration for unregistered caches - uses default TTL and generic Object serializer
        return baseConfiguration(cacheTtlProperties.getDefaultTtl(),
                new Jackson2JsonRedisSerializer<>(cacheObjectMapper, Object.class));
    }

    private Map<String, RedisCacheConfiguration> buildPerCacheConfigurations() {
        Map<String, RedisCacheConfiguration> configs = new HashMap<>();

        for (CacheSpec<?> spec : Caches.all()) {
            Jackson2JsonRedisSerializer<?> serializer =
                    new Jackson2JsonRedisSerializer<>(cacheObjectMapper, spec.javaType());
            configs.put(spec.name(), baseConfiguration(resolveTtl(spec.name()), serializer));
        }

        return configs;
    }


    private Duration resolveTtl(String cacheName) {
        Duration ttl = cacheTtlProperties.getEntries().get(cacheName);
        return ttl != null ? ttl : cacheTtlProperties.getDefaultTtl();
    }

    private RedisCacheConfiguration baseConfiguration(Duration ttl,
                                                      Jackson2JsonRedisSerializer<?> valueSerializer) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues()
                .entryTtl(ttl)
                .computePrefixWith(cacheName -> "liftsync:" + keyVersion + ":" + cacheName + "::")
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(valueSerializer));
    }
}
