package com.company.ambassador.application.service

import com.company.ambassador.domain.model.User
import com.company.ambassador.domain.model.UserCreateRequest
import com.company.ambassador.infrastructure.client.user.UserServiceClient
import com.company.ambassador.infrastructure.client.user.dto.UserCreateRequest as ClientUserCreateRequest
import com.company.ambassador.infrastructure.client.user.dto.UserResponse
import com.company.ambassador.config.RateLimiterConfig
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.retry.annotation.Retry
import io.github.resilience4j.timelimiter.annotation.TimeLimiter
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
class UserServiceImpl(
    private val userServiceClient: UserServiceClient,
    private val rateLimiterConfig: RateLimiterConfig
) : UserService {

    private val logger = LoggerFactory.getLogger(UserServiceImpl::class.java)

    @Cacheable(value = ["users"], key = "#id")
    @CircuitBreaker(name = "user-service", fallbackMethod = "getUserByIdFallback")
    @Retry(name = "user-service")
    override fun getUserById(id: Long): User {
        checkRateLimit("user-service")
        logger.info("Fetching user with id: $id")
        val response = userServiceClient.getUserById(id)
        return response.toDomain()
    }

    @Cacheable(value = ["users"], key = "'page:' + #page + ':size:' + #size")
    @CircuitBreaker(name = "user-service", fallbackMethod = "getUsersFallback")
    @Retry(name = "user-service")
    override fun getUsers(page: Int, size: Int): List<User> {
        checkRateLimit("user-service")
        logger.info("Fetching users - page: $page, size: $size")
        val responses = userServiceClient.getUsers(page, size)
        return responses.map { it.toDomain() }
    }

    @CacheEvict(value = ["users"], allEntries = true)
    @CircuitBreaker(name = "user-service", fallbackMethod = "createUserFallback")
    @Retry(name = "user-service")
    override fun createUser(request: UserCreateRequest): User {
        checkRateLimit("user-service")
        logger.info("Creating user: ${request.name}")
        val outputRequest = request.toClientRequest()
        val response = userServiceClient.createUser(outputRequest)
        return response.toDomain()
    }

    @CacheEvict(value = ["users"], key = "#id")
    @CircuitBreaker(name = "user-service", fallbackMethod = "updateUserFallback")
    @Retry(name = "user-service")
    override fun updateUser(id: Long, request: UserCreateRequest): User {
        checkRateLimit("user-service")
        logger.info("Updating user with id: $id")
        val outputRequest = request.toClientRequest()
        val response = userServiceClient.updateUser(id, outputRequest)
        return response.toDomain()
    }

    @CacheEvict(value = ["users"], key = "#id")
    @CircuitBreaker(name = "user-service", fallbackMethod = "deleteUserFallback")
    @Retry(name = "user-service")
    override fun deleteUser(id: Long) {
        checkRateLimit("user-service")
        logger.info("Deleting user with id: $id")
        userServiceClient.deleteUser(id)
    }

    @Cacheable(value = ["user-search"], key = "#query")
    @CircuitBreaker(name = "user-service", fallbackMethod = "searchUsersFallback")
    @Retry(name = "user-service")
    override fun searchUsers(query: String): List<User> {
        checkRateLimit("user-service")
        logger.info("Searching users with query: $query")
        val responses = userServiceClient.searchUsers(query)
        return responses.map { it.toDomain() }
    }

    private fun checkRateLimit(serviceName: String) {
        val bucket = rateLimiterConfig.getBucket(serviceName)
        if (bucket != null && !bucket.tryConsume(1)) {
            throw RuntimeException("Rate limit exceeded for service: $serviceName")
        }
    }

    private fun UserResponse.toDomain(): User {
        return User(
            id = this.id,
            name = this.name,
            email = this.email,
            status = this.status,
            createdAt = this.createdAt
        )
    }

    private fun UserCreateRequest.toClientRequest(): ClientUserCreateRequest {
        return ClientUserCreateRequest(
            name = this.name,
            email = this.email
        )
    }

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