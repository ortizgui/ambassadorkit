package com.company.ambassador.config

import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.timelimiter.TimeLimiter
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.function.Supplier

class Resilience4jConfigTest {

    private lateinit var resilience4jConfig: Resilience4jConfig

    @BeforeEach
    fun setUp() {
        resilience4jConfig = Resilience4jConfig()
    }

    @Test
    fun `should create circuit breaker registry with correct configuration`() {
        // When
        val registry = resilience4jConfig.circuitBreakerRegistry()

        // Then
        assertNotNull(registry)
        
        // Test default configuration
        val defaultConfig = registry.defaultConfig
        assertEquals(10, defaultConfig.slidingWindowSize)
        assertEquals(5, defaultConfig.minimumNumberOfCalls)
        assertEquals(3, defaultConfig.permittedNumberOfCallsInHalfOpenState)
        assertTrue(defaultConfig.isAutomaticTransitionFromOpenToHalfOpenEnabled)
        assertEquals(Duration.ofSeconds(5), defaultConfig.waitIntervalFunctionInOpenState.apply(1))
        assertEquals(50f, defaultConfig.failureRateThreshold)
    }

    @Test
    fun `should create retry registry with correct configuration`() {
        // When
        val registry = resilience4jConfig.retryRegistry()

        // Then
        assertNotNull(registry)
        
        // Test default configuration
        val defaultConfig = registry.defaultConfig
        assertEquals(3, defaultConfig.maxAttempts)
        assertEquals(Duration.ofMillis(500), defaultConfig.intervalFunction.apply(1))
    }

    @Test
    fun `should create time limiter registry with correct configuration`() {
        // When
        val registry = resilience4jConfig.timeLimiterRegistry()

        // Then
        assertNotNull(registry)
        
        // Test default configuration
        val defaultConfig = registry.defaultConfig
        assertEquals(Duration.ofSeconds(3), defaultConfig.timeoutDuration)
        assertTrue(defaultConfig.shouldCancelRunningFuture())
    }

