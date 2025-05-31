package com.company.ambassador.domain.model

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

data class User(
    val id: Long,
    val name: String,
    val email: String,
    val status: String,
    @JsonProperty("created_at")
    val createdAt: LocalDateTime? = null
)

data class Product(
    val id: Long,
    val name: String,
    val description: String,
    val price: Double,
    val category: String,
    val available: Boolean = true,
    @JsonProperty("created_at")
    val createdAt: LocalDateTime? = null
)

data class UserCreateRequest(
    val name: String,
    val email: String
)

data class ProductCreateRequest(
    val name: String,
    val description: String,
    val price: Double,
    val category: String
)

// Response DTOs
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String? = null,
    val timestamp: LocalDateTime = LocalDateTime.now()
)

data class ErrorResponse(
    val error: String,
    val message: String,
    val timestamp: LocalDateTime = LocalDateTime.now()
)