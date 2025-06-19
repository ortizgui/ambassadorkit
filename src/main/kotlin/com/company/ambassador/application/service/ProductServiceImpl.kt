package com.company.ambassador.application.service

import com.company.ambassador.domain.model.Product
import com.company.ambassador.domain.model.ProductCreateRequest
import com.company.ambassador.infrastructure.client.product.ProductServiceClient
import com.company.ambassador.infrastructure.client.product.dto.ProductCreateRequest as ClientProductCreateRequest
import com.company.ambassador.infrastructure.client.product.dto.ProductResponse
import com.company.ambassador.config.RateLimiterConfig
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
    override fun getProductById(id: Long): Product {
        checkRateLimit("product-service")
        logger.info("Fetching product with id: $id")
        val response = productServiceClient.getProductById(id)
        return response.toDomain()
    }

    @Cacheable(value = ["products"], key = "'page:' + #page + ':size:' + #size + ':category:' + #category")
    @CircuitBreaker(name = "product-service", fallbackMethod = "getProductsFallback")
    @Retry(name = "product-service")
    override fun getProducts(page: Int, size: Int, category: String?): List<Product> {
        checkRateLimit("product-service")
        logger.info("Fetching products - page: $page, size: $size, category: $category")
        val responses = productServiceClient.getProducts(page, size, category)
        return responses.map { it.toDomain() }
    }

    @CacheEvict(value = ["products"], allEntries = true)
    @CircuitBreaker(name = "product-service", fallbackMethod = "createProductFallback")
    @Retry(name = "product-service")
    override fun createProduct(request: ProductCreateRequest): Product {
        checkRateLimit("product-service")
        logger.info("Creating product: ${request.name}")
        val outputRequest = request.toClientRequest()
        val response = productServiceClient.createProduct(outputRequest)
        return response.toDomain()
    }

    @CacheEvict(value = ["products"], key = "#id")
    @CircuitBreaker(name = "product-service", fallbackMethod = "updateProductFallback")
    @Retry(name = "product-service")
    override fun updateProduct(id: Long, request: ProductCreateRequest): Product {
        checkRateLimit("product-service")
        logger.info("Updating product with id: $id")
        val outputRequest = request.toClientRequest()
        val response = productServiceClient.updateProduct(id, outputRequest)
        return response.toDomain()
    }

    @CacheEvict(value = ["products"], key = "#id")
    @CircuitBreaker(name = "product-service", fallbackMethod = "deleteProductFallback")
    @Retry(name = "product-service")
    override fun deleteProduct(id: Long) {
        checkRateLimit("product-service")
        logger.info("Deleting product with id: $id")
        productServiceClient.deleteProduct(id)
    }

    @Cacheable(value = ["product-category"], key = "#category")
    @CircuitBreaker(name = "product-service", fallbackMethod = "getProductsByCategoryFallback")
    @Retry(name = "product-service")
    override fun getProductsByCategory(category: String): List<Product> {
        checkRateLimit("product-service")
        logger.info("Fetching products by category: $category")
        val responses = productServiceClient.getProductsByCategory(category)
        return responses.map { it.toDomain() }
    }

    private fun checkRateLimit(serviceName: String) {
        val bucket = rateLimiterConfig.getBucket(serviceName)
        if (bucket != null && !bucket.tryConsume(1)) {
            throw RuntimeException("Rate limit exceeded for service: $serviceName")
        }
    }

    private fun ProductResponse.toDomain(): Product {
        return Product(
            id = this.id,
            name = this.name,
            description = this.description,
            price = this.price,
            category = this.category,
            available = this.available,
            createdAt = this.createdAt
        )
    }

    private fun ProductCreateRequest.toClientRequest(): ClientProductCreateRequest {
        return ClientProductCreateRequest(
            name = this.name,
            description = this.description,
            price = this.price,
            category = this.category
        )
    }

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