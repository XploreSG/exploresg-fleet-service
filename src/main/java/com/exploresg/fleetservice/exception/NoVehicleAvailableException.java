package com.exploresg.fleetservice.exception;

import java.util.UUID;

/**
 * Exception thrown when no vehicles are available for the requested dates
 */
public class NoVehicleAvailableException extends RuntimeException {
    private final UUID modelPublicId;

    public NoVehicleAvailableException(UUID modelPublicId) {
        super("No vehicles available for model: " + modelPublicId);
        this.modelPublicId = modelPublicId;
    }

    public NoVehicleAvailableException(UUID modelPublicId, String message) {
        super(message);
        this.modelPublicId = modelPublicId;
    }

    public UUID getModelPublicId() {
        return modelPublicId;
    }
}
