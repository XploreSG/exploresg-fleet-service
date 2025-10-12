package com.exploresg.fleetservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * Request DTO for confirming a reservation
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfirmReservationRequest {

    @NotNull(message = "Payment reference is required")
    private String paymentReference;

    private String notes;
}
