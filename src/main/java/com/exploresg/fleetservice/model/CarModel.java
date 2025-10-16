package com.exploresg.fleetservice.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

/**
 * Represents the master template for a car model, managed by platform admins.
 * This contains the static, unchangeable details about a specific type of car.
 */
@Entity
@Table(name = "car_models")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CarModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private UUID publicId; // <-- NEW PUBLIC REFERENCE ID (UUID)

    @Column(nullable = false)
    @Size(min = 1, max = 200)
    private String model;

    @Column(nullable = false)
    @Size(min = 1, max = 200)
    private String manufacturer;

    @Column(nullable = false)
    @Min(1)
    @Max(10)
    private Integer seats;

    @Column(nullable = false)
    private Integer luggage; // Luggage capacity in bags

    @Column(nullable = false)
    private String transmission; // e.g., "Automatic", "Manual"

    @Column(nullable = false, length = 1024)
    private String imageUrl; // URL to an image in AWS S3

    @Column(nullable = false)
    private String category; // e.g., "Sedan", "SUV", "Luxury"

    // --- Static Performance & Specs ---
    @Column(nullable = false)
    private String fuelType; // e.g., "Petrol", "Electric", "Hybrid"

    private Integer modelYear;

    // Engine size is defined by the model design
    private Integer engineCapacityCc; // Engine size in cubic centimeters

    // Weight limits are structural/design specifications
    private Integer maxUnladenWeightKg;
    private Integer maxLadenWeightKg;

    private Integer rangeInKm; // Estimated driving range in kilometers

    private Boolean hasAirConditioning;

    private Boolean hasInfotainmentSystem;

    private String safetyRating; // e.g., "5-Star ANCAP"

    private Integer topSpeedKph; // Top speed in km/h

    private Double zeroToHundredSec; // Acceleration time (0-100 km/h) in seconds

    @PrePersist // <-- JPA HOOK to automatically set the UUID before saving
    protected void onCreate() {
        if (publicId == null) {
            publicId = UUID.randomUUID();
        }
    }
}