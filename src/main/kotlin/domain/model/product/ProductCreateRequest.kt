package com.company.ambassador.domain.model.product

data class ProductCreateRequest(
    val name: String,
    val description: String,
    val price: Double,
    val category: String
) 