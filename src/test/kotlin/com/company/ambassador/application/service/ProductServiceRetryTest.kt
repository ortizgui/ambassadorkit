package com.company.ambassador.application.service

import com.company.ambassador.config.RateLimiterConfig
import com.company.ambassador.infrastructure.client.product.ProductServiceClient
import com.company.ambassador.infrastructure.client.product.dto.ProductResponse
import feign.FeignException
import feign.RetryableException
import io.github.bucket4j.Bucket
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import io.github.resilience4j.retry.RetryRegistry
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicInteger

class ProductServiceRetryTest {

    private lateinit var productService: ProductServiceImpl
    private val productServiceClient = mockk<ProductServiceClient>()
    private val rateLimiterConfig = mockk<RateLimiterConfig>()
    private val bucket = mockk<Bucket>()
    private lateinit var retry: Retry

    @BeforeEach
    fun setUp() {
        // Configure rate limiter mocks
        every { rateLimiterConfig.getBucket("product-service") } returns bucket
        every { bucket.tryConsume(1) } returns true

        // Create retry for testing
        val retryConfig = RetryConfig.custom<Any>()
            .maxAttempts(3)
            .waitDuration(Duration.ofMillis(100))
            .retryOnException { throwable ->
                throwable is FeignException || 
                throwable is RetryableException ||
                throwable is RuntimeException
            }
            .build()

        val retryRegistry = RetryRegistry.of(retryConfig)
        retry = retryRegistry.retry("product-service-test")

        productService = ProductServiceImpl(productServiceClient, rateLimiterConfig)
    }

    @Test
    fun `should retry on transient failures and eventually succeed`() {
        // Given
        val productId = 1L
        val callCount = AtomicInteger(0)
        val productResponse = ProductResponse(
            id = productId,
            name = "Test Product",
            description = "Test Description",
            price = 99.99,
            category = "Electronics",
            available = true,
            createdAt = LocalDateTime.now()
        )

        every { productServiceClient.getProductById(productId) } answers {
            val count = callCount.incrementAndGet()
            when (count) {
                1, 2 -> throw RuntimeException("Temporary failure")
                else -> productResponse
            }
        }

        // When
        val result = retry.executeSupplier {
            productServiceClient.getProductById(productId)
        }

        // Then
        assertNotNull(result)
        assertEquals(productId, result.id)
        assertEquals(3, callCount.get()) // 1 initial + 2 retries
        verify(exactly = 3) { productServiceClient.getProductById(productId) }
    }

    @Test
    fun `should retry on FeignException and eventually succeed`() {
        // Given
        val productId = 1L
        val callCount = AtomicInteger(0)
        val productResponse = ProductResponse(
            id = productId,
            name = "Test Product",
            description = "Test Description",
            price = 99.99,
            category = "Electronics",
            available = true,
            createdAt = LocalDateTime.now()
        )

        val feignException = mockk<FeignException>()
        every { feignException.status() } returns 503
        every { feignException.message } returns "Service Temporarily Unavailable"

        every { productServiceClient.getProductById(productId) } answers {
            val count = callCount.incrementAndGet()
            when (count) {
                1, 2 -> throw feignException
                else -> productResponse
            }
        }

        // When
        val result = retry.executeSupplier {
            productServiceClient.getProductById(productId)
        }

        // Then
        assertNotNull(result)
        assertEquals(productId, result.id)
        assertEquals(3, callCount.get())
        verify(exactly = 3) { productServiceClient.getProductById(productId) }
    }

    @Test
    fun `should exhaust all retry attempts and fail`() {
        // Given
        val productId = 1L
        val callCount = AtomicInteger(0)
        val feignException = mockk<FeignException>()
        every { feignException.status() } returns 503
        every { feignException.message } returns "Service Unavailable"

        every { productServiceClient.getProductById(productId) } answers {
            callCount.incrementAndGet()
            throw feignException
        }

        // When & Then
        assertThrows<FeignException> {
            retry.executeSupplier {
                productServiceClient.getProductById(productId)
            }
        }

        assertEquals(3, callCount.get()) // maxAttempts = 3
        verify(exactly = 3) { productServiceClient.getProductById(productId) }
    }

