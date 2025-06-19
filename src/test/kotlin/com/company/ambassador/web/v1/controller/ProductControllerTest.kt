package com.company.ambassador.web.v1.controller

import com.company.ambassador.application.service.ProductService
import com.company.ambassador.domain.model.Product
import com.company.ambassador.domain.model.ProductCreateRequest
import com.company.ambassador.web.v1.dto.ProductCreateRequestDTO
import com.company.ambassador.web.v1.dto.ProductUpdateRequestDTO
import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.just
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.LocalDateTime

@WebMvcTest(ProductController::class)
class ProductControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockkBean
    private lateinit var productService: ProductService

    @Test
    fun `should get product by id successfully`() {
        // Given
        val productId = 1L
        val product = Product(
            id = productId,
            name = "Test Product",
            description = "Test Description",
            price = 99.99,
            category = "Electronics",
            available = true,
            createdAt = LocalDateTime.parse("2023-01-01T10:00:00")
        )
        every { productService.getProductById(productId) } returns product

        // When & Then
        mockMvc.perform(get("/api/v1/products/$productId"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(productId))
            .andExpect(jsonPath("$.data.name").value("Test Product"))
            .andExpect(jsonPath("$.data.description").value("Test Description"))
            .andExpect(jsonPath("$.data.price").value(99.99))
            .andExpect(jsonPath("$.data.category").value("Electronics"))
            .andExpect(jsonPath("$.data.available").value(true))

        verify(exactly = 1) { productService.getProductById(productId) }
    }

    @Test
    fun `should handle product not found exception`() {
        // Given
        val productId = 999L
        every { productService.getProductById(productId) } throws RuntimeException("Product not found")

        // When & Then
        mockMvc.perform(get("/api/v1/products/$productId"))
            .andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error").value("Failed to retrieve product: Product not found"))

        verify(exactly = 1) { productService.getProductById(productId) }
    }

    @Test
    fun `should get products with pagination successfully`() {
        // Given
        val products = listOf(
            Product(1L, "Product 1", "Description 1", 99.99, "Electronics", true, LocalDateTime.parse("2023-01-01T10:00:00")),
            Product(2L, "Product 2", "Description 2", 149.99, "Electronics", true, LocalDateTime.parse("2023-01-01T11:00:00"))
        )
        every { productService.getProducts(0, 10, "Electronics") } returns products

        // When & Then
        mockMvc.perform(get("/api/v1/products")
                .param("page", "0")
                .param("size", "10")
                .param("category", "Electronics"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray)
            .andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.data[0].id").value(1))
            .andExpect(jsonPath("$.data[1].id").value(2))

        verify(exactly = 1) { productService.getProducts(0, 10, "Electronics") }
    }

    @Test
    fun `should create product successfully`() {
        // Given
        val request = ProductCreateRequestDTO(
            name = "New Product",
            description = "New Description",
            price = 199.99,
            category = "Electronics"
        )
        val createdProduct = Product(
            id = 1L,
            name = request.name,
            description = request.description,
            price = request.price,
            category = request.category,
            available = true,
            createdAt = LocalDateTime.parse("2023-01-01T10:00:00")
        )
        every { productService.createProduct(any()) } returns createdProduct

        // When & Then
        mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.name").value("New Product"))
            .andExpect(jsonPath("$.data.price").value(199.99))

        verify(exactly = 1) { productService.createProduct(any()) }
    }

    @Test
    fun `should validate product creation request`() {
        // Given
        val invalidRequest = ProductCreateRequestDTO(
            name = "",  // Invalid: empty name
            description = "Description",
            price = -10.0,  // Invalid: negative price
            category = "Electronics"
        )

        // When & Then
        mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `should update product successfully`() {
        // Given
        val productId = 1L
        val request = ProductUpdateRequestDTO(
            name = "Updated Product",
            description = "Updated Description",
            price = 299.99
        )
        val updatedProduct = Product(
            id = productId,
            name = "Updated Product",
            description = "Updated Description",
            price = 299.99,
            category = "Electronics",
            available = true,
            createdAt = LocalDateTime.parse("2023-01-01T10:00:00")
        )
        every { productService.updateProduct(productId, any()) } returns updatedProduct

        // When & Then
        mockMvc.perform(put("/api/v1/products/$productId")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.name").value("Updated Product"))
            .andExpect(jsonPath("$.data.price").value(299.99))

        verify(exactly = 1) { productService.updateProduct(productId, any()) }
    }

    @Test
    fun `should delete product successfully`() {
        // Given
        val productId = 1L
        every { productService.deleteProduct(productId) } just runs

        // When & Then
        mockMvc.perform(delete("/api/v1/products/$productId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("Product deleted successfully"))

        verify(exactly = 1) { productService.deleteProduct(productId) }
    }

    @Test
    fun `should get products by category successfully`() {
        // Given
        val category = "Electronics"
        val products = listOf(
            Product(1L, "Product 1", "Description 1", 99.99, category, true, LocalDateTime.parse("2023-01-01T10:00:00")),
            Product(2L, "Product 2", "Description 2", 149.99, category, true, LocalDateTime.parse("2023-01-01T11:00:00"))
        )
        every { productService.getProductsByCategory(category) } returns products

        // When & Then
        mockMvc.perform(get("/api/v1/products/category/$category"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray)
            .andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.data[0].category").value(category))
            .andExpect(jsonPath("$.data[1].category").value(category))

        verify(exactly = 1) { productService.getProductsByCategory(category) }
    }
}