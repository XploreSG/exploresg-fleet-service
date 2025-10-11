package com.exploresg.fleetservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a vehicle booking/reservation record.
 * This is the definitive ledger for vehicle unavailability.
 */
@Entity
@Table(name = "vehicle_booking_records", indexes = {
        @Index(name = "idx_vehicle_dates", columnList = "vehicle_id, booking_start_date, booking_end_date"),
        @Index(name = "idx_booking_id", columnList = "booking_id"),
        @Index(name = "idx_status_expires", columnList = "reservation_status, expires_at")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_vehicle_booking", columnNames = { "vehicle_id", "booking_id" })
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleBookingRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "UUID", updatable = false, nullable = false)
    private UUID id;

    /**
     * The specific vehicle that was booked
     */
    @Column(name = "vehicle_id", nullable = false, columnDefinition = "UUID")
    private UUID vehicleId;

    /**
     * Reference to the booking in the Booking Service
     */
    @Column(name = "booking_id", nullable = false, columnDefinition = "UUID")
    private UUID bookingId;

    /**
     * Booking start date/time
     */
    @Column(name = "booking_start_date", nullable = false)
    private LocalDateTime bookingStartDate;

    /**
     * Booking end date/time
     */
    @Column(name = "booking_end_date", nullable = false)
    private LocalDateTime bookingEndDate;

    /**
     * Current status of the reservation
     * PENDING: Temporary hold (30 seconds) while payment processes
     * CONFIRMED: Payment successful, booking confirmed
     * CANCELLED: User cancelled or payment failed
     * EXPIRED: Temporary reservation timed out
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "reservation_status", nullable = false)
    private ReservationStatus reservationStatus;

    /**
     * When this temporary reservation expires (only for PENDING status)
     * Background job will auto-cancel expired PENDING reservations
     */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    /**
     * Payment reference from the payment service
     * Can be used to verify payment status or trigger refunds
     */
    @Column(name = "payment_reference")
    private String paymentReference;

    /**
     * Optional notes about the booking
     */
    @Column(name = "notes", length = 2048)
    private String notes;

    // Audit fields
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "last_updated_at", nullable = false)
    private LocalDateTime lastUpdatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        lastUpdatedAt = now;

        // Set expiration for PENDING reservations (30 seconds from creation)
        if (reservationStatus == ReservationStatus.PENDING) {
            expiresAt = now.plusSeconds(30);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        lastUpdatedAt = LocalDateTime.now();

        // Set confirmed/cancelled timestamps based on status change
        if (reservationStatus == ReservationStatus.CONFIRMED && confirmedAt == null) {
            confirmedAt = LocalDateTime.now();
        } else if (reservationStatus == ReservationStatus.CANCELLED && cancelledAt == null) {
            cancelledAt = LocalDateTime.now();
        }
    }

    /**
     * Check if this reservation has expired
     */
    public boolean isExpired() {
        return reservationStatus == ReservationStatus.PENDING
                && expiresAt != null
                && LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Check if booking dates overlap with another booking
     */
    public boolean overlapsWith(LocalDateTime otherStart, LocalDateTime otherEnd) {
        // Bookings overlap if:
        // (otherStart < this.end) AND (otherEnd > this.start)
        return otherStart.isBefore(bookingEndDate) && otherEnd.isAfter(bookingStartDate);
    }

    public enum ReservationStatus {
        PENDING, // Temporary hold during payment
        CONFIRMED, // Payment successful
        CANCELLED, // User cancelled or payment failed
        EXPIRED // Temporary reservation timed out
    }
}