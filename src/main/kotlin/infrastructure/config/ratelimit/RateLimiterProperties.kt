package com.company.ambassador.infrastructure.config.ratelimit

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@ConfigurationProperties(prefix = "external-services")
@Component
data class ExternalServicesProperties(
    val userService: ServiceConfig = ServiceConfig(),
    val productService: ServiceConfig = ServiceConfig()
) {
    data class ServiceConfig(
        val url: String = "",
        val rateLimit: RateLimitConfig = RateLimitConfig()
    )

    data class RateLimitConfig(
        val capacity: Long = 100,
        val tokens: Long = 10,
        val refillPeriod: String = "1s"
    )
} 