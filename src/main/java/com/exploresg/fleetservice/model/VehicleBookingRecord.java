package com.exploresg.fleetservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "vehicle_booking_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleBookingRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false, columnDefinition = "UUID")
    private UUID vehicleId; // The ID of the FleetVehicle

    @Column(nullable = false, columnDefinition = "UUID")
    private UUID bookingId; // The ID from the main Booking Service

    @Column(nullable = false)
    private LocalDateTime bookingStartDate;

    @Column(nullable = false)
    private LocalDateTime bookingEndDate;

    @Column(nullable = false, updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}