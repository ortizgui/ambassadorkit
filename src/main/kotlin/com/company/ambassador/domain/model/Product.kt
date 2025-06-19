package com.company.ambassador.domain.model

import java.time.LocalDateTime

data class Product(
    val id: Long,
    val name: String,
    val description: String,
    val price: Double,
    val category: String,
    val available: Boolean,
    val createdAt: LocalDateTime? = null
)

data class ProductCreateRequest(
    val name: String,
    val description: String,
    val price: Double,
    val category: String
)

data class ProductUpdateRequest(
    val name: String? = null,
    val description: String? = null,
    val price: Double? = null,
    val category: String? = null,
    val available: Boolean? = null
)