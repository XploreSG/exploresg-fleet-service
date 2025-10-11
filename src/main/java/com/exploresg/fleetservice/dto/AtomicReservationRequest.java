package com.exploresg.fleetservice.dto;

import java.time.LocalDateTime;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AtomicReservationRequest {

    @NotNull
    private String modelId;

    @NotNull
    private String vendorId;

    @NotNull
    private String customerId;

    @NotNull
    private String bookingId;

    @NotNull
    private LocalDateTime startDate;

    @NotNull
    private LocalDateTime endDate;
}