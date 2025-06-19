package com.company.ambassador.application.service

import com.company.ambassador.domain.model.Product
import com.company.ambassador.domain.model.ProductCreateRequest

interface ProductService {
    fun getProductById(id: Long): Product
    fun getProducts(page: Int, size: Int, category: String?): List<Product>
    fun createProduct(request: ProductCreateRequest): Product
    fun updateProduct(id: Long, request: ProductCreateRequest): Product
    fun deleteProduct(id: Long)
    fun getProductsByCategory(category: String): List<Product>
}