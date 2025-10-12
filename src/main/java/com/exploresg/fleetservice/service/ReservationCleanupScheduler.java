package com.exploresg.fleetservice.service;

import com.exploresg.fleetservice.repository.VehicleBookingRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 🧹 Scheduled Cleanup Job
 * 
 * Automatically expires PENDING reservations that have passed their expiry
 * time.
 * Runs every 10 seconds to ensure timely cleanup.
 * 
 * This prevents resource leaks when users:
 * - Abandon the payment screen
 * - Close browser during payment
 * - Take too long to complete payment
 * 
 * Expired reservations automatically free up the vehicle for other users.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationCleanupScheduler {

    private final VehicleBookingRecordRepository bookingRecordRepository;

    /**
     * Cleanup expired PENDING reservations
     * 
     * Runs every 10 seconds (10000 milliseconds)
     * Uses fixedDelay to wait 10s after previous execution completes
     * 
     * This ensures expired reservations are cleaned up within 10-40 seconds:
     * - Reservation expires at T+30s
     * - Cleanup runs at most every 10s
     * - So cleanup happens between T+30s and T+40s
     */
    @Scheduled(fixedDelay = 10000, initialDelay = 5000)
    @Transactional
    public void cleanupExpiredReservations() {
        try {
            LocalDateTime now = LocalDateTime.now();

            // Bulk update all expired PENDING reservations to EXPIRED
            int expiredCount = bookingRecordRepository.expirePendingReservations(now);

            if (expiredCount > 0) {
                log.info("Expired {} PENDING reservation(s) at {}", expiredCount, now);
            } else {
                log.debug("No expired reservations found at {}", now);
            }

        } catch (Exception e) {
            // Log error but don't stop the scheduler
            log.error("Error during reservation cleanup: {}", e.getMessage(), e);
        }
    }

    /**
     * Optional: Cleanup very old CANCELLED/EXPIRED records (run daily)
     * 
     * This is for housekeeping - removes records older than 30 days
     * to keep the table size manageable.
     */
    @Scheduled(cron = "0 0 2 * * *") // Run at 2 AM daily
    @Transactional
    public void archiveOldReservations() {
        try {
            // TODO: Implement archival if needed
            // Could move old records to an archive table or delete them
            log.debug("Running daily archival job");

        } catch (Exception e) {
            log.error("Error during reservation archival: {}", e.getMessage(), e);
        }
    }
}