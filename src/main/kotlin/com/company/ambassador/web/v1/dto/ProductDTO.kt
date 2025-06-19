package com.company.ambassador.web.v1.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

data class ProductResponseDTO(
    val id: Long,
    val name: String,
    val description: String,
    val price: Double,
    val category: String,
    val available: Boolean,
    val createdAt: LocalDateTime?
)

data class ProductCreateRequestDTO(
    @field:NotBlank(message = "Name is required")
    @field:Size(min = 3, max = 100, message = "Name must be between 3 and 100 characters")
    val name: String,

    @field:NotBlank(message = "Description is required")
    @field:Size(max = 500, message = "Description must not exceed 500 characters")
    val description: String,

    @field:NotNull(message = "Price is required")
    @field:Positive(message = "Price must be positive")
    val price: Double,

    @field:NotBlank(message = "Category is required")
    val category: String
)

data class ProductUpdateRequestDTO(
    @field:Size(min = 3, max = 100, message = "Name must be between 3 and 100 characters")
    val name: String? = null,

    @field:Size(max = 500, message = "Description must not exceed 500 characters")
    val description: String? = null,

    @field:Positive(message = "Price must be positive")
    val price: Double? = null,

    val category: String? = null,

    val available: Boolean? = null
)