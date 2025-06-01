package com.company.ambassador.web.controller

import com.company.ambassador.domain.service.product.ProductService
import com.company.ambassador.web.dto.ApiResponseDTO
import com.company.ambassador.web.dto.ProductCreateRequestDTO
import com.company.ambassador.web.dto.ProductResponseDTO
import com.company.ambassador.web.dto.ProductUpdateRequestDTO
import com.company.ambassador.web.dto.toDomainRequest
import com.company.ambassador.web.dto.toResponseDTO
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

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
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Product found successfully"),
        ApiResponse(responseCode = "404", description = "Product not found"),
        ApiResponse(responseCode = "500", description = "Internal server error")
    ])
    fun getProductById(
        @Parameter(description = "Product ID", required = true)
        @PathVariable @Min(1) id: Long
    ): ResponseEntity<ApiResponseDTO<ProductResponseDTO>> {
        logger.info("Request to get product by id: $id")
        return try {
            val product = productService.getProductById(id)
            ResponseEntity.ok(ApiResponseDTO(
                success = true,
                data = product.toResponseDTO(),
                message = "Product retrieved successfully"
            ))
        } catch (ex: Exception) {
            logger.error("Error getting product by id $id: ${ex.message}", ex)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDTO(
                    success = false,
                    error = "Failed to retrieve product: ${ex.message}"
                ))
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
    ): ResponseEntity<ApiResponseDTO<List<ProductResponseDTO>>> {
        logger.info("Request to get products - page: $page, size: $size, category: $category")
        return try {
            val products = productService.getProducts(page, size, category)
            ResponseEntity.ok(ApiResponseDTO(
                success = true,
                data = products.map { it.toResponseDTO() },
                message = "Products retrieved successfully"
            ))
        } catch (ex: Exception) {
            logger.error("Error getting products: ${ex.message}", ex)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDTO(
                    success = false,
                    error = "Failed to retrieve products: ${ex.message}"
                ))
        }
    }

    @PostMapping
    @Operation(summary = "Create new product", description = "Creates a new product")
    fun createProduct(
        @Parameter(description = "Product creation request", required = true)
        @Valid @RequestBody request: ProductCreateRequestDTO
    ): ResponseEntity<ApiResponseDTO<ProductResponseDTO>> {
        logger.info("Request to create product: ${request.name}")
        return try {
            val product = productService.createProduct(request.toDomainRequest())
            ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDTO(
                    success = true,
                    data = product.toResponseDTO(),
                    message = "Product created successfully"
                ))
        } catch (ex: Exception) {
            logger.error("Error creating product: ${ex.message}", ex)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDTO(
                    success = false,
                    error = "Failed to create product: ${ex.message}"
                ))
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update product", description = "Updates an existing product")
    fun updateProduct(
        @Parameter(description = "Product ID", required = true)
        @PathVariable @Min(1) id: Long,
        @Parameter(description = "Product update request", required = true)
        @Valid @RequestBody request: ProductUpdateRequestDTO
    ): ResponseEntity<ApiResponseDTO<ProductResponseDTO>> {
        logger.info("Request to update product with id: $id")
        return try {
            val product = productService.updateProduct(id, request.toDomainRequest())
            ResponseEntity.ok(ApiResponseDTO(
                success = true,
                data = product.toResponseDTO(),
                message = "Product updated successfully"
            ))
        } catch (ex: Exception) {
            logger.error("Error updating product $id: ${ex.message}", ex)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDTO(
                    success = false,
                    error = "Failed to update product: ${ex.message}"
                ))
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete product", description = "Deletes a product by ID")
    fun deleteProduct(
        @Parameter(description = "Product ID", required = true)
        @PathVariable @Min(1) id: Long
    ): ResponseEntity<ApiResponseDTO<Unit>> {
        logger.info("Request to delete product with id: $id")
        return try {
            productService.deleteProduct(id)
            ResponseEntity.ok(ApiResponseDTO(
                success = true,
                message = "Product deleted successfully"
            ))
        } catch (ex: Exception) {
            logger.error("Error deleting product $id: ${ex.message}", ex)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDTO(
                    success = false,
                    error = "Failed to delete product: ${ex.message}"
                ))
        }
    }

    @GetMapping("/category/{category}")
    @Operation(summary = "Get products by category", description = "Retrieves all products in a specific category")
    fun getProductsByCategory(
        @Parameter(description = "Product category", required = true)
        @PathVariable category: String
    ): ResponseEntity<ApiResponseDTO<List<ProductResponseDTO>>> {
        logger.info("Request to get products by category: $category")
        return try {
            val products = productService.getProductsByCategory(category)
            ResponseEntity.ok(ApiResponseDTO(
                success = true,
                data = products.map { it.toResponseDTO() },
                message = "Products retrieved successfully"
            ))
        } catch (ex: Exception) {
            logger.error("Error getting products by category $category: ${ex.message}", ex)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDTO(
                    success = false,
                    error = "Failed to retrieve products: ${ex.message}"
                ))
        }
    }
} 