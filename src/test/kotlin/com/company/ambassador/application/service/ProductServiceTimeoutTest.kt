package com.company.ambassador.application.service

import com.company.ambassador.config.RateLimiterConfig
import com.company.ambassador.infrastructure.client.product.ProductServiceClient
import com.company.ambassador.infrastructure.client.product.dto.ProductResponse
import io.github.bucket4j.Bucket
import io.github.resilience4j.timelimiter.TimeLimiter
import io.github.resilience4j.timelimiter.TimeLimiterConfig
import io.github.resilience4j.timelimiter.TimeLimiterRegistry
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.*
import java.util.function.Supplier

class ProductServiceTimeoutTest {

    private lateinit var productService: ProductServiceImpl
    private val productServiceClient = mockk<ProductServiceClient>()
    private val rateLimiterConfig = mockk<RateLimiterConfig>()
    private val bucket = mockk<Bucket>()
    private lateinit var timeLimiter: TimeLimiter
    private lateinit var executorService: ScheduledExecutorService

    @BeforeEach
    fun setUp() {
        // Configure rate limiter mocks
        every { rateLimiterConfig.getBucket("product-service") } returns bucket
        every { bucket.tryConsume(1) } returns true

        // Create time limiter for testing
        val timeLimiterConfig = TimeLimiterConfig.custom()
            .timeoutDuration(Duration.ofMillis(500))
            .cancelRunningFuture(true)
            .build()

        val timeLimiterRegistry = TimeLimiterRegistry.of(timeLimiterConfig)
        timeLimiter = timeLimiterRegistry.timeLimiter("product-service-test")

        executorService = Executors.newSingleThreadScheduledExecutor()
        
        productService = ProductServiceImpl(productServiceClient, rateLimiterConfig)
    }

    @Test
    fun `should complete within timeout successfully`() {
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

        every { productServiceClient.getProductById(productId) } answers {
            // Simulate quick response (within timeout)
            Thread.sleep(100)
            productResponse
        }

        // When
        val result = timeLimiter.executeFutureSupplier(Supplier {
            CompletableFuture.supplyAsync {
                productServiceClient.getProductById(productId)
            }
        })

        // Then
        assertNotNull(result)
        assertEquals(productId, result.id)
        verify(exactly = 1) { productServiceClient.getProductById(productId) }
    }

    @Test
    fun `should timeout when operation takes too long`() {
        // Given
        val productId = 1L

        every { productServiceClient.getProductById(productId) } answers {
            // Simulate slow response (exceeds timeout)
            Thread.sleep(1000) // Longer than 500ms timeout
            ProductResponse(productId, "Test", "Test", 99.99, "Electronics", true, LocalDateTime.now())
        }

        // When & Then
        assertThrows<TimeoutException> {
            timeLimiter.executeFutureSupplier(Supplier {
                CompletableFuture.supplyAsync {
                    productServiceClient.getProductById(productId)
                }
            })
        }
    }

    @Test
    fun `should cancel running future when timeout occurs`() {
        // Given
        val productId = 1L
        var operationCompleted = false

        every { productServiceClient.getProductById(productId) } answers {
            try {
                Thread.sleep(1000) // Longer than timeout
                operationCompleted = true
                ProductResponse(productId, "Test", "Test", 99.99, "Electronics", true, LocalDateTime.now())
            } catch (e: InterruptedException) {
                // Expected when future is cancelled
                throw e
            }
        }

        // When & Then
        assertThrows<TimeoutException> {
            timeLimiter.executeFutureSupplier(Supplier {
                CompletableFuture.supplyAsync {
                    productServiceClient.getProductById(productId)
                }
            })
        }

        // Give some time for the operation to potentially complete
        Thread.sleep(200)
        
        // Then - Operation should have been cancelled and not completed
        assertFalse(operationCompleted)
    }

    @Test
    fun `should handle timeout with different timeout durations`() {
        // Given
        val productId = 1L
        val shortTimeoutConfig = TimeLimiterConfig.custom()
            .timeoutDuration(Duration.ofMillis(100))
            .cancelRunningFuture(true)
            .build()

        val shortTimeLimiter = TimeLimiterRegistry.of(shortTimeoutConfig)
            .timeLimiter("short-timeout-test")

        every { productServiceClient.getProductById(productId) } answers {
            Thread.sleep(200) // Longer than 100ms timeout
            ProductResponse(productId, "Test", "Test", 99.99, "Electronics", true, LocalDateTime.now())
        }

        // When & Then
        val startTime = System.currentTimeMillis()
        
        assertThrows<TimeoutException> {
            shortTimeLimiter.executeFutureSupplier(Supplier {
                CompletableFuture.supplyAsync {
                    productServiceClient.getProductById(productId)
                }
            })
        }
        
        val duration = System.currentTimeMillis() - startTime
        // Should timeout around 100ms, allowing some variance
        assertTrue(duration < 200)
    }

