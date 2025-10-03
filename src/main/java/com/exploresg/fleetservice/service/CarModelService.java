package com.exploresg.fleetservice.service;

import com.exploresg.fleetservice.dto.CreateCarModelRequest;
import com.exploresg.fleetservice.dto.CarModelResponseDto;
import com.exploresg.fleetservice.model.CarModel;
import com.exploresg.fleetservice.repository.CarModelRepository;
import com.exploresg.fleetservice.repository.FleetVehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service class containing business logic for managing CarModels.
 */
@Service
@RequiredArgsConstructor
public class CarModelService {

    private final CarModelRepository carModelRepository;
    private final FleetVehicleRepository fleetVehicleRepository; // Injected for availability logic

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

                // --- MAPPING NEW/UPDATED FIELDS ---
                .fuelType(request.getFuelType())
                .modelYear(request.getModelYear())

                // New fields from the updated CarModel entity:
                .engineCapacityCc(request.getEngineCapacityCc())
                .maxUnladenWeightKg(request.getMaxUnladenWeightKg())
                .maxLadenWeightKg(request.getMaxLadenWeightKg())

                // Renamed field: rangeKm -> rangeInKm
                .rangeInKm(request.getRangeInKm())

                // Existing fields:
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

    // ----------------------------------------------------------------------
    // --- NEW LOGIC: Availability for Customer Frontend ----------------------
    // ----------------------------------------------------------------------

    /**
     * Retrieves all unique CarModel templates that have at least one physical car
     * instance available
     * in the fleet. This is used to populate the main car selection screen (Car
     * Cards).
     * * @return A list of DTOs representing the available unique car models.
     */
    public List<CarModelResponseDto> getAvailableCarModels() {
        // Calls the custom query in the FleetVehicleRepository to get distinct
        // available CarModel entities.
        List<CarModel> availableModels = fleetVehicleRepository.findAvailableCarModels();

        // Map the entities to the response DTO
        return availableModels.stream()
                .map(this::mapToCarModelResponseDto)
                .toList();
    }

    /**
     * Utility method to map a CarModel entity to a CarModelResponseDto.
     * NOTE: In a complete system, this mapping might be handled by MapStruct.
     */
    private CarModelResponseDto mapToCarModelResponseDto(CarModel carModel) {
        // For a full DTO, you'd map all fields. This is a simplified example.
        return CarModelResponseDto.builder()
                .modelId(carModel.getId())
                .model(carModel.getModel())
                .manufacturer(carModel.getManufacturer())
                .imageUrl(carModel.getImageUrl())
                .seats(carModel.getSeats())
                // ... map all other CarModel fields...
                .build();
    }
}