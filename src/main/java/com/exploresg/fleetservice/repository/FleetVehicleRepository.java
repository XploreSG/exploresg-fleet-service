package com.exploresg.fleetservice.repository;

import com.exploresg.fleetservice.model.CarModel;
import com.exploresg.fleetservice.model.FleetVehicle;
import com.exploresg.fleetservice.model.VehicleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.UUID; // <-- NEW IMPORT
import java.util.List;

// JpaRepository<Entity, ID_TYPE>
public interface FleetVehicleRepository extends JpaRepository<FleetVehicle, UUID> { // <-- ID TYPE CHANGED TO UUID

    /**
     * Retrieves all unique CarModel entities that currently have at least one
     * physical vehicle instance available in the fleet.
     * Uses a JOIN (implicit via object path) and DISTINCT to prevent duplicate
     * models.
     */
    @Query("SELECT DISTINCT f.carModel FROM FleetVehicle f WHERE f.status = 'AVAILABLE'")
    List<CarModel> findAvailableCarModels();

    /**
     * Retrieves all specific FleetVehicle instances for a given model that are
     * AVAILABLE.
     * This is used for the user to select a specific car/location.
     */
    List<FleetVehicle> findByCarModelAndStatus(CarModel carModel, VehicleStatus status);

    /**
     * Find all available vehicles for a specific car model.
     * Used to aggregate pricing and availability data.
     */
    List<FleetVehicle> findByCarModelIdAndStatus(Long carModelId, VehicleStatus status);

    /**
     * Find all vehicles with a specific status.
     */
    List<FleetVehicle> findByStatus(VehicleStatus status);

    /**
     * Retrieves all physical vehicles for a specific operator that are AVAILABLE.
     * This is the source data for the per-operator model list.
     */
    List<FleetVehicle> findByOwnerIdAndStatus(Long ownerId, VehicleStatus status);
}