    @Test
    fun `should record timeout events`() {
        // Given
        val productId = 1L
        var timeoutEventFired = false

        // Register event listeners
        timeLimiter.eventPublisher
            .onTimeout { event ->
                timeoutEventFired = true
                println("Timeout event: ${event.eventType}")
            }

        every { productServiceClient.getProductById(productId) } answers {
            Thread.sleep(1000) // Exceeds timeout
            ProductResponse(productId, "Test", "Test", 99.99, "Electronics", true, LocalDateTime.now())
        }

        // When & Then
        assertThrows<TimeoutException> {
            timeLimiter.executeFutureSupplier(Supplier {
                CompletableFuture.supplyAsync {
                    productServiceClient.getProductById(productId)
                }
            })
        }

        // Then
        assertTrue(timeoutEventFired)
    }

    @Test
    fun `should handle successful completion events`() {
        // Given
        val productId = 1L
        var successEventFired = false

        timeLimiter.eventPublisher
            .onSuccess { event ->
                successEventFired = true
                println("Success event: ${event.eventType}")
            }

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
            Thread.sleep(50) // Well within timeout
            productResponse
        }

        // When
        val result = timeLimiter.executeFutureSupplier(Supplier {
            CompletableFuture.supplyAsync {
                productServiceClient.getProductById(productId)
            }
        })

        // Then
        assertNotNull(result)
        assertTrue(successEventFired)
    }

    @Test
    fun `should handle concurrent timeout operations`() {
        // Given
        val productIds = listOf(1L, 2L, 3L)
        val results = mutableListOf<Result<ProductResponse>>()

        every { productServiceClient.getProductById(any()) } answers {
            val id = firstArg<Long>()
            when (id) {
                1L -> {
                    Thread.sleep(100) // Within timeout
                    ProductResponse(id, "Product 1", "Description", 99.99, "Electronics", true, LocalDateTime.now())
                }
                2L -> {
                    Thread.sleep(600) // Exceeds timeout
                    ProductResponse(id, "Product 2", "Description", 99.99, "Electronics", true, LocalDateTime.now())
                }
                3L -> {
                    Thread.sleep(200) // Within timeout
                    ProductResponse(id, "Product 3", "Description", 99.99, "Electronics", true, LocalDateTime.now())
                }
                else -> throw IllegalArgumentException("Unknown product ID")
            }
        }

        // When
        val futures = productIds.map { productId ->
            CompletableFuture.supplyAsync {
                try {
                    val result = timeLimiter.executeFutureSupplier(Supplier {
                        CompletableFuture.supplyAsync {
                            productServiceClient.getProductById(productId)
                        }
                    })
                    Result.success(result)
                } catch (e: TimeoutException) {
                    Result.failure<ProductResponse>(e)
                } catch (e: Exception) {
                    Result.failure<ProductResponse>(e)
                }
            }
        }

        // Wait for all to complete
        futures.forEach { future ->
            results.add(future.get())
        }

        // Then
        assertEquals(3, results.size)
        assertTrue(results[0].isSuccess) // Product 1 should succeed
        assertTrue(results[1].isFailure) // Product 2 should timeout
        assertTrue(results[2].isSuccess) // Product 3 should succeed
    }

    @Test
    fun `should handle timeout with custom executor service`() {
        // Given
        val productId = 1L
        val customExecutor = Executors.newFixedThreadPool(2)
        
        val customTimeLimiterConfig = TimeLimiterConfig.custom()
            .timeoutDuration(Duration.ofMillis(300))
            .cancelRunningFuture(true)
            .build()
            
        val customTimeLimiter = TimeLimiterRegistry.of(customTimeLimiterConfig)
            .timeLimiter("custom-executor-test")

        every { productServiceClient.getProductById(productId) } answers {
            Thread.sleep(500) // Exceeds timeout
            ProductResponse(productId, "Test", "Test", 99.99, "Electronics", true, LocalDateTime.now())
        }

        try {
            // When & Then
            assertThrows<TimeoutException> {
                customTimeLimiter.executeFutureSupplier(Supplier {
                    CompletableFuture.supplyAsync({
                        productServiceClient.getProductById(productId)
                    }, customExecutor)
                })
            }
        } finally {
            customExecutor.shutdown()
        }
    }

    @Test
    fun `should handle supplier that returns CompletableFuture`() {
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

        every { productServiceClient.getProductById(productId) } answers {
            Thread.sleep(50) // Within timeout
            productResponse
        }

        // When - Using Supplier<CompletableFuture<T>> directly
        val supplier = Supplier {
            CompletableFuture.supplyAsync {
                productServiceClient.getProductById(productId)
            }
        }

        val result = timeLimiter.executeFutureSupplier(supplier)

        // Then
        assertNotNull(result)
        assertEquals(productId, result.id)
        verify(exactly = 1) { productServiceClient.getProductById(productId) }
    }

    @Test
    fun `should handle exception thrown before timeout`() {
        // Given
        val productId = 1L

        every { productServiceClient.getProductById(productId) } throws RuntimeException("Immediate failure")

        // When & Then
        assertThrows<RuntimeException> {
            timeLimiter.executeFutureSupplier(Supplier {
                CompletableFuture.supplyAsync {
                    productServiceClient.getProductById(productId)
                }
            })
        }

        verify(exactly = 1) { productServiceClient.getProductById(productId) }
    }
}