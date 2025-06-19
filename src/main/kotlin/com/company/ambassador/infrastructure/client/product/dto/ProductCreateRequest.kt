package com.company.ambassador.infrastructure.client.product.dto

data class ProductCreateRequest(
    val name: String,
    val description: String,
    val price: Double,
    val category: String
)