package com.exploresg.fleetservice.controller;

import com.exploresg.fleetservice.constants.SecurityConstants;
import com.exploresg.fleetservice.dto.CreateCarModelRequest;
import com.exploresg.fleetservice.dto.OperatorCarModelDto;
import com.exploresg.fleetservice.model.CarModel;
import com.exploresg.fleetservice.service.CarModelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/fleet")
@RequiredArgsConstructor
public class FleetController {

    private final CarModelService carModelService;

    /**
     * Endpoint for an ADMIN to create a new master CarModel.
     * POST /api/v1/fleet/models
     * * @param request The request body containing car model details.
     * 
     * @return The created CarModel.
     */
    @PostMapping("/models")
    @PreAuthorize(SecurityConstants.HAS_ROLE_ADMIN)
    public ResponseEntity<CarModel> createCarModel(@Valid @RequestBody CreateCarModelRequest request) {
        CarModel createdCarModel = carModelService.createCarModel(request);
        return new ResponseEntity<>(createdCarModel, HttpStatus.CREATED);
    }

    /**
     * Public endpoint to browse available car models.
     * Returns one entry per operator-model combination.
     * GET /api/v1/fleet/models
     * * @return A list of available car models grouped by operator.
     */
    @GetMapping("/models")
    public ResponseEntity<List<OperatorCarModelDto>> getAvailableModels() {
        List<OperatorCarModelDto> models = carModelService.getAvailableModelsPerOperator();
        return ResponseEntity.ok(models);
    }

