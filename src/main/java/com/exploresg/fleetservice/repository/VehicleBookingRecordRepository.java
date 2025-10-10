package com.exploresg.fleetservice.repository;

import com.exploresg.fleetservice.model.VehicleBookingRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface VehicleBookingRecordRepository extends JpaRepository<VehicleBookingRecord, UUID> {

    /**
     * Finds any booking record for a specific vehicle that overlaps with the given date range.
     * This is the definitive check for vehicle availability.
     *
     * Two periods overlap if (StartA < EndB) and (EndA > StartB).
     *
     * @param vehicleId The ID of the vehicle to check.
     * @param startDate The start of the desired booking period.
     * @param endDate The end of the desired booking period.
     * @return A list of overlapping booking records. If the list is empty, the vehicle is available.
     */
    @Query("SELECT vbr FROM VehicleBookingRecord vbr " +
           "WHERE vbr.vehicleId = :vehicleId " +
           "AND vbr.bookingStartDate < :endDate " +
           "AND vbr.bookingEndDate > :startDate")
    List<VehicleBookingRecord> findOverlappingBooking(
            @Param("vehicleId") UUID vehicleId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}