package com.company.ambassador.infrastructure.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.CacheManager
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer
import com.github.benmanes.caffeine.cache.Caffeine
import java.time.Duration
import java.util.concurrent.TimeUnit

@Configuration
class CacheConfig {

    @Value("\${cache.user.ttl:300}")
    private val userCacheTtl: Long = 300

    @Value("\${cache.user.max-size:1000}")
    private val userCacheMaxSize: Long = 1000

    @Value("\${cache.product.ttl:600}")
    private val productCacheTtl: Long = 600

    @Value("\${cache.product.max-size:500}")
    private val productCacheMaxSize: Long = 500

    @Bean
    @Primary
    fun redisCacheManager(redisConnectionFactory: RedisConnectionFactory): CacheManager {
        val userCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofSeconds(userCacheTtl))
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(StringRedisSerializer())
            )
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(GenericJackson2JsonRedisSerializer())
            )
            .disableCachingNullValues()

        val productCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofSeconds(productCacheTtl))
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(StringRedisSerializer())
            )
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(GenericJackson2JsonRedisSerializer())
            )
            .disableCachingNullValues()

        return RedisCacheManager.builder(redisConnectionFactory)
            .cacheDefaults(RedisCacheConfiguration.defaultCacheConfig())
            .withCacheConfiguration("users", userCacheConfig)
            .withCacheConfiguration("products", productCacheConfig)
            .withCacheConfiguration("user-search", userCacheConfig)
            .withCacheConfiguration("product-category", productCacheConfig)
            .build()
    }

    @Bean("localCacheManager")
    fun caffeineCacheManager(): CacheManager {
        val cacheManager = CaffeineCacheManager()

        cacheManager.setCaffeine(
            Caffeine.newBuilder()
                .maximumSize(userCacheMaxSize)
                .expireAfterWrite(userCacheTtl, TimeUnit.SECONDS)
                .recordStats()
        )

        cacheManager.setCacheNames(setOf("users-local", "products-local"))
        return cacheManager
    }
}