    @Test
    fun `should create user service circuit breaker`() {
        // Given
        val registry = resilience4jConfig.circuitBreakerRegistry()

        // When
        val circuitBreaker = resilience4jConfig.userServiceCircuitBreaker(registry)

        // Then
        assertNotNull(circuitBreaker)
        assertEquals("user-service", circuitBreaker.name)
        assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.state)
    }

    @Test
    fun `should create product service circuit breaker`() {
        // Given
        val registry = resilience4jConfig.circuitBreakerRegistry()

        // When
        val circuitBreaker = resilience4jConfig.productServiceCircuitBreaker(registry)

        // Then
        assertNotNull(circuitBreaker)
        assertEquals("product-service", circuitBreaker.name)
        assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.state)
    }

    @Test
    fun `should create user service retry`() {
        // Given
        val registry = resilience4jConfig.retryRegistry()

        // When
        val retry = resilience4jConfig.userServiceRetry(registry)

        // Then
        assertNotNull(retry)
        assertEquals("user-service", retry.name)
    }

    @Test
    fun `should create product service retry`() {
        // Given
        val registry = resilience4jConfig.retryRegistry()

        // When
        val retry = resilience4jConfig.productServiceRetry(registry)

        // Then
        assertNotNull(retry)
        assertEquals("product-service", retry.name)
    }

    @Test
    fun `should create user service time limiter`() {
        // Given
        val registry = resilience4jConfig.timeLimiterRegistry()

        // When
        val timeLimiter = resilience4jConfig.userServiceTimeLimiter(registry)

        // Then
        assertNotNull(timeLimiter)
        assertEquals("user-service", timeLimiter.name)
    }

    @Test
    fun `should create product service time limiter`() {
        // Given
        val registry = resilience4jConfig.timeLimiterRegistry()

        // When
        val timeLimiter = resilience4jConfig.productServiceTimeLimiter(registry)

        // Then
        assertNotNull(timeLimiter)
        assertEquals("product-service", timeLimiter.name)
    }

    @Test
    fun `should handle circuit breaker state transitions`() {
        // Given
        val registry = resilience4jConfig.circuitBreakerRegistry()
        val circuitBreaker = resilience4jConfig.userServiceCircuitBreaker(registry)
        
        var stateTransitions = 0
        circuitBreaker.eventPublisher.onStateTransition { 
            stateTransitions++
        }

        // When - Force state transitions by simulating failures
        repeat(10) {
            try {
                circuitBreaker.executeSupplier {
                    throw RuntimeException("Test failure")
                }
            } catch (e: Exception) {
                // Expected
            }
        }

        // Then
        assertTrue(stateTransitions > 0)
        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.state)
    }

    @Test
    fun `should handle retry events`() {
        // Given
        val registry = resilience4jConfig.retryRegistry()
        val retry = resilience4jConfig.userServiceRetry(registry)
        
        var retryEvents = 0
        retry.eventPublisher.onRetry {
            retryEvents++
        }

        // When
        try {
            retry.executeSupplier {
                throw RuntimeException("Test failure")
            }
        } catch (e: Exception) {
            // Expected
        }

        // Then
        assertEquals(2, retryEvents) // maxAttempts = 3, so 2 retries
    }

    @Test
    fun `should handle timeout events`() {
        // Given
        val registry = resilience4jConfig.timeLimiterRegistry()
        val timeLimiter = resilience4jConfig.userServiceTimeLimiter(registry)
        
        var timeoutEvents = 0
        timeLimiter.eventPublisher.onTimeout {
            timeoutEvents++
        }

        // When
        try {
            val futureSupplier = java.util.concurrent.CompletableFuture.supplyAsync {
                Thread.sleep(5000) // Longer than 3s timeout
                "result"
            }
            timeLimiter.executeFutureSupplier(Supplier { futureSupplier })
        } catch (e: Exception) {
            // Expected timeout
        }

        // Then
        assertEquals(1, timeoutEvents)
    }

    @Test
    fun `should create multiple circuit breakers with same configuration`() {
        // Given
        val registry = resilience4jConfig.circuitBreakerRegistry()

        // When
        val userCircuitBreaker = resilience4jConfig.userServiceCircuitBreaker(registry)
        val productCircuitBreaker = resilience4jConfig.productServiceCircuitBreaker(registry)

        // Then
        assertNotSame(userCircuitBreaker, productCircuitBreaker)
        assertEquals("user-service", userCircuitBreaker.name)
        assertEquals("product-service", productCircuitBreaker.name)
        
        // Both should have the same configuration
        assertEquals(userCircuitBreaker.circuitBreakerConfig.slidingWindowSize, 
                    productCircuitBreaker.circuitBreakerConfig.slidingWindowSize)
    }

    @Test
    fun `should create multiple retries with same configuration`() {
        // Given
        val registry = resilience4jConfig.retryRegistry()

        // When
        val userRetry = resilience4jConfig.userServiceRetry(registry)
        val productRetry = resilience4jConfig.productServiceRetry(registry)

        // Then
        assertNotSame(userRetry, productRetry)
        assertEquals("user-service", userRetry.name)
        assertEquals("product-service", productRetry.name)
        
        // Both should have the same configuration
        assertEquals(userRetry.retryConfig.maxAttempts, productRetry.retryConfig.maxAttempts)
    }

    @Test
    fun `should create multiple time limiters with same configuration`() {
        // Given
        val registry = resilience4jConfig.timeLimiterRegistry()

        // When
        val userTimeLimiter = resilience4jConfig.userServiceTimeLimiter(registry)
        val productTimeLimiter = resilience4jConfig.productServiceTimeLimiter(registry)

        // Then
        assertNotSame(userTimeLimiter, productTimeLimiter)
        assertEquals("user-service", userTimeLimiter.name)
        assertEquals("product-service", productTimeLimiter.name)
        
        // Both should have the same configuration
        assertEquals(userTimeLimiter.timeLimiterConfig.timeoutDuration, 
                    productTimeLimiter.timeLimiterConfig.timeoutDuration)
    }
}