package com.exploresg.fleetservice.service;

import com.exploresg.fleetservice.dto.CreateCarModelRequest;
import com.exploresg.fleetservice.dto.CarModelResponseDto;
import com.exploresg.fleetservice.model.CarModel;
import com.exploresg.fleetservice.model.FleetVehicle;
import com.exploresg.fleetservice.model.VehicleStatus;
import com.exploresg.fleetservice.repository.CarModelRepository;
import com.exploresg.fleetservice.repository.FleetVehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service class containing business logic for managing CarModels.
 */
@Service
@RequiredArgsConstructor
public class CarModelService {

    private final CarModelRepository carModelRepository;
    private final FleetVehicleRepository fleetVehicleRepository;

    /**
     * Creates a new CarModel and saves it to the database.
     * 
     * @param request The DTO containing the details for the new car model.
     * @return The saved CarModel entity.
     */
    public CarModel createCarModel(CreateCarModelRequest request) {
        CarModel carModel = CarModel.builder()
                .model(request.getModel())
                .manufacturer(request.getManufacturer())
                .seats(request.getSeats())
                .luggage(request.getLuggage())
                .transmission(request.getTransmission())
                .imageUrl(request.getImageUrl())
                .category(request.getCategory())
                .fuelType(request.getFuelType())
                .modelYear(request.getModelYear())
                .engineCapacityCc(request.getEngineCapacityCc())
                .maxUnladenWeightKg(request.getMaxUnladenWeightKg())
                .maxLadenWeightKg(request.getMaxLadenWeightKg())
                .rangeInKm(request.getRangeInKm())
                .hasAirConditioning(request.isHasAirConditioning())
                .hasInfotainmentSystem(request.isHasInfotainmentSystem())
                .safetyRating(request.getSafetyRating())
                .topSpeedKph(request.getTopSpeedKph())
                .zeroToHundredSec(request.getZeroToHundredSec())
                .build();

        return carModelRepository.save(carModel);
    }

    /**
     * Retrieves all car models from the database (admin view).
     * 
     * @return A list of all CarModel entities.
     */
    public List<CarModel> getAllCarModels() {
        return carModelRepository.findAll();
    }

    /**
     * Retrieves all unique CarModel templates that have at least one physical car
     * instance available in the fleet.
     * Shows one model per car type with the lowest daily rental rate across all
     * operators.
     * 
     * @return A list of DTOs representing the available unique car models.
     */
    @Transactional(readOnly = true)
    public List<CarModelResponseDto> getAvailableCarModels() {
        // Get all distinct car models that have available vehicles
        List<CarModel> availableModels = fleetVehicleRepository.findAvailableCarModels();

        // Map each model to DTO with aggregated pricing
        return availableModels.stream()
                .map(this::mapToCarModelResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * Utility method to map a CarModel entity to a CarModelResponseDto.
     * Includes the lowest daily rental rate from all available vehicles of this
     * model.
     */
    private CarModelResponseDto mapToCarModelResponseDto(CarModel carModel) {
        // Get all available vehicles for this model to find the lowest price
        List<FleetVehicle> availableVehicles = fleetVehicleRepository
                .findByCarModelIdAndStatus(carModel.getId(), VehicleStatus.AVAILABLE);

        // Calculate the lowest daily rate across all operators
        BigDecimal lowestDailyRate = availableVehicles.stream()
                .map(FleetVehicle::getDailyPrice)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        return CarModelResponseDto.builder()
                .modelId(carModel.getId())
                .model(carModel.getModel())
                .manufacturer(carModel.getManufacturer())
                .category(carModel.getCategory())
                .imageUrl(carModel.getImageUrl())
                .dailyRentalRate(lowestDailyRate) // Lowest price across all operators
                .seats(carModel.getSeats())
                .luggage(carModel.getLuggage())
                .transmission(carModel.getTransmission())
                .fuelType(carModel.getFuelType())
                .modelYear(carModel.getModelYear())
                .topSpeedKph(carModel.getTopSpeedKph())
                .zeroToHundredSec(carModel.getZeroToHundredSec())
                .rangeInKm(carModel.getRangeInKm())
                .engineCapacityCc(carModel.getEngineCapacityCc())
                .safetyRating(carModel.getSafetyRating())
                .hasAirConditioning(carModel.isHasAirConditioning())
                .hasInfotainmentSystem(carModel.isHasInfotainmentSystem())
                .build();
    }
}