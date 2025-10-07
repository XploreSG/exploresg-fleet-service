package com.exploresg.fleetservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO representing a car model available from a specific fleet operator.
 * If multiple operators have the same model, each appears as a separate entry.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OperatorCarModelDto {
    // Operator information
    private Long operatorId;
    private String operatorName;

    // Car model information
    private String publicModelId; // <-- USING PUBLIC UUID STRING
    private String model;
    private String manufacturer;
    private Integer seats;
    private Integer luggage;
    private String transmission;
    private String imageUrl;
    private String category;
    private String fuelType;
    private Integer modelYear;

    // Pricing and availability
    private BigDecimal dailyPrice; // Lowest price from this operator for this model
    private Integer availableVehicleCount; // Number of vehicles available (for allocation)
}