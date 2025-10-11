package com.exploresg.fleetservice.scheduler;

import com.exploresg.fleetservice.service.ReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Background scheduler to clean up expired PENDING reservations
 * 
 * Runs every 10 seconds to ensure expired temporary reservations
 * are released quickly and vehicles become available again
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationCleanupScheduler {

    private final ReservationService reservationService;

    /**
     * Clean up expired PENDING reservations every 10 seconds
     * 
     * This ensures that vehicles with expired temporary holds
     * become available for other users quickly
     */
    @Scheduled(fixedDelay = 10000) // Run every 10 seconds
    public void cleanupExpiredReservations() {
        try {
            int cleaned = reservationService.cleanupExpiredReservations();

            if (cleaned > 0) {
                log.info("Cleaned up {} expired temporary reservations", cleaned);
            }

        } catch (Exception e) {
            log.error("Error cleaning up expired reservations", e);
            // Don't throw - let the scheduler continue
        }
    }
}