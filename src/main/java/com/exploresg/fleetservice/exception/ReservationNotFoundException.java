package com.exploresg.fleetservice.exception;

import java.util.UUID;

/**
 * Exception thrown when reservation is not found
 */
public class ReservationNotFoundException extends RuntimeException {
    private final UUID reservationId;

    public ReservationNotFoundException(UUID reservationId) {
        super("Reservation not found: " + reservationId);
        this.reservationId = reservationId;
    }

    public UUID getReservationId() {
        return reservationId;
    }
}