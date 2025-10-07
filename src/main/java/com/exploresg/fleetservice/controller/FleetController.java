package com.exploresg.fleetservice.controller;

import com.exploresg.fleetservice.dto.CreateCarModelRequest;
import com.exploresg.fleetservice.dto.OperatorCarModelDto;
import com.exploresg.fleetservice.model.CarModel;
import com.exploresg.fleetservice.service.CarModelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
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
    public ResponseEntity<List<OperatorCarModelDto>> getAvailableModelsByOperator(@PathVariable Long operatorId) {
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
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<CarModel>> getAllCarModels() {
        List<CarModel> carModels = carModelService.getAllCarModels();
        return ResponseEntity.ok(carModels);
    }
}