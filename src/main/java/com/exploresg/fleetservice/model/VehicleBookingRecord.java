package com.exploresg.fleetservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "vehicle_booking_records", uniqueConstraints = @UniqueConstraint(name = "uk_vehicle_booking", columnNames = {
        "vehicle_id", "booking_id" }), indexes = {
                @Index(name = "idx_vehicle_dates", columnList = "vehicle_id, booking_start_date, booking_end_date"),
                @Index(name = "idx_booking_id", columnList = "booking_id"),
                @Index(name = "idx_status_expires", columnList = "reservation_status, expires_at")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleBookingRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private FleetVehicle vehicle;

    @Column(name = "booking_id", nullable = false)
    private UUID bookingId;

    @Column(name = "booking_start_date", nullable = false)
    private LocalDateTime bookingStartDate;

    @Column(name = "booking_end_date", nullable = false)
    private LocalDateTime bookingEndDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "reservation_status", nullable = false, length = 20)
    private ReservationStatus reservationStatus;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "payment_reference", length = 255)
    private String paymentReference;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @UpdateTimestamp
    @Column(name = "last_updated_at", nullable = false)
    private LocalDateTime lastUpdatedAt;

    // Enum for reservation status
    public enum ReservationStatus {
        PENDING,
        CONFIRMED,
        CANCELLED,
        EXPIRED
    }

    // Helper method to check if reservation has expired
    public boolean isExpired() {
        return reservationStatus == ReservationStatus.PENDING
                && expiresAt != null
                && LocalDateTime.now().isAfter(expiresAt);
    }

    // Helper method to check if reservation can be confirmed
    public boolean canBeConfirmed() {
        return reservationStatus == ReservationStatus.PENDING
                && (expiresAt == null || LocalDateTime.now().isBefore(expiresAt));
    }
}