package com.company.ambassador.web.v1.controller

import com.company.ambassador.application.service.UserService
import com.company.ambassador.domain.model.User
import com.company.ambassador.domain.model.UserCreateRequest
import com.company.ambassador.web.v1.dto.UserCreateRequestDTO
import com.company.ambassador.web.v1.dto.UserUpdateRequestDTO
import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.just
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.LocalDateTime

@WebMvcTest(UserController::class)
class UserControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockkBean
    private lateinit var userService: UserService

    @Test
    fun `should get user by id successfully`() {
        // Given
        val userId = 1L
        val user = User(
            id = userId,
            name = "John Doe",
            email = "john.doe@example.com",
            status = "ACTIVE",
            createdAt = LocalDateTime.parse("2023-01-01T10:00:00")
        )
        every { userService.getUserById(userId) } returns user

        // When & Then
        mockMvc.perform(get("/api/v1/users/$userId"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(userId))
            .andExpect(jsonPath("$.data.name").value("John Doe"))
            .andExpect(jsonPath("$.data.email").value("john.doe@example.com"))
            .andExpect(jsonPath("$.data.status").value("ACTIVE"))

        verify(exactly = 1) { userService.getUserById(userId) }
    }

    @Test
    fun `should handle user not found exception`() {
        // Given
        val userId = 999L
        every { userService.getUserById(userId) } throws RuntimeException("User not found")

        // When & Then
        mockMvc.perform(get("/api/v1/users/$userId"))
            .andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error").value("Failed to retrieve user: User not found"))

        verify(exactly = 1) { userService.getUserById(userId) }
    }

    @Test
    fun `should get users with pagination successfully`() {
        // Given
        val users = listOf(
            User(1L, "John Doe", "john@example.com", "ACTIVE", LocalDateTime.parse("2023-01-01T10:00:00")),
            User(2L, "Jane Smith", "jane@example.com", "ACTIVE", LocalDateTime.parse("2023-01-01T11:00:00"))
        )
        every { userService.getUsers(0, 10) } returns users

        // When & Then
        mockMvc.perform(get("/api/v1/users")
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray)
            .andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.data[0].id").value(1))
            .andExpect(jsonPath("$.data[1].id").value(2))

        verify(exactly = 1) { userService.getUsers(0, 10) }
    }

    @Test
    fun `should create user successfully`() {
        // Given
        val request = UserCreateRequestDTO(
            name = "New User",
            email = "newuser@example.com"
        )
        val createdUser = User(
            id = 1L,
            name = request.name,
            email = request.email,
            status = "ACTIVE",
            createdAt = LocalDateTime.parse("2023-01-01T10:00:00")
        )
        every { userService.createUser(any()) } returns createdUser

        // When & Then
        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.name").value("New User"))
            .andExpect(jsonPath("$.data.email").value("newuser@example.com"))
            .andExpect(jsonPath("$.data.status").value("ACTIVE"))

        verify(exactly = 1) { userService.createUser(any()) }
    }

    @Test
    fun `should validate user creation request`() {
        // Given
        val invalidRequest = UserCreateRequestDTO(
            name = "",  // Invalid: empty name
            email = "invalid-email"  // Invalid: invalid email format
        )

        // When & Then
        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `should update user successfully`() {
        // Given
        val userId = 1L
        val request = UserUpdateRequestDTO(
            name = "Updated User",
            email = "updated@example.com"
        )
        val updatedUser = User(
            id = userId,
            name = "Updated User",
            email = "updated@example.com",
            status = "ACTIVE",
            createdAt = LocalDateTime.parse("2023-01-01T10:00:00")
        )
        every { userService.updateUser(userId, any()) } returns updatedUser

        // When & Then
        mockMvc.perform(put("/api/v1/users/$userId")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.name").value("Updated User"))
            .andExpect(jsonPath("$.data.email").value("updated@example.com"))

        verify(exactly = 1) { userService.updateUser(userId, any()) }
    }

    @Test
    fun `should delete user successfully`() {
        // Given
        val userId = 1L
        every { userService.deleteUser(userId) } just runs

        // When & Then
        mockMvc.perform(delete("/api/v1/users/$userId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("User deleted successfully"))

        verify(exactly = 1) { userService.deleteUser(userId) }
    }

    @Test
    fun `should search users successfully`() {
        // Given
        val query = "john"
        val users = listOf(
            User(1L, "John Doe", "john@example.com", "ACTIVE", LocalDateTime.parse("2023-01-01T10:00:00")),
            User(2L, "Johnny Smith", "johnny@example.com", "ACTIVE", LocalDateTime.parse("2023-01-01T11:00:00"))
        )
        every { userService.searchUsers(query) } returns users

        // When & Then
        mockMvc.perform(get("/api/v1/users/search")
                .param("query", query))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray)
            .andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.message").value("Search completed successfully"))

        verify(exactly = 1) { userService.searchUsers(query) }
    }

    @Test
    fun `should validate search query parameter`() {
        // When & Then
        mockMvc.perform(get("/api/v1/users/search")
                .param("query", ""))  // Empty query
            .andExpect(status().isBadRequest)
    }
}