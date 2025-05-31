package com.company.ambassador.presentation.controller

import com.company.ambassador.domain.model.*
import com.company.ambassador.domain.service.UserService
import com.company.ambassador.domain.service.ProductService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import javax.validation.Valid
import javax.validation.constraints.Min
import javax.validation.constraints.NotBlank

@RestController
@RequestMapping("/api/v1/users")
@Validated
@Tag(name = "User Management", description = "Ambassador service for user operations")
class UserController(
    private val userService: UserService
) {

    private val logger = LoggerFactory.getLogger(UserController::class.java)

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID", description = "Retrieves a user by their unique identifier")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "User found successfully"),
        ApiResponse(responseCode = "404", description = "User not found"),
        ApiResponse(responseCode = "500", description = "Internal server error")
    ])
    fun getUserById(
        @Parameter(description = "User ID", required = true)
        @PathVariable @Min(1) id: Long
    ): ResponseEntity<ApiResponse<User>> {
        logger.info("Request to get user by id: $id")
        return try {
            val user = userService.getUserById(id)
            ResponseEntity.ok(ApiResponse(success = true, data = user, message = "User retrieved successfully"))
        } catch (ex: Exception) {
            logger.error("Error getting user by id $id: ${ex.message}", ex)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse(success = false, message = "Failed to retrieve user: ${ex.message}"))
        }
    }

    @GetMapping
    @Operation(summary = "Get users with pagination", description = "Retrieves a paginated list of users")
    fun getUsers(
        @Parameter(description = "Page number (0-based)")
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @Parameter(description = "Page size")
        @RequestParam(defaultValue = "10") @Min(1) size: Int
    ): ResponseEntity<ApiResponse<List<User>>> {
        logger.info("Request to get users - page: $page, size: $size")
        return try {
            val users = userService.getUsers(page, size)
            ResponseEntity.ok(ApiResponse(success = true, data = users, message = "Users retrieved successfully"))
        } catch (ex: Exception) {
            logger.error("Error getting users: ${ex.message}", ex)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse(success = false, message = "Failed to retrieve users: ${ex.message}"))
        }
    }

    @PostMapping
    @Operation(summary = "Create new user", description = "Creates a new user")
    fun createUser(
        @Parameter(description = "User creation request", required = true)
        @Valid @RequestBody request: UserCreateRequest
    ): ResponseEntity<ApiResponse<User>> {
        logger.info("Request to create user: ${request.name}")
        return try {
            val user = userService.createUser(request)
            ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse(success = true, data = user, message = "User created successfully"))
        } catch (ex: Exception) {
            logger.error("Error creating user: ${ex.message}", ex)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse(success = false, message = "Failed to create user: ${ex.message}"))
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update user", description = "Updates an existing user")
    fun updateUser(
        @Parameter(description = "User ID", required = true)
        @PathVariable @Min(1) id: Long,
        @Parameter(description = "User update request", required = true)
        @Valid @RequestBody request: UserCreateRequest
    ): ResponseEntity<ApiResponse<User>> {
        logger.info("Request to update user with id: $id")
        return try {
            val user = userService.updateUser(id, request)
            ResponseEntity.ok(ApiResponse(success = true, data = user, message = "User updated successfully"))
        } catch (ex: Exception) {
            logger.error("Error updating user $id: ${ex.message}", ex)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse(success = false, message = "Failed to update user: ${ex.message}"))
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete user", description = "Deletes a user by ID")
    fun deleteUser(
        @Parameter(description = "User ID", required = true)
        @PathVariable @Min(1) id: Long
    ): ResponseEntity<ApiResponse<Unit>> {
        logger.info("Request to delete user with id: $id")
        return try {
            userService.deleteUser(id)
            ResponseEntity.ok(ApiResponse(success = true, message = "User deleted successfully"))
        } catch (ex: Exception) {
            logger.error("Error deleting user $id: ${ex.message}", ex)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse(success = false, message = "Failed to delete user: ${ex.message}"))
        }
    }

    @GetMapping("/search")
    @Operation(summary = "Search users", description = "Searches users by query string")
    fun searchUsers(
        @Parameter(description = "Search query", required = true)
        @RequestParam @NotBlank query: String
    ): ResponseEntity<ApiResponse<List<User>>> {
        logger.info("Request to search users with query: $query")
        return try {
            val users = userService.searchUsers(query)
            ResponseEntity.ok(ApiResponse(success = true, data = users, message = "Search completed successfully"))
        } catch (ex: Exception) {
            logger.error("Error searching users: ${ex.message}", ex)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse(success = false, message = "Failed to search users: ${ex.message}"))
        }
    }
}

