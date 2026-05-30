/*
 * Copyright (c) 2026 ChronoClass. All rights reserved.
 * This software is submitted for evaluation purposes only.
 * Unauthorized commercial use, reproduction, or distribution is prohibited.
 */
package com.chronoclass.exception;

import com.chronoclass.dto.response.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Global exception handler for consistent API error responses.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage(), null, request);
    }

    @ExceptionHandler(TimeConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleTimeConflict(
            TimeConflictException ex, HttpServletRequest request) {
        log.warn("Time conflict detected: {}", ex.getMessage());
        return buildResponse(HttpStatus.CONFLICT, "Time Conflict", ex.getMessage(),
                ex.getConflicts(), request);
    }

    @ExceptionHandler(ConcurrentBookingException.class)
    public ResponseEntity<ApiErrorResponse> handleConcurrentBooking(
            ConcurrentBookingException ex, HttpServletRequest request) {
        log.warn("Concurrent booking conflict: {}", ex.getMessage());
        return buildResponse(HttpStatus.CONFLICT, "Booking Conflict", ex.getMessage(),
                null, request);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiErrorResponse> handleOptimisticLock(
            ObjectOptimisticLockingFailureException ex, HttpServletRequest request) {
        log.warn("Optimistic lock conflict: {}", ex.getMessage());
        return buildResponse(HttpStatus.CONFLICT, "Concurrent Update",
                "This resource was modified by another request. Please retry.",
                null, request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.toList());
        log.warn("Validation failed: {}", details);
        return buildResponse(HttpStatus.BAD_REQUEST, "Validation Failed",
                "Request validation failed", details, request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("Invalid argument: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage(),
                null, request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrity(
            DataIntegrityViolationException ex, HttpServletRequest request) {
        log.error("Data integrity violation: {}", ex.getMessage());

        String message = "A data integrity constraint was violated.";
        if (ex.getMessage() != null && ex.getMessage().contains("uq_parent_offering")) {
            message = "You have already booked this offering.";
        }

        return buildResponse(HttpStatus.CONFLICT, "Data Conflict", message, null, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneral(
            Exception ex, HttpServletRequest request) {
        log.error("Unexpected error: ", ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "An unexpected error occurred. Please try again later.", null, request);
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(
            HttpStatus status, String error, String message,
            List<String> details, HttpServletRequest request) {
        ApiErrorResponse response = ApiErrorResponse.builder()
                .status(status.value())
                .error(error)
                .message(message)
                .details(details)
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(status).body(response);
    }
}
