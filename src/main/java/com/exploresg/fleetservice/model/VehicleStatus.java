package com.exploresg.fleetservice.model;

/**
 * Represents the possible statuses of a vehicle in the fleet.
 */
public enum VehicleStatus {
    /**
     * The vehicle is available for rent.
     */
    AVAILABLE,

    /**
     * The vehicle is currently undergoing maintenance and cannot be rented.
     */
    UNDER_MAINTENANCE,

    /**
     * The vehicle is currently booked and unavailable.
     */
    BOOKED
}
