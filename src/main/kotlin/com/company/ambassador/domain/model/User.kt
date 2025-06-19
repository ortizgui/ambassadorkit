package com.company.ambassador.domain.model

import java.time.LocalDateTime

data class User(
    val id: Long,
    val name: String,
    val email: String,
    val status: String,
    val createdAt: LocalDateTime? = null
)

data class UserCreateRequest(
    val name: String,
    val email: String
)

data class UserUpdateRequest(
    val name: String? = null,
    val email: String? = null
)