    /**
     * NEW CORE ENDPOINT: Public endpoint to browse available car models for a
     * specific operator.
     * Returns one entry per Car Model that has at least one vehicle available
     * in the specified operator's fleet.
     * GET /api/v1/fleet/operators/{operatorId}/models
     * * @param operatorId The ID of the fleet operator.
     * 
     * @return A list of available car models aggregated for the given operator.
     */
    @GetMapping("/operators/{operatorId}/models")
    public ResponseEntity<List<OperatorCarModelDto>> getAvailableModelsByOperator(@PathVariable UUID operatorId) {
        List<OperatorCarModelDto> models = carModelService.getAvailableModelsByOperator(operatorId);

        if (models.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(models);
    }

    /**
     * Admin endpoint to view ALL car models (master catalog).
     * GET /api/v1/fleet/models/all
     * * @return A list of all car models in the system.
     */
    @GetMapping("/models/all")
    @PreAuthorize(SecurityConstants.HAS_ROLE_ADMIN)
    public ResponseEntity<List<CarModel>> getAllCarModels() {
        List<CarModel> carModels = carModelService.getAllCarModels();
        return ResponseEntity.ok(carModels);
    }

    /**
     * Fleet Manager endpoint to view their own operator's car models.
     * Extracts the user ID from the JWT token and returns models under their
     * ownership.
     * The userId from JWT is used as the ownerId to filter vehicles in the fleet
     * table.
     * GET /api/v1/fleet/operators/fleet
     * 
     * @param jwt The authenticated user's JWT token containing userId.
     * @return A list of car models under the fleet manager's ownership.
     */
    @GetMapping("/operators/fleet")
    @PreAuthorize(SecurityConstants.HAS_ROLE_FLEET_MANAGER)
    public ResponseEntity<List<OperatorCarModelDto>> getMyFleetModels(@AuthenticationPrincipal Jwt jwt) {
        // Extract user ID from JWT token (userId is the ownerId in the fleet table)
        String userIdStr = jwt.getClaimAsString("userId");

        if (userIdStr == null || userIdStr.isEmpty()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        UUID userId = UUID.fromString(userIdStr);
        List<OperatorCarModelDto> models = carModelService.getAvailableModelsByOperator(userId);

        if (models.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(models);
    }

    /**
     * Fleet Manager endpoint to view ALL individual vehicles in their fleet (for
     * testing).
     * Returns detailed information about each physical vehicle owned by the fleet
     * manager.
     * GET /api/v1/fleet/operators/fleet/all
     * 
     * @param jwt The authenticated user's JWT token containing userId.
     * @return A list of all fleet vehicles owned by the fleet manager.
     */
    @GetMapping("/operators/fleet/all")
    @PreAuthorize(SecurityConstants.HAS_ROLE_FLEET_MANAGER)
    public ResponseEntity<List<com.exploresg.fleetservice.model.FleetVehicle>> getAllMyFleetVehicles(
            @AuthenticationPrincipal Jwt jwt) {
        // Extract user ID from JWT token (userId is the ownerId in the fleet table)
        String userIdStr = jwt.getClaimAsString("userId");

        if (userIdStr == null || userIdStr.isEmpty()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        UUID userId = UUID.fromString(userIdStr);
        List<com.exploresg.fleetservice.model.FleetVehicle> vehicles = carModelService
                .getAllFleetVehiclesByOwner(userId);

        if (vehicles.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(vehicles);
    }

    /**
     * Fleet Manager endpoint to view ALL individual vehicles in their fleet with
     * pagination and search support.
     * Returns detailed information about each physical vehicle owned by the fleet
     * manager with pagination and optional filtering.
     * GET /api/v1/fleet/operators/fleet/all/paginated
     * 
     * @param jwt           The authenticated user's JWT token containing userId.
     * @param page          The page number (0-indexed, default: 0).
     * @param size          The page size (default: 10).
     * @param sortBy        The field to sort by (default: "licensePlate").
     * @param sortDirection The sort direction - "asc" or "desc" (default: "asc").
     * @param licensePlate  Optional: Filter by license plate (partial match).
     * @param status        Optional: Filter by vehicle status (AVAILABLE, BOOKED,
     *                      UNDER_MAINTENANCE).
     * @param model         Optional: Filter by car model name (partial match).
     * @param manufacturer  Optional: Filter by manufacturer name (partial match).
     * @param location      Optional: Filter by current location (partial match).
     * @return A paginated response of fleet vehicles matching the search criteria.
     */
    @GetMapping("/operators/fleet/all/paginated")
    @PreAuthorize(SecurityConstants.HAS_ROLE_FLEET_MANAGER)
    public ResponseEntity<Page<com.exploresg.fleetservice.model.FleetVehicle>> getAllMyFleetVehiclesPaginated(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "licensePlate") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection,
            @RequestParam(required = false) String licensePlate,
            @RequestParam(required = false) com.exploresg.fleetservice.model.VehicleStatus status,
            @RequestParam(required = false) String model,
            @RequestParam(required = false) String manufacturer,
            @RequestParam(required = false) String location) {
        // Extract user ID from JWT token (userId is the ownerId in the fleet table)
        String userIdStr = jwt.getClaimAsString("userId");

        if (userIdStr == null || userIdStr.isEmpty()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        UUID userId = UUID.fromString(userIdStr);

        // Create sort object based on direction
        Sort sort = sortDirection.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        // Create pageable object
        Pageable pageable = PageRequest.of(page, size, sort);

        // Use search method if any search parameters are provided, otherwise use the
        // basic method
        Page<com.exploresg.fleetservice.model.FleetVehicle> vehiclesPage;

        if (licensePlate != null || status != null || model != null || manufacturer != null || location != null) {
            // Search with filters
            vehiclesPage = carModelService.searchFleetVehicles(
                    userId, licensePlate, status, model, manufacturer, location, pageable);
        } else {
            // No filters - use basic pagination
            vehiclesPage = carModelService.getAllFleetVehiclesByOwnerPaginated(userId, pageable);
        }

        return ResponseEntity.ok(vehiclesPage);
    }

    /**
     * Fleet Manager dashboard endpoint.
     * Returns comprehensive fleet statistics and breakdowns for the authenticated
     * fleet manager.
     * Includes vehicle status summary, fleet statistics, and breakdown by car
     * model.
     * GET /api/v1/fleet/operators/dashboard
     * 
     * @param jwt The authenticated user's JWT token containing userId.
     * @return FleetDashboardDto containing all dashboard metrics and breakdowns.
     */
    @GetMapping("/operators/dashboard")
    @PreAuthorize(SecurityConstants.HAS_ROLE_FLEET_MANAGER)
    public ResponseEntity<com.exploresg.fleetservice.dto.FleetDashboardDto> getFleetDashboard(
            @AuthenticationPrincipal Jwt jwt) {
        // Extract user ID from JWT token (userId is the ownerId in the fleet table)
        String userIdStr = jwt.getClaimAsString("userId");

        if (userIdStr == null || userIdStr.isEmpty()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        UUID userId = UUID.fromString(userIdStr);
        com.exploresg.fleetservice.dto.FleetDashboardDto dashboard = carModelService.getFleetDashboard(userId);

        return ResponseEntity.ok(dashboard);
    }

    /**
     * Customer endpoint: Book a car (set status to BOOKED).
     * PATCH /api/v1/fleet/vehicles/{id}/book
     */
    // @PatchMapping("/vehicles/{id}/book")
    // public ResponseEntity<?> bookFleetVehicle(@PathVariable UUID id) {
    // var vehicle =
    // carModelService.updateFleetVehicleStatusToBookedWithDetails(id);
    // return vehicle != null ? ResponseEntity.ok(vehicle) :
    // ResponseEntity.notFound().build();
    // }

    /**
     * Fleet Manager endpoint: Update car status to any valid value.
     * PATCH /api/v1/fleet/operators/fleet/{id}/status
     */
    @PatchMapping("/operators/fleet/{id}/status")
    @PreAuthorize(SecurityConstants.HAS_ROLE_FLEET_MANAGER)
    public ResponseEntity<?> updateFleetVehicleStatus(
            @PathVariable UUID id,
            @RequestBody java.util.Map<String, String> body) {
        String statusStr = body.get("status");
        if (statusStr == null)
            return ResponseEntity.badRequest().body("Missing status");
        com.exploresg.fleetservice.model.VehicleStatus status;
        try {
            status = com.exploresg.fleetservice.model.VehicleStatus.valueOf(statusStr);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid status");
        }
        var vehicle = carModelService.updateFleetVehicleStatusWithDetails(id, status);
        return vehicle != null ? ResponseEntity.ok(vehicle) : ResponseEntity.notFound().build();
    }
}