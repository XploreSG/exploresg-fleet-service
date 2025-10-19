package com.exploresg.fleetservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for reservation confirmation
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfirmReservationResponse {

    private boolean success;
    private String message;
    private UUID reservationId;
    private UUID vehicleId;
    private String status;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime confirmedAt;

    // Success response factory method
    public static ConfirmReservationResponse success(
            UUID reservationId,
            UUID vehicleId,
            LocalDateTime confirmedAt) {
        return ConfirmReservationResponse.builder()
                .success(true)
                .message("Reservation confirmed successfully")
                .reservationId(reservationId)
                .vehicleId(vehicleId)
                .status("CONFIRMED")
                .confirmedAt(confirmedAt)
                .build();
    }

    // Failure response factory method
    public static ConfirmReservationResponse failure(String message) {
        return ConfirmReservationResponse.builder()
                .success(false)
                .message(message)
                .reservationId(null)
                .vehicleId(null)
                .status(null)
                .confirmedAt(null)
                .build();
    }
}