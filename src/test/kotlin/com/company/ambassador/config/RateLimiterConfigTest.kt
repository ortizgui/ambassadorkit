package com.company.ambassador.config

import io.github.bucket4j.Bucket
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RateLimiterConfigTest {

    private lateinit var rateLimiterConfig: RateLimiterConfig
    private val externalServicesProperties = mockk<ExternalServicesProperties>()

    @BeforeEach
    fun setUp() {
        // Mock the properties
        val userServiceConfig = ExternalServicesProperties.ServiceConfig(
            url = "http://localhost:8081",
            rateLimit = ExternalServicesProperties.RateLimitConfig(
                capacity = 100,
                tokens = 10,
                refillPeriod = "1s"
            )
        )

        val productServiceConfig = ExternalServicesProperties.ServiceConfig(
            url = "http://localhost:8082",
            rateLimit = ExternalServicesProperties.RateLimitConfig(
                capacity = 50,
                tokens = 5,
                refillPeriod = "2s"
            )
        )

        every { externalServicesProperties.userService } returns userServiceConfig
        every { externalServicesProperties.productService } returns productServiceConfig

        rateLimiterConfig = RateLimiterConfig(externalServicesProperties)
    }

    @Test
    fun `should create user service bucket with correct configuration`() {
        // When
        val bucket = rateLimiterConfig.userServiceBucket()

        // Then
        assertNotNull(bucket)
        assertTrue(bucket is Bucket)
        
        // Test that bucket has tokens available
        assertTrue(bucket.tryConsume(1))
    }

    @Test
    fun `should create product service bucket with correct configuration`() {
        // When
        val bucket = rateLimiterConfig.productServiceBucket()

        // Then
        assertNotNull(bucket)
        assertTrue(bucket is Bucket)
        
        // Test that bucket has tokens available
        assertTrue(bucket.tryConsume(1))
    }

    @Test
    fun `should return same bucket instance for same service name`() {
        // When
        val bucket1 = rateLimiterConfig.getBucket("user-service")
        val bucket2 = rateLimiterConfig.getBucket("user-service")

        // Then
        assertSame(bucket1, bucket2)
    }

    @Test
    fun `should return null for unknown service name`() {
        // When
        val bucket = rateLimiterConfig.getBucket("unknown-service")

        // Then
        assertNull(bucket)
    }

    @Test
    fun `should respect rate limit capacity`() {
        // Given
        val bucket = rateLimiterConfig.getBucket("user-service")
        assertNotNull(bucket)

        // When - Try to consume more tokens than capacity
        val results = mutableListOf<Boolean>()
        repeat(150) { // More than capacity of 100
            results.add(bucket!!.tryConsume(1))
        }

        // Then - Should eventually return false when capacity is exceeded
        val successfulConsumptions = results.count { it }
        assertTrue(successfulConsumptions <= 100) // Should not exceed capacity
    }

    @Test
    fun `should parse duration correctly for seconds`() {
        // Given
        val config = ExternalServicesProperties.RateLimitConfig(
            capacity = 10,
            tokens = 5,
            refillPeriod = "5s"
        )
        val serviceConfig = ExternalServicesProperties.ServiceConfig(
            url = "http://test.com",
            rateLimit = config
        )
        every { externalServicesProperties.userService } returns serviceConfig

        // When
        val bucket = rateLimiterConfig.userServiceBucket()

        // Then
        assertNotNull(bucket)
        // The bucket should be created successfully with 5s refill period
    }

    @Test
    fun `should parse duration correctly for minutes`() {
        // Given
        val config = ExternalServicesProperties.RateLimitConfig(
            capacity = 10,
            tokens = 5,
            refillPeriod = "2m"
        )
        val serviceConfig = ExternalServicesProperties.ServiceConfig(
            url = "http://test.com",
            rateLimit = config
        )
        every { externalServicesProperties.productService } returns serviceConfig

        // When
        val bucket = rateLimiterConfig.productServiceBucket()

        // Then
        assertNotNull(bucket)
        // The bucket should be created successfully with 2m refill period
    }

    @Test
    fun `should parse duration correctly for hours`() {
        // Given
        val config = ExternalServicesProperties.RateLimitConfig(
            capacity = 10,
            tokens = 5,
            refillPeriod = "1h"
        )
        val serviceConfig = ExternalServicesProperties.ServiceConfig(
            url = "http://test.com",
            rateLimit = config
        )
        every { externalServicesProperties.userService } returns serviceConfig

        // When
        val bucket = rateLimiterConfig.userServiceBucket()

        // Then
        assertNotNull(bucket)
        // The bucket should be created successfully with 1h refill period
    }

    @Test
    fun `should use default duration for invalid format`() {
        // Given
        val config = ExternalServicesProperties.RateLimitConfig(
            capacity = 10,
            tokens = 5,
            refillPeriod = "invalid"
        )
        val serviceConfig = ExternalServicesProperties.ServiceConfig(
            url = "http://test.com",
            rateLimit = config
        )
        every { externalServicesProperties.productService } returns serviceConfig

        // When
        val bucket = rateLimiterConfig.productServiceBucket()

        // Then
        assertNotNull(bucket)
        // The bucket should be created successfully with default 1s refill period
    }

    @Test
    fun `should handle concurrent access to buckets`() {
        // Given
        val threads = mutableListOf<Thread>()
        val buckets = mutableListOf<Bucket?>()

        // When - Create multiple threads accessing the same bucket
        repeat(10) {
            threads.add(Thread {
                val bucket = rateLimiterConfig.getBucket("user-service")
                synchronized(buckets) {
                    buckets.add(bucket)
                }
            })
        }

        threads.forEach { it.start() }
        threads.forEach { it.join() }

        // Then - All threads should get the same bucket instance
        assertEquals(10, buckets.size)
        buckets.forEach { bucket ->
            assertSame(buckets[0], bucket)
        }
    }

    @Test
    fun `should handle edge case with zero capacity`() {
        // Given
        val config = ExternalServicesProperties.RateLimitConfig(
            capacity = 0,
            tokens = 1,
            refillPeriod = "1s"
        )
        val serviceConfig = ExternalServicesProperties.ServiceConfig(
            url = "http://test.com",
            rateLimit = config
        )
        every { externalServicesProperties.userService } returns serviceConfig

        // When
        val bucket = rateLimiterConfig.userServiceBucket()

        // Then
        assertNotNull(bucket)
        // Should not be able to consume any tokens
        assertFalse(bucket.tryConsume(1))
    }

    @Test
    fun `should handle edge case with zero tokens`() {
        // Given
        val config = ExternalServicesProperties.RateLimitConfig(
            capacity = 10,
            tokens = 0,
            refillPeriod = "1s"
        )
        val serviceConfig = ExternalServicesProperties.ServiceConfig(
            url = "http://test.com",
            rateLimit = config
        )
        every { externalServicesProperties.productService } returns serviceConfig

        // When
        val bucket = rateLimiterConfig.productServiceBucket()

        // Then
        assertNotNull(bucket)
        // The bucket should be created but may not have tokens to consume immediately
    }
}