package com.company.ambassador.infrastructure.client.product

import com.company.ambassador.domain.model.product.Product
import com.company.ambassador.domain.model.product.ProductCreateRequest
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.*

@FeignClient(
    name = "product-service",
    url = "\${external-services.product-service.url}",
    configuration = [ProductServiceFeignConfig::class]
)
interface ProductServiceClient {

    @GetMapping("/products/{id}")
    fun getProductById(@PathVariable id: Long): Product

    @GetMapping("/products")
    fun getProducts(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(required = false) category: String?
    ): List<Product>

    @PostMapping("/products")
    fun createProduct(@RequestBody request: ProductCreateRequest): Product

    @PutMapping("/products/{id}")
    fun updateProduct(@PathVariable id: Long, @RequestBody request: ProductCreateRequest): Product

    @DeleteMapping("/products/{id}")
    fun deleteProduct(@PathVariable id: Long)

    @GetMapping("/products/category/{category}")
    fun getProductsByCategory(@PathVariable category: String): List<Product>
} 