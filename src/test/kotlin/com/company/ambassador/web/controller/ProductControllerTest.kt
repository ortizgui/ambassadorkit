package com.company.ambassador.web.controller

import com.company.ambassador.domain.model.product.Product
import com.company.ambassador.domain.model.product.ProductCreateRequest
import com.company.ambassador.domain.service.product.ProductService
import com.company.ambassador.web.dto.ApiResponseDTO
import com.company.ambassador.web.dto.ProductCreateRequestDTO
import com.company.ambassador.web.dto.ProductResponseDTO
import com.company.ambassador.web.dto.ProductUpdateRequestDTO
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
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

    @MockBean
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
            createdAt = LocalDateTime.now()
        )
        `when`(productService.getProductById(productId)).thenReturn(product)

        // When/Then
        mockMvc.perform(get("/api/v1/products/$productId"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(productId))
            .andExpect(jsonPath("$.data.name").value(product.name))
            .andExpect(jsonPath("$.data.description").value(product.description))
            .andExpect(jsonPath("$.data.price").value(product.price))
            .andExpect(jsonPath("$.data.category").value(product.category))
    }

    @Test
    fun `should get products with pagination successfully`() {
        // Given
        val page = 0
        val size = 10
        val category = "Electronics"
        val products = listOf(
            Product(
                id = 1L,
                name = "Product 1",
                description = "Description 1",
                price = 99.99,
                category = category
            ),
            Product(
                id = 2L,
                name = "Product 2",
                description = "Description 2",
                price = 149.99,
                category = category
            )
        )
        `when`(productService.getProducts(page, size, category)).thenReturn(products)

        // When/Then
        mockMvc.perform(get("/api/v1/products")
                .param("page", page.toString())
                .param("size", size.toString())
                .param("category", category))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray)
            .andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.data[0].id").value(1))
            .andExpect(jsonPath("$.data[1].id").value(2))
    }

    @Test
    fun `should delete product successfully`() {
        // Given
        val productId = 1L
        doNothing().`when`(productService).deleteProduct(productId)

        // When/Then
        mockMvc.perform(delete("/api/v1/products/$productId"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("Product deleted successfully"))
    }

    @Test
    fun `should get products by category successfully`() {
        // Given
        val category = "Electronics"
        val products = listOf(
            Product(
                id = 1L,
                name = "Product 1",
                description = "Description 1",
                price = 99.99,
                category = category
            ),
            Product(
                id = 2L,
                name = "Product 2",
                description = "Description 2",
                price = 149.99,
                category = category
            )
        )
        `when`(productService.getProductsByCategory(category)).thenReturn(products)

        // When/Then
        mockMvc.perform(get("/api/v1/products/category/$category"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray)
            .andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.data[0].category").value(category))
            .andExpect(jsonPath("$.data[1].category").value(category))
    }
} 