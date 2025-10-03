package com.exploresg.fleetservice.controller;

import com.exploresg.fleetservice.dto.CarModelResponseDto;
import com.exploresg.fleetservice.dto.CreateCarModelRequest;
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
     * 
     * @param request The request body containing car model details.
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
     * Returns only models that have at least one AVAILABLE vehicle.
     * Shows the lowest daily rental rate across all fleet operators.
     * GET /api/v1/fleet/models
     * 
     * @return A list of available car models for browsing.
     */
    @GetMapping("/models")
    public ResponseEntity<List<CarModelResponseDto>> getAvailableCarModels() {
        List<CarModelResponseDto> carModels = carModelService.getAvailableCarModels();
        return ResponseEntity.ok(carModels);
    }

    /**
     * Admin endpoint to view ALL car models (master catalog).
     * GET /api/v1/fleet/models/all
     * 
     * @return A list of all car models in the system.
     */
    @GetMapping("/models/all")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<CarModel>> getAllCarModels() {
        List<CarModel> carModels = carModelService.getAllCarModels();
        return ResponseEntity.ok(carModels);
    }
}