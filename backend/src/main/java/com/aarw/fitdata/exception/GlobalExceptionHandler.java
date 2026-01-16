package com.aarw.fitdata.exception;

import com.aarw.fitdata.fitbit.FitbitRateLimitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(FitbitRateLimitException.class)
    public ResponseEntity<Map<String, String>> handleFitbitRateLimitException(FitbitRateLimitException ex) {
        log.warn("Fitbit rate limit hit: {}. Retry-after: {}", ex.getMessage(), ex.getRetryAfter());
        HttpHeaders headers = new HttpHeaders();
        if (ex.getRetryAfter() != null) {
            headers.set(HttpHeaders.RETRY_AFTER, ex.getRetryAfter());
        }
        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .headers(headers)
                .body(Map.of(
                        "error", "Fitbit API rate limit exceeded",
                        "retryAfter", ex.getRetryAfter() != null ? ex.getRetryAfter() : ""
                ));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalStateException(IllegalStateException ex) {
        log.warn("Illegal state: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<Void> handleNoResourceFoundException(org.springframework.web.servlet.resource.NoResourceFoundException ex) {
        // Log at trace or debug level to avoid cluttering logs with scanner probes
        log.trace("Resource not found: {}", ex.getResourcePath());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneralException(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Internal server error: " + ex.getMessage()));
    }
}
