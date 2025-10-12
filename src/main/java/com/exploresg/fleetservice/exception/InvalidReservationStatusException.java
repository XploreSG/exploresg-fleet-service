package com.exploresg.fleetservice.exception;

import java.util.UUID;

/**
 * Exception thrown when trying to operate on a reservation with invalid status
 */
public class InvalidReservationStatusException extends RuntimeException {
    private final UUID reservationId;
    private final String currentStatus;
    private final String expectedStatus;

    public InvalidReservationStatusException(UUID reservationId, String currentStatus, String expectedStatus) {
        super(String.format("Invalid reservation status. Reservation: %s, Current: %s, Expected: %s",
                reservationId, currentStatus, expectedStatus));
        this.reservationId = reservationId;
        this.currentStatus = currentStatus;
        this.expectedStatus = expectedStatus;
    }

    public UUID getReservationId() {
        return reservationId;
    }

    public String getCurrentStatus() {
        return currentStatus;
    }

    public String getExpectedStatus() {
        return expectedStatus;
    }
}