package com.exploresg.fleetservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Request DTO for creating a temporary reservation
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTemporaryReservationRequest {

    @NotNull(message = "Model Public ID is required")
    private UUID modelPublicId;

    @NotNull(message = "Booking ID is required")
    private UUID bookingId;

    @NotNull(message = "Start date is required")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startDate;

    @NotNull(message = "End date is required")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endDate;
}