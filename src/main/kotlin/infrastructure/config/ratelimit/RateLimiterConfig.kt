package com.company.ambassador.infrastructure.config.ratelimit

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import io.github.bucket4j.Refill
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

@Configuration
class RateLimiterConfig(
    private val externalServicesProperties: ExternalServicesProperties
) {

    private val buckets = ConcurrentHashMap<String, Bucket>()

    @Bean
    fun userServiceBucket(): Bucket {
        return createBucket("user-service", externalServicesProperties.userService.rateLimit)
    }

    @Bean
    fun productServiceBucket(): Bucket {
        return createBucket("product-service", externalServicesProperties.productService.rateLimit)
    }

    private fun createBucket(serviceName: String, config: ExternalServicesProperties.RateLimitConfig): Bucket {
        return buckets.computeIfAbsent(serviceName) {
            val refillPeriod = parseDuration(config.refillPeriod)
            val bandwidth = Bandwidth.classic(config.capacity, Refill.intervally(config.tokens, refillPeriod))
            Bucket.builder()
                .addLimit(bandwidth)
                .build()
        }
    }

    private fun parseDuration(duration: String): Duration {
        return when {
            duration.endsWith("s") -> Duration.ofSeconds(duration.dropLast(1).toLong())
            duration.endsWith("m") -> Duration.ofMinutes(duration.dropLast(1).toLong())
            duration.endsWith("h") -> Duration.ofHours(duration.dropLast(1).toLong())
            else -> Duration.ofSeconds(1)
        }
    }

    fun getBucket(serviceName: String): Bucket? {
        return buckets[serviceName]
    }
} 