package com.exploresg.fleetservice.model;

/**
 * Represents the possible OPERATIONAL statuses of a vehicle in the fleet.
 * The BOOKED status is now managed by the VehicleBookingRecord table.
 */
public enum VehicleStatus {
    /**
     * The vehicle is available for rent.
     */
    AVAILABLE,

    /**
     * The vehicle is currently undergoing maintenance and cannot be rented.
     */
    UNDER_MAINTENANCE

    // The 'BOOKED' status has been removed. A vehicle's booking status is now
    // determined
    // by the presence of a corresponding entry in the VehicleBookingRecord table.
}