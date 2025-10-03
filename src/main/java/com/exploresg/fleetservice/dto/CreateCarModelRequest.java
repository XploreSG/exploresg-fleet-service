package com.exploresg.fleetservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating a new CarModel.
 * Includes validation annotations to ensure data integrity.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
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

    // --- Static Performance & Specs ---

    @NotBlank(message = "Fuel Type is required")
    private String fuelType;

    @NotNull(message = "Model year is required")
    @Positive(message = "Model year must be a valid year")
    private Integer modelYear;

    // Correctly named to match entity
    @NotNull(message = "Range in KM is required")
    @Positive(message = "Range must be a positive number")
    private Integer rangeInKm;

    private boolean hasAirConditioning;
    private boolean hasInfotainmentSystem;

    @NotBlank(message = "Safety rating is required")
    private String safetyRating;

    @NotNull(message = "Top Speed KPH is required")
    @Positive(message = "Top Speed must be a positive number")
    private Integer topSpeedKph;

    @NotNull(message = "0-100 KPH acceleration is required")
    @Positive(message = "Acceleration time must be a positive number")
    private Double zeroToHundredSec;

    // --- NEW FIELDS (Moved from FleetVehicle to CarModel) ---

    @NotNull(message = "Engine capacity (CC) is required")
    @Positive(message = "Engine capacity must be a positive number")
    private Integer engineCapacityCc;

    @NotNull(message = "Max Unladen Weight (Kg) is required")
    @Positive(message = "Unladen Weight must be a positive number")
    private Integer maxUnladenWeightKg;

    @NotNull(message = "Max Laden Weight (Kg) is required")
    @Positive(message = "Laden Weight must be a positive number")
    private Integer maxLadenWeightKg;
}