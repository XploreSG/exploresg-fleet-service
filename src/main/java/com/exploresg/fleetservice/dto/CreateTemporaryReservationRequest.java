package com.exploresg.fleetservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Request DTO for creating a temporary reservation
 * 
 * Accepts ISO-8601 date formats:
 * - With timezone: "2025-10-20T03:00:00Z"
 * - Without timezone: "2025-10-20T03:00:00"
 * - With milliseconds: "2025-10-20T03:00:00.123Z"
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
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime startDate;

    @NotNull(message = "End date is required")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime endDate;
}