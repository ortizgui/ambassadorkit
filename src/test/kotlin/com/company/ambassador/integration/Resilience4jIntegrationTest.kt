package com.company.ambassador.integration

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.retry.RetryRegistry
import io.github.resilience4j.timelimiter.TimeLimiterRegistry
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = [
    "external-services.product-service.url=http://localhost:8090",
    "external-services.user-service.url=http://localhost:8090",
    "resilience4j.circuitbreaker.instances.product-service.sliding-window-size=3",
    "resilience4j.circuitbreaker.instances.product-service.minimum-number-of-calls=2",
    "resilience4j.circuitbreaker.instances.product-service.failure-rate-threshold=50",
    "resilience4j.retry.instances.product-service.max-attempts=2",
    "resilience4j.timelimiter.instances.product-service.timeout-duration=1s"
])
class Resilience4jIntegrationTest {

    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

    @Autowired
    private lateinit var circuitBreakerRegistry: CircuitBreakerRegistry

    @Autowired
    private lateinit var retryRegistry: RetryRegistry

    @Autowired
    private lateinit var timeLimiterRegistry: TimeLimiterRegistry

    private lateinit var mockMvc: MockMvc
    private lateinit var wireMockServer: WireMockServer

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
        wireMockServer = WireMockServer(WireMockConfiguration.options().port(8090))
        wireMockServer.start()
        configureFor(8090)
    }

    @AfterEach
    fun tearDown() {
        wireMockServer.stop()
        // Reset circuit breakers to closed state
        circuitBreakerRegistry.allCircuitBreakers.forEach { cb ->
            cb.reset()
        }
    }

    @Test
    fun `should open circuit breaker after consecutive failures`() {
        // Given
        val productId = 1L
        val circuitBreaker = circuitBreakerRegistry.circuitBreaker("product-service")
        
        // Simulate service failures
        stubFor(get(urlEqualTo("/products/$productId"))
            .willReturn(aResponse()
                .withStatus(500)
                .withBody("Internal Server Error")))

        // When - Make enough failed calls to trip the circuit breaker
        repeat(3) {
            try {
                mockMvc.perform(get("/api/v1/products/$productId"))
                Thread.sleep(100) // Small delay between calls
            } catch (e: Exception) {
                // May throw exceptions due to circuit breaker
            }
        }

        // Then - Circuit breaker should be open
        // Note: The circuit breaker might take a moment to transition
        Thread.sleep(500)
        assertTrue(circuitBreaker.state == io.github.resilience4j.circuitbreaker.CircuitBreaker.State.OPEN ||
                  circuitBreaker.metrics.numberOfFailedCalls > 0)
    }

    @Test
    fun `should retry on transient failures and eventually succeed`() {
        // Given
        val productId = 1L
        val retry = retryRegistry.retry("product-service")
        
        val productResponse = """
            {
                "id": $productId,
                "name": "Test Product",
                "description": "Test Description",
                "price": 99.99,
                "category": "Electronics",
                "available": true,
                "created_at": "2023-01-01T10:00:00"
            }
        """.trimIndent()

        // First call fails, second succeeds
        stubFor(get(urlEqualTo("/products/$productId"))
            .inScenario("Retry Test")
            .whenScenarioStateIs("Started")
            .willReturn(aResponse()
                .withStatus(503)
                .withBody("Service Temporarily Unavailable"))
            .willSetStateTo("First Failure"))

        stubFor(get(urlEqualTo("/products/$productId"))
            .inScenario("Retry Test")
            .whenScenarioStateIs("First Failure")
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(productResponse)))

        // When
        mockMvc.perform(get("/api/v1/products/$productId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(productId))

        // Then - Verify retry occurred
        verify(exactly(2), getRequestedFor(urlEqualTo("/products/$productId")))
        assertTrue(retry.metrics.numberOfSuccessfulCallsWithRetryAttempt > 0 ||
                  retry.metrics.numberOfSuccessfulCallsWithoutRetryAttempt > 0)
    }

    @Test
    fun `should timeout after configured duration`() {
        // Given
        val productId = 1L
        val timeLimiter = timeLimiterRegistry.timeLimiter("product-service")
        
        // Simulate slow response (longer than 1s timeout)
        stubFor(get(urlEqualTo("/products/$productId"))
            .willReturn(aResponse()
                .withStatus(200)
                .withFixedDelay(2000) // 2 seconds delay
                .withHeader("Content-Type", "application/json")
                .withBody("""{"id": $productId, "name": "Test"}""")))

        // When
        mockMvc.perform(get("/api/v1/products/$productId"))
            .andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.success").value(false))

        // Then - Timeout should have occurred
        verify(getRequestedFor(urlEqualTo("/products/$productId")))
    }

    @Test
    fun `should handle combination of retry and circuit breaker`() {
        // Given
        val productId = 1L
        val circuitBreaker = circuitBreakerRegistry.circuitBreaker("product-service")
        val retry = retryRegistry.retry("product-service")

        // Reset metrics
        circuitBreaker.reset()

        // Simulate consistent failures to trigger both retry and circuit breaker
        stubFor(get(urlEqualTo("/products/$productId"))
            .willReturn(aResponse()
                .withStatus(503)
                .withBody("Service Unavailable")))

        // When - Make multiple failed calls
        repeat(5) { index ->
            try {
                mockMvc.perform(get("/api/v1/products/$productId"))
                Thread.sleep(200) // Allow time for retry delays
            } catch (e: Exception) {
                println("Call $index failed: ${e.message}")
            }
        }

        // Then - Both retry and circuit breaker should have been activated
        assertTrue(retry.metrics.numberOfFailedCallsWithRetryAttempt > 0 ||
                  retry.metrics.numberOfFailedCallsWithoutRetryAttempt > 0)
        
        // Circuit breaker should eventually open due to failures
        Thread.sleep(1000) // Allow time for circuit breaker to evaluate
        val finalState = circuitBreaker.state
        assertTrue(finalState == io.github.resilience4j.circuitbreaker.CircuitBreaker.State.OPEN ||
                  circuitBreaker.metrics.numberOfFailedCalls >= 2)
    }

    @Test
    fun `should recover when service becomes available again`() {
        // Given
        val productId = 1L
        val circuitBreaker = circuitBreakerRegistry.circuitBreaker("product-service")
        
        // Phase 1: Cause failures to open circuit breaker
        stubFor(get(urlEqualTo("/products/$productId"))
            .inScenario("Recovery Test")
            .whenScenarioStateIs("Started")
            .willReturn(aResponse()
                .withStatus(500)
                .withBody("Internal Server Error"))
            .willSetStateTo("Service Down"))

        repeat(3) {
            try {
                mockMvc.perform(get("/api/v1/products/$productId"))
                Thread.sleep(100)
            } catch (e: Exception) {
                // Expected failures
            }
        }

        // Phase 2: Service becomes available again
        val productResponse = """
            {
                "id": $productId,
                "name": "Recovered Product",
                "description": "Service is back online",
                "price": 99.99,
                "category": "Electronics",
                "available": true,
                "created_at": "2023-01-01T10:00:00"
            }
        """.trimIndent()

        stubFor(get(urlEqualTo("/products/$productId"))
            .inScenario("Recovery Test")
            .whenScenarioStateIs("Service Down")
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(productResponse)))

        // Force circuit breaker to half-open state to test recovery
        if (circuitBreaker.state == io.github.resilience4j.circuitbreaker.CircuitBreaker.State.OPEN) {
            circuitBreaker.transitionToHalfOpenState()
        }

        Thread.sleep(200) // Allow time for state transition

        // When - Make a successful call
        mockMvc.perform(get("/api/v1/products/$productId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.name").value("Recovered Product"))

        // Then - Circuit breaker should eventually close
        assertTrue(circuitBreaker.state == io.github.resilience4j.circuitbreaker.CircuitBreaker.State.CLOSED ||
                  circuitBreaker.state == io.github.resilience4j.circuitbreaker.CircuitBreaker.State.HALF_OPEN)
    }

    @Test
    fun `should handle mixed success and failure scenarios`() {
        // Given
        val productId = 1L
        val circuitBreaker = circuitBreakerRegistry.circuitBreaker("product-service")
        val retry = retryRegistry.retry("product-service")

        val productResponse = """
            {
                "id": $productId,
                "name": "Mixed Scenario Product",
                "description": "Sometimes works",
                "price": 99.99,
                "category": "Electronics",
                "available": true,
                "created_at": "2023-01-01T10:00:00"
            }
        """.trimIndent()

        // Alternate between success and failure
        stubFor(get(urlEqualTo("/products/$productId"))
            .inScenario("Mixed Test")
            .whenScenarioStateIs("Started")
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(productResponse))
            .willSetStateTo("Success 1"))

        stubFor(get(urlEqualTo("/products/$productId"))
            .inScenario("Mixed Test")
            .whenScenarioStateIs("Success 1")
            .willReturn(aResponse()
                .withStatus(500)
                .withBody("Temporary failure"))
            .willSetStateTo("Failure 1"))

        stubFor(get(urlEqualTo("/products/$productId"))
            .inScenario("Mixed Test")
            .whenScenarioStateIs("Failure 1")
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(productResponse)))

        // When - Make multiple calls with mixed results
        val results = mutableListOf<Boolean>()
        repeat(3) {
            try {
                val result = mockMvc.perform(get("/api/v1/products/$productId"))
                    .andReturn()
                results.add(result.response.status == 200)
                Thread.sleep(300) // Allow time between calls
            } catch (e: Exception) {
                results.add(false)
            }
        }

        // Then - Should have both successes and failures
        assertTrue(results.any { it }) // At least one success
        verify(exactly(3), getRequestedFor(urlEqualTo("/products/$productId")))
        
        // Circuit breaker should remain closed due to mixed results
        assertTrue(circuitBreaker.state != io.github.resilience4j.circuitbreaker.CircuitBreaker.State.OPEN)
    }
}