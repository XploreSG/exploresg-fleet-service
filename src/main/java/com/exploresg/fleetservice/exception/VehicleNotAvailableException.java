package com.exploresg.fleetservice.exception;

/**
 * Exception thrown when attempting to book a vehicle that is not available.
 */
public class VehicleNotAvailableException extends RuntimeException {

    public VehicleNotAvailableException(String message) {
        super(message);
    }

    public VehicleNotAvailableException(String message, Throwable cause) {
        super(message, cause);
    }
}