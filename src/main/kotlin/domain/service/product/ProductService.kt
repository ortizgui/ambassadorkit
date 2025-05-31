package com.company.ambassador.domain.service.product

import com.company.ambassador.domain.model.product.Product
import com.company.ambassador.domain.model.product.ProductCreateRequest

interface ProductService {
    fun getProductById(id: Long): Product
    fun getProducts(page: Int, size: Int, category: String?): List<Product>
    fun createProduct(request: ProductCreateRequest): Product
    fun updateProduct(id: Long, request: ProductCreateRequest): Product
    fun deleteProduct(id: Long)
    fun getProductsByCategory(category: String): List<Product>
} 