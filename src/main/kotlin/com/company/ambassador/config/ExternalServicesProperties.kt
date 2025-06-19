package com.company.ambassador.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@ConfigurationProperties(prefix = "external-services")
@Component
class ExternalServicesProperties {
    var userService: ServiceConfig = ServiceConfig()
    var productService: ServiceConfig = ServiceConfig()

    class ServiceConfig {
        var url: String = ""
        var rateLimit: RateLimitConfig = RateLimitConfig()
    }

    class RateLimitConfig {
        var capacity: Long = 100
        var tokens: Long = 10
        var refillPeriod: String = "1s"
    }
}