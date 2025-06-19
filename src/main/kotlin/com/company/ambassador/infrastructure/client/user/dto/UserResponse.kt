package com.company.ambassador.infrastructure.client.user.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

data class UserResponse(
    val id: Long,
    val name: String,
    val email: String,
    val status: String,
    @JsonProperty("created_at")
    val createdAt: LocalDateTime? = null
)