package com.exploresg.fleetservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for availability check
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvailabilityCheckResponse {

    private UUID modelPublicId;
    private long availableCount;
    private boolean available;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startDate;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endDate;

    public static AvailabilityCheckResponse of(
            UUID modelPublicId,
            long count,
            LocalDateTime startDate,
            LocalDateTime endDate) {
        return AvailabilityCheckResponse.builder()
                .modelPublicId(modelPublicId)
                .availableCount(count)
                .available(count > 0)
                .startDate(startDate)
                .endDate(endDate)
                .build();
    }
}