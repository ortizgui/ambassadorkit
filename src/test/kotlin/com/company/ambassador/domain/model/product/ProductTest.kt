package com.company.ambassador.domain.model.product

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.time.LocalDateTime

class ProductTest {

    @Test
    fun `should create product with all fields`() {
        val now = LocalDateTime.now()
        val product = Product(
            id = 1L,
            name = "Test Product",
            description = "Test Description",
            price = 99.99,
            category = "Test Category",
            available = true,
            createdAt = now
        )

        assertEquals(1L, product.id)
        assertEquals("Test Product", product.name)
        assertEquals("Test Description", product.description)
        assertEquals(99.99, product.price)
        assertEquals("Test Category", product.category)
        assertTrue(product.available)
        assertEquals(now, product.createdAt)
    }

    @Test
    fun `should create product with default values`() {
        val product = Product(
            id = 1L,
            name = "Test Product",
            description = "Test Description",
            price = 99.99,
            category = "Test Category"
        )

        assertEquals(1L, product.id)
        assertEquals("Test Product", product.name)
        assertEquals("Test Description", product.description)
        assertEquals(99.99, product.price)
        assertEquals("Test Category", product.category)
        assertTrue(product.available)
        assertNull(product.createdAt)
    }
} 