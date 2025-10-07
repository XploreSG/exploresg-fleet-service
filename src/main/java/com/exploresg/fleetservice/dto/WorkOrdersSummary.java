package com.exploresg.fleetservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for active work orders in the fleet dashboard.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkOrdersSummary {
    private long active;     // vehicles currently in maintenance (UNDER_MAINTENANCE status)
    private long pending;    // vehicles scheduled for maintenance
}
