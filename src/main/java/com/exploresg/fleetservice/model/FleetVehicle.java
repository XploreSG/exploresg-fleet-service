package com.exploresg.fleetservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents a specific, physical vehicle instance owned by a fleet operator.
 * This entity links a master CarModel to an owner, their custom pricing, and
 * real-world operational data.
 */
@Entity
@Table(name = "fleet_vehicles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FleetVehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "car_model_id", nullable = false)
    private CarModel carModel;

    @Column(nullable = false)
    private Long ownerId; // The user ID of the FLEET_MANAGER

    @Column(nullable = false)
    private BigDecimal dailyPrice;

    @Column(nullable = false, unique = true)
    private String licensePlate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VehicleStatus status;

    // --- Enhanced Fields for Capstone Project ---

    private Integer yearOfManufacture;

    @Column(unique = true)
    private String chassisNumber;

    @Column(unique = true)
    private String engineNumber;

    private String insuranceReference; // Policy ID or document link

    private String coeReference; // Certificate of Entitlement reference

    private String primaryColour;

    private String secondaryColour;

    private Integer passengerCapacity; // Can differ from model spec (e.g., modifications)

    private Integer engineCapacityCc; // Engine size in cubic centimeters

    private Integer maxUnladenWeightKg;

    private Integer maxLadenWeightKg;

    @Column(length = 1024)
    private String vehicleAttachment1; // URL to a document/image in S3 (e.g., log card)

    @Column(length = 1024)
    private String vehicleAttachment2; // URL to another document/image in S3

    private String currentLocation; // GPS coordinates or zone name

    private LocalDateTime availableFrom; // Start of an availability window

    private LocalDateTime availableUntil; // End of an availability window

    @Column(length = 2048)
    private String maintenanceNote; // Reason for downtime if status is UNDER_MAINTENANCE

    private LocalDateTime expectedReturnDate; // ETA for when the vehicle will be available again

    @Column(nullable = false, updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime createdAt;

    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime lastUpdatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        lastUpdatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        lastUpdatedAt = LocalDateTime.now();
    }
}
