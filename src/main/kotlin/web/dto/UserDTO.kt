package com.company.ambassador.web.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

data class UserResponseDTO(
    val id: Long,
    val name: String,
    val email: String,
    val status: String,
    val createdAt: LocalDateTime?
)

data class UserCreateRequestDTO(
    @field:NotBlank(message = "Name is required")
    @field:Size(min = 3, max = 100, message = "Name must be between 3 and 100 characters")
    val name: String,

    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Invalid email format")
    val email: String
)

data class UserUpdateRequestDTO(
    @field:Size(min = 3, max = 100, message = "Name must be between 3 and 100 characters")
    val name: String? = null,

    @field:Email(message = "Invalid email format")
    val email: String? = null
) 