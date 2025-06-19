package com.company.ambassador.infrastructure.client.product

import com.company.ambassador.infrastructure.client.product.dto.ProductResponse
import com.company.ambassador.infrastructure.client.product.dto.ProductCreateRequest
import com.company.ambassador.infrastructure.config.feign.ProductServiceFeignConfig
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.*

@FeignClient(
    name = "product-service",
    url = "\${external-services.product-service.url}",
    configuration = [ProductServiceFeignConfig::class]
)
interface ProductServiceClient {

    @GetMapping("/products/{id}")
    fun getProductById(@PathVariable id: Long): ProductResponse

    @GetMapping("/products")
    fun getProducts(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(required = false) category: String?
    ): List<ProductResponse>

    @PostMapping("/products")
    fun createProduct(@RequestBody request: ProductCreateRequest): ProductResponse

    @PutMapping("/products/{id}")
    fun updateProduct(@PathVariable id: Long, @RequestBody request: ProductCreateRequest): ProductResponse

    @DeleteMapping("/products/{id}")
    fun deleteProduct(@PathVariable id: Long)

    @GetMapping("/products/category/{category}")
    fun getProductsByCategory(@PathVariable category: String): List<ProductResponse>
}