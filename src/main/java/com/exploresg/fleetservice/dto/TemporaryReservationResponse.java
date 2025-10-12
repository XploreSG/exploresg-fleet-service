package com.exploresg.fleetservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for temporary reservation creation
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TemporaryReservationResponse {

    private boolean success;
    private String message;
    private UUID reservationId;
    private UUID vehicleId;
    private UUID bookingId;
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime expiresAt;

    // Success response factory method
    public static TemporaryReservationResponse success(
            UUID reservationId,
            UUID vehicleId,
            UUID bookingId,
            LocalDateTime expiresAt) {
        return TemporaryReservationResponse.builder()
                .success(true)
                .message("Reservation created successfully")
                .reservationId(reservationId)
                .vehicleId(vehicleId)
                .bookingId(bookingId)
                .status("PENDING")
                .expiresAt(expiresAt)
                .build();
    }

    // Failure response factory method
    public static TemporaryReservationResponse failure(String message) {
        return TemporaryReservationResponse.builder()
                .success(false)
                .message(message)
                .reservationId(null)
                .vehicleId(null)
                .bookingId(null)
                .status(null)
                .expiresAt(null)
                .build();
    }
}
