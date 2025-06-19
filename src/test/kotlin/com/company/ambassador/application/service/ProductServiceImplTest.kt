package com.company.ambassador.application.service

import com.company.ambassador.config.RateLimiterConfig
import com.company.ambassador.domain.model.Product
import com.company.ambassador.domain.model.ProductCreateRequest
import com.company.ambassador.infrastructure.client.product.ProductServiceClient
import com.company.ambassador.infrastructure.client.product.dto.ProductCreateRequest as ClientProductCreateRequest
import com.company.ambassador.infrastructure.client.product.dto.ProductResponse
import feign.FeignException
import feign.Request
import feign.RequestTemplate
import io.github.bucket4j.Bucket
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.cache.CacheManager
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDateTime
import java.util.concurrent.TimeoutException

class ProductServiceImplTest {

    private lateinit var productService: ProductServiceImpl
    private val productServiceClient = mockk<ProductServiceClient>()
    private val rateLimiterConfig = mockk<RateLimiterConfig>()
    private val bucket = mockk<Bucket>()
    private lateinit var cacheManager: CacheManager

    @BeforeEach
    fun setUp() {
        cacheManager = ConcurrentMapCacheManager("products", "product-category")
        productService = ProductServiceImpl(productServiceClient, rateLimiterConfig)
        
        // Configure rate limiter mocks
        every { rateLimiterConfig.getBucket("product-service") } returns bucket
        every { bucket.tryConsume(1) } returns true
    }

    @Test
    fun `should get product by id successfully`() {
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
        val result = productService.getProductById(productId)

        // Then
        assertEquals(productId, result.id)
        assertEquals("Test Product", result.name)
        assertEquals("Test Description", result.description)
        assertEquals(99.99, result.price)
        assertEquals("Electronics", result.category)
        assertTrue(result.available)
        verify(exactly = 1) { productServiceClient.getProductById(productId) }
    }

    @Test
    fun `should handle rate limit exceeded`() {
        // Given
        val productId = 1L
        every { bucket.tryConsume(1) } returns false

        // When & Then
        val exception = assertThrows<RuntimeException> {
            productService.getProductById(productId)
        }
        assertEquals("Rate limit exceeded for service: product-service", exception.message)
        verify(exactly = 0) { productServiceClient.getProductById(any()) }
    }

    @Test
    fun `should create product successfully`() {
        // Given
        val createRequest = ProductCreateRequest(
            name = "New Product",
            description = "New Description",
            price = 199.99,
            category = "Electronics"
        )
        val productResponse = ProductResponse(
            id = 1L,
            name = createRequest.name,
            description = createRequest.description,
            price = createRequest.price,
            category = createRequest.category,
            available = true,
            createdAt = LocalDateTime.now()
        )
        every { productServiceClient.createProduct(any()) } returns productResponse

        // When
        val result = productService.createProduct(createRequest)

        // Then
        assertEquals(1L, result.id)
        assertEquals("New Product", result.name)
        assertEquals("New Description", result.description)
        assertEquals(199.99, result.price)
        assertEquals("Electronics", result.category)
        assertTrue(result.available)
        verify(exactly = 1) { productServiceClient.createProduct(any()) }
    }

    @Test
    fun `should get products with pagination successfully`() {
        // Given
        val page = 0
        val size = 10
        val category = "Electronics"
        val productResponses = listOf(
            ProductResponse(1L, "Product 1", "Description 1", 99.99, category, true, LocalDateTime.now()),
            ProductResponse(2L, "Product 2", "Description 2", 149.99, category, true, LocalDateTime.now())
        )
        every { productServiceClient.getProducts(page, size, category) } returns productResponses

        // When
        val result = productService.getProducts(page, size, category)

        // Then
        assertEquals(2, result.size)
        assertEquals(1L, result[0].id)
        assertEquals(2L, result[1].id)
        assertEquals(category, result[0].category)
        assertEquals(category, result[1].category)
        verify(exactly = 1) { productServiceClient.getProducts(page, size, category) }
    }

