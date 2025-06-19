package com.company.ambassador.web.v1.dto

import com.company.ambassador.domain.model.Product
import com.company.ambassador.domain.model.ProductCreateRequest
import com.company.ambassador.domain.model.User
import com.company.ambassador.domain.model.UserCreateRequest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class DTOExtensionsTest {

    @Test
    fun `should convert Product to ProductResponseDTO correctly`() {
        // Given
        val product = Product(
            id = 1L,
            name = "Test Product",
            description = "Test Description",
            price = 99.99,
            category = "Electronics",
            available = true,
            createdAt = LocalDateTime.parse("2023-01-01T10:00:00")
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
    fun `should convert ProductCreateRequestDTO to domain ProductCreateRequest correctly`() {
        // Given
        val requestDTO = ProductCreateRequestDTO(
            name = "New Product",
            description = "New Description",
            price = 199.99,
            category = "Electronics"
        )

        // When
        val domainRequest = requestDTO.toDomainRequest()

        // Then
        assertEquals(requestDTO.name, domainRequest.name)
        assertEquals(requestDTO.description, domainRequest.description)
        assertEquals(requestDTO.price, domainRequest.price)
        assertEquals(requestDTO.category, domainRequest.category)
    }

    @Test
    fun `should convert ProductUpdateRequestDTO to domain ProductCreateRequest correctly`() {
        // Given
        val updateRequestDTO = ProductUpdateRequestDTO(
            name = "Updated Product",
            description = "Updated Description",
            price = 299.99,
            category = "Updated Category",
            available = false
        )

        // When
        val domainRequest = updateRequestDTO.toDomainRequest()

        // Then
        assertEquals(updateRequestDTO.name, domainRequest.name)
        assertEquals(updateRequestDTO.description, domainRequest.description)
        assertEquals(updateRequestDTO.price, domainRequest.price)
        assertEquals(updateRequestDTO.category, domainRequest.category)
    }

    @Test
    fun `should handle null values in ProductUpdateRequestDTO conversion`() {
        // Given
        val updateRequestDTO = ProductUpdateRequestDTO(
            name = null,
            description = null,
            price = null,
            category = null,
            available = null
        )

        // When
        val domainRequest = updateRequestDTO.toDomainRequest()

        // Then
        assertEquals("", domainRequest.name)
        assertEquals("", domainRequest.description)
        assertEquals(0.0, domainRequest.price)
        assertEquals("", domainRequest.category)
    }

    @Test
    fun `should convert User to UserResponseDTO correctly`() {
        // Given
        val user = User(
            id = 1L,
            name = "John Doe",
            email = "john.doe@example.com",
            status = "ACTIVE",
            createdAt = LocalDateTime.parse("2023-01-01T10:00:00")
        )

        // When
        val responseDTO = user.toResponseDTO()

        // Then
        assertEquals(user.id, responseDTO.id)
        assertEquals(user.name, responseDTO.name)
        assertEquals(user.email, responseDTO.email)
        assertEquals(user.status, responseDTO.status)
        assertEquals(user.createdAt, responseDTO.createdAt)
    }

    @Test
    fun `should convert UserCreateRequestDTO to domain UserCreateRequest correctly`() {
        // Given
        val requestDTO = UserCreateRequestDTO(
            name = "Jane Smith",
            email = "jane.smith@example.com"
        )

        // When
        val domainRequest = requestDTO.toDomainRequest()

        // Then
        assertEquals(requestDTO.name, domainRequest.name)
        assertEquals(requestDTO.email, domainRequest.email)
    }

    @Test
    fun `should convert UserUpdateRequestDTO to domain UserCreateRequest correctly`() {
        // Given
        val updateRequestDTO = UserUpdateRequestDTO(
            name = "Updated User",
            email = "updated@example.com"
        )

        // When
        val domainRequest = updateRequestDTO.toDomainRequest()

        // Then
        assertEquals(updateRequestDTO.name, domainRequest.name)
        assertEquals(updateRequestDTO.email, domainRequest.email)
    }

    @Test
    fun `should handle null values in UserUpdateRequestDTO conversion`() {
        // Given
        val updateRequestDTO = UserUpdateRequestDTO(
            name = null,
            email = null
        )

        // When
        val domainRequest = updateRequestDTO.toDomainRequest()

        // Then
        assertEquals("", domainRequest.name)
        assertEquals("", domainRequest.email)
    }

    @Test
    fun `should handle edge cases for Product conversion`() {
        // Given
        val product = Product(
            id = 0L,
            name = "",
            description = "",
            price = 0.0,
            category = "",
            available = false,
            createdAt = null
        )

        // When
        val responseDTO = product.toResponseDTO()

        // Then
        assertEquals(0L, responseDTO.id)
        assertEquals("", responseDTO.name)
        assertEquals("", responseDTO.description)
        assertEquals(0.0, responseDTO.price)
        assertEquals("", responseDTO.category)
        assertFalse(responseDTO.available)
        assertNull(responseDTO.createdAt)
    }

    @Test
    fun `should handle edge cases for User conversion`() {
        // Given
        val user = User(
            id = 0L,
            name = "",
            email = "",
            status = "",
            createdAt = null
        )

        // When
        val responseDTO = user.toResponseDTO()

        // Then
        assertEquals(0L, responseDTO.id)
        assertEquals("", responseDTO.name)
        assertEquals("", responseDTO.email)
        assertEquals("", responseDTO.status)
        assertNull(responseDTO.createdAt)
    }

    @Test
    fun `should handle special characters in conversion`() {
        // Given
        val product = Product(
            id = 1L,
            name = "Product with special chars: !@#$%^&*()",
            description = "Description with émojis 🎉🚀 and ümlauts",
            price = 99.99,
            category = "Category/Subcategory",
            available = true,
            createdAt = LocalDateTime.parse("2023-01-01T10:00:00")
        )

        // When
        val responseDTO = product.toResponseDTO()

        // Then
        assertEquals("Product with special chars: !@#$%^&*()", responseDTO.name)
        assertEquals("Description with émojis 🎉🚀 and ümlauts", responseDTO.description)
        assertEquals("Category/Subcategory", responseDTO.category)
    }

    @Test
    fun `should handle large numbers in Product conversion`() {
        // Given
        val product = Product(
            id = Long.MAX_VALUE,
            name = "Expensive Product",
            description = "Very expensive",
            price = Double.MAX_VALUE,
            category = "Luxury",
            available = true,
            createdAt = LocalDateTime.parse("2023-01-01T10:00:00")
        )

        // When
        val responseDTO = product.toResponseDTO()

        // Then
        assertEquals(Long.MAX_VALUE, responseDTO.id)
        assertEquals(Double.MAX_VALUE, responseDTO.price)
    }

    @Test
    fun `should convert multiple Products to ResponseDTOs correctly`() {
        // Given
        val products = listOf(
            Product(1L, "Product 1", "Description 1", 99.99, "Category 1", true, LocalDateTime.parse("2023-01-01T10:00:00")),
            Product(2L, "Product 2", "Description 2", 149.99, "Category 2", false, LocalDateTime.parse("2023-01-01T11:00:00")),
            Product(3L, "Product 3", "Description 3", 199.99, "Category 3", true, LocalDateTime.parse("2023-01-01T12:00:00"))
        )

        // When
        val responseDTOs = products.map { it.toResponseDTO() }

        // Then
        assertEquals(3, responseDTOs.size)
        assertEquals("Product 1", responseDTOs[0].name)
        assertEquals("Product 2", responseDTOs[1].name)
        assertEquals("Product 3", responseDTOs[2].name)
        assertTrue(responseDTOs[0].available)
        assertFalse(responseDTOs[1].available)
        assertTrue(responseDTOs[2].available)
    }

    @Test
    fun `should convert multiple Users to ResponseDTOs correctly`() {
        // Given
        val users = listOf(
            User(1L, "User 1", "user1@example.com", "ACTIVE", LocalDateTime.parse("2023-01-01T10:00:00")),
            User(2L, "User 2", "user2@example.com", "INACTIVE", LocalDateTime.parse("2023-01-01T11:00:00")),
            User(3L, "User 3", "user3@example.com", "PENDING", LocalDateTime.parse("2023-01-01T12:00:00"))
        )

        // When
        val responseDTOs = users.map { it.toResponseDTO() }

        // Then
        assertEquals(3, responseDTOs.size)
        assertEquals("User 1", responseDTOs[0].name)
        assertEquals("User 2", responseDTOs[1].name)
        assertEquals("User 3", responseDTOs[2].name)
        assertEquals("ACTIVE", responseDTOs[0].status)
        assertEquals("INACTIVE", responseDTOs[1].status)
        assertEquals("PENDING", responseDTOs[2].status)
    }
}