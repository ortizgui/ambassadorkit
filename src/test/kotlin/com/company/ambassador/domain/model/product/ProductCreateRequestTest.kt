package com.company.ambassador.domain.model.product

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class ProductCreateRequestTest {

    @Test
    fun `should create product request with all fields`() {
        val request = ProductCreateRequest(
            name = "Test Product",
            description = "Test Description",
            price = 99.99,
            category = "Test Category"
        )

        assertEquals("Test Product", request.name)
        assertEquals("Test Description", request.description)
        assertEquals(99.99, request.price)
        assertEquals("Test Category", request.category)
    }
} 