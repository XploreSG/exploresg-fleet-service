package com.exploresg.fleetservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for fleet breakdown by car model in the dashboard.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FleetModelBreakdown {
    private String manufacturer;
    private String model;
    private String imageUrl;
    private long totalCount;
    private long availableCount;
    private long bookedCount;
    private long underMaintenanceCount;
    private Double averageMileage;
    private BigDecimal averageDailyPrice;
}
