package com.company.ambassador.web.dto

import com.company.ambassador.domain.model.product.Product
import com.company.ambassador.domain.model.product.ProductCreateRequest
import com.company.ambassador.domain.model.user.User
import com.company.ambassador.domain.model.user.UserCreateRequest
import java.time.LocalDateTime

// User DTO Extensions
fun User.toResponseDTO() = UserResponseDTO(
    id = id,
    name = name,
    email = email,
    status = status,
    createdAt = createdAt
)

fun UserCreateRequestDTO.toDomainRequest() = UserCreateRequest(
    name = name,
    email = email
)

fun UserUpdateRequestDTO.toDomainRequest() = UserCreateRequest(
    name = name ?: "",
    email = email ?: ""
)

// Product DTO Extensions
fun Product.toResponseDTO() = ProductResponseDTO(
    id = id,
    name = name,
    description = description,
    price = price,
    category = category,
    available = available,
    createdAt = createdAt
)

fun ProductCreateRequestDTO.toDomainRequest() = ProductCreateRequest(
    name = name,
    description = description,
    price = price,
    category = category
)

fun ProductUpdateRequestDTO.toDomainRequest() = ProductCreateRequest(
    name = name ?: "",
    description = description ?: "",
    price = price ?: 0.0,
    category = category ?: ""
) 