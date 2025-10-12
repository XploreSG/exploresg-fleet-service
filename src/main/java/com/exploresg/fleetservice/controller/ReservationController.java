package com.exploresg.fleetservice.controller;

import com.exploresg.fleetservice.dto.*;
import com.exploresg.fleetservice.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 🚗 Fleet Reservation Controller
 * 
 * Provides REST API endpoints for the two-phase reservation system:
 * 1. Create temporary reservation (BEFORE payment)
 * 2. Confirm reservation (AFTER payment)
 * 3. Cancel reservation (if payment fails or user cancels)
 * 4. Check availability (optional pre-check)
 */
@RestController
@RequestMapping("/api/v1/fleet")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Fleet Reservations", description = "Vehicle reservation management endpoints")
public class ReservationController {

        private final ReservationService reservationService;

        /**
         * ⭐ ENDPOINT 1: Create Temporary Reservation (BEFORE Payment)
         * 
         * This is the FIRST step in the booking flow.
         * It locks ONE available vehicle for 30 seconds while user completes payment.
         * 
         * Flow:
         * 1. User selects: Toyota Camry, Jan 1-5, 2025
         * 2. Frontend calls this endpoint
         * 3. Backend locks one vehicle with pessimistic locking
         * 4. Returns reservationId and expiresAt
         * 5. Frontend shows payment screen with countdown timer
         * 
         * @param request Contains modelPublicId, bookingId, startDate, endDate
         * @return 201 CREATED with reservationId and expiresAt
         *         409 CONFLICT if no vehicles available
         *         400 BAD REQUEST if invalid date range
         */
        @PostMapping("/reservations/temporary")
        @Operation(summary = "Create temporary reservation", description = "Locks a vehicle for 30 seconds before payment. Returns reservation ID and expiry time.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "201", description = "Reservation created successfully", content = @Content(schema = @Schema(implementation = TemporaryReservationResponse.class))),
                        @ApiResponse(responseCode = "409", description = "No vehicles available for the requested dates"),
                        @ApiResponse(responseCode = "400", description = "Invalid request data or date range")
        })
        public ResponseEntity<TemporaryReservationResponse> createTemporaryReservation(
                        @Valid @RequestBody CreateTemporaryReservationRequest request) {

                log.info("POST /reservations/temporary - Creating reservation for model: {}, bookingId: {}",
                                request.getModelPublicId(), request.getBookingId());

                TemporaryReservationResponse response = reservationService.createTemporaryReservation(request);

                log.info("Temporary reservation created: reservationId={}, expiresAt={}",
                                response.getReservationId(), response.getExpiresAt());

                return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }

        /**
         * ⭐ ENDPOINT 2: Confirm Reservation (AFTER Successful Payment)
         * 
         * This is the SECOND step in the booking flow.
         * Called after payment succeeds to finalize the reservation.
         * 
         * Flow:
         * 1. User completes payment (within 30 seconds)
         * 2. Payment service returns success
         * 3. Frontend calls this endpoint with reservationId and paymentReference
         * 4. Backend updates reservation status: PENDING → CONFIRMED
         * 5. Vehicle is now officially booked
         * 
         * @param reservationId UUID of the temporary reservation
         * @param request       Contains paymentReference
         * @return 200 OK with confirmation details
         *         410 GONE if reservation expired
         *         404 NOT FOUND if reservation doesn't exist
         *         400 BAD REQUEST if reservation is not PENDING
         */
        @PostMapping("/reservations/{reservationId}/confirm")
        @Operation(summary = "Confirm reservation after payment", description = "Confirms a temporary reservation after successful payment. Must be called within 30 seconds.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Reservation confirmed successfully", content = @Content(schema = @Schema(implementation = ConfirmReservationResponse.class))),
                        @ApiResponse(responseCode = "410", description = "Reservation has expired (took longer than 30 seconds)"),
                        @ApiResponse(responseCode = "404", description = "Reservation not found"),
                        @ApiResponse(responseCode = "400", description = "Reservation is not in PENDING status")
        })
        public ResponseEntity<ConfirmReservationResponse> confirmReservation(
                        @Parameter(description = "Reservation ID from temporary reservation", required = true) @PathVariable UUID reservationId,
                        @Valid @RequestBody ConfirmReservationRequest request) {

                log.info("POST /reservations/{}/confirm - Confirming with payment reference: {}",
                                reservationId, request.getPaymentReference());

                ConfirmReservationResponse response = reservationService.confirmReservation(
                                reservationId,
                                request);

                log.info("Reservation confirmed: reservationId={}, vehicleId={}",
                                response.getReservationId(), response.getVehicleId());

                return ResponseEntity.ok(response);
        }

        /**
         * ⭐ ENDPOINT 3: Cancel Reservation
         * 
         * Called when:
         * - Payment fails
         * - User cancels during payment
         * - User takes too long and wants to start over
         * 
         * Sets reservation status to CANCELLED, freeing the vehicle.
         * 
         * @param reservationId UUID of the reservation to cancel
         * @param reason        Optional cancellation reason (query parameter)
         * @return 204 NO CONTENT on success
         *         404 NOT FOUND if reservation doesn't exist
         *         400 BAD REQUEST if reservation is not PENDING
         */
        @DeleteMapping("/reservations/{reservationId}")
        @Operation(summary = "Cancel reservation", description = "Cancels a PENDING reservation. Used when payment fails or user cancels.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "204", description = "Reservation cancelled successfully"),
                        @ApiResponse(responseCode = "404", description = "Reservation not found"),
                        @ApiResponse(responseCode = "400", description = "Reservation is not in PENDING status")
        })
        public ResponseEntity<Void> cancelReservation(
                        @Parameter(description = "Reservation ID to cancel", required = true) @PathVariable UUID reservationId,
                        @Parameter(description = "Reason for cancellation") @RequestParam(required = false) String reason) {

                log.info("DELETE /reservations/{} - Cancelling reservation, reason: {}",
                                reservationId, reason);

                reservationService.cancelReservation(reservationId, reason);

                log.info("Reservation cancelled: reservationId={}", reservationId);

                return ResponseEntity.noContent().build();
        }

        /**
         * 📊 ENDPOINT 4: Check Availability (Optional Pre-Check)
         * 
         * Returns count of available vehicles for a model in a date range.
         * This is a lightweight check BEFORE creating a reservation.
         * 
         * Note: This is eventually consistent - availability can change
         * between this check and actual reservation creation.
         * 
         * Use case:
         * - Show "3 vehicles available" on the booking form
         * - Disable booking button if availableCount = 0
         * 
         * @param modelPublicId Car model UUID to check
         * @param startDate     Booking start date (ISO 8601 format)
         * @param endDate       Booking end date (ISO 8601 format)
         * @return 200 OK with availability count
         *         400 BAD REQUEST if invalid date range
         */
        @GetMapping("/models/{modelPublicId}/availability-count")
        @Operation(summary = "Check vehicle availability", description = "Returns count of available vehicles for a model in a date range. This is a pre-check before creating a reservation.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Availability check successful", content = @Content(schema = @Schema(implementation = AvailabilityCheckResponse.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid date range")
        })
        public ResponseEntity<AvailabilityCheckResponse> checkAvailability(
                        @Parameter(description = "Car model public ID", required = true) @PathVariable UUID modelPublicId,
                        @Parameter(description = "Booking start date (ISO 8601: yyyy-MM-dd'T'HH:mm:ss)", required = true) @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
                        @Parameter(description = "Booking end date (ISO 8601: yyyy-MM-dd'T'HH:mm:ss)", required = true) @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

                log.debug("GET /models/{}/availability-count - Checking availability for dates: {} to {}",
                                modelPublicId, startDate, endDate);

                AvailabilityCheckResponse response = reservationService.checkAvailability(
                                modelPublicId,
                                startDate,
                                endDate);

                log.debug("Availability check result: {} vehicles available", response.getAvailableCount());

                return ResponseEntity.ok(response);
        }

        /**
         * 🔍 BONUS ENDPOINT: Get Reservation Details
         * 
         * Allows checking the status of an existing reservation.
         * Useful for:
         * - Debugging
         * - Customer support
         * - Recovering from page refresh during payment
         * 
         * @param reservationId UUID of the reservation
         * @return 200 OK with reservation details
         *         404 NOT FOUND if reservation doesn't exist
         */
        @GetMapping("/reservations/{reservationId}")
        @Operation(summary = "Get reservation details", description = "Retrieves details of an existing reservation by ID")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Reservation found"),
                        @ApiResponse(responseCode = "404", description = "Reservation not found")
        })
        public ResponseEntity<?> getReservationDetails(
                        @Parameter(description = "Reservation ID", required = true) @PathVariable UUID reservationId) {

                log.debug("GET /reservations/{} - Fetching reservation details", reservationId);

                // This would need a getReservationById method in the service
                // For now, just return a placeholder
                // TODO: Implement getReservationById in ReservationService

                return ResponseEntity.ok()
                                .body("Reservation details endpoint - To be implemented");
        }
}