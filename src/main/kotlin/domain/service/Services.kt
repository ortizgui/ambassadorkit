package com.company.ambassador.domain.service

import com.company.ambassador.domain.model.User
import com.company.ambassador.domain.model.UserCreateRequest
import com.company.ambassador.domain.model.Product
import com.company.ambassador.domain.model.ProductCreateRequest
import com.company.ambassador.infrastructure.client.UserServiceClient
import com.company.ambassador.infrastructure.client.ProductServiceClient
import com.company.ambassador.infrastructure.config.RateLimiterConfig
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.retry.annotation.Retry
import io.github.resilience4j.timelimiter.annotation.TimeLimiter
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture

interface UserService {
    fun getUserById(id: Long): User
    fun getUsers(page: Int, size: Int): List<User>
    fun createUser(request: UserCreateRequest): User
    fun updateUser(id: Long, request: UserCreateRequest): User
    fun deleteUser(id: Long)
    fun searchUsers(query: String): List<User>
}

interface ProductService {
    fun getProductById(id: Long): Product
    fun getProducts(page: Int, size: Int, category: String?): List<Product>
    fun createProduct(request: ProductCreateRequest): Product
    fun updateProduct(id: Long, request: ProductCreateRequest): Product
    fun deleteProduct(id: Long)
    fun getProductsByCategory(category: String): List<Product>
}

@Service
class UserServiceImpl(
    private val userServiceClient: UserServiceClient,
    private val rateLimiterConfig: RateLimiterConfig
) : UserService {

    private val logger = LoggerFactory.getLogger(UserServiceImpl::class.java)

    @Cacheable(value = ["users"], key = "#id")
    @CircuitBreaker(name = "user-service", fallbackMethod = "getUserByIdFallback")
    @Retry(name = "user-service")
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
} "user-service")
override fun getUserById(id: Long): User {
    checkRateLimit("user-service")
    logger.info("Fetching user with id: $id")
    return userServiceClient.getUserById(id)
}

@Cacheable(value = ["users"], key = "'page:' + #page + ':size:' + #size")
@CircuitBreaker(name = "user-service", fallbackMethod = "getUsersFallback")
@Retry(name = "user-service")
@TimeLimiter(name = "user-service")
override fun getUsers(page: Int, size: Int): List<User> {
    checkRateLimit("user-service")
    logger.info("Fetching users - page: $page, size: $size")
    return userServiceClient.getUsers(page, size)
}

@CacheEvict(value = ["users"], allEntries = true)
@CircuitBreaker(name = "user-service", fallbackMethod = "createUserFallback")
@Retry(name = "user-service")
@TimeLimiter(name = "user-service")
override fun createUser(request: UserCreateRequest): User {
    checkRateLimit("user-service")
    logger.info("Creating user: ${request.name}")
    return userServiceClient.createUser(request)
}

@CacheEvict(value = ["users"], key = "#id")
@CircuitBreaker(name = "user-service", fallbackMethod = "updateUserFallback")
@Retry(name = "user-service")
@TimeLimiter(name = "user-service")
override fun updateUser(id: Long, request: UserCreateRequest): User {
    checkRateLimit("user-service")
    logger.info("Updating user with id: $id")
    return userServiceClient.updateUser(id, request)
}

@CacheEvict(value = ["users"], key = "#id")
@CircuitBreaker(name = "user-service", fallbackMethod = "deleteUserFallback")
@Retry(name = "user-service")
@TimeLimiter(name = "user-service")
override fun deleteUser(id: Long) {
    checkRateLimit("user-service")
    logger.info("Deleting user with id: $id")
    userServiceClient.deleteUser(id)
}

@Cacheable(value = ["user-search"], key = "#query")
@CircuitBreaker(name = "user-service", fallbackMethod = "searchUsersFallback")
@Retry(name = "user-service")
@TimeLimiter(name = "user-service")
override fun searchUsers(query: String): List<User> {
    checkRateLimit("user-service")
    logger.info("Searching users with query: $query")
    return userServiceClient.searchUsers(query)
}

private fun checkRateLimit(serviceName: String) {
    val bucket = rateLimiterConfig.getBucket(serviceName)
    if (bucket != null && !bucket.tryConsume(1)) {
        throw RuntimeException("Rate limit exceeded for service: $serviceName")
    }
}

// Fallback methods
fun getUserByIdFallback(id: Long, ex: Exception): User {
    logger.warn("Fallback triggered for getUserById($id): ${ex.message}")
    return User(id = id, name = "Unknown", email = "unknown@example.com", status = "UNAVAILABLE")
}

fun getUsersFallback(page: Int, size: Int, ex: Exception): List<User> {
    logger.warn("Fallback triggered for getUsers($page, $size): ${ex.message}")
    return emptyList()
}

fun createUserFallback(request: UserCreateRequest, ex: Exception): User {
    logger.warn("Fallback triggered for createUser: ${ex.message}")
    throw RuntimeException("User creation is currently unavailable")
}

fun updateUserFallback(id: Long, request: UserCreateRequest, ex: Exception): User {
    logger.warn("Fallback triggered for updateUser($id): ${ex.message}")
    throw RuntimeException("User update is currently unavailable")
}

fun deleteUserFallback(id: Long, ex: Exception) {
    logger.warn("Fallback triggered for deleteUser($id): ${ex.message}")
    throw RuntimeException("User deletion is currently unavailable")
}

fun searchUsersFallback(query: String, ex: Exception): List<User> {
    logger.warn("Fallback triggered for searchUsers($query): ${ex.message}")
    return emptyList()
}
}

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

    @Cacheable(value = ["products"], key = "'page:' + #page + ':size:' + #size + ':category:' + (#category ?: 'all')")
    @CircuitBreaker(name = "product-service", fallbackMethod = "getProductsFallback")
    @Retry(name = "product-service")
    @TimeLimiter(name =