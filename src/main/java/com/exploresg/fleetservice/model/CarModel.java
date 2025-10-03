package com.exploresg.fleetservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    @Column(nullable = false)
    private String model;

    @Column(nullable = false)
    private String manufacturer;

    @Column(nullable = false)
    private Integer seats;

    @Column(nullable = false)
    private Integer luggage; // Luggage capacity in bags

    @Column(nullable = false)
    private String transmission; // e.g., "Automatic", "Manual"

    @Column(nullable = false, length = 1024)
    private String imageUrl; // URL to an image in AWS S3

    @Column(nullable = false)
    private String category; // e.g., "Sedan", "SUV", "Luxury"

    // --- Enhanced Fields for Capstone Project ---

    private String fuelType; // e.g., "Petrol", "Electric", "Hybrid"

    private Integer modelYear;

    private Integer rangeKm; // Estimated driving range in kilometers

    private boolean hasAirConditioning;

    private boolean hasInfotainmentSystem;

    private String safetyRating; // e.g., "5-Star ANCAP"

    private Integer topSpeedKph; // Top speed in km/h

    private Double zeroToHundredSec; // Acceleration time (0-100 km/h) in seconds
}
