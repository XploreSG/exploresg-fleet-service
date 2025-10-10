package com.exploresg.fleetservice.controller;

import com.exploresg.fleetservice.dto.CreateBookingRecordRequest;
import com.exploresg.fleetservice.exception.VehicleNotAvailableException;
import com.exploresg.fleetservice.model.VehicleBookingRecord;
import com.exploresg.fleetservice.service.VehicleBookingRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for managing vehicle booking records.
 * This is the integration point for the Booking Service.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/fleet/booking-records")
@RequiredArgsConstructor
public class VehicleBookingRecordController {

    private final VehicleBookingRecordService bookingRecordService;

    /**
     * Check if a vehicle is available for the specified date range.
     * This is the PRIMARY endpoint the booking service should call before creating
     * a booking.
     * 
     * GET
     * /api/v1/fleet/booking-records/availability?vehicleId={id}&startDate={date}&endDate={date}
     */
    @GetMapping("/availability")
    public ResponseEntity<AvailabilityResponse> checkAvailability(
            @RequestParam UUID vehicleId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        log.info("Checking availability for vehicle {} from {} to {}", vehicleId, startDate, endDate);

        boolean available = bookingRecordService.isVehicleAvailable(vehicleId, startDate, endDate);

        return ResponseEntity.ok(new AvailabilityResponse(available));
    }

    /**
     * Create a new booking record.
     * Called by the booking service when a booking is confirmed.
     * 
     * POST /api/v1/fleet/booking-records
     */
    @PostMapping
    public ResponseEntity<?> createBookingRecord(@Valid @RequestBody CreateBookingRecordRequest request) {
        log.info("Creating booking record for vehicle {} (booking: {})",
                request.getVehicleId(), request.getBookingId());

        try {
            VehicleBookingRecord bookingRecord = bookingRecordService.createBookingRecord(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(bookingRecord);
        } catch (VehicleNotAvailableException e) {
            log.warn("Failed to create booking record: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Cancel a booking record by the booking service's booking ID.
     * Called when a booking is cancelled in the booking service.
     * 
     * DELETE /api/v1/fleet/booking-records/by-booking/{bookingId}
     */
    @DeleteMapping("/by-booking/{bookingId}")
    public ResponseEntity<?> cancelBookingByBookingId(@PathVariable UUID bookingId) {
        log.info("Cancelling booking records for booking {}", bookingId);

        boolean cancelled = bookingRecordService.cancelBookingByBookingId(bookingId);

        if (cancelled) {
            return ResponseEntity.ok(new MessageResponse("Booking cancelled successfully"));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("No booking records found for booking ID: " + bookingId));
        }
    }

    /**
     * Cancel a specific booking record by its ID.
     * 
     * DELETE /api/v1/fleet/booking-records/{recordId}
     */
    @DeleteMapping("/{recordId}")
    public ResponseEntity<?> cancelBooking(@PathVariable UUID recordId) {
        log.info("Cancelling booking record {}", recordId);

        boolean cancelled = bookingRecordService.cancelBooking(recordId);

        if (cancelled) {
            return ResponseEntity.ok(new MessageResponse("Booking record cancelled successfully"));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("Booking record not found: " + recordId));
        }
    }

    /**
     * Get all booking records for a specific vehicle.
     * 
     * GET /api/v1/fleet/booking-records/vehicle/{vehicleId}
     */
    @GetMapping("/vehicle/{vehicleId}")
    public ResponseEntity<List<VehicleBookingRecord>> getVehicleBookings(@PathVariable UUID vehicleId) {
        log.info("Fetching booking records for vehicle {}", vehicleId);

        List<VehicleBookingRecord> bookings = bookingRecordService.getVehicleBookings(vehicleId);
        return ResponseEntity.ok(bookings);
    }

    /**
     * Get active (current and future) bookings for a vehicle.
     * 
     * GET /api/v1/fleet/booking-records/vehicle/{vehicleId}/active
     */
    @GetMapping("/vehicle/{vehicleId}/active")
    public ResponseEntity<List<VehicleBookingRecord>> getActiveVehicleBookings(@PathVariable UUID vehicleId) {
        log.info("Fetching active booking records for vehicle {}", vehicleId);

        List<VehicleBookingRecord> bookings = bookingRecordService.getActiveVehicleBookings(vehicleId);
        return ResponseEntity.ok(bookings);
    }

    /**
     * Check if a vehicle is currently booked (has an active booking right now).
     * 
     * GET /api/v1/fleet/booking-records/vehicle/{vehicleId}/is-booked
     */
    @GetMapping("/vehicle/{vehicleId}/is-booked")
    public ResponseEntity<BookingStatusResponse> isVehicleCurrentlyBooked(@PathVariable UUID vehicleId) {
        log.info("Checking if vehicle {} is currently booked", vehicleId);

        boolean isBooked = bookingRecordService.isVehicleCurrentlyBooked(vehicleId);
        return ResponseEntity.ok(new BookingStatusResponse(isBooked));
    }

    // ===== Response DTOs =====

    record AvailabilityResponse(boolean available) {
    }

    record BookingStatusResponse(boolean isBooked) {
    }

    record MessageResponse(String message) {
    }

    record ErrorResponse(String error) {
    }
}