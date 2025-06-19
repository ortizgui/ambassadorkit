package com.company.ambassador.infrastructure.config.feign

import feign.Logger
import feign.RequestInterceptor
import feign.codec.ErrorDecoder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class UserServiceFeignConfig {

    @Bean
    fun userServiceFeignLoggerLevel(): Logger.Level {
        return Logger.Level.BASIC
    }

    @Bean
    fun userServiceRequestInterceptor(): RequestInterceptor {
        return RequestInterceptor { template ->
            template.header("X-Service-Name", "ambassador-service")
        }
    }

    @Bean
    fun userServiceErrorDecoder(): ErrorDecoder {
        return ErrorDecoder { methodKey, response ->
            when (response.status()) {
                404 -> throw RuntimeException("User not found")
                503 -> throw RuntimeException("User service unavailable")
                else -> throw RuntimeException("Error calling user service")
            }
        }
    }
}