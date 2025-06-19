package com.company.ambassador.integration

import com.company.ambassador.domain.model.ProductCreateRequest
import com.company.ambassador.web.v1.dto.ApiResponseDTO
import com.company.ambassador.web.v1.dto.ProductCreateRequestDTO
import com.company.ambassador.web.v1.dto.ProductResponseDTO
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.LocalDateTime

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = [
    "external-services.product-service.url=http://localhost:8089",
    "external-services.user-service.url=http://localhost:8089"
])
class ProductServiceIntegrationTest {

    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private lateinit var mockMvc: MockMvc
    private lateinit var wireMockServer: WireMockServer

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
        wireMockServer = WireMockServer(WireMockConfiguration.options().port(8089))
        wireMockServer.start()
        configureFor(8089)
    }

    @AfterEach
    fun tearDown() {
        wireMockServer.stop()
    }

    @Test
    fun `should get product by id successfully with external service integration`() {
        // Given
        val productId = 1L
        val productResponse = """
            {
                "id": $productId,
                "name": "Integration Test Product",
                "description": "Integration Test Description",
                "price": 99.99,
                "category": "Electronics",
                "available": true,
                "created_at": "2023-01-01T10:00:00"
            }
        """.trimIndent()

        stubFor(get(urlEqualTo("/products/$productId"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(productResponse)))

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/products/$productId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(productId))
            .andExpect(jsonPath("$.data.name").value("Integration Test Product"))
            .andExpect(jsonPath("$.data.price").value(99.99))

        // Verify the external service was called
        verify(exactly(1), getRequestedFor(urlEqualTo("/products/$productId")))
    }

    @Test
    fun `should handle external service error with circuit breaker`() {
        // Given
        val productId = 1L

        // Simulate external service failure
        stubFor(get(urlEqualTo("/products/$productId"))
            .willReturn(aResponse()
                .withStatus(500)
                .withBody("Internal Server Error")))

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/products/$productId"))
            .andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error").exists())

        // Verify the external service was called
        verify(getRequestedFor(urlEqualTo("/products/$productId")))
    }

    @Test
    fun `should retry on transient failures`() {
        // Given
        val productId = 1L
        val productResponse = """
            {
                "id": $productId,
                "name": "Integration Test Product",
                "description": "Integration Test Description",
                "price": 99.99,
                "category": "Electronics",
                "available": true,
                "created_at": "2023-01-01T10:00:00"
            }
        """.trimIndent()

        // First call fails, second succeeds
        stubFor(get(urlEqualTo("/products/$productId"))
            .inScenario("Retry Scenario")
            .whenScenarioStateIs("Started")
            .willReturn(aResponse()
                .withStatus(503)
                .withBody("Service Unavailable"))
            .willSetStateTo("First Failure"))

        stubFor(get(urlEqualTo("/products/$productId"))
            .inScenario("Retry Scenario")
            .whenScenarioStateIs("First Failure")
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(productResponse)))

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/products/$productId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(productId))

        // Verify retry occurred (should be called twice)
        verify(exactly(2), getRequestedFor(urlEqualTo("/products/$productId")))
    }

    @Test
    fun `should create product successfully with external service integration`() {
        // Given
        val createRequest = ProductCreateRequestDTO(
            name = "New Integration Product",
            description = "New Integration Description",
            price = 199.99,
            category = "Electronics"
        )

        val createResponseBody = """
            {
                "id": 1,
                "name": "${createRequest.name}",
                "description": "${createRequest.description}",
                "price": ${createRequest.price},
                "category": "${createRequest.category}",
                "available": true,
                "created_at": "2023-01-01T10:00:00"
            }
        """.trimIndent()

        stubFor(post(urlEqualTo("/products"))
            .willReturn(aResponse()
                .withStatus(201)
                .withHeader("Content-Type", "application/json")
                .withBody(createResponseBody)))

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.name").value(createRequest.name))
            .andExpect(jsonPath("$.data.price").value(createRequest.price))

        // Verify the external service was called with correct data
        verify(exactly(1), postRequestedFor(urlEqualTo("/products"))
            .withRequestBody(containing(createRequest.name)))
    }

    @Test
    fun `should handle timeout from external service`() {
        // Given
        val productId = 1L

        // Simulate timeout (delay longer than configured timeout)
        stubFor(get(urlEqualTo("/products/$productId"))
            .willReturn(aResponse()
                .withStatus(200)
                .withFixedDelay(10000) // 10 seconds delay
                .withHeader("Content-Type", "application/json")
                .withBody("""{"id": $productId, "name": "Test"}""")))

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/products/$productId"))
            .andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error").exists())

        // Verify the external service was called
        verify(getRequestedFor(urlEqualTo("/products/$productId")))
    }

    @Test
    fun `should get products with pagination from external service`() {
        // Given
        val productsResponse = """
            [
                {
                    "id": 1,
                    "name": "Product 1",
                    "description": "Description 1",
                    "price": 99.99,
                    "category": "Electronics",
                    "available": true,
                    "created_at": "2023-01-01T10:00:00"
                },
                {
                    "id": 2,
                    "name": "Product 2", 
                    "description": "Description 2",
                    "price": 149.99,
                    "category": "Electronics",
                    "available": true,
                    "created_at": "2023-01-01T11:00:00"
                }
            ]
        """.trimIndent()

        stubFor(get(urlPathEqualTo("/products"))
            .withQueryParam("page", equalTo("0"))
            .withQueryParam("size", equalTo("10"))
            .withQueryParam("category", equalTo("Electronics"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(productsResponse)))

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/products")
                .param("page", "0")
                .param("size", "10")
                .param("category", "Electronics"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray)
            .andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.data[0].id").value(1))
            .andExpect(jsonPath("$.data[1].id").value(2))

        // Verify the external service was called with correct parameters
        verify(exactly(1), getRequestedFor(urlPathEqualTo("/products"))
            .withQueryParam("page", equalTo("0"))
            .withQueryParam("size", equalTo("10"))
            .withQueryParam("category", equalTo("Electronics")))
    }

    @Test
    fun `should handle rate limiting scenarios`() {
        // Given
        val productId = 1L

        // Simulate rate limit exceeded response
        stubFor(get(urlEqualTo("/products/$productId"))
            .willReturn(aResponse()
                .withStatus(429)
                .withHeader("Content-Type", "application/json")
                .withBody("""{"error": "Rate limit exceeded"}""")))

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/products/$productId"))
            .andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error").exists())

        // Verify the external service was called
        verify(getRequestedFor(urlEqualTo("/products/$productId")))
    }

    @Test
    fun `should delete product successfully with external service integration`() {
        // Given
        val productId = 1L

        stubFor(delete(urlEqualTo("/products/$productId"))
            .willReturn(aResponse()
                .withStatus(204)))

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/products/$productId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("Product deleted successfully"))

        // Verify the external service was called
        verify(exactly(1), deleteRequestedFor(urlEqualTo("/products/$productId")))
    }

    @Test
    fun `should handle malformed response from external service`() {
        // Given
        val productId = 1L

        // Return malformed JSON
        stubFor(get(urlEqualTo("/products/$productId"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{ invalid json }")))

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/products/$productId"))
            .andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error").exists())

        // Verify the external service was called
        verify(getRequestedFor(urlEqualTo("/products/$productId")))
    }
}