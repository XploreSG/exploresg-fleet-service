package com.exploresg.fleetservice.exception;

/**
 * Exception thrown when invalid date range is provided
 */
public class InvalidDateRangeException extends RuntimeException {
    public InvalidDateRangeException(String message) {
        super(message);
    }
}
