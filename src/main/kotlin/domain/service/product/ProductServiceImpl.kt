package com.company.ambassador.domain.service.product

import com.company.ambassador.domain.model.product.Product
import com.company.ambassador.domain.model.product.ProductCreateRequest
import com.company.ambassador.infrastructure.client.product.ProductServiceClient
import com.company.ambassador.infrastructure.config.ratelimit.RateLimiterConfig
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.retry.annotation.Retry
import io.github.resilience4j.timelimiter.annotation.TimeLimiter
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
class ProductServiceImpl(
    private val productServiceClient: ProductServiceClient,
    private val rateLimiterConfig: RateLimiterConfig
) : ProductService {

    private val logger = LoggerFactory.getLogger(ProductServiceImpl::class.java)

    @Cacheable(value = ["products"], key = "#id")
    @CircuitBreaker(name = "product-service", fallbackMethod = "getProductByIdFallback")
    @Retry(name = "product-service")
    @TimeLimiter(name = "product-service")
    override fun getProductById(id: Long): Product {
        checkRateLimit("product-service")
        logger.info("Fetching product with id: $id")
        return productServiceClient.getProductById(id)
    }

    @Cacheable(value = ["products"], key = "'page:' + #page + ':size:' + #size + ':category:' + #category")
    @CircuitBreaker(name = "product-service", fallbackMethod = "getProductsFallback")
    @Retry(name = "product-service")
    @TimeLimiter(name = "product-service")
    override fun getProducts(page: Int, size: Int, category: String?): List<Product> {
        checkRateLimit("product-service")
        logger.info("Fetching products - page: $page, size: $size, category: $category")
        return productServiceClient.getProducts(page, size, category)
    }

    @CacheEvict(value = ["products"], allEntries = true)
    @CircuitBreaker(name = "product-service", fallbackMethod = "createProductFallback")
    @Retry(name = "product-service")
    @TimeLimiter(name = "product-service")
    override fun createProduct(request: ProductCreateRequest): Product {
        checkRateLimit("product-service")
        logger.info("Creating product: ${request.name}")
        return productServiceClient.createProduct(request)
    }

    @CacheEvict(value = ["products"], key = "#id")
    @CircuitBreaker(name = "product-service", fallbackMethod = "updateProductFallback")
    @Retry(name = "product-service")
    @TimeLimiter(name = "product-service")
    override fun updateProduct(id: Long, request: ProductCreateRequest): Product {
        checkRateLimit("product-service")
        logger.info("Updating product with id: $id")
        return productServiceClient.updateProduct(id, request)
    }

    @CacheEvict(value = ["products"], key = "#id")
    @CircuitBreaker(name = "product-service", fallbackMethod = "deleteProductFallback")
    @Retry(name = "product-service")
    @TimeLimiter(name = "product-service")
    override fun deleteProduct(id: Long) {
        checkRateLimit("product-service")
        logger.info("Deleting product with id: $id")
        productServiceClient.deleteProduct(id)
    }

    @Cacheable(value = ["product-category"], key = "#category")
    @CircuitBreaker(name = "product-service", fallbackMethod = "getProductsByCategoryFallback")
    @Retry(name = "product-service")
    @TimeLimiter(name = "product-service")
    override fun getProductsByCategory(category: String): List<Product> {
        checkRateLimit("product-service")
        logger.info("Fetching products by category: $category")
        return productServiceClient.getProductsByCategory(category)
    }

    private fun checkRateLimit(serviceName: String) {
        val bucket = rateLimiterConfig.getBucket(serviceName)
        if (bucket != null && !bucket.tryConsume(1)) {
            throw RuntimeException("Rate limit exceeded for service: $serviceName")
        }
    }

    // Fallback methods
    fun getProductByIdFallback(id: Long, ex: Exception): Product {
        logger.warn("Fallback triggered for getProductById($id): ${ex.message}")
        return Product(
            id = id,
            name = "Product Unavailable",
            description = "Service temporarily unavailable",
            price = 0.0,
            category = "UNKNOWN",
            available = false
        )
    }

    fun getProductsFallback(page: Int, size: Int, category: String?, ex: Exception): List<Product> {
        logger.warn("Fallback triggered for getProducts($page, $size, $category): ${ex.message}")
        return emptyList()
    }

    fun createProductFallback(request: ProductCreateRequest, ex: Exception): Product {
        logger.warn("Fallback triggered for createProduct: ${ex.message}")
        throw RuntimeException("Product creation is currently unavailable")
    }

    fun updateProductFallback(id: Long, request: ProductCreateRequest, ex: Exception): Product {
        logger.warn("Fallback triggered for updateProduct($id): ${ex.message}")
        throw RuntimeException("Product update is currently unavailable")
    }

    fun deleteProductFallback(id: Long, ex: Exception) {
        logger.warn("Fallback triggered for deleteProduct($id): ${ex.message}")
        throw RuntimeException("Product deletion is currently unavailable")
    }

    fun getProductsByCategoryFallback(category: String, ex: Exception): List<Product> {
        logger.warn("Fallback triggered for getProductsByCategory($category): ${ex.message}")
        return emptyList()
    }
} 