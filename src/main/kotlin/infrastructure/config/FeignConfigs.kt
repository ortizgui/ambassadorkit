package com.company.ambassador.infrastructure.client

import feign.Logger
import feign.Request
import feign.Retryer
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.feign.FeignDecorators
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.timelimiter.TimeLimiter
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.TimeUnit

@Configuration
class UserServiceFeignConfig {

    @Bean
    fun userServiceLoggerLevel(): Logger.Level = Logger.Level.BASIC

    @Bean
    fun userServiceRequestOptions(): Request.Options {
        return Request.Options(3000, TimeUnit.MILLISECONDS, 5000, TimeUnit.MILLISECONDS, true)
    }

    @Bean
    fun userServiceRetryer(): Retryer = Retryer.NEVER_RETRY

    @Bean
    fun userServiceFeignDecorators(
        @Qualifier("user-service") circuitBreaker: CircuitBreaker,
        @Qualifier("user-service") retry: Retry,
        @Qualifier("user-service") timeLimiter: TimeLimiter
    ): FeignDecorators {
        return FeignDecorators.builder()
            .withCircuitBreaker(circuitBreaker)
            .withRetry(retry)
            .withTimeLimiter(timeLimiter)
            .build()
    }
}

@Configuration
class ProductServiceFeignConfig {

    @Bean
    fun productServiceLoggerLevel(): Logger.Level = Logger.Level.BASIC

    @Bean
    fun productServiceRequestOptions(): Request.Options {
        return Request.Options(2000, TimeUnit.MILLISECONDS, 8000, TimeUnit.MILLISECONDS, true)
    }

    @Bean
    fun productServiceRetryer(): Retryer = Retryer.NEVER_RETRY

    @Bean
    fun productServiceFeignDecorators(
        @Qualifier("product-service") circuitBreaker: CircuitBreaker,
        @Qualifier("product-service") retry: Retry,
        @Qualifier("product-service") timeLimiter: TimeLimiter
    ): FeignDecorators {
        return FeignDecorators.builder()
            .withCircuitBreaker(circuitBreaker)
            .withRetry(retry)
            .withTimeLimiter(timeLimiter)
            .build()
    }
}