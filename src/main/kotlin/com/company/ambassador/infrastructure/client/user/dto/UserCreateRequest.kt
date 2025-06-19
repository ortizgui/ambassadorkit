package com.company.ambassador.infrastructure.client.user.dto

data class UserCreateRequest(
    val name: String,
    val email: String
)