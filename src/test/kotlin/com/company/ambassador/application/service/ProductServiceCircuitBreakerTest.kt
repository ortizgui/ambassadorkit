package com.company.ambassador.application.service

import com.company.ambassador.config.RateLimiterConfig
import com.company.ambassador.infrastructure.client.product.ProductServiceClient
import com.company.ambassador.infrastructure.client.product.dto.ProductResponse
import feign.FeignException
import feign.Request
import feign.RequestTemplate
import io.github.bucket4j.Bucket
import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Duration
import java.time.LocalDateTime

class ProductServiceCircuitBreakerTest {

    private lateinit var productService: ProductServiceImpl
    private val productServiceClient = mockk<ProductServiceClient>()
    private val rateLimiterConfig = mockk<RateLimiterConfig>()
    private val bucket = mockk<Bucket>()
    private lateinit var circuitBreaker: CircuitBreaker

    @BeforeEach
    fun setUp() {
        // Configure rate limiter mocks
        every { rateLimiterConfig.getBucket("product-service") } returns bucket
        every { bucket.tryConsume(1) } returns true

        // Create circuit breaker for testing
        val circuitBreakerConfig = CircuitBreakerConfig.custom()
            .slidingWindowSize(5)
            .minimumNumberOfCalls(3)
            .failureRateThreshold(50f)
            .waitDurationInOpenState(Duration.ofSeconds(1))
            .permittedNumberOfCallsInHalfOpenState(2)
            .build()

        val circuitBreakerRegistry = CircuitBreakerRegistry.of(circuitBreakerConfig)
        circuitBreaker = circuitBreakerRegistry.circuitBreaker("product-service-test")

        productService = ProductServiceImpl(productServiceClient, rateLimiterConfig)
    }

    @Test
    fun `should open circuit breaker after consecutive failures`() {
        // Given
        val productId = 1L
        val feignException = mockk<FeignException>()
        every { feignException.status() } returns 500
        every { feignException.message } returns "Internal Server Error"
        every { productServiceClient.getProductById(productId) } throws feignException

        // When - Make enough failed calls to trip the circuit breaker
        repeat(5) {
            try {
                circuitBreaker.executeSupplier {
                    productServiceClient.getProductById(productId)
                }
            } catch (e: Exception) {
                // Expected failures
            }
        }

        // Then - Circuit breaker should be open
        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.state)
        
        // Verify that subsequent calls are not permitted
        assertThrows<CallNotPermittedException> {
            circuitBreaker.executeSupplier {
                productServiceClient.getProductById(productId)
            }
        }
    }

    @Test
    fun `should transition to half-open state after wait duration`() {
        // Given
        val productId = 1L
        val feignException = mockk<FeignException>()
        every { feignException.status() } returns 500
        every { feignException.message } returns "Internal Server Error"
        every { productServiceClient.getProductById(productId) } throws feignException

        // Trip the circuit breaker
        repeat(5) {
            try {
                circuitBreaker.executeSupplier {
                    productServiceClient.getProductById(productId)
                }
            } catch (e: Exception) {
                // Expected failures
            }
        }

        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.state)

        // When - Transition to half-open manually for testing
        circuitBreaker.transitionToHalfOpenState()

        // Then
        assertEquals(CircuitBreaker.State.HALF_OPEN, circuitBreaker.state)
    }

    @Test
    fun `should close circuit breaker after successful calls in half-open state`() {
        // Given
        val productId = 1L
        val productResponse = ProductResponse(
            id = productId,
            name = "Test Product",
            description = "Test Description",
            price = 99.99,
            category = "Electronics",
            available = true,
            createdAt = LocalDateTime.now()
        )

        // Trip the circuit breaker first
        val feignException = mockk<FeignException>()
        every { feignException.status() } returns 500
        every { productServiceClient.getProductById(productId) } throws feignException

        repeat(5) {
            try {
                circuitBreaker.executeSupplier {
                    productServiceClient.getProductById(productId)
                }
            } catch (e: Exception) {
                // Expected failures
            }
        }

        // Transition to half-open
        circuitBreaker.transitionToHalfOpenState()
        assertEquals(CircuitBreaker.State.HALF_OPEN, circuitBreaker.state)

        // When - Make successful calls
        every { productServiceClient.getProductById(productId) } returns productResponse
        
        repeat(2) { // permittedNumberOfCallsInHalfOpenState = 2
            circuitBreaker.executeSupplier {
                productServiceClient.getProductById(productId)
            }
        }

        // Then - Circuit breaker should be closed
        assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.state)
    }

    @Test
    fun `should record different types of exceptions`() {
        // Given
        val productId = 1L
        
        // Test FeignException
        val feignException = mockk<FeignException>()
        every { feignException.status() } returns 503
        every { feignException.message } returns "Service Unavailable"
        every { productServiceClient.getProductById(productId) } throws feignException

        // When & Then
        assertThrows<FeignException> {
            circuitBreaker.executeSupplier {
                productServiceClient.getProductById(productId)
            }
        }

        // Verify that the failure was recorded
        val metrics = circuitBreaker.metrics
        assertEquals(1, metrics.numberOfFailedCalls)
    }

    @Test
    fun `should handle timeout exceptions`() {
        // Given
        val productId = 1L
        every { productServiceClient.getProductById(productId) } throws RuntimeException("Timeout")

        // When & Then
        assertThrows<RuntimeException> {
            circuitBreaker.executeSupplier {
                productServiceClient.getProductById(productId)
            }
        }

        // Verify metrics
        val metrics = circuitBreaker.metrics
        assertEquals(1, metrics.numberOfFailedCalls)
    }

    @Test
    fun `should handle successful calls and update metrics`() {
        // Given
        val productId = 1L
        val productResponse = ProductResponse(
            id = productId,
            name = "Test Product",
            description = "Test Description", 
            price = 99.99,
            category = "Electronics",
            available = true,
            createdAt = LocalDateTime.now()
        )
        every { productServiceClient.getProductById(productId) } returns productResponse

        // When
        val result = circuitBreaker.executeSupplier {
            productServiceClient.getProductById(productId)
        }

        // Then
        assertNotNull(result)
        assertEquals(productId, result.id)
        
        val metrics = circuitBreaker.metrics
        assertEquals(1, metrics.numberOfSuccessfulCalls)
        assertEquals(0, metrics.numberOfFailedCalls)
    }

    @Test
    fun `should handle circuit breaker events`() {
        // Given
        val productId = 1L
        var stateTransitionEventFired = false
        var callNotPermittedEventFired = false

        // Register event listeners
        circuitBreaker.eventPublisher
            .onStateTransition { event ->
                stateTransitionEventFired = true
                println("Circuit breaker state transition: ${event.stateTransition}")
            }
            .onCallNotPermitted { event ->
                callNotPermittedEventFired = true
                println("Call not permitted: ${event.eventType}")
            }

        val feignException = mockk<FeignException>()
        every { feignException.status() } returns 500
        every { productServiceClient.getProductById(productId) } throws feignException

        // When - Trip the circuit breaker
        repeat(5) {
            try {
                circuitBreaker.executeSupplier {
                    productServiceClient.getProductById(productId)
                }
            } catch (e: Exception) {
                // Expected failures
            }
        }

        // Try to make a call when circuit is open
        try {
            circuitBreaker.executeSupplier {
                productServiceClient.getProductById(productId)
            }
        } catch (e: CallNotPermittedException) {
            // Expected
        }

        // Then
        assertTrue(stateTransitionEventFired)
        assertTrue(callNotPermittedEventFired)
        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.state)
    }
}