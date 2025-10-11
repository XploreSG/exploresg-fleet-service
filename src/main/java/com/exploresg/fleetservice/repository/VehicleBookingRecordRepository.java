package com.exploresg.fleetservice.repository;

import com.exploresg.fleetservice.model.VehicleBookingRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface VehicleBookingRecordRepository extends JpaRepository<VehicleBookingRecord, UUID> {

  /**
   * Check if there are any CONFIRMED or PENDING bookings that overlap with the
   * given date range
   * Uses efficient date range overlap logic: (start1 < end2) AND (end1 > start2)
   */
  @Query("""
      SELECT COUNT(vbr) > 0
      FROM VehicleBookingRecord vbr
      WHERE vbr.vehicleId = :vehicleId
        AND vbr.reservationStatus IN ('CONFIRMED', 'PENDING')
        AND vbr.bookingStartDate < :endDate
        AND vbr.bookingEndDate > :startDate
      """)
  boolean existsConflictingBooking(
      @Param("vehicleId") UUID vehicleId,
      @Param("startDate") LocalDateTime startDate,
      @Param("endDate") LocalDateTime endDate);

  /**
   * Find all expired PENDING reservations
   * Used by background cleanup job
   */
  @Query("""
      SELECT vbr
      FROM VehicleBookingRecord vbr
      WHERE vbr.reservationStatus = 'PENDING'
        AND vbr.expiresAt < :currentTime
      """)
  List<VehicleBookingRecord> findExpiredPendingReservations(
      @Param("currentTime") LocalDateTime currentTime);

  /**
   * Find all bookings for a specific vehicle within a date range
   * Useful for debugging and reporting
   */
  @Query("""
      SELECT vbr
      FROM VehicleBookingRecord vbr
      WHERE vbr.vehicleId = :vehicleId
        AND vbr.reservationStatus IN ('CONFIRMED', 'PENDING')
        AND vbr.bookingStartDate < :endDate
        AND vbr.bookingEndDate > :startDate
      ORDER BY vbr.bookingStartDate ASC
      """)
  List<VehicleBookingRecord> findBookingsForVehicleInDateRange(
      @Param("vehicleId") UUID vehicleId,
      @Param("startDate") LocalDateTime startDate,
      @Param("endDate") LocalDateTime endDate);

  /**
   * Find all bookings for a specific booking ID (from booking service)
   */
  List<VehicleBookingRecord> findByBookingId(UUID bookingId);

  /**
   * Count active (CONFIRMED + PENDING) bookings for a vehicle
   */
  @Query("""
      SELECT COUNT(vbr)
      FROM VehicleBookingRecord vbr
      WHERE vbr.vehicleId = :vehicleId
        AND vbr.reservationStatus IN ('CONFIRMED', 'PENDING')
        AND vbr.bookingEndDate > :currentTime
      """)
  long countActiveBookingsForVehicle(
      @Param("vehicleId") UUID vehicleId,
      @Param("currentTime") LocalDateTime currentTime);
}