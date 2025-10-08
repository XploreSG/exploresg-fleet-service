package com.exploresg.fleetservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for vehicle status summary in the fleet dashboard.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleStatusSummary {
    private long available;
    private long underMaintenance;
    private long booked;
    private long total;
}
