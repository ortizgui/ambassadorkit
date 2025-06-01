package com.company.ambassador.web.exception

import com.company.ambassador.domain.model.common.ErrorResponse
import feign.FeignException
import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import jakarta.validation.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import java.util.concurrent.TimeoutException

@RestControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(FeignException::class)
    fun handleFeignException(ex: FeignException, request: WebRequest): ResponseEntity<ErrorResponse> {
        logger.error("Feign exception occurred: ${ex.message}", ex)

        val status = when (ex.status()) {
            404 -> HttpStatus.NOT_FOUND
            400 -> HttpStatus.BAD_REQUEST
            401 -> HttpStatus.UNAUTHORIZED
            403 -> HttpStatus.FORBIDDEN
            500, 502, 503, 504 -> HttpStatus.SERVICE_UNAVAILABLE
            else -> HttpStatus.INTERNAL_SERVER_ERROR
        }

        val errorResponse = ErrorResponse(
            error = "External Service Error",
            message = "External service call failed: ${ex.message}"
        )

        return ResponseEntity.status(status).body(errorResponse)
    }

    @ExceptionHandler(CallNotPermittedException::class)
    fun handleCircuitBreakerException(ex: CallNotPermittedException, request: WebRequest): ResponseEntity<ErrorResponse> {
        logger.warn("Circuit breaker is open: ${ex.message}")

        val errorResponse = ErrorResponse(
            error = "Service Unavailable",
            message = "Service is currently unavailable due to circuit breaker. Please try again later."
        )

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse)
    }

    @ExceptionHandler(TimeoutException::class)
    fun handleTimeoutException(ex: TimeoutException, request: WebRequest): ResponseEntity<ErrorResponse> {
        logger.error("Timeout exception occurred: ${ex.message}", ex)

        val errorResponse = ErrorResponse(
            error = "Request Timeout",
            message = "The request timed out. Please try again later."
        )

        return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).body(errorResponse)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(ex: MethodArgumentNotValidException, request: WebRequest): ResponseEntity<ErrorResponse> {
        logger.warn("Validation exception occurred: ${ex.message}")

        val errors = ex.bindingResult.allErrors.joinToString(", ") { error ->
            when (error) {
                is FieldError -> "${error.field}: ${error.defaultMessage}"
                else -> error.defaultMessage ?: "Validation error"
            }
        }

        val errorResponse = ErrorResponse(
            error = "Validation Error",
            message = "Invalid request data: $errors"
        )

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse)
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolationException(ex: ConstraintViolationException, request: WebRequest): ResponseEntity<ErrorResponse> {
        logger.warn("Constraint violation exception occurred: ${ex.message}")

        val errors = ex.constraintViolations.joinToString(", ") { violation ->
            "${violation.propertyPath}: ${violation.message}"
        }

        val errorResponse = ErrorResponse(
            error = "Validation Error",
            message = "Invalid request parameters: $errors"
        )

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse)
    }

    @ExceptionHandler(RuntimeException::class)
    fun handleRuntimeException(ex: RuntimeException, request: WebRequest): ResponseEntity<ErrorResponse> {
        logger.error("Runtime exception occurred: ${ex.message}", ex)

        val errorResponse = when {
            ex.message?.contains("Rate limit exceeded") == true -> {
                ErrorResponse(
                    error = "Rate Limit Exceeded",
                    message = ex.message ?: "Too many requests"
                )
            }
            ex.message?.contains("unavailable") == true -> {
                ErrorResponse(
                    error = "Service Unavailable",
                    message = ex.message ?: "Service is temporarily unavailable"
                )
            }
            else -> {
                ErrorResponse(
                    error = "Internal Server Error",
                    message = "An unexpected error occurred"
                )
            }
        }

        val status = when {
            ex.message?.contains("Rate limit exceeded") == true -> HttpStatus.TOO_MANY_REQUESTS
            ex.message?.contains("unavailable") == true -> HttpStatus.SERVICE_UNAVAILABLE
            else -> HttpStatus.INTERNAL_SERVER_ERROR
        }

        return ResponseEntity.status(status).body(errorResponse)
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(ex: Exception, request: WebRequest): ResponseEntity<ErrorResponse> {
        logger.error("Unexpected exception occurred: ${ex.message}", ex)

        val errorResponse = ErrorResponse(
            error = "Internal Server Error",
            message = "An unexpected error occurred. Please try again later."
        )

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse)
    }
}