    @Test
    fun `should update product successfully`() {
        // Given
        val productId = 1L
        val updateRequest = ProductCreateRequest(
            name = "Updated Product",
            description = "Updated Description",
            price = 299.99,
            category = "Electronics"
        )
        val productResponse = ProductResponse(
            id = productId,
            name = updateRequest.name,
            description = updateRequest.description,
            price = updateRequest.price,
            category = updateRequest.category,
            available = true,
            createdAt = LocalDateTime.now()
        )
        every { productServiceClient.updateProduct(productId, any()) } returns productResponse

        // When
        val result = productService.updateProduct(productId, updateRequest)

        // Then
        assertEquals(productId, result.id)
        assertEquals("Updated Product", result.name)
        assertEquals("Updated Description", result.description)
        assertEquals(299.99, result.price)
        verify(exactly = 1) { productServiceClient.updateProduct(productId, any()) }
    }

    @Test
    fun `should delete product successfully`() {
        // Given
        val productId = 1L
        every { productServiceClient.deleteProduct(productId) } just runs

        // When
        productService.deleteProduct(productId)

        // Then
        verify(exactly = 1) { productServiceClient.deleteProduct(productId) }
    }

    @Test
    fun `should get products by category successfully`() {
        // Given
        val category = "Electronics"
        val productResponses = listOf(
            ProductResponse(1L, "Product 1", "Description 1", 99.99, category, true, LocalDateTime.now()),
            ProductResponse(2L, "Product 2", "Description 2", 149.99, category, true, LocalDateTime.now())
        )
        every { productServiceClient.getProductsByCategory(category) } returns productResponses

        // When
        val result = productService.getProductsByCategory(category)

        // Then
        assertEquals(2, result.size)
        assertEquals(category, result[0].category)
        assertEquals(category, result[1].category)
        verify(exactly = 1) { productServiceClient.getProductsByCategory(category) }
    }

    @Test
    fun `should use fallback when circuit breaker is open`() {
        // Given
        val productId = 1L
        every { productServiceClient.getProductById(productId) } throws RuntimeException("Service unavailable")

        // When
        val result = productService.getProductByIdFallback(productId, RuntimeException("Circuit breaker open"))

        // Then
        assertEquals(productId, result.id)
        assertEquals("Product Unavailable", result.name)
        assertEquals("Service temporarily unavailable", result.description)
        assertEquals(0.0, result.price)
        assertEquals("UNKNOWN", result.category)
        assertFalse(result.available)
    }

    @Test
    fun `should use fallback for get products when service fails`() {
        // Given
        val page = 0
        val size = 10
        val category = "Electronics"

        // When
        val result = productService.getProductsFallback(page, size, category, RuntimeException("Service down"))

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `should throw exception in create product fallback`() {
        // Given
        val createRequest = ProductCreateRequest("Test", "Test", 99.99, "Electronics")

        // When & Then
        val exception = assertThrows<RuntimeException> {
            productService.createProductFallback(createRequest, RuntimeException("Service down"))
        }
        assertEquals("Product creation is currently unavailable", exception.message)
    }

    @Test
    fun `should throw exception in update product fallback`() {
        // Given
        val productId = 1L
        val updateRequest = ProductCreateRequest("Test", "Test", 99.99, "Electronics")

        // When & Then
        val exception = assertThrows<RuntimeException> {
            productService.updateProductFallback(productId, updateRequest, RuntimeException("Service down"))
        }
        assertEquals("Product update is currently unavailable", exception.message)
    }

    @Test
    fun `should throw exception in delete product fallback`() {
        // Given
        val productId = 1L

        // When & Then
        val exception = assertThrows<RuntimeException> {
            productService.deleteProductFallback(productId, RuntimeException("Service down"))
        }
        assertEquals("Product deletion is currently unavailable", exception.message)
    }

    @Test
    fun `should use fallback for get products by category when service fails`() {
        // Given
        val category = "Electronics"

        // When
        val result = productService.getProductsByCategoryFallback(category, RuntimeException("Service down"))

        // Then
        assertTrue(result.isEmpty())
    }
}