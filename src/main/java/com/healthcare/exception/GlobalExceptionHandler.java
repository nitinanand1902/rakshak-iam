package com.healthcare.exception;

import com.healthcare.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * Centralized exception handler.
 * Converts all exceptions to a consistent {@link ErrorResponse} JSON format.
 * Business logic must NEVER exist here — only response transformation.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /** Handles all custom application exceptions (AppException subclasses). */
    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponse> handleAppException(AppException ex, HttpServletRequest request) {
        log.warn("AppException [{}]: {} — path={}", ex.getErrorCode(), ex.getMessage(), request.getRequestURI());
        return ResponseEntity
                .status(ex.getStatusCode())
                .body(new ErrorResponse(
                        LocalDateTime.now(),
                        ex.getStatusCode(),
                        ex.getMessage(),
                        request.getRequestURI(),
                        ex.getErrorCode()
                ));
    }

    /** Handles @Valid / @Validated bean validation failures. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                           HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("Validation failed: {} — path={}", message, request.getRequestURI());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        LocalDateTime.now(), 400, message,
                        request.getRequestURI(), "VALIDATION_ERROR"
                ));
    }

    /** Handles @PreAuthorize access denied — returns 403. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex,
                                                             HttpServletRequest request) {
        log.warn("Access denied: {} — path={}", ex.getMessage(), request.getRequestURI());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(
                        LocalDateTime.now(), 403,
                        "Access denied — insufficient permissions",
                        request.getRequestURI(), "ACCESS_DENIED"
                ));
    }

    /** Handles IllegalArgumentException (e.g., invalid enum values). */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex,
                                                                HttpServletRequest request) {
        log.warn("IllegalArgument: {} — path={}", ex.getMessage(), request.getRequestURI());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        LocalDateTime.now(), 400, ex.getMessage(),
                        request.getRequestURI(), "INVALID_ARGUMENT"
                ));
    }

    /** Catch-all for unexpected errors. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error at {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(
                        LocalDateTime.now(), 500,
                        "An unexpected error occurred. Please contact support.",
                        request.getRequestURI(), "INTERNAL_ERROR"
                ));
    }
}
