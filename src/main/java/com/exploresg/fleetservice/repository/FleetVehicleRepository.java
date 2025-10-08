package com.exploresg.fleetservice.repository;

import com.exploresg.fleetservice.model.CarModel;
import com.exploresg.fleetservice.model.FleetVehicle;
import com.exploresg.fleetservice.model.VehicleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    List<FleetVehicle> findByOwnerIdAndStatus(UUID ownerId, VehicleStatus status);

    /**
     * Retrieves ALL physical vehicles for a specific operator (regardless of
     * status).
     * Useful for fleet management and testing purposes.
     */
    List<FleetVehicle> findByOwnerId(UUID ownerId);

    /**
     * Retrieves ALL physical vehicles for a specific operator with pagination
     * support.
     * Useful for fleet management and testing purposes.
     * 
     * @param ownerId  The owner/operator UUID
     * @param pageable Pagination information
     * @return Page of FleetVehicle entities
     */
    Page<FleetVehicle> findByOwnerId(UUID ownerId, Pageable pageable);

    /**
     * Search fleet vehicles with optional filters and pagination.
     * Uses native SQL query to avoid JPQL parameter binding issues with PostgreSQL.
     * All search parameters are optional (use null to skip filtering).
     * 
     * @param ownerId      The owner/operator UUID (required)
     * @param licensePlate License plate to search (partial match)
     * @param status       Vehicle status to filter by
     * @param model        Car model name to search (partial match)
     * @param manufacturer Manufacturer name to search (partial match)
     * @param location     Current location to search (partial match)
     * @param pageable     Pagination information
     * @return Page of FleetVehicle entities matching the search criteria
     */
    @Query(value = "SELECT f.* FROM fleet_vehicles f " +
            "JOIN car_models cm ON cm.id = f.car_model_id " +
            "WHERE f.owner_id = :ownerId " +
            "AND (:licensePlate IS NULL OR LOWER(f.license_plate) LIKE LOWER(CONCAT('%', :licensePlate, '%'))) " +
            "AND (:status IS NULL OR f.status = CAST(:status AS TEXT)) " +
            "AND (:model IS NULL OR LOWER(cm.model) LIKE LOWER(CONCAT('%', :model, '%'))) " +
            "AND (:manufacturer IS NULL OR LOWER(cm.manufacturer) LIKE LOWER(CONCAT('%', :manufacturer, '%'))) " +
            "AND (:location IS NULL OR LOWER(f.current_location) LIKE LOWER(CONCAT('%', :location, '%')))", countQuery = "SELECT COUNT(*) FROM fleet_vehicles f "
                    +
                    "JOIN car_models cm ON cm.id = f.car_model_id " +
                    "WHERE f.owner_id = :ownerId " +
                    "AND (:licensePlate IS NULL OR LOWER(f.license_plate) LIKE LOWER(CONCAT('%', :licensePlate, '%'))) "
                    +
                    "AND (:status IS NULL OR f.status = CAST(:status AS TEXT)) " +
                    "AND (:model IS NULL OR LOWER(cm.model) LIKE LOWER(CONCAT('%', :model, '%'))) " +
                    "AND (:manufacturer IS NULL OR LOWER(cm.manufacturer) LIKE LOWER(CONCAT('%', :manufacturer, '%'))) "
                    +
                    "AND (:location IS NULL OR LOWER(f.current_location) LIKE LOWER(CONCAT('%', :location, '%')))", nativeQuery = true)
    Page<FleetVehicle> searchFleetVehicles(
            UUID ownerId,
            String licensePlate,
            String status,
            String model,
            String manufacturer,
            String location,
            Pageable pageable);

    /**
     * Count vehicles by owner and status.
     * Used for dashboard statistics.
     */
    long countByOwnerIdAndStatus(UUID ownerId, VehicleStatus status);

    /**
     * Count total vehicles by owner.
     * Used for dashboard statistics.
     */
    long countByOwnerId(UUID ownerId);
}