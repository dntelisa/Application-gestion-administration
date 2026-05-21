package es.codeurjc.helloword_vscode.security.jwt;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import es.codeurjc.helloword_vscode.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestControllerAdvice
public class GlobalRestExceptionHandler {

    // Logger to log error messages
    private static final Logger logger = LoggerFactory.getLogger(GlobalRestExceptionHandler.class);

    /* This exception is thrown when a requested resource is not found, HTTP 404 Not Found */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Object> handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        logger.error("Resource not found: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.NOT_FOUND, "Resource not found", ex.getMessage(), request);
    }

    /* This exception is thrown when an illegal argument is passed to a method, HTTP 400 Bad Request */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        logger.error("Bad request: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Bad request", ex.getMessage(), request);
    }

    /* This exception is thrown when a security violation occurs, HTTP 403 Forbidden */
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Object> handleSecurity(SecurityException ex, HttpServletRequest request) {
        logger.error("Forbidden: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.FORBIDDEN, "Forbidden", ex.getMessage(), request);
    }

    /* This is a catch-all handler for any exceptions not specifically handled by other methods, HTTP 500 Internal Server Error */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGeneric(Exception ex, HttpServletRequest request) {
        logger.error("Unexpected error: {}", ex.getMessage(), ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", ex.getMessage(), request);
    }

    /* Builds a standardized error response */
    private ResponseEntity<Object> buildErrorResponse(HttpStatus status, String error, String message, HttpServletRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", status.value());
        body.put("error", error);
        body.put("message", message);
        body.put("path", request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }
}

