package com.company.ambassador.infrastructure.config.feign

import feign.Logger
import feign.RequestInterceptor
import feign.codec.ErrorDecoder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ProductServiceFeignConfig {

    @Bean
    fun productServiceFeignLoggerLevel(): Logger.Level {
        return Logger.Level.BASIC
    }

    @Bean
    fun productServiceRequestInterceptor(): RequestInterceptor {
        return RequestInterceptor { template ->
            template.header("X-Service-Name", "ambassador-service")
        }
    }

    @Bean
    fun productServiceErrorDecoder(): ErrorDecoder {
        return ErrorDecoder { methodKey, response ->
            when (response.status()) {
                404 -> throw RuntimeException("Product not found")
                503 -> throw RuntimeException("Product service unavailable")
                else -> throw RuntimeException("Error calling product service")
            }
        }
    }
}