@RestController
@RequestMapping("/api/v1/products")
@Validated
@Tag(name = "Product Management", description = "Ambassador service for product operations")
class ProductController(
    private val productService: ProductService
) {

    private val logger = LoggerFactory.getLogger(ProductController::class.java)

    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID", description = "Retrieves a product by its unique identifier")
    fun getProductById(
        @Parameter(description = "Product ID", required = true)
        @PathVariable @Min(1) id: Long
    ): ResponseEntity<ApiResponse<Product>> {
        logger.info("Request to get product by id: $id")
        return try {
            val product = productService.getProductById(id)
            ResponseEntity.ok(ApiResponse(success = true, data = product, message = "Product retrieved successfully"))
        } catch (ex: Exception) {
            logger.error("Error getting product by id $id: ${ex.message}", ex)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse(success = false, message = "Failed to retrieve product: ${ex.message}"))
        }
    }

    @GetMapping
    @Operation(summary = "Get products with pagination", description = "Retrieves a paginated list of products")
    fun getProducts(
        @Parameter(description = "Page number (0-based)")
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @Parameter(description = "Page size")
        @RequestParam(defaultValue = "10") @Min(1) size: Int,
        @Parameter(description = "Product category filter")
        @RequestParam(required = false) category: String?
    ): ResponseEntity<ApiResponse<List<Product>>> {
        logger.info("Request to get products - page: $page, size: $size, category: $category")
        return try {
            val products = productService.getProducts(page, size, category)
            ResponseEntity.ok(ApiResponse(success = true, data = products, message = "Products retrieved successfully"))
        } catch (ex: Exception) {
            logger.error("Error getting products: ${ex.message}", ex)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse(success = false, message = "Failed to retrieve products: ${ex.message}"))
        }
    }

    @PostMapping
    @Operation(summary = "Create new product", description = "Creates a new product")
    fun createProduct(
        @Parameter(description = "Product creation request", required = true)
        @Valid @RequestBody request: ProductCreateRequest
    ): ResponseEntity<ApiResponse<Product>> {
        logger.info("Request to create product: ${request.name}")
        return try {
            val product = productService.createProduct(request)
            ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse(success = true, data = product, message = "Product created successfully"))
        } catch (ex: Exception) {
            logger.error("Error creating product: ${ex.message}", ex)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse(success = false, message = "Failed to create product: ${ex.message}"))
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update product", description = "Updates an existing product")
    fun updateProduct(
        @Parameter(description = "Product ID", required = true)
        @PathVariable @Min(1) id: Long,
        @Parameter(description = "Product update request", required = true)
        @Valid @RequestBody request: ProductCreateRequest
    ): ResponseEntity<ApiResponse<Product>> {
        logger.info("Request to update product with id: $id")
        return try {
            val product = productService.updateProduct(id, request)
            ResponseEntity.ok(ApiResponse(success = true, data = product, message = "Product updated successfully"))
        } catch (ex: Exception) {
            logger.error("Error updating product $id: ${ex.message}", ex)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse(success = false, message = "Failed to update product: ${ex.message}"))
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete product", description = "Deletes a product by ID")
    fun deleteProduct(
        @Parameter(description = "Product ID", required = true)
        @PathVariable @Min(1) id: Long
    ): ResponseEntity<ApiResponse<Unit>> {
        logger.info("Request to delete product with id: $id")
        return try {
            productService.deleteProduct(id)
            ResponseEntity.ok(ApiResponse(success = true, message = "Product deleted successfully"))
        } catch (ex: Exception) {
            logger.error("Error deleting product $id: ${ex.message}", ex)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse(success = false, message = "Failed to delete product: ${ex.message}"))
        }
    }

    @GetMapping("/category/{category}")
    @Operation(summary = "Get products by category", description = "Retrieves products filtered by category")
    fun getProductsByCategory(
        @Parameter(description = "Product category", required = true)
        @PathVariable @NotBlank category: String
    ): ResponseEntity<ApiResponse<List<Product>>> {
        logger.info("Request to get products by category: $category")
        return try {
            val products = productService.getProductsByCategory(category)
            ResponseEntity.ok(ApiResponse(success = true, data = products, message = "Products retrieved successfully"))
        } catch (ex: Exception) {
            logger.error("Error getting products by category $category: ${ex.message}", ex)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse(success = false, message = "Failed to retrieve products: ${ex.message}"))
        }
    }
}