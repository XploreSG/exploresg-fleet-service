package com.exploresg.fleetservice.controller;

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
     * Public endpoint for anyone to view all available car models.
     * 
     * @return A list of all car models.
     */
    @GetMapping("/models")
    public ResponseEntity<List<CarModel>> getAllCarModels() {
        List<CarModel> carModels = carModelService.getAllCarModels();
        return ResponseEntity.ok(carModels);
    }
}
