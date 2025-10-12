package com.exploresg.fleetservice.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global Exception Handler for REST API
 * 
 * Catches all exceptions and converts them to appropriate HTTP responses
 * with consistent error message format.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Error response structure
     */
    @Data
    @AllArgsConstructor
    public static class ErrorResponse {
        private LocalDateTime timestamp;
        private int status;
        private String error;
        private String message;
        private String path;
        private Map<String, Object> details;

        public ErrorResponse(LocalDateTime timestamp, int status, String error,
                String message, String path) {
            this.timestamp = timestamp;
            this.status = status;
            this.error = error;
            this.message = message;
            this.path = path;
            this.details = null;
        }
    }

    /**
     * Handle NoVehicleAvailableException
     * Returns 409 CONFLICT
     */
    @ExceptionHandler(NoVehicleAvailableException.class)
    public ResponseEntity<ErrorResponse> handleNoVehicleAvailable(
            NoVehicleAvailableException ex,
            WebRequest request) {

        log.warn("No vehicle available: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.CONFLICT.value(),
                "No Vehicle Available",
                ex.getMessage(),
                getPath(request));

        Map<String, Object> details = new HashMap<>();
        details.put("modelPublicId", ex.getModelPublicId());
        error.setDetails(details);

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    /**
     * Handle ReservationExpiredException
     * Returns 410 GONE
     */
    @ExceptionHandler(ReservationExpiredException.class)
    public ResponseEntity<ErrorResponse> handleReservationExpired(
            ReservationExpiredException ex,
            WebRequest request) {

        log.warn("Reservation expired: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.GONE.value(),
                "Reservation Expired",
                "Reservation has expired. Please create a new reservation.",
                getPath(request));

        Map<String, Object> details = new HashMap<>();
        details.put("reservationId", ex.getReservationId());
        error.setDetails(details);

        return ResponseEntity.status(HttpStatus.GONE).body(error);
    }

    /**
     * Handle ReservationNotFoundException
     * Returns 404 NOT FOUND
     */
    @ExceptionHandler(ReservationNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleReservationNotFound(
            ReservationNotFoundException ex,
            WebRequest request) {

        log.warn("Reservation not found: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.NOT_FOUND.value(),
                "Reservation Not Found",
                ex.getMessage(),
                getPath(request));

        Map<String, Object> details = new HashMap<>();
        details.put("reservationId", ex.getReservationId());
        error.setDetails(details);

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Handle InvalidReservationStatusException
     * Returns 400 BAD REQUEST
     */
    @ExceptionHandler(InvalidReservationStatusException.class)
    public ResponseEntity<ErrorResponse> handleInvalidReservationStatus(
            InvalidReservationStatusException ex,
            WebRequest request) {

        log.warn("Invalid reservation status: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Invalid Reservation Status",
                ex.getMessage(),
                getPath(request));

        Map<String, Object> details = new HashMap<>();
        details.put("reservationId", ex.getReservationId());
        details.put("currentStatus", ex.getCurrentStatus());
        details.put("expectedStatus", ex.getExpectedStatus());
        error.setDetails(details);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle InvalidDateRangeException
     * Returns 400 BAD REQUEST
     */
    @ExceptionHandler(InvalidDateRangeException.class)
    public ResponseEntity<ErrorResponse> handleInvalidDateRange(
            InvalidDateRangeException ex,
            WebRequest request) {

        log.warn("Invalid date range: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Invalid Date Range",
                ex.getMessage(),
                getPath(request));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle validation errors (e.g., @NotNull violations)
     * Returns 400 BAD REQUEST
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex,
            WebRequest request) {

        Map<String, String> validationErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> validationErrors.put(error.getField(), error.getDefaultMessage()));

        log.warn("Validation errors: {}", validationErrors);

        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Validation Failed",
                "Request validation failed",
                getPath(request));

        Map<String, Object> details = new HashMap<>();
        details.put("validationErrors", validationErrors);
        error.setDetails(details);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle all other exceptions
     * Returns 500 INTERNAL SERVER ERROR
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(
            Exception ex,
            WebRequest request) {

        log.error("Unexpected error: {}", ex.getMessage(), ex);

        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "An unexpected error occurred. Please try again later.",
                getPath(request));

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    /**
     * Extract request path from WebRequest
     */
    private String getPath(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }
}