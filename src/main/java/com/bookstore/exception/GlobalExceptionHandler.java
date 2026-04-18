package com.bookstore.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Translates common backend exceptions into user-friendly HTTP responses
 * so the frontend can display a clear error message.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Database constraint violations (duplicate key, not-null, etc.)
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDataIntegrity(DataIntegrityViolationException ex) {
        String msg = extractConstraintMessage(ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", msg));
    }

    /**
     * Generic unhandled RuntimeException — returns 400 with the raw message.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntime(RuntimeException ex) {
        ex.printStackTrace(); 
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred"));
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleJsonError(org.springframework.http.converter.HttpMessageNotReadableException ex) {
        ex.printStackTrace();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "JSON mapping error: " + ex.getMessage()));
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(org.springframework.security.access.AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "Access Denied: You do not have permission to perform this action."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleAll(Exception ex) {
        ex.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Unexpected error: " + ex.getClass().getSimpleName() + " - " + ex.getMessage()));
    }

    // -------------------------------------------------------------------------

    private String extractConstraintMessage(String raw) {
        if (raw == null)
            return "A database constraint was violated.";

        // Unique constraint on slug
        if (raw.contains("uk_") && raw.contains("slug") || raw.contains("(slug)=")) {
            return "A shop with this slug already exists. Please choose a different slug.";
        }
        // Unique constraint on shop_number
        if (raw.contains("shop_number") || raw.contains("(shop_number)=")) {
            return "A shop with this shop number already exists.";
        }
        // Unique constraint on custom_domain (empty string treated as null — this
        // should no longer occur)
        if (raw.contains("custom_domain") || raw.contains("(custom_domain)=")) {
            return "A shop with this custom domain already exists. Leave the field empty if not using a custom domain.";
        }
        // Unique constraint on email
        if (raw.contains("(email)=")) {
            return "A shop with this email already exists.";
        }
        // Fall back
        if (raw.contains("duplicate key")) {
            return "A record with these details already exists. Please check for duplicate values (slug, shop number, email).";
        }
        return "A database constraint was violated. Please check your input for duplicate values.";
    }
}
