package com.company.ambassador.infrastructure.client

import com.company.ambassador.domain.model.Product
import com.company.ambassador.domain.model.ProductCreateRequest
import com.company.ambassador.domain.model.User
import com.company.ambassador.domain.model.UserCreateRequest
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.*

@FeignClient(
    name = "user-service",
    url = "\${external-services.user-service.url}",
    configuration = [UserServiceFeignConfig::class]
)
interface UserServiceClient {

    @GetMapping("/users/{id}")
    fun getUserById(@PathVariable id: Long): User

    @GetMapping("/users")
    fun getUsers(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): List<User>

    @PostMapping("/users")
    fun createUser(@RequestBody request: UserCreateRequest): User

    @PutMapping("/users/{id}")
    fun updateUser(@PathVariable id: Long, @RequestBody request: UserCreateRequest): User

    @DeleteMapping("/users/{id}")
    fun deleteUser(@PathVariable id: Long)

    @GetMapping("/users/search")
    fun searchUsers(@RequestParam query: String): List<User>
}

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