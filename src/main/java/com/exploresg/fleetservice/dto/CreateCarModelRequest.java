package com.exploresg.fleetservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * DTO for creating a new CarModel.
 * Includes validation annotations to ensure data integrity.
 */
@Data
public class CreateCarModelRequest {

    @NotBlank(message = "Model name is required")
    private String model;

    @NotBlank(message = "Manufacturer is required")
    private String manufacturer;

    @NotNull(message = "Seat count is required")
    @Positive(message = "Seats must be a positive number")
    private Integer seats;

    @NotNull(message = "Luggage capacity is required")
    @Positive(message = "Luggage must be a positive number")
    private Integer luggage;

    @NotBlank(message = "Transmission type is required")
    private String transmission;

    @NotBlank(message = "Image URL is required")
    private String imageUrl;

    @NotBlank(message = "Category is required")
    private String category;

    // Optional fields
    private String fuelType;
    private Integer modelYear;
    private Integer rangeKm;
    private boolean hasAirConditioning;
    private boolean hasInfotainmentSystem;
    private String safetyRating;
    private Integer topSpeedKph;
    private Double zeroToHundredSec;
}
