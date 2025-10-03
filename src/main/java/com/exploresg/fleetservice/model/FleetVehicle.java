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
    private BigDecimal dailyPrice; // The owner's specific price for this instance

    @Column(nullable = false, unique = true)
    private String licensePlate;

    // --- Instance-Specific Identity & Compliance ---

    // The VIN and engine number are unique to the physical car
    @Column(unique = true)
    private String chassisNumber;

    @Column(unique = true)
    private String engineNumber;

    private String insuranceReference; // Policy ID or document link
    private String coeReference; // Certificate of Entitlement reference

    // Color and capacity can be customized per instance/fleet
    private String primaryColour;
    private String secondaryColour;

    private Integer passengerCapacity; // Can differ from model spec (e.g., modifications)

    // --- Dynamic Operational Data ---

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VehicleStatus status;

    // The current usage of this specific vehicle instance
    private Integer mileageKm;

    // Current location/availability
    private String currentLocation; // GPS coordinates (e.g., "1.345678, 103.923456") or zone name
    private LocalDateTime availableFrom; // Start of an availability window
    private LocalDateTime availableUntil; // End of an availability window

    // Maintenance and Expected Return
    @Column(length = 2048)
    private String maintenanceNote; // Reason for downtime if status is UNDER_MAINTENANCE
    private LocalDateTime expectedReturnDate; // ETA for when the vehicle will be available again

    // Documentation/Attachments unique to this physical car
    @Column(length = 1024)
    private String vehicleAttachment1; // URL to a document/image in S3 (e.g., log card)
    @Column(length = 1024)
    private String vehicleAttachment2; // URL to another document/image in S3

    // --- Audit Fields ---
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