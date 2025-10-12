package com.exploresg.fleetservice.service;

import com.exploresg.fleetservice.dto.*;
import com.exploresg.fleetservice.model.FleetVehicle;
import com.exploresg.fleetservice.model.VehicleBookingRecord;
import com.exploresg.fleetservice.model.VehicleBookingRecord.ReservationStatus;
import com.exploresg.fleetservice.exception.*;
import com.exploresg.fleetservice.repository.FleetVehicleRepository;
import com.exploresg.fleetservice.repository.VehicleBookingRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationService {

        private final FleetVehicleRepository fleetVehicleRepository;
        private final VehicleBookingRecordRepository bookingRecordRepository;

        // @Value("${booking.reservation.expiry-seconds:30}")
        @Value("${booking.reservation.expiry-seconds:300}")
        private int reservationExpirySeconds;

        /**
         * 🔐 PHASE 1: Create Temporary Reservation (BEFORE Payment)
         * 
         * This method uses pessimistic locking to prevent race conditions.
         * It locks ONE available vehicle for 30 seconds while user completes payment.
         * 
         * Flow:
         * 1. Validate date range
         * 2. Check for duplicate booking (idempotency)
         * 3. Find ONE available vehicle with SELECT FOR UPDATE SKIP LOCKED
         * 4. Create PENDING reservation that expires in 30 seconds
         * 5. Commit transaction (vehicle is now locked)
         * 
         * @param request Contains modelPublicId, bookingId, startDate, endDate
         * @return TemporaryReservationResponse with reservationId and expiresAt
         * @throws NoVehicleAvailableException if no vehicles available
         * @throws InvalidDateRangeException   if dates are invalid
         */
        @Transactional(isolation = Isolation.READ_COMMITTED, timeout = 10)
        public TemporaryReservationResponse createTemporaryReservation(
                        CreateTemporaryReservationRequest request) {

                log.info("Creating temporary reservation for model: {}, bookingId: {}, dates: {} to {}",
                                request.getModelPublicId(), request.getBookingId(),
                                request.getStartDate(), request.getEndDate());

                // 1. Validate date range
                validateDateRange(request.getStartDate(), request.getEndDate());

                // 2. Check for duplicate booking (idempotency)
                Optional<VehicleBookingRecord> existingReservation = bookingRecordRepository
                                .findByBookingId(request.getBookingId());

                if (existingReservation.isPresent()) {
                        VehicleBookingRecord existing = existingReservation.get();
                        log.warn("Duplicate reservation request detected for bookingId: {}", request.getBookingId());

                        // If existing reservation is still valid, return it
                        if (existing.getReservationStatus() == ReservationStatus.PENDING
                                        && !existing.isExpired()) {
                                return TemporaryReservationResponse.success(
                                                existing.getId(),
                                                existing.getVehicle().getId(),
                                                existing.getBookingId(),
                                                existing.getExpiresAt());
                        }

                        // If expired, we can create a new one (fall through)
                        log.info("Previous reservation expired, creating new one");
                }

                // 3. Find ONE available vehicle with pessimistic locking
                // This is THE MOST CRITICAL part - prevents race conditions
                Optional<FleetVehicle> availableVehicle = fleetVehicleRepository
                                .findOneAvailableVehicleForBooking(
                                                request.getModelPublicId(),
                                                request.getStartDate(),
                                                request.getEndDate());

                if (availableVehicle.isEmpty()) {
                        log.warn("No vehicles available for model: {} in date range: {} to {}",
                                        request.getModelPublicId(), request.getStartDate(), request.getEndDate());
                        throw new NoVehicleAvailableException(request.getModelPublicId(),
                                        "No vehicles available for the requested dates");
                }

                FleetVehicle vehicle = availableVehicle.get();
                log.info("Vehicle locked for booking: vehicleId={}, licensePlate={}",
                                vehicle.getId(), vehicle.getLicensePlate());

                // 4. Create PENDING reservation with expiry time
                LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(reservationExpirySeconds);

                VehicleBookingRecord reservation = VehicleBookingRecord.builder()
                                .vehicle(vehicle)
                                .bookingId(request.getBookingId())
                                .bookingStartDate(request.getStartDate())
                                .bookingEndDate(request.getEndDate())
                                .reservationStatus(ReservationStatus.PENDING)
                                .expiresAt(expiresAt)
                                .build();

                reservation = bookingRecordRepository.save(reservation);

                log.info("Temporary reservation created successfully: reservationId={}, expiresAt={}",
                                reservation.getId(), expiresAt);

                // 5. Transaction commits here - vehicle is now locked for 30 seconds
                return TemporaryReservationResponse.success(
                                reservation.getId(),
                                vehicle.getId(),
                                request.getBookingId(),
                                expiresAt);
        }

        /**
         * ✅ PHASE 2: Confirm Reservation (AFTER Successful Payment)
         * 
         * This method confirms the reservation after payment succeeds.
         * It verifies the reservation hasn't expired and updates status to CONFIRMED.
         * 
         * Flow:
         * 1. Find reservation by ID
         * 2. Check reservation exists and is PENDING
         * 3. Check reservation hasn't expired
         * 4. Update status to CONFIRMED
         * 5. Record payment reference and confirmation time
         * 
         * @param reservationId UUID of the temporary reservation
         * @param request       Contains paymentReference
         * @return ConfirmReservationResponse with confirmation details
         * @throws ReservationNotFoundException      if reservation not found
         * @throws ReservationExpiredException       if reservation has expired
         * @throws InvalidReservationStatusException if reservation is not PENDING
         */
        @Transactional
        public ConfirmReservationResponse confirmReservation(
                        UUID reservationId,
                        ConfirmReservationRequest request) {

                log.info("Confirming reservation: reservationId={}, paymentRef={}",
                                reservationId, request.getPaymentReference());

                // 1. Find reservation
                VehicleBookingRecord reservation = bookingRecordRepository
                                .findById(reservationId)
                                .orElseThrow(() -> new ReservationNotFoundException(reservationId));

                // 2. Check reservation status
                if (reservation.getReservationStatus() != ReservationStatus.PENDING) {
                        throw new InvalidReservationStatusException(
                                        reservationId,
                                        reservation.getReservationStatus().name(),
                                        ReservationStatus.PENDING.name());
                }

                // 3. Check if reservation has expired
                if (reservation.isExpired()) {
                        log.warn("Attempt to confirm expired reservation: reservationId={}, expiresAt={}",
                                        reservationId, reservation.getExpiresAt());
                        throw new ReservationExpiredException(reservationId);
                }

                // 4. Update reservation to CONFIRMED
                LocalDateTime confirmedAt = LocalDateTime.now();
                reservation.setReservationStatus(ReservationStatus.CONFIRMED);
                reservation.setPaymentReference(request.getPaymentReference());
                reservation.setConfirmedAt(confirmedAt);
                reservation.setExpiresAt(null); // Clear expiry since it's now confirmed

                if (request.getNotes() != null) {
                        reservation.setNotes(request.getNotes());
                }

                reservation = bookingRecordRepository.save(reservation);

                log.info("Reservation confirmed successfully: reservationId={}, vehicleId={}",
                                reservation.getId(), reservation.getVehicle().getId());

                return ConfirmReservationResponse.success(
                                reservation.getId(),
                                reservation.getVehicle().getId(),
                                confirmedAt);
        }

        /**
         * ❌ PHASE 3: Cancel Reservation
         * 
         * Used when payment fails or user cancels.
         * Sets reservation status to CANCELLED, freeing the vehicle.
         * 
         * @param reservationId UUID of the reservation to cancel
         * @param reason        Optional cancellation reason
         */
        @Transactional
        public void cancelReservation(UUID reservationId, String reason) {
                log.info("Cancelling reservation: reservationId={}, reason={}", reservationId, reason);

                VehicleBookingRecord reservation = bookingRecordRepository
                                .findById(reservationId)
                                .orElseThrow(() -> new ReservationNotFoundException(reservationId));

                // Only PENDING reservations can be cancelled this way
                if (reservation.getReservationStatus() != ReservationStatus.PENDING) {
                        throw new InvalidReservationStatusException(
                                        reservationId,
                                        reservation.getReservationStatus().name(),
                                        ReservationStatus.PENDING.name());
                }

                reservation.setReservationStatus(ReservationStatus.CANCELLED);
                reservation.setCancelledAt(LocalDateTime.now());
                if (reason != null) {
                        reservation.setNotes(reason);
                }

                bookingRecordRepository.save(reservation);

                log.info("Reservation cancelled: reservationId={}, vehicleId={}",
                                reservationId, reservation.getVehicle().getId());
        }

        /**
         * 📊 Check Availability (Optional Pre-Check)
         * 
         * Returns count of available vehicles for a model in a date range.
         * This is a lightweight check before creating a reservation.
         * 
         * Note: This is eventually consistent - availability can change
         * between this check and actual reservation creation.
         * 
         * @param modelPublicId Car model to check
         * @param startDate     Booking start date
         * @param endDate       Booking end date
         * @return AvailabilityCheckResponse with available count
         */
        @Transactional(readOnly = true)
        public AvailabilityCheckResponse checkAvailability(
                        UUID modelPublicId,
                        LocalDateTime startDate,
                        LocalDateTime endDate) {

                log.debug("Checking availability for model: {}, dates: {} to {}",
                                modelPublicId, startDate, endDate);

                validateDateRange(startDate, endDate);

                long availableCount = bookingRecordRepository.countAvailableVehicles(
                                modelPublicId,
                                startDate,
                                endDate);

                log.debug("Available vehicles found: {}", availableCount);

                return AvailabilityCheckResponse.of(
                                modelPublicId,
                                availableCount,
                                startDate,
                                endDate);
        }

        /**
         * Validate date range
         */
        private void validateDateRange(LocalDateTime startDate, LocalDateTime endDate) {
                if (startDate == null || endDate == null) {
                        throw new InvalidDateRangeException("Start date and end date are required");
                }

                if (!endDate.isAfter(startDate)) {
                        throw new InvalidDateRangeException(
                                        "End date must be after start date. Start: " + startDate + ", End: " + endDate);
                }

                // Optional: Check if start date is in the past
                if (startDate.isBefore(LocalDateTime.now())) {
                        throw new InvalidDateRangeException("Start date cannot be in the past");
                }

                // Optional: Check maximum booking duration (e.g., 30 days)
                if (startDate.plusDays(30).isBefore(endDate)) {
                        throw new InvalidDateRangeException("Booking duration cannot exceed 30 days");
                }
        }
}