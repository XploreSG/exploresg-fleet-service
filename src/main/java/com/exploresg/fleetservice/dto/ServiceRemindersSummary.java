package com.exploresg.fleetservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for service reminders in the fleet dashboard.
 * Tracks vehicles that need maintenance.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceRemindersSummary {
    private long overdue;    // vehicles past maintenance date
    private long dueSoon;    // vehicles due for maintenance in next 30 days
}
