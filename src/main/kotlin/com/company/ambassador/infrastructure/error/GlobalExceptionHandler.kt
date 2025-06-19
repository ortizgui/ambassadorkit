package com.company.ambassador.infrastructure.error

import com.company.ambassador.domain.model.ErrorDetail
import com.company.ambassador.domain.model.ErrorListResponse
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
    fun handleFeignException(ex: FeignException, request: WebRequest): ResponseEntity<ErrorListResponse> {
        logger.error("Feign exception occurred: ${ex.message}", ex)

        val (status, errorCode) = when (ex.status()) {
            404 -> HttpStatus.NOT_FOUND to ErrorCode.RESOURCE_NOT_FOUND
            400 -> HttpStatus.BAD_REQUEST to ErrorCode.INVALID_REQUEST_DATA
            401 -> HttpStatus.UNAUTHORIZED to ErrorCode.UNAUTHORIZED
            403 -> HttpStatus.FORBIDDEN to ErrorCode.FORBIDDEN
            500, 502, 503, 504 -> HttpStatus.SERVICE_UNAVAILABLE to ErrorCode.EXTERNAL_SERVICE_ERROR
            else -> HttpStatus.INTERNAL_SERVER_ERROR to ErrorCode.EXTERNAL_SERVICE_ERROR
        }

        val errorResponse = ErrorListResponse(
            errors = listOf(
                ErrorDetail(
                    code = errorCode.code,
                    message = "External service call failed: ${ex.message}"
                )
            )
        )

        return ResponseEntity.status(status).body(errorResponse)
    }

    @ExceptionHandler(CallNotPermittedException::class)
    fun handleCircuitBreakerException(ex: CallNotPermittedException, request: WebRequest): ResponseEntity<ErrorListResponse> {
        logger.warn("Circuit breaker is open: ${ex.message}")

        val errorResponse = ErrorListResponse(
            errors = listOf(
                ErrorDetail(
                    code = ErrorCode.CIRCUIT_BREAKER_OPEN.code,
                    message = "Service is currently unavailable due to circuit breaker. Please try again later."
                )
            )
        )

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse)
    }

    @ExceptionHandler(TimeoutException::class)
    fun handleTimeoutException(ex: TimeoutException, request: WebRequest): ResponseEntity<ErrorListResponse> {
        logger.error("Timeout exception occurred: ${ex.message}", ex)

        val errorResponse = ErrorListResponse(
            errors = listOf(
                ErrorDetail(
                    code = ErrorCode.REQUEST_TIMEOUT.code,
                    message = "The request timed out. Please try again later."
                )
            )
        )

        return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).body(errorResponse)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(ex: MethodArgumentNotValidException, request: WebRequest): ResponseEntity<ErrorListResponse> {
        logger.warn("Validation exception occurred: ${ex.message}")

        val errors = ex.bindingResult.allErrors.map { error ->
            when (error) {
                is FieldError -> ErrorDetail(
                    code = ErrorCode.VALIDATION_ERROR.code,
                    message = "${error.field}: ${error.defaultMessage}"
                )
                else -> ErrorDetail(
                    code = ErrorCode.VALIDATION_ERROR.code,
                    message = error.defaultMessage ?: "Validation error"
                )
            }
        }

        val errorResponse = ErrorListResponse(errors = errors)

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse)
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolationException(ex: ConstraintViolationException, request: WebRequest): ResponseEntity<ErrorListResponse> {
        logger.warn("Constraint violation exception occurred: ${ex.message}")

        val errors = ex.constraintViolations.map { violation ->
            ErrorDetail(
                code = ErrorCode.INVALID_REQUEST_PARAMETER.code,
                message = "${violation.propertyPath}: ${violation.message}"
            )
        }

        val errorResponse = ErrorListResponse(errors = errors)

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse)
    }

    @ExceptionHandler(RuntimeException::class)
    fun handleRuntimeException(ex: RuntimeException, request: WebRequest): ResponseEntity<ErrorListResponse> {
        logger.error("Runtime exception occurred: ${ex.message}", ex)

        val (status, errorDetail) = when {
            ex.message?.contains("Rate limit exceeded") == true -> {
                HttpStatus.TOO_MANY_REQUESTS to ErrorDetail(
                    code = ErrorCode.RATE_LIMIT_EXCEEDED.code,
                    message = ex.message ?: "Too many requests"
                )
            }
            ex.message?.contains("unavailable") == true -> {
                HttpStatus.SERVICE_UNAVAILABLE to ErrorDetail(
                    code = ErrorCode.SERVICE_UNAVAILABLE.code,
                    message = ex.message ?: "Service is temporarily unavailable"
                )
            }
            ex.message?.contains("not found") == true -> {
                HttpStatus.NOT_FOUND to ErrorDetail(
                    code = ErrorCode.RESOURCE_NOT_FOUND.code,
                    message = ex.message ?: "Resource not found"
                )
            }
            else -> {
                HttpStatus.INTERNAL_SERVER_ERROR to ErrorDetail(
                    code = ErrorCode.INTERNAL_SERVER_ERROR.code,
                    message = "An unexpected error occurred"
                )
            }
        }

        val errorResponse = ErrorListResponse(
            errors = listOf(errorDetail)
        )

        return ResponseEntity.status(status).body(errorResponse)
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(ex: Exception, request: WebRequest): ResponseEntity<ErrorListResponse> {
        logger.error("Unexpected exception occurred: ${ex.message}", ex)

        val errorResponse = ErrorListResponse(
            errors = listOf(
                ErrorDetail(
                    code = ErrorCode.UNEXPECTED_ERROR.code,
                    message = "An unexpected error occurred. Please try again later."
                )
            )
        )

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse)
    }
}