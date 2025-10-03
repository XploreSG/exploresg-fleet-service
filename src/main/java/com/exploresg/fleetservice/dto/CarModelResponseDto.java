package com.exploresg.fleetservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for responding to clients with Car Model details.
 * Used primarily for the list of available models (Car Cards).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CarModelResponseDto {

    private Long modelId;
    private String model;
    private String manufacturer;
    private String category;
    private String imageUrl;

    // --- Pricing (Often displayed on the car card, might be min price across all
    // instances)
    private BigDecimal dailyRentalRate;

    // --- Key Specs for Display ---
    private Integer seats;
    private Integer luggage;
    private String transmission;
    private String fuelType;
    private Integer modelYear;

    // Performance & Capabilities
    private Integer topSpeedKph;
    private Double zeroToHundredSec;
    private Integer rangeInKm;

    // Structural Specs (for reference)
    private Integer engineCapacityCc;
    private String safetyRating;

    // Features
    private boolean hasAirConditioning;
    private boolean hasInfotainmentSystem;
}