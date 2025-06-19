package com.company.ambassador

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AmbassadorMicroserviceApplicationTests {

    @Test
    fun contextLoads() {
        // Test that the Spring context loads successfully
    }
}