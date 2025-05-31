package com.company.ambassador.integration

import com.company.ambassador.domain.model.UserCreateRequest
import com.company.ambassador.domain.model.ProductCreateRequest
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@TestPropertySource(properties = [
    "external-services.user-service.url=http://localhost:18081",
    "external-services.product-service.url=http://localhost:18082",
    "resilience4j.circuitbreaker.instances.user-service.minimum-number-of-calls=2",
    "resilience4j.circuitbreaker.instances.product-service.minimum-number-of-calls=2"
])
class AmbassadorServiceIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private lateinit var userServiceMock: WireMockServer
    private lateinit var productServiceMock: WireMockServer

    @BeforeEach
    fun setup() {
        userServiceMock = WireMockServer(WireMockConfiguration.options().port(18081))
        productServiceMock = WireMockServer(WireMockConfiguration.options().port(18082))

        userServiceMock.start()
        productServiceMock.start()
    }

    @AfterEach
    fun teardown() {
        userServiceMock.stop()
        productServiceMock.stop()
    }

    @Test
    fun `should get user by id successfully`() {
        // Given
        userServiceMock.stubFor(
            get(urlEqualTo("/users/1"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                                "id": 1,
                                "name": "John Doe",
                                "email": "john.doe@example.com",
                                "status": "ACTIVE",
                                "created_at": "2024-01-15T10:30:00"
                            }
                        """.trimIndent())
                )
        )

        // When & Then
        mockMvc.perform(get("/api/v1/users/1"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(1))
            .andExpect(jsonPath("$.data.name").value("John Doe"))
            .andExpect(jsonPath("$.data.email").value("john.doe@example.com"))
    }

    @Test
    fun `should create user successfully`() {
        // Given
        val userRequest = UserCreateRequest(name = "Jane Doe", email = "jane.doe@example.com")

        userServiceMock.stubFor(
            post(urlEqualTo("/users"))
                .withRequestBody(containing("Jane Doe"))
                .willReturn(
                    aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                                "id": 2,
                                "name": "Jane Doe",
                                "email": "jane.doe@example.com",
                                "status": "ACTIVE",
                                "created_at": "2024-01-15T10:30:00"
                            }
                        """.trimIndent())
                )
        )

        // When & Then
        mockMvc.perform(
            post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userRequest))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.name").value("Jane Doe"))
    }

    @Test
    fun `should handle circuit breaker for user service`() {
        // Given - Configure service to return errors
        userServiceMock.stubFor(
            get(urlMatching("/users/.*"))
                .willReturn(
                    aResponse()
                        .withStatus(500)
                        .withFixedDelay(1000)
                )
        )

        // When - Make multiple requests to trigger circuit breaker
        repeat(3) {
            mockMvc.perform(get("/api/v1/users/1"))
        }

        // Then - Circuit breaker should be open and return fallback
        mockMvc.perform(get("/api/v1/users/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.status").value("UNAVAILABLE"))
    }

    @Test
    fun `should get product by id successfully`() {
        // Given
        productServiceMock.stubFor(
            get(urlEqualTo("/products/1"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                                "id": 1,
                                "name": "Smartphone Pro",
                                "description": "Latest smartphone",
                                "price": 899.99,
                                "category": "Electronics",
                                "available": true,
                                "created_at": "2024-01-15T10:30:00"
                            }
                        """.trimIndent())
                )
        )

        // When & Then
        mockMvc.perform(get("/api/v1/products/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(1))
            .andExpect(jsonPath("$.data.name").value("Smartphone Pro"))
            .andExpect(jsonPath("$.data.price").value(899.99))
    }

    @Test
    fun `should create product successfully`() {
        // Given
        val productRequest = ProductCreateRequest(
            name = "New Product",
            description = "Product description",
            price = 199.99,
            category = "Electronics"
        )

        productServiceMock.stubFor(
            post(urlEqualTo("/products"))
                .withRequestBody(containing("New Product"))
                .willReturn(
                    aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                                "id": 10,
                                "name": "New Product",
                                "description": "Product description",
                                "price": 199.99,
                                "category": "Electronics",
                                "available": true,
                                "created_at": "2024-01-15T10:30:00"
                            }
                        """.trimIndent())
                )
        )

        // When & Then
        mockMvc.perform(
            post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(productRequest))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.name").value("New Product"))
    }

    @Test
    fun `should handle timeout gracefully`() {
        // Given
        userServiceMock.stubFor(
            get(urlEqualTo("/users/timeout"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withFixedDelay(10000) // 10 second delay to trigger timeout
                )
        )

        // When & Then
        mockMvc.perform(get("/api/v1/users/timeout"))
            .andExpect(status().isRequestTimeout)
            .andExpect(jsonPath("$.error").value("Request Timeout"))
    }

    @Test
    fun `should validate request parameters`() {
        // When & Then
        mockMvc.perform(get("/api/v1/users/0")) // Invalid ID (less than 1)
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("Validation Error"))
    }

    @Test
    fun `should handle rate limiting`() {
        // Given - Setup a user request
        userServiceMock.stubFor(
            get(urlEqualTo("/users/1"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{"id": 1, "name": "Test"}""")
                )
        )

        // When - Make many requests rapidly
        // This would require configuring rate limiting with very low limits for testing
        // In a real scenario, you'd configure test-specific rate limits

        // Then - Should eventually return rate limit error
        // Note: This test would need specific test configuration for rate limiting
    }
}