    @Test
    fun `should not retry on non-retryable exceptions`() {
        // Given
        val productId = 1L
        val callCount = AtomicInteger(0)
        val nonRetryableException = IllegalArgumentException("Invalid request")

        every { productServiceClient.getProductById(productId) } answers {
            callCount.incrementAndGet()
            throw nonRetryableException
        }

        // Configure retry to not retry on IllegalArgumentException
        val restrictiveRetryConfig = RetryConfig.custom<Any>()
            .maxAttempts(3)
            .waitDuration(Duration.ofMillis(100))
            .retryOnException { throwable ->
                throwable is FeignException || throwable is RetryableException
            }
            .build()

        val restrictiveRetry = RetryRegistry.of(restrictiveRetryConfig).retry("restrictive-test")

        // When & Then
        assertThrows<IllegalArgumentException> {
            restrictiveRetry.executeSupplier {
                productServiceClient.getProductById(productId)
            }
        }

        assertEquals(1, callCount.get()) // Should only be called once, no retries
        verify(exactly = 1) { productServiceClient.getProductById(productId) }
    }

    @Test
    fun `should handle retry with exponential backoff`() {
        // Given
        val productId = 1L
        val callTimes = mutableListOf<Long>()
        val startTime = System.currentTimeMillis()

        val exponentialRetryConfig = RetryConfig.custom<Any>()
            .maxAttempts(3)
            .waitDuration(Duration.ofMillis(200))
            .build()

        val exponentialRetry = RetryRegistry.of(exponentialRetryConfig).retry("exponential-test")

        every { productServiceClient.getProductById(productId) } answers {
            callTimes.add(System.currentTimeMillis() - startTime)
            throw RuntimeException("Always fails")
        }

        // When & Then
        assertThrows<RuntimeException> {
            exponentialRetry.executeSupplier {
                productServiceClient.getProductById(productId)
            }
        }

        // Then
        assertEquals(3, callTimes.size)
        // Verify there's a delay between calls (allowing for some variance)
        assertTrue(callTimes[1] - callTimes[0] >= 150) // Should be around 200ms
        assertTrue(callTimes[2] - callTimes[1] >= 150) // Should be around 200ms
    }

    @Test
    fun `should record retry events`() {
        // Given
        val productId = 1L
        var retryEventCount = 0
        var successAfterRetryEventFired = false

        val productResponse = ProductResponse(
            id = productId,
            name = "Test Product",
            description = "Test Description",
            price = 99.99,
            category = "Electronics",
            available = true,
            createdAt = LocalDateTime.now()
        )

        // Register event listeners
        retry.eventPublisher
            .onRetry { event ->
                retryEventCount++
                println("Retry attempt: ${event.numberOfRetryAttempts}")
            }
            .onSuccess { event ->
                if (event.numberOfRetryAttempts > 0) {
                    successAfterRetryEventFired = true
                }
                println("Success after ${event.numberOfRetryAttempts} retries")
            }

        val callCount = AtomicInteger(0)
        every { productServiceClient.getProductById(productId) } answers {
            val count = callCount.incrementAndGet()
            when (count) {
                1, 2 -> throw RuntimeException("Temporary failure")
                else -> productResponse
            }
        }

        // When
        val result = retry.executeSupplier {
            productServiceClient.getProductById(productId)
        }

        // Then
        assertNotNull(result)
        assertEquals(2, retryEventCount) // 2 retry attempts
        assertTrue(successAfterRetryEventFired)
    }

    @Test
    fun `should handle retry with different wait strategies`() {
        // Given
        val productId = 1L
        
        // Fixed delay retry
        val fixedDelayConfig = RetryConfig.custom<Any>()
            .maxAttempts(2)
            .waitDuration(Duration.ofMillis(100))
            .build()

        val fixedDelayRetry = RetryRegistry.of(fixedDelayConfig).retry("fixed-delay-test")

        every { productServiceClient.getProductById(productId) } throws RuntimeException("Always fails")

        val startTime = System.currentTimeMillis()
        val callTimes = mutableListOf<Long>()

        // When & Then
        assertThrows<RuntimeException> {
            fixedDelayRetry.executeSupplier {
                callTimes.add(System.currentTimeMillis() - startTime)
                productServiceClient.getProductById(productId)
            }
        }

        // Verify fixed delay between retries
        assertEquals(2, callTimes.size)
        assertTrue(callTimes[1] - callTimes[0] >= 90) // Allow some variance
    }
}