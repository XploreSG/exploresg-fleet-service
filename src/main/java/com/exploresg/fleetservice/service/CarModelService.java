package com.exploresg.fleetservice.service;

import com.exploresg.fleetservice.dto.CreateCarModelRequest;
import com.exploresg.fleetservice.dto.CarModelResponseDto;
import com.exploresg.fleetservice.dto.OperatorCarModelDto;
import com.exploresg.fleetservice.model.CarModel;
import com.exploresg.fleetservice.model.FleetVehicle;
import com.exploresg.fleetservice.model.VehicleStatus;
import com.exploresg.fleetservice.repository.CarModelRepository;
import com.exploresg.fleetservice.repository.FleetVehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
         * * @param request The DTO containing the details for the new car model.
         * 
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
         * * @return A list of all CarModel entities.
         */
        public List<CarModel> getAllCarModels() {
                return carModelRepository.findAll();
        }

        /**
         * Retrieves available car models with one entry per operator-model combination.
         * Note: This method is still required for the existing `/api/v1/fleet/models`
         * endpoint.
         * * @return A list of car models grouped by operator.
         */
        @Transactional(readOnly = true)
        public List<OperatorCarModelDto> getAvailableModelsPerOperator() {
                // Get all available vehicles across ALL operators
                List<FleetVehicle> availableVehicles = fleetVehicleRepository
                                .findByStatus(VehicleStatus.AVAILABLE);

                // Group by (ownerId, carModelId) combination
                Map<String, List<FleetVehicle>> groupedVehicles = availableVehicles.stream()
                                .collect(Collectors.groupingBy(v -> v.getOwnerId() + "-" + v.getCarModel().getId()));

                List<OperatorCarModelDto> result = new ArrayList<>();

                for (List<FleetVehicle> vehicles : groupedVehicles.values()) {
                        if (vehicles.isEmpty())
                                continue;

                        FleetVehicle firstVehicle = vehicles.get(0);
                        CarModel carModel = firstVehicle.getCarModel();

                        // Find lowest price from this operator for this model
                        BigDecimal lowestPrice = vehicles.stream()
                                        .map(FleetVehicle::getDailyPrice)
                                        .min(BigDecimal::compareTo)
                                        .orElse(BigDecimal.ZERO);

                        OperatorCarModelDto dto = OperatorCarModelDto.builder()
                                        .operatorId(firstVehicle.getOwnerId())
                                        .operatorName("Fleet Operator " + firstVehicle.getOwnerId()) // TODO: Fetch from
                                                                                                     // user service
                                        .publicModelId(carModel.getPublicId().toString()) // <-- USE NEW PUBLIC ID
                                        .model(carModel.getModel())
                                        .manufacturer(carModel.getManufacturer())
                                        .seats(carModel.getSeats())
                                        .luggage(carModel.getLuggage())
                                        .transmission(carModel.getTransmission())
                                        .imageUrl(carModel.getImageUrl())
                                        .category(carModel.getCategory())
                                        .fuelType(carModel.getFuelType())
                                        .modelYear(carModel.getModelYear())
                                        .dailyPrice(lowestPrice)
                                        .availableVehicleCount(vehicles.size()) // Count for allocation
                                        .build();

                        result.add(dto);
                }

                return result;
        }

        /**
         * NEW CORE LOGIC: Retrieves available car models for a specific operator.
         * Filters by operator ID and availability, then groups by CarModel (blueprint).
         *
         * @param operatorId The ID of the fleet operator.
         * @return A list of available car models aggregated for the given operator.
         */
        @Transactional(readOnly = true)
        public List<OperatorCarModelDto> getAvailableModelsByOperator(Long operatorId) {
                // 1. Fetch all available physical vehicles belonging to the specific operator
                List<FleetVehicle> availableVehicles = fleetVehicleRepository
                                .findByOwnerIdAndStatus(operatorId, VehicleStatus.AVAILABLE);

                if (availableVehicles.isEmpty()) {
                        return List.of();
                }

                // 2. Group vehicles by CarModel (the blueprint)
                // This ensures one entry per model per operator, satisfying the request.
                Map<CarModel, List<FleetVehicle>> groupedVehicles = availableVehicles.stream()
                                .collect(Collectors.groupingBy(FleetVehicle::getCarModel));

                // 3. Aggregate and Map to the DTO
                return groupedVehicles.entrySet().stream()
                                .map(entry -> {
                                        CarModel model = entry.getKey();
                                        List<FleetVehicle> physicalCars = entry.getValue();

                                        // Find lowest price offered by this operator for this model
                                        BigDecimal lowestPrice = physicalCars.stream()
                                                        .map(FleetVehicle::getDailyPrice)
                                                        .min(BigDecimal::compareTo)
                                                        .orElse(BigDecimal.ZERO);

                                        // Map to the aggregated DTO
                                        return OperatorCarModelDto.builder()
                                                        .operatorId(operatorId)
                                                        .operatorName("Fleet Operator " + operatorId)
                                                        .publicModelId(model.getPublicId().toString()) // Use the new
                                                                                                       // Public ID
                                                        .model(model.getModel())
                                                        .manufacturer(model.getManufacturer())
                                                        .seats(model.getSeats())
                                                        .luggage(model.getLuggage())
                                                        .transmission(model.getTransmission())
                                                        .imageUrl(model.getImageUrl())
                                                        .category(model.getCategory())
                                                        .fuelType(model.getFuelType())
                                                        .modelYear(model.getModelYear())
                                                        .dailyPrice(lowestPrice)
                                                        .availableVehicleCount(physicalCars.size())
                                                        .build();
                                })
                                .collect(Collectors.toList());
        }

        /**
         * Retrieves all unique CarModel templates that have at least one physical car
         * instance available in the fleet.
         * Returns detailed response DTOs.
         * * @return A list of DTOs representing the available unique car models.
         */
        public List<CarModelResponseDto> getAvailableCarModels() {
                List<CarModel> availableModels = fleetVehicleRepository.findAvailableCarModels();

                return availableModels.stream()
                                .map(this::mapToCarModelResponseDto)
                                .toList();
        }

        /**
         * Utility method to map a CarModel entity to a CarModelResponseDto.
         */
        private CarModelResponseDto mapToCarModelResponseDto(CarModel carModel) {
                return CarModelResponseDto.builder()
                                .modelId(carModel.getId())
                                .model(carModel.getModel())
                                .manufacturer(carModel.getManufacturer())
                                .imageUrl(carModel.getImageUrl())
                                .seats(carModel.getSeats())
                                .luggage(carModel.getLuggage())
                                .transmission(carModel.getTransmission())
                                .category(carModel.getCategory())
                                .fuelType(carModel.getFuelType())
                                .modelYear(carModel.getModelYear())
                                .build();
        }
}