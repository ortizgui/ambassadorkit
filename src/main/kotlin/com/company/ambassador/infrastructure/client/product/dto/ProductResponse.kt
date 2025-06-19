package com.company.ambassador.infrastructure.client.product.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

data class ProductResponse(
    val id: Long,
    val name: String,
    val description: String,
    val price: Double,
    val category: String,
    val available: Boolean = true,
    @JsonProperty("created_at")
    val createdAt: LocalDateTime? = null
)