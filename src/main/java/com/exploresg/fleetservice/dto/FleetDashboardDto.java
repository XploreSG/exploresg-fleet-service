package com.exploresg.fleetservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Main DTO for the Fleet Manager dashboard.
 * Contains all aggregated statistics and breakdowns for the fleet.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FleetDashboardDto {
    private VehicleStatusSummary vehicleStatus;
    private ServiceRemindersSummary serviceReminders;
    private WorkOrdersSummary workOrders;
    private VehicleAssignmentsSummary vehicleAssignments;
    private FleetStatistics statistics;
    private List<FleetModelBreakdown> fleetByModel;
}
