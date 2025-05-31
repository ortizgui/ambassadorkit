package com.company.ambassador.domain.model.common

import java.time.LocalDateTime

data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String? = null,
    val timestamp: LocalDateTime = LocalDateTime.now()
)

data class ErrorResponse(
    val error: String,
    val message: String,
    val timestamp: LocalDateTime = LocalDateTime.now()
) 