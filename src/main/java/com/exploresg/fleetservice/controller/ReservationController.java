package com.exploresg.fleetservice.controller;

import com.exploresg.fleetservice.model.VehicleBookingRecord;
import com.exploresg.fleetservice.service.ReservationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Internal-facing API for the Booking Service to manage vehicle reservations
 */
@RestController
@RequestMapping("/api/v1/fleet")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    /**
     * STEP 1: Check availability count
     * 
     * GET
     * /api/v1/fleet/models/{modelPublicId}/availability-count?startDate=...&endDate=...
     * 
     * Called by Booking Service BEFORE showing payment screen
     */
    @GetMapping("/models/{modelPublicId}/availability-count")
    @PreAuthorize("hasAuthority('ROLE_INTERNAL_SERVICE') or hasAuthority('ROLE_USER')")
    public ResponseEntity<AvailabilityResponse> checkAvailability(
            @PathVariable UUID modelPublicId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        int count = reservationService.getAvailabilityCount(modelPublicId, startDate, endDate);

        return ResponseEntity.ok(new AvailabilityResponse(
                modelPublicId,
                count,
                count > 0,
                startDate,
                endDate));
    }

    /**
     * STEP 2: Create temporary reservation (PRE-PAYMENT)
     * 
     * POST /api/v1/fleet/reservations/temporary
     * 
     * Called by Booking Service BEFORE payment processing
     * Creates a 30-second hold on ONE vehicle
     */
    @PostMapping("/reservations/temporary")
    @PreAuthorize("hasAuthority('ROLE_INTERNAL_SERVICE') or hasAuthority('ROLE_USER')")
    public ResponseEntity<ReservationResponse> createTemporaryReservation(
            @Valid @RequestBody CreateReservationRequest request) {

        try {
            VehicleBookingRecord reservation = reservationService.createTemporaryReservation(
                    request.getModelPublicId(),
                    request.getBookingId(),
                    request.getStartDate(),
                    request.getEndDate());

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(ReservationResponse.from(reservation));

        } catch (ReservationService.NoVehicleAvailableException e) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(ReservationResponse.error(e.getMessage()));
        }
    }

    /**
     * STEP 3: Confirm reservation (POST-PAYMENT)
     * 
     * POST /api/v1/fleet/reservations/{reservationId}/confirm
     * 
     * Called by Booking Service AFTER successful payment
     * Converts PENDING reservation to CONFIRMED
     */
    @PostMapping("/reservations/{reservationId}/confirm")
    @PreAuthorize("hasAuthority('ROLE_INTERNAL_SERVICE')")
    public ResponseEntity<ReservationResponse> confirmReservation(
            @PathVariable UUID reservationId,
            @Valid @RequestBody ConfirmReservationRequest request) {

        try {
            VehicleBookingRecord reservation = reservationService.confirmReservation(
                    reservationId,
                    request.getPaymentReference());

            return ResponseEntity.ok(ReservationResponse.from(reservation));

        } catch (ReservationService.ReservationNotFoundException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ReservationResponse.error(e.getMessage()));

        } catch (ReservationService.ReservationExpiredException e) {
            return ResponseEntity
                    .status(HttpStatus.GONE)
                    .body(ReservationResponse.error(e.getMessage()));
        }
    }

    /**
     * Cancel a reservation
     * Can be called by user or automatically if payment fails
     */
    @DeleteMapping("/reservations/{reservationId}")
    @PreAuthorize("hasAuthority('ROLE_INTERNAL_SERVICE') or hasAuthority('ROLE_USER')")
    public ResponseEntity<Void> cancelReservation(
            @PathVariable UUID reservationId,
            @RequestParam(required = false) String reason) {

        try {
            reservationService.cancelReservation(
                    reservationId,
                    reason != null ? reason : "User cancelled");
            return ResponseEntity.noContent().build();

        } catch (ReservationService.ReservationNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ===== DTOs =====

    @Data
    public static class AvailabilityResponse {
        private UUID modelPublicId;
        private int availableCount;
        private boolean available;
        private LocalDateTime startDate;
        private LocalDateTime endDate;

        public AvailabilityResponse(UUID modelPublicId, int count, boolean available,
                LocalDateTime startDate, LocalDateTime endDate) {
            this.modelPublicId = modelPublicId;
            this.availableCount = count;
            this.available = available;
            this.startDate = startDate;
            this.endDate = endDate;
        }
    }

    @Data
    public static class CreateReservationRequest {
        @NotNull(message = "Model public ID is required")
        private UUID modelPublicId;

        @NotNull(message = "Booking ID is required")
        private UUID bookingId;

        @NotNull(message = "Start date is required")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        private LocalDateTime startDate;

        @NotNull(message = "End date is required")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        private LocalDateTime endDate;
    }

    @Data
    public static class ConfirmReservationRequest {
        @NotNull(message = "Payment reference is required")
        private String paymentReference;
    }

    @Data
    public static class ReservationResponse {
        private boolean success;
        private String message;
        private UUID reservationId;
        private UUID vehicleId;
        private UUID bookingId;
        private String status;
        private LocalDateTime expiresAt;

        public static ReservationResponse from(VehicleBookingRecord record) {
            ReservationResponse response = new ReservationResponse();
            response.setSuccess(true);
            response.setReservationId(record.getId());
            response.setVehicleId(record.getVehicleId());
            response.setBookingId(record.getBookingId());
            response.setStatus(record.getReservationStatus().name());
            response.setExpiresAt(record.getExpiresAt());
            response.setMessage("Reservation " +
                    (record.getReservationStatus() == VehicleBookingRecord.ReservationStatus.CONFIRMED
                            ? "confirmed"
                            : "created"));
            return response;
        }

        public static ReservationResponse error(String message) {
            ReservationResponse response = new ReservationResponse();
            response.setSuccess(false);
            response.setMessage(message);
            return response;
        }
    }
}