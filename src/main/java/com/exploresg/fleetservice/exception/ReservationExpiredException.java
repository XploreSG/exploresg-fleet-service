package com.exploresg.fleetservice.exception;

import java.util.UUID;

/**
 * Exception thrown when trying to confirm an expired reservation
 */
public class ReservationExpiredException extends RuntimeException {
    private final UUID reservationId;

    public ReservationExpiredException(UUID reservationId) {
        super("Reservation has expired: " + reservationId);
        this.reservationId = reservationId;
    }

    public UUID getReservationId() {
        return reservationId;
    }
}
