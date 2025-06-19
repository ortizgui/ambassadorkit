package com.company.ambassador.config

import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import io.github.resilience4j.retry.RetryRegistry
import io.github.resilience4j.timelimiter.TimeLimiter
import io.github.resilience4j.timelimiter.TimeLimiterConfig
import io.github.resilience4j.timelimiter.TimeLimiterRegistry
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration
class Resilience4jConfig {

    @Bean
    fun circuitBreakerRegistry(): CircuitBreakerRegistry {
        val circuitBreakerConfig = CircuitBreakerConfig.custom()
            .slidingWindowSize(10)
            .minimumNumberOfCalls(5)
            .permittedNumberOfCallsInHalfOpenState(3)
            .automaticTransitionFromOpenToHalfOpenEnabled(true)
            .waitDurationInOpenState(Duration.ofSeconds(5))
            .failureRateThreshold(50f)
            .build()

        return CircuitBreakerRegistry.of(circuitBreakerConfig)
    }

    @Bean
    fun retryRegistry(): RetryRegistry {
        val retryConfig = RetryConfig.custom<Any>()
            .maxAttempts(3)
            .waitDuration(Duration.ofMillis(500))
            .build()

        return RetryRegistry.of(retryConfig)
    }

    @Bean
    fun timeLimiterRegistry(): TimeLimiterRegistry {
        val timeLimiterConfig = TimeLimiterConfig.custom()
            .timeoutDuration(Duration.ofSeconds(3))
            .cancelRunningFuture(true)
            .build()

        return TimeLimiterRegistry.of(timeLimiterConfig)
    }

    @Bean
    fun userServiceCircuitBreaker(circuitBreakerRegistry: CircuitBreakerRegistry): CircuitBreaker {
        return circuitBreakerRegistry.circuitBreaker("user-service")
    }

    @Bean
    fun productServiceCircuitBreaker(circuitBreakerRegistry: CircuitBreakerRegistry): CircuitBreaker {
        return circuitBreakerRegistry.circuitBreaker("product-service")
    }

    @Bean
    fun userServiceRetry(retryRegistry: RetryRegistry): Retry {
        return retryRegistry.retry("user-service")
    }

    @Bean
    fun productServiceRetry(retryRegistry: RetryRegistry): Retry {
        return retryRegistry.retry("product-service")
    }

    @Bean
    fun userServiceTimeLimiter(timeLimiterRegistry: TimeLimiterRegistry): TimeLimiter {
        return timeLimiterRegistry.timeLimiter("user-service")
    }

    @Bean
    fun productServiceTimeLimiter(timeLimiterRegistry: TimeLimiterRegistry): TimeLimiter {
        return timeLimiterRegistry.timeLimiter("product-service")
    }
}