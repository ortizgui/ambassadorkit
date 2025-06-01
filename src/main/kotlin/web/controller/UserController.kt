package com.company.ambassador.web.controller

import com.company.ambassador.domain.service.user.UserService
import com.company.ambassador.web.dto.ApiResponseDTO
import com.company.ambassador.web.dto.UserCreateRequestDTO
import com.company.ambassador.web.dto.UserResponseDTO
import com.company.ambassador.web.dto.UserUpdateRequestDTO
import com.company.ambassador.web.dto.toDomainRequest
import com.company.ambassador.web.dto.toResponseDTO
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/users")
@Validated
@Tag(name = "User Management", description = "Ambassador service for user operations")
class UserController(
    private val userService: UserService
) {
    private val logger = LoggerFactory.getLogger(UserController::class.java)

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID", description = "Retrieves a user by their unique identifier")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "User found successfully"),
        ApiResponse(responseCode = "404", description = "User not found"),
        ApiResponse(responseCode = "500", description = "Internal server error")
    ])
    fun getUserById(
        @Parameter(description = "User ID", required = true)
        @PathVariable @Min(1) id: Long
    ): ResponseEntity<ApiResponseDTO<UserResponseDTO>> {
        logger.info("Request to get user by id: $id")
        return try {
            val user = userService.getUserById(id)
            ResponseEntity.ok(ApiResponseDTO(
                success = true,
                data = user.toResponseDTO(),
                message = "User retrieved successfully"
            ))
        } catch (ex: Exception) {
            logger.error("Error getting user by id $id: ${ex.message}", ex)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDTO(
                    success = false,
                    error = "Failed to retrieve user: ${ex.message}"
                ))
        }
    }

    @GetMapping
    @Operation(summary = "Get users with pagination", description = "Retrieves a paginated list of users")
    fun getUsers(
        @Parameter(description = "Page number (0-based)")
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @Parameter(description = "Page size")
        @RequestParam(defaultValue = "10") @Min(1) size: Int
    ): ResponseEntity<ApiResponseDTO<List<UserResponseDTO>>> {
        logger.info("Request to get users - page: $page, size: $size")
        return try {
            val users = userService.getUsers(page, size)
            ResponseEntity.ok(ApiResponseDTO(
                success = true,
                data = users.map { it.toResponseDTO() },
                message = "Users retrieved successfully"
            ))
        } catch (ex: Exception) {
            logger.error("Error getting users: ${ex.message}", ex)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDTO(
                    success = false,
                    error = "Failed to retrieve users: ${ex.message}"
                ))
        }
    }

    @PostMapping
    @Operation(summary = "Create new user", description = "Creates a new user")
    fun createUser(
        @Parameter(description = "User creation request", required = true)
        @Valid @RequestBody request: UserCreateRequestDTO
    ): ResponseEntity<ApiResponseDTO<UserResponseDTO>> {
        logger.info("Request to create user: ${request.name}")
        return try {
            val user = userService.createUser(request.toDomainRequest())
            ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDTO(
                    success = true,
                    data = user.toResponseDTO(),
                    message = "User created successfully"
                ))
        } catch (ex: Exception) {
            logger.error("Error creating user: ${ex.message}", ex)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDTO(
                    success = false,
                    error = "Failed to create user: ${ex.message}"
                ))
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update user", description = "Updates an existing user")
    fun updateUser(
        @Parameter(description = "User ID", required = true)
        @PathVariable @Min(1) id: Long,
        @Parameter(description = "User update request", required = true)
        @Valid @RequestBody request: UserUpdateRequestDTO
    ): ResponseEntity<ApiResponseDTO<UserResponseDTO>> {
        logger.info("Request to update user with id: $id")
        return try {
            val user = userService.updateUser(id, request.toDomainRequest())
            ResponseEntity.ok(ApiResponseDTO(
                success = true,
                data = user.toResponseDTO(),
                message = "User updated successfully"
            ))
        } catch (ex: Exception) {
            logger.error("Error updating user $id: ${ex.message}", ex)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDTO(
                    success = false,
                    error = "Failed to update user: ${ex.message}"
                ))
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete user", description = "Deletes a user by ID")
    fun deleteUser(
        @Parameter(description = "User ID", required = true)
        @PathVariable @Min(1) id: Long
    ): ResponseEntity<ApiResponseDTO<Unit>> {
        logger.info("Request to delete user with id: $id")
        return try {
            userService.deleteUser(id)
            ResponseEntity.ok(ApiResponseDTO(
                success = true,
                message = "User deleted successfully"
            ))
        } catch (ex: Exception) {
            logger.error("Error deleting user $id: ${ex.message}", ex)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDTO(
                    success = false,
                    error = "Failed to delete user: ${ex.message}"
                ))
        }
    }

    @GetMapping("/search")
    @Operation(summary = "Search users", description = "Searches users by query string")
    fun searchUsers(
        @Parameter(description = "Search query", required = true)
        @RequestParam @NotBlank query: String
    ): ResponseEntity<ApiResponseDTO<List<UserResponseDTO>>> {
        logger.info("Request to search users with query: $query")
        return try {
            val users = userService.searchUsers(query)
            ResponseEntity.ok(ApiResponseDTO(
                success = true,
                data = users.map { it.toResponseDTO() },
                message = "Search completed successfully"
            ))
        } catch (ex: Exception) {
            logger.error("Error searching users: ${ex.message}", ex)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDTO(
                    success = false,
                    error = "Failed to search users: ${ex.message}"
                ))
        }
    }
} 