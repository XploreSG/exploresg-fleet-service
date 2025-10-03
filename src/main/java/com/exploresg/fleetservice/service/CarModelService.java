package com.exploresg.fleetservice.service;

import com.exploresg.fleetservice.dto.CreateCarModelRequest;
import com.exploresg.fleetservice.model.CarModel;
import com.exploresg.fleetservice.repository.CarModelRepository;
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
                .rangeKm(request.getRangeKm())
                .hasAirConditioning(request.isHasAirConditioning())
                .hasInfotainmentSystem(request.isHasInfotainmentSystem())
                .safetyRating(request.getSafetyRating())
                .topSpeedKph(request.getTopSpeedKph())
                .zeroToHundredSec(request.getZeroToHundredSec())
                .build();

        return carModelRepository.save(carModel);
    }

    /**
     * Retrieves all car models from the database.
     * 
     * @return A list of all CarModel entities.
     */
    public List<CarModel> getAllCarModels() {
        return carModelRepository.findAll();
    }
}
