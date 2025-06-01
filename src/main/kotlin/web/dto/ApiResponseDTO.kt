package com.company.ambassador.web.dto

data class ApiResponseDTO<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String? = null,
    val error: String? = null
) 