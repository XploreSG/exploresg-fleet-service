package com.exploresg.fleetservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for overall fleet statistics in the dashboard.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FleetStatistics {
    private long totalVehicles;
    private long totalModels;
    private Double averageMileage;
    private Long totalMileage; // Sum of all mileageKm
    private BigDecimal totalPotentialDailyRevenue;
    private BigDecimal totalRevenue; // Total potential revenue (sum of all dailyPrice)
    private Double utilizationRate; // Percentage of vehicles currently in use (booked)
}
