package com.exploresg.fleetservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID; // <-- NEW IMPORT

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
    // Changed primary key to UUID for cross-service compatibility
    @Column(columnDefinition = "UUID", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "car_model_id", nullable = false)
    private CarModel carModel; // Foreign Key to CarModel (references Long ID internally)

    @Column(nullable = false, columnDefinition = "UUID")
    private UUID ownerId; // The user ID of the FLEET_MANAGER

    @Column(nullable = false)
    private BigDecimal dailyPrice; // The owner's specific price for this instance

    @Column(nullable = false, unique = true)
    private String licensePlate;

    // --- Instance-Specific Identity & Compliance ---
    @Column(unique = true)
    private String chassisNumber;

    @Column(unique = true)
    private String engineNumber;

    private String insuranceReference; // Policy ID or document link
    private String coeReference; // Certificate of Entitlement reference

    private String primaryColour;
    private String secondaryColour;

    private Integer passengerCapacity; // Can differ from model spec (e.g., modifications)

    // --- Dynamic Operational Data ---

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VehicleStatus status; // Must be an enum VehicleStatus

    private Integer mileageKm; // Odometer reading

    private String currentLocation; // GPS coordinates (e.g., "1.345678, 103.923456") or zone name
    private LocalDateTime availableFrom; // Start of an availability window
    private LocalDateTime availableUntil; // End of an availability window

    // Maintenance and Expected Return
    @Column(length = 2048)
    private String maintenanceNote; // Reason for downtime if status is UNDER_MAINTENANCE
    private LocalDateTime expectedReturnDate; // ETA for when the vehicle will be available again

    // Documentation/Attachments unique to this physical car
    @Column(length = 1024)
    private String vehicleAttachment1;
    @Column(length = 1024)
    private String vehicleAttachment2;

    // --- Audit Fields ---
    @Column(nullable = false, updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime createdAt;

    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime lastUpdatedAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID(); // <-- GENERATE UUID FOR PK
        }
        createdAt = LocalDateTime.now();
        lastUpdatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        lastUpdatedAt = LocalDateTime.now();
    }
}