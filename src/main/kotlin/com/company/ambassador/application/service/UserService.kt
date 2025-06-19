package com.company.ambassador.application.service

import com.company.ambassador.domain.model.User
import com.company.ambassador.domain.model.UserCreateRequest

interface UserService {
    fun getUserById(id: Long): User
    fun getUsers(page: Int, size: Int): List<User>
    fun createUser(request: UserCreateRequest): User
    fun updateUser(id: Long, request: UserCreateRequest): User
    fun deleteUser(id: Long)
    fun searchUsers(query: String): List<User>
}