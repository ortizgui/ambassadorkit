package com.company.ambassador.infrastructure.client.user

import com.company.ambassador.domain.model.user.User
import com.company.ambassador.domain.model.user.UserCreateRequest
import com.company.ambassador.infrastructure.config.feign.UserServiceFeignConfig
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.*

@FeignClient(
    name = "user-service",
    url = "\${external-services.user-service.url}",
    configuration = [UserServiceFeignConfig::class]
)
interface UserServiceClient {

    @GetMapping("/users/{id}")
    fun getUserById(@PathVariable id: Long): User

    @GetMapping("/users")
    fun getUsers(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): List<User>

    @PostMapping("/users")
    fun createUser(@RequestBody request: UserCreateRequest): User

    @PutMapping("/users/{id}")
    fun updateUser(@PathVariable id: Long, @RequestBody request: UserCreateRequest): User

    @DeleteMapping("/users/{id}")
    fun deleteUser(@PathVariable id: Long)

    @GetMapping("/users/search")
    fun searchUsers(@RequestParam query: String): List<User>
} 