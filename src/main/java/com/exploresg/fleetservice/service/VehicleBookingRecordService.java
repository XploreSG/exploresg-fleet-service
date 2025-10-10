package com.exploresg.fleetservice.service;

import com.exploresg.fleetservice.dto.CreateBookingRecordRequest;
import com.exploresg.fleetservice.exception.VehicleNotAvailableException;
import com.exploresg.fleetservice.model.FleetVehicle;
import com.exploresg.fleetservice.model.VehicleBookingRecord;
import com.exploresg.fleetservice.model.VehicleStatus;
import com.exploresg.fleetservice.repository.FleetVehicleRepository;
import com.exploresg.fleetservice.repository.VehicleBookingRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing vehicle booking records.
 * Handles the creation, validation, and management of vehicle bookings
 * separate from the vehicle's operational status.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleBookingRecordService {

    private final VehicleBookingRecordRepository bookingRecordRepository;
    private final FleetVehicleRepository fleetVehicleRepository;

    /**
     * Checks if a vehicle is available for booking during the specified period.
     * A vehicle is available if:
     * 1. It exists
     * 2. Its operational status is AVAILABLE (not under maintenance)
     * 3. It has no overlapping bookings in the date range
     *
     * @param vehicleId The ID of the vehicle to check
     * @param startDate Start of the desired booking period
     * @param endDate   End of the desired booking period
     * @return true if the vehicle is available, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean isVehicleAvailable(UUID vehicleId, LocalDateTime startDate, LocalDateTime endDate) {
        // 1. Check if vehicle exists and is operationally available
        Optional<FleetVehicle> vehicleOpt = fleetVehicleRepository.findById(vehicleId);
        if (vehicleOpt.isEmpty()) {
            log.debug("Vehicle {} not found", vehicleId);
            return false;
        }

        FleetVehicle vehicle = vehicleOpt.get();
        if (vehicle.getStatus() != VehicleStatus.AVAILABLE) {
            log.debug("Vehicle {} is not operationally available (status: {})", vehicleId, vehicle.getStatus());
            return false;
        }

        // 2. Check for booking conflicts
        List<VehicleBookingRecord> overlappingBookings = bookingRecordRepository
                .findOverlappingBooking(vehicleId, startDate, endDate);

        boolean hasConflict = !overlappingBookings.isEmpty();
        if (hasConflict) {
            log.debug("Vehicle {} has {} overlapping bookings", vehicleId, overlappingBookings.size());
        }

        return !hasConflict;
    }

    /**
     * Creates a new booking record for a vehicle.
     * Validates availability before creating the booking.
     *
     * @param request The booking record creation request
     * @return The created booking record
     * @throws VehicleNotAvailableException if the vehicle is not available
     */
    @Transactional
    public VehicleBookingRecord createBookingRecord(CreateBookingRecordRequest request) {
        // Validate availability
        if (!isVehicleAvailable(request.getVehicleId(), request.getStartDate(), request.getEndDate())) {
            throw new VehicleNotAvailableException(
                    "Vehicle " + request.getVehicleId() + " is not available for the requested period");
        }

        // Create booking record
        VehicleBookingRecord bookingRecord = VehicleBookingRecord.builder()
                .vehicleId(request.getVehicleId())
                .bookingId(request.getBookingId())
                .bookingStartDate(request.getStartDate())
                .bookingEndDate(request.getEndDate())
                .build();

        VehicleBookingRecord saved = bookingRecordRepository.save(bookingRecord);
        log.info("Created booking record {} for vehicle {} (booking: {})",
                saved.getId(), request.getVehicleId(), request.getBookingId());

        return saved;
    }

    /**
     * Checks if a vehicle is currently booked (has an active booking).
     *
     * @param vehicleId The ID of the vehicle
     * @return true if the vehicle has any active booking, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean isVehicleCurrentlyBooked(UUID vehicleId) {
        LocalDateTime now = LocalDateTime.now();
        List<VehicleBookingRecord> currentBookings = bookingRecordRepository
                .findOverlappingBooking(vehicleId, now, now);
        return !currentBookings.isEmpty();
    }

    /**
     * Gets all booking records for a specific vehicle.
     *
     * @param vehicleId The ID of the vehicle
     * @return List of all booking records for the vehicle
     */
    @Transactional(readOnly = true)
    public List<VehicleBookingRecord> getVehicleBookings(UUID vehicleId) {
        return bookingRecordRepository.findAll().stream()
                .filter(record -> record.getVehicleId().equals(vehicleId))
                .toList();
    }

    /**
     * Gets all active (current and future) bookings for a vehicle.
     *
     * @param vehicleId The ID of the vehicle
     * @return List of active booking records
     */
    @Transactional(readOnly = true)
    public List<VehicleBookingRecord> getActiveVehicleBookings(UUID vehicleId) {
        LocalDateTime now = LocalDateTime.now();
        return bookingRecordRepository.findAll().stream()
                .filter(record -> record.getVehicleId().equals(vehicleId))
                .filter(record -> record.getBookingEndDate().isAfter(now))
                .toList();
    }

    /**
     * Cancels a booking by deleting the booking record.
     * This makes the vehicle available for other bookings during that period.
     *
     * @param bookingRecordId The ID of the booking record to cancel
     * @return true if the booking was cancelled, false if not found
     */
    @Transactional
    public boolean cancelBooking(UUID bookingRecordId) {
        Optional<VehicleBookingRecord> bookingOpt = bookingRecordRepository.findById(bookingRecordId);
        if (bookingOpt.isEmpty()) {
            log.warn("Attempted to cancel non-existent booking record {}", bookingRecordId);
            return false;
        }

        bookingRecordRepository.deleteById(bookingRecordId);
        log.info("Cancelled booking record {} for vehicle {}",
                bookingRecordId, bookingOpt.get().getVehicleId());
        return true;
    }

    /**
     * Cancels a booking by the main booking service's booking ID.
     * Useful when the booking service needs to cancel a booking.
     *
     * @param bookingId The booking ID from the booking service
     * @return true if a booking was found and cancelled, false otherwise
     */
    @Transactional
    public boolean cancelBookingByBookingId(UUID bookingId) {
        List<VehicleBookingRecord> records = bookingRecordRepository.findAll().stream()
                .filter(record -> record.getBookingId().equals(bookingId))
                .toList();

        if (records.isEmpty()) {
            log.warn("No booking records found for booking ID {}", bookingId);
            return false;
        }

        records.forEach(record -> {
            bookingRecordRepository.delete(record);
            log.info("Cancelled booking record {} for booking {}", record.getId(), bookingId);
        });

        return true;
    }

    /**
     * Gets all currently booked vehicle IDs for a specific owner.
     * Useful for dashboard statistics.
     *
     * @param ownerId The fleet owner ID
     * @return List of vehicle IDs that are currently booked
     */
    @Transactional(readOnly = true)
    public List<UUID> getCurrentlyBookedVehicleIds(UUID ownerId) {
        LocalDateTime now = LocalDateTime.now();

        // Get all vehicles for this owner
        List<FleetVehicle> ownerVehicles = fleetVehicleRepository.findByOwnerId(ownerId);
        List<UUID> ownerVehicleIds = ownerVehicles.stream()
                .map(FleetVehicle::getId)
                .toList();

        // Find which ones are currently booked
        return ownerVehicleIds.stream()
                .filter(vehicleId -> {
                    List<VehicleBookingRecord> bookings = bookingRecordRepository
                            .findOverlappingBooking(vehicleId, now, now);
                    return !bookings.isEmpty();
                })
                .toList();
    }

    /**
     * Counts how many vehicles are currently booked for an owner.
     *
     * @param ownerId The fleet owner ID
     * @return The count of currently booked vehicles
     */
    @Transactional(readOnly = true)
    public long countCurrentlyBookedVehicles(UUID ownerId) {
        return getCurrentlyBookedVehicleIds(ownerId).size();
    }
}