package com.exploresg.fleetservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for vehicle assignments in the fleet dashboard.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleAssignmentsSummary {
    private long assigned;      // vehicles currently rented (BOOKED status)
    private long unassigned;    // vehicles available (AVAILABLE status)
}
