package com.company.ambassador.web.v1.dto

data class ApiResponseDTO<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String? = null,
    val error: String? = null
)