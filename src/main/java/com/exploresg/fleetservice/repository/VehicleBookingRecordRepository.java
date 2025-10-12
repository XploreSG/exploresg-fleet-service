package com.exploresg.fleetservice.repository;

// import com.exploresg.fleetservice.entity.VehicleBookingRecord;
// import com.exploresg.fleetservice.entity.VehicleBookingRecord.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.exploresg.fleetservice.model.VehicleBookingRecord;
import com.exploresg.fleetservice.model.VehicleBookingRecord.ReservationStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VehicleBookingRecordRepository extends JpaRepository<VehicleBookingRecord, UUID> {

  /**
   * Find reservation by booking ID (for idempotency checks)
   */
  Optional<VehicleBookingRecord> findByBookingId(UUID bookingId);

  /**
   * Find all PENDING reservations that have expired
   */
  @Query("SELECT vbr FROM VehicleBookingRecord vbr " +
      "WHERE vbr.reservationStatus = 'PENDING' " +
      "AND vbr.expiresAt < :now")
  List<VehicleBookingRecord> findExpiredPendingReservations(@Param("now") LocalDateTime now);

  /**
   * Bulk update expired PENDING reservations to EXPIRED status
   * Returns the number of records updated
   */
  @Modifying
  @Query("UPDATE VehicleBookingRecord vbr " +
      "SET vbr.reservationStatus = 'EXPIRED', " +
      "    vbr.lastUpdatedAt = :now " +
      "WHERE vbr.reservationStatus = 'PENDING' " +
      "AND vbr.expiresAt < :now")
  int expirePendingReservations(@Param("now") LocalDateTime now);

  /**
   * Check if a vehicle has any overlapping bookings (CONFIRMED or PENDING)
   */
  @Query("SELECT COUNT(vbr) > 0 FROM VehicleBookingRecord vbr " +
      "WHERE vbr.vehicle.id = :vehicleId " +
      "AND vbr.reservationStatus IN ('CONFIRMED', 'PENDING') " +
      "AND vbr.bookingStartDate < :endDate " +
      "AND vbr.bookingEndDate > :startDate")
  boolean hasOverlappingBookings(
      @Param("vehicleId") UUID vehicleId,
      @Param("startDate") LocalDateTime startDate,
      @Param("endDate") LocalDateTime endDate);

  /**
   * Count available vehicles for a car model in a date range
   * Excludes vehicles with CONFIRMED or PENDING bookings that overlap
   */
  @Query("SELECT COUNT(DISTINCT fv.id) " +
      "FROM FleetVehicle fv " +
      "WHERE fv.carModel.publicId = :modelPublicId " +
      "AND fv.status = 'AVAILABLE' " +
      "AND NOT EXISTS (" +
      "    SELECT 1 FROM VehicleBookingRecord vbr " +
      "    WHERE vbr.vehicle.id = fv.id " +
      "    AND vbr.reservationStatus IN ('CONFIRMED', 'PENDING') " +
      "    AND vbr.bookingStartDate < :endDate " +
      "    AND vbr.bookingEndDate > :startDate" +
      ")")
  long countAvailableVehicles(
      @Param("modelPublicId") UUID modelPublicId,
      @Param("startDate") LocalDateTime startDate,
      @Param("endDate") LocalDateTime endDate);

  /**
   * Find all bookings for a specific booking ID across all services
   */
  List<VehicleBookingRecord> findAllByBookingId(UUID bookingId);

  /**
   * Find all CONFIRMED bookings for a vehicle
   */
  List<VehicleBookingRecord> findByVehicleIdAndReservationStatus(
      UUID vehicleId,
      ReservationStatus status);
}