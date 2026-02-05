package com.example.camp.common

import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleBadRequest(e: IllegalArgumentException): ResponseEntity<ApiError> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiError("BAD_REQUEST", e.message ?: "bad request"))

    @ExceptionHandler(SecurityException::class)
    fun handleForbidden(e: SecurityException): ResponseEntity<ApiError> =
        ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ApiError("FORBIDDEN", e.message ?: "forbidden"))

    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleDataIntegrity(e: DataIntegrityViolationException): ResponseEntity<ApiError> {
        val msg = e.mostSpecificCause?.message ?: e.message ?: "constraint violation"
        return if (msg.contains("booking_nights") && msg.contains("site_id") && msg.contains("night_date")) {
            ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError("NOT_AVAILABLE", "이미 예약된 날짜가 포함되어 있습니다."))
        } else if (msg.contains("refunds") && msg.contains("idempotency_key")) {
            ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError("DUPLICATE_IDEMPOTENCY", "이미 처리된 요청입니다."))
        } else {
            ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError("CONSTRAINT_VIOLATION", "요청을 처리할 수 없습니다."))
        }
    }
}
