package com.company.ambassador.domain.service.product

import com.company.ambassador.domain.model.product.Product
import com.company.ambassador.domain.model.product.ProductCreateRequest
import com.company.ambassador.infrastructure.client.product.ProductServiceClient
import com.company.ambassador.infrastructure.config.ratelimit.RateLimiterConfig
import io.github.bucket4j.Bucket
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryRegistry
import io.github.resilience4j.timelimiter.TimeLimiter
import io.github.resilience4j.timelimiter.TimeLimiterRegistry
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.cache.CacheManager
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDateTime
import org.junit.jupiter.api.Assertions.*

@ExtendWith(MockitoExtension::class)
class ProductServiceImplTest {

    @Mock
    private lateinit var productServiceClient: ProductServiceClient

    @Mock
    private lateinit var rateLimiterConfig: RateLimiterConfig

    @Mock
    private lateinit var circuitBreakerRegistry: CircuitBreakerRegistry

    @Mock
    private lateinit var retryRegistry: RetryRegistry

    @Mock
    private lateinit var timeLimiterRegistry: TimeLimiterRegistry

    @Mock
    private lateinit var bucket: Bucket

    private lateinit var productService: ProductServiceImpl
    private lateinit var cacheManager: CacheManager

    @BeforeEach
    fun setup() {
        cacheManager = ConcurrentMapCacheManager("products", "product-category")
        productService = ProductServiceImpl(productServiceClient, rateLimiterConfig)
        
        // Setup mocks
        `when`(rateLimiterConfig.getBucket("product-service")).thenReturn(bucket)
        `when`(bucket.tryConsume(1)).thenReturn(true)
    }

    @Test
    fun `should get product by id successfully`() {
        // Given
        val productId = 1L
        val expectedProduct = Product(
            id = productId,
            name = "Test Product",
            description = "Test Description",
            price = 99.99,
            category = "Test Category",
            createdAt = LocalDateTime.now()
        )
        `when`(productServiceClient.getProductById(productId)).thenReturn(expectedProduct)

        // When
        val result = productService.getProductById(productId)

        // Then
        assertEquals(expectedProduct, result)
        verify(productServiceClient).getProductById(productId)
    }

    @Test
    fun `should get products with pagination successfully`() {
        // Given
        val page = 0
        val size = 10
        val category = "Electronics"
        val expectedProducts = listOf(
            Product(
                id = 1L,
                name = "Product 1",
                description = "Description 1",
                price = 99.99,
                category = category
            ),
            Product(
                id = 2L,
                name = "Product 2",
                description = "Description 2",
                price = 149.99,
                category = category
            )
        )
        `when`(productServiceClient.getProducts(page, size, category)).thenReturn(expectedProducts)

        // When
        val result = productService.getProducts(page, size, category)

        // Then
        assertEquals(expectedProducts, result)
        verify(productServiceClient).getProducts(page, size, category)
    }

    @Test
    fun `should create product successfully`() {
        // Given
        val request = ProductCreateRequest(
            name = "New Product",
            description = "New Description",
            price = 199.99,
            category = "Electronics"
        )
        val expectedProduct = Product(
            id = 1L,
            name = request.name,
            description = request.description,
            price = request.price,
            category = request.category,
            createdAt = LocalDateTime.now()
        )
        `when`(productServiceClient.createProduct(request)).thenReturn(expectedProduct)

        // When
        val result = productService.createProduct(request)

        // Then
        assertEquals(expectedProduct, result)
        verify(productServiceClient).createProduct(request)
    }

    @Test
    fun `should update product successfully`() {
        // Given
        val productId = 1L
        val request = ProductCreateRequest(
            name = "Updated Product",
            description = "Updated Description",
            price = 299.99,
            category = "Electronics"
        )
        val expectedProduct = Product(
            id = productId,
            name = request.name,
            description = request.description,
            price = request.price,
            category = request.category,
            createdAt = LocalDateTime.now()
        )
        `when`(productServiceClient.updateProduct(productId, request)).thenReturn(expectedProduct)

        // When
        val result = productService.updateProduct(productId, request)

        // Then
        assertEquals(expectedProduct, result)
        verify(productServiceClient).updateProduct(productId, request)
    }

    @Test
    fun `should delete product successfully`() {
        // Given
        val productId = 1L
        doNothing().`when`(productServiceClient).deleteProduct(productId)

        // When
        productService.deleteProduct(productId)

        // Then
        verify(productServiceClient).deleteProduct(productId)
    }

    @Test
    fun `should get products by category successfully`() {
        // Given
        val category = "Electronics"
        val expectedProducts = listOf(
            Product(
                id = 1L,
                name = "Product 1",
                description = "Description 1",
                price = 99.99,
                category = category
            ),
            Product(
                id = 2L,
                name = "Product 2",
                description = "Description 2",
                price = 149.99,
                category = category
            )
        )
        `when`(productServiceClient.getProductsByCategory(category)).thenReturn(expectedProducts)

        // When
        val result = productService.getProductsByCategory(category)

        // Then
        assertEquals(expectedProducts, result)
        verify(productServiceClient).getProductsByCategory(category)
    }
} 