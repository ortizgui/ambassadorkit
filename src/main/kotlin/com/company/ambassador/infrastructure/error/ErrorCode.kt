package com.company.ambassador.infrastructure.error

enum class ErrorCode(val code: String) {
    // Validation Errors
    VALIDATION_ERROR("VALIDATION_ERROR"),
    INVALID_REQUEST_DATA("INVALID_REQUEST_DATA"),
    INVALID_REQUEST_PARAMETER("INVALID_REQUEST_PARAMETER"),
    
    // Resource Errors
    RESOURCE_NOT_FOUND("RESOURCE_NOT_FOUND"),
    USER_NOT_FOUND("USER_NOT_FOUND"),
    PRODUCT_NOT_FOUND("PRODUCT_NOT_FOUND"),
    
    // Service Errors
    EXTERNAL_SERVICE_ERROR("EXTERNAL_SERVICE_ERROR"),
    SERVICE_UNAVAILABLE("SERVICE_UNAVAILABLE"),
    CIRCUIT_BREAKER_OPEN("CIRCUIT_BREAKER_OPEN"),
    REQUEST_TIMEOUT("REQUEST_TIMEOUT"),
    RATE_LIMIT_EXCEEDED("RATE_LIMIT_EXCEEDED"),
    
    // Authentication & Authorization
    UNAUTHORIZED("UNAUTHORIZED"),
    FORBIDDEN("FORBIDDEN"),
    
    // Server Errors
    INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR"),
    UNEXPECTED_ERROR("UNEXPECTED_ERROR")
}