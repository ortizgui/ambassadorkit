package com.company.ambassador.web.dto

import com.company.ambassador.domain.model.product.Product
import com.company.ambassador.domain.model.product.ProductCreateRequest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.time.LocalDateTime

class DTOExtensionsTest {

    @Test
    fun `should convert Product to ProductResponseDTO`() {
        // Given
        val now = LocalDateTime.now()
        val product = Product(
            id = 1L,
            name = "Test Product",
            description = "Test Description",
            price = 99.99,
            category = "Electronics",
            available = true,
            createdAt = now
        )

        // When
        val responseDTO = product.toResponseDTO()

        // Then
        assertEquals(product.id, responseDTO.id)
        assertEquals(product.name, responseDTO.name)
        assertEquals(product.description, responseDTO.description)
        assertEquals(product.price, responseDTO.price)
        assertEquals(product.category, responseDTO.category)
        assertEquals(product.available, responseDTO.available)
        assertEquals(product.createdAt, responseDTO.createdAt)
    }

    @Test
    fun `should convert ProductCreateRequestDTO to ProductCreateRequest`() {
        // Given
        val requestDTO = ProductCreateRequestDTO(
            name = "New Product",
            description = "New Description",
            price = 199.99,
            category = "Electronics"
        )

        // When
        val request = requestDTO.toDomainRequest()

        // Then
        assertEquals(requestDTO.name, request.name)
        assertEquals(requestDTO.description, request.description)
        assertEquals(requestDTO.price, request.price)
        assertEquals(requestDTO.category, request.category)
    }

    @Test
    fun `should convert ProductUpdateRequestDTO to ProductCreateRequest with null values`() {
        // Given
        val requestDTO = ProductUpdateRequestDTO(
            name = null,
            description = null,
            price = null,
            category = null,
            available = null
        )

        // When
        val request = requestDTO.toDomainRequest()

        // Then
        assertEquals("", request.name)
        assertEquals("", request.description)
        assertEquals(0.0, request.price)
        assertEquals("", request.category)
    }

    @Test
    fun `should convert ProductUpdateRequestDTO to ProductCreateRequest with non-null values`() {
        // Given
        val requestDTO = ProductUpdateRequestDTO(
            name = "Updated Product",
            description = "Updated Description",
            price = 299.99,
            category = "Electronics",
            available = true
        )

        // When
        val request = requestDTO.toDomainRequest()

        // Then
        assertEquals(requestDTO.name, request.name)
        assertEquals(requestDTO.description, request.description)
        assertEquals(requestDTO.price, request.price)
        assertEquals(requestDTO.category, request.category)
    }
} 