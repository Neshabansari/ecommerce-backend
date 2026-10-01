package com.ecommerce.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<com.ecommerce.exception.ErrorResponse> handleNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request, Map.of());
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<com.ecommerce.exception.ErrorResponse> handleDuplicate(
            DuplicateResourceException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, Map.of());
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<com.ecommerce.exception.ErrorResponse> handleBadRequest(
            BadRequestException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request, Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<com.ecommerce.exception.ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return build(HttpStatus.BAD_REQUEST, "Validation failed", request, fieldErrors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<com.ecommerce.exception.ErrorResponse> handleUnreadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Request body is missing or malformed", request, Map.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<com.ecommerce.exception.ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST,
                "Invalid value for parameter '" + ex.getName() + "'", request, Map.of());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<com.ecommerce.exception.ErrorResponse> handleInvalidCredentials(
            InvalidCredentialsException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), request, Map.of());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<com.ecommerce.exception.ErrorResponse> handleAuthentication(
            AuthenticationException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "Authentication is required", request, Map.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<com.ecommerce.exception.ErrorResponse> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "You do not have permission to do this", request, Map.of());
    }

    // Safety net: anything unexpected becomes a clean 500 and is logged on the server.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<com.ecommerce.exception.ErrorResponse> handleAny(
            Exception ex, HttpServletRequest request) {
        // Standard Spring MVC errors (404 for unknown URL, 405, 415, ...) already know their status.
        if (ex instanceof ErrorResponse springError) {
            String detail = springError.getBody().getDetail();
            return build(springError.getStatusCode(),
                    detail != null ? detail : "Request could not be processed", request, Map.of());
        }
        log.error("Unexpected error on {}", request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again later.", request, Map.of());
    }

    private ResponseEntity<com.ecommerce.exception.ErrorResponse> build(
            HttpStatusCode status, String message, HttpServletRequest request,
            Map<String, String> validationErrors) {
        String reason = status instanceof HttpStatus httpStatus ? httpStatus.getReasonPhrase() : "Error";
        com.ecommerce.exception.ErrorResponse body = new com.ecommerce.exception.ErrorResponse(
                Instant.now(), status.value(), reason, message,
                request.getRequestURI(), validationErrors);
        return ResponseEntity.status(status).body(body);
    }
}