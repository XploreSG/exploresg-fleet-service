package com.exploresg.fleetservice.service;

import com.exploresg.fleetservice.model.VehicleBookingRecord;
import com.exploresg.fleetservice.model.FleetVehicle;
import com.exploresg.fleetservice.repository.VehicleBookingRecordRepository;
import com.exploresg.fleetservice.repository.FleetVehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final VehicleBookingRecordRepository bookingRecordRepository;
    private final FleetVehicleRepository fleetVehicleRepository;

    /**
     * STEP 1: Check availability count for a car model within date range
     * 
     * @param modelPublicId The public UUID of the car model
     * @param startDate     Booking start date
     * @param endDate       Booking end date
     * @return Number of available vehicles
     */
    @Transactional(readOnly = true)
    public int getAvailabilityCount(UUID modelPublicId, LocalDateTime startDate, LocalDateTime endDate) {
        // Find all vehicles of this model that are AVAILABLE
        List<FleetVehicle> candidateVehicles = fleetVehicleRepository
                .findAvailableVehiclesByModelPublicId(modelPublicId);

        // Filter out vehicles with overlapping bookings
        return (int) candidateVehicles.stream()
                .filter(vehicle -> !hasConflictingBooking(vehicle.getId(), startDate, endDate))
                .count();
    }

    /**
     * STEP 2: Create a temporary reservation (pre-payment)
     * This atomically finds and locks ONE available vehicle
     * 
     * @param modelPublicId The car model to book
     * @param bookingId     Reference from booking service
     * @param startDate     Booking start
     * @param endDate       Booking end
     * @return The created temporary reservation
     * @throws NoVehicleAvailableException if no vehicles available
     */
    @Transactional
    public VehicleBookingRecord createTemporaryReservation(
            UUID modelPublicId,
            UUID bookingId,
            LocalDateTime startDate,
            LocalDateTime endDate) {

        // Find ONE available vehicle using pessimistic locking
        FleetVehicle vehicle = fleetVehicleRepository
                .findOneAvailableVehicleForBooking(modelPublicId, startDate, endDate)
                .orElseThrow(() -> new NoVehicleAvailableException(
                        "No vehicles available for the requested dates"));

        // Create PENDING reservation with 30-second expiration
        VehicleBookingRecord reservation = VehicleBookingRecord.builder()
                .vehicleId(vehicle.getId())
                .bookingId(bookingId)
                .bookingStartDate(startDate)
                .bookingEndDate(endDate)
                .reservationStatus(VehicleBookingRecord.ReservationStatus.PENDING)
                .build();

        return bookingRecordRepository.save(reservation);
    }

    /**
     * STEP 3: Confirm reservation after successful payment
     * 
     * @param reservationId    The temporary reservation ID
     * @param paymentReference Payment system reference
     * @return The confirmed reservation
     * @throws ReservationExpiredException  if reservation has expired
     * @throws ReservationNotFoundException if reservation not found
     */
    @Transactional
    public VehicleBookingRecord confirmReservation(UUID reservationId, String paymentReference) {
        VehicleBookingRecord reservation = bookingRecordRepository
                .findById(reservationId)
                .orElseThrow(() -> new ReservationNotFoundException(
                        "Reservation not found: " + reservationId));

        // Check if reservation has expired
        if (reservation.isExpired()) {
            reservation.setReservationStatus(VehicleBookingRecord.ReservationStatus.EXPIRED);
            bookingRecordRepository.save(reservation);
            throw new ReservationExpiredException(
                    "Reservation has expired. Please create a new reservation.");
        }

        // Confirm the reservation
        reservation.setReservationStatus(VehicleBookingRecord.ReservationStatus.CONFIRMED);
        reservation.setPaymentReference(paymentReference);

        return bookingRecordRepository.save(reservation);
    }

    /**
     * Cancel a reservation (user-initiated or payment failure)
     */
    @Transactional
    public void cancelReservation(UUID reservationId, String reason) {
        VehicleBookingRecord reservation = bookingRecordRepository
                .findById(reservationId)
                .orElseThrow(() -> new ReservationNotFoundException(
                        "Reservation not found: " + reservationId));

        reservation.setReservationStatus(VehicleBookingRecord.ReservationStatus.CANCELLED);
        reservation.setNotes(reason);
        bookingRecordRepository.save(reservation);
    }

    /**
     * Background job: Clean up expired PENDING reservations
     * Should run every 10 seconds
     */
    @Transactional
    public int cleanupExpiredReservations() {
        LocalDateTime now = LocalDateTime.now();
        List<VehicleBookingRecord> expiredReservations = bookingRecordRepository.findExpiredPendingReservations(now);

        expiredReservations.forEach(reservation -> {
            reservation.setReservationStatus(VehicleBookingRecord.ReservationStatus.EXPIRED);
        });

        bookingRecordRepository.saveAll(expiredReservations);
        return expiredReservations.size();
    }

    /**
     * Check if a vehicle has any confirmed or pending bookings that overlap with
     * the date range
     */
    private boolean hasConflictingBooking(UUID vehicleId, LocalDateTime startDate, LocalDateTime endDate) {
        return bookingRecordRepository
                .existsConflictingBooking(vehicleId, startDate, endDate);
    }

    // Custom exceptions
    public static class NoVehicleAvailableException extends RuntimeException {
        public NoVehicleAvailableException(String message) {
            super(message);
        }
    }

    public static class ReservationNotFoundException extends RuntimeException {
        public ReservationNotFoundException(String message) {
            super(message);
        }
    }

    public static class ReservationExpiredException extends RuntimeException {
        public ReservationExpiredException(String message) {
            super(message);
        }
    }
}