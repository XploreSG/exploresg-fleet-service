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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
        public List<OperatorCarModelDto> getAvailableModelsByOperator(UUID operatorId) {
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
         * Retrieves all fleet vehicles owned by a specific operator.
         * Returns ALL vehicles regardless of status - useful for fleet management and
         * testing.
         * * @param ownerId The ID of the fleet operator (fleet manager's userId).
         * 
         * @return A list of all FleetVehicle entities owned by the operator.
         */
        @Transactional(readOnly = true)
        public List<FleetVehicle> getAllFleetVehiclesByOwner(UUID ownerId) {
                return fleetVehicleRepository.findByOwnerId(ownerId);
        }

        /**
         * Retrieves all fleet vehicles owned by a specific operator with pagination
         * support.
         * Returns ALL vehicles regardless of status - useful for fleet management and
         * testing.
         * 
         * @param ownerId  The ID of the fleet operator (fleet manager's userId).
         * @param pageable Pagination information (page number, size, sort).
         * @return A Page of FleetVehicle entities owned by the operator.
         */
        @Transactional(readOnly = true)
        public Page<FleetVehicle> getAllFleetVehiclesByOwnerPaginated(UUID ownerId, Pageable pageable) {
                return fleetVehicleRepository.findByOwnerId(ownerId, pageable);
        }

        /**
         * Search fleet vehicles with optional filters and pagination support.
         * All search parameters are optional - if null/empty, they are ignored.
         * 
         * @param ownerId      The ID of the fleet operator (fleet manager's userId).
         * @param licensePlate Optional license plate search term (partial match).
         * @param status       Optional vehicle status filter.
         * @param model        Optional car model name search term (partial match).
         * @param manufacturer Optional manufacturer name search term (partial match).
         * @param location     Optional location search term (partial match).
         * @param pageable     Pagination information (page number, size, sort).
         * @return A Page of FleetVehicle entities matching the search criteria.
         */
        @Transactional(readOnly = true)
        public Page<FleetVehicle> searchFleetVehicles(
                        UUID ownerId,
                        String licensePlate,
                        VehicleStatus status,
                        String model,
                        String manufacturer,
                        String location,
                        Pageable pageable) {

                // Convert empty strings to null for cleaner query handling
                licensePlate = (licensePlate != null && licensePlate.trim().isEmpty()) ? null : licensePlate;
                model = (model != null && model.trim().isEmpty()) ? null : model;
                manufacturer = (manufacturer != null && manufacturer.trim().isEmpty()) ? null : manufacturer;
                location = (location != null && location.trim().isEmpty()) ? null : location;

                // Convert status enum to string for native query
                String statusStr = (status != null) ? status.name() : null;

                // Convert Java property names to database column names for native SQL sorting
                Pageable transformedPageable = transformPageableForNativeQuery(pageable);

                return fleetVehicleRepository.searchFleetVehicles(
                                ownerId,
                                licensePlate,
                                statusStr,
                                model,
                                manufacturer,
                                location,
                                transformedPageable);
        }

        /**
         * Transforms a Pageable object to use database column names instead of Java
         * property names.
         * This is necessary for native SQL queries where Spring Data JPA doesn't
         * automatically
         * convert property names to column names.
         * 
         * @param pageable The original Pageable with Java property names
         * @return A new Pageable with database column names
         */
        private Pageable transformPageableForNativeQuery(Pageable pageable) {
                if (pageable.getSort().isUnsorted()) {
                        return pageable;
                }

                // Map Java property names to database column names
                Sort transformedSort = Sort.by(
                                pageable.getSort().stream()
                                                .map(order -> {
                                                        String property = order.getProperty();
                                                        // Convert camelCase to snake_case for database columns
                                                        String columnName = convertToSnakeCase(property);
                                                        return order.isAscending()
                                                                        ? Sort.Order.asc(columnName)
                                                                        : Sort.Order.desc(columnName);
                                                })
                                                .toList());

                return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), transformedSort);
        }

        /**
         * Converts camelCase property name to snake_case column name.
         * Examples: licensePlate -> license_plate, carModel -> car_model
         * 
         * @param camelCase The Java property name in camelCase
         * @return The database column name in snake_case
         */
        private String convertToSnakeCase(String camelCase) {
                return camelCase.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
        }

        /**
         * Retrieves comprehensive dashboard statistics for a fleet manager.
         * Includes vehicle status summary, fleet statistics, and breakdown by model.
         * * @param ownerId The ID of the fleet operator (fleet manager's userId).
         * 
         * @return FleetDashboardDto containing all dashboard metrics.
         */
        @Transactional(readOnly = true)
        public com.exploresg.fleetservice.dto.FleetDashboardDto getFleetDashboard(UUID ownerId) {
                // 1. Get all vehicles for this owner
                List<FleetVehicle> allVehicles = fleetVehicleRepository.findByOwnerId(ownerId);

                if (allVehicles.isEmpty()) {
                        // Return empty dashboard if no vehicles
                        return com.exploresg.fleetservice.dto.FleetDashboardDto.builder()
                                        .vehicleStatus(com.exploresg.fleetservice.dto.VehicleStatusSummary.builder()
                                                        .available(0)
                                                        .underMaintenance(0)
                                                        .booked(0)
                                                        .total(0)
                                                        .build())
                                        .serviceReminders(
                                                        com.exploresg.fleetservice.dto.ServiceRemindersSummary.builder()
                                                                        .overdue(0)
                                                                        .dueSoon(0)
                                                                        .build())
                                        .workOrders(com.exploresg.fleetservice.dto.WorkOrdersSummary.builder()
                                                        .active(0)
                                                        .pending(0)
                                                        .build())
                                        .vehicleAssignments(com.exploresg.fleetservice.dto.VehicleAssignmentsSummary
                                                        .builder()
                                                        .assigned(0)
                                                        .unassigned(0)
                                                        .build())
                                        .statistics(com.exploresg.fleetservice.dto.FleetStatistics.builder()
                                                        .totalVehicles(0)
                                                        .totalModels(0)
                                                        .averageMileage(0.0)
                                                        .totalMileage(0L)
                                                        .totalPotentialDailyRevenue(BigDecimal.ZERO)
                                                        .totalRevenue(BigDecimal.ZERO)
                                                        .utilizationRate(0.0)
                                                        .build())
                                        .fleetByModel(List.of())
                                        .build();
                }

                // 2. Calculate vehicle status counts
                long availableCount = allVehicles.stream()
                                .filter(v -> v.getStatus() == VehicleStatus.AVAILABLE)
                                .count();
                long bookedCount = allVehicles.stream()
                                .filter(v -> v.getStatus() == VehicleStatus.BOOKED)
                                .count();
                long underMaintenanceCount = allVehicles.stream()
                                .filter(v -> v.getStatus() == VehicleStatus.UNDER_MAINTENANCE)
                                .count();

                com.exploresg.fleetservice.dto.VehicleStatusSummary vehicleStatus = com.exploresg.fleetservice.dto.VehicleStatusSummary
                                .builder()
                                .available(availableCount)
                                .booked(bookedCount)
                                .underMaintenance(underMaintenanceCount)
                                .total(allVehicles.size())
                                .build();

                // 3. Calculate service reminders (based on maintenance schedule)
                // For now, using simple logic: vehicles with high mileage need service
                java.time.LocalDateTime now = java.time.LocalDateTime.now();

                long overdueCount = allVehicles.stream()
                                .filter(v -> v.getExpectedReturnDate() != null
                                                && v.getExpectedReturnDate().isBefore(now)
                                                && v.getStatus() == VehicleStatus.UNDER_MAINTENANCE)
                                .count();

                long dueSoonCount = allVehicles.stream()
                                .filter(v -> v.getMileageKm() != null && v.getMileageKm() > 50000)
                                .filter(v -> v.getStatus() != VehicleStatus.UNDER_MAINTENANCE)
                                .count();

                com.exploresg.fleetservice.dto.ServiceRemindersSummary serviceReminders = com.exploresg.fleetservice.dto.ServiceRemindersSummary
                                .builder()
                                .overdue(overdueCount)
                                .dueSoon(dueSoonCount)
                                .build();

                // 4. Calculate work orders
                long activeWorkOrders = underMaintenanceCount; // Vehicles currently in maintenance
                long pendingWorkOrders = allVehicles.stream()
                                .filter(v -> v.getExpectedReturnDate() != null
                                                && v.getExpectedReturnDate().isAfter(now)
                                                && v.getStatus() != VehicleStatus.UNDER_MAINTENANCE)
                                .count();

                com.exploresg.fleetservice.dto.WorkOrdersSummary workOrders = com.exploresg.fleetservice.dto.WorkOrdersSummary
                                .builder()
                                .active(activeWorkOrders)
                                .pending(pendingWorkOrders)
                                .build();

                // 5. Calculate vehicle assignments
                com.exploresg.fleetservice.dto.VehicleAssignmentsSummary vehicleAssignments = com.exploresg.fleetservice.dto.VehicleAssignmentsSummary
                                .builder()
                                .assigned(bookedCount) // Currently booked/rented
                                .unassigned(availableCount) // Available for rent
                                .build();

                // 6. Calculate overall fleet statistics
                Double averageMileage = allVehicles.stream()
                                .filter(v -> v.getMileageKm() != null)
                                .mapToInt(FleetVehicle::getMileageKm)
                                .average()
                                .orElse(0.0);

                Long totalMileage = allVehicles.stream()
                                .filter(v -> v.getMileageKm() != null)
                                .mapToLong(FleetVehicle::getMileageKm)
                                .sum();

                BigDecimal totalPotentialRevenue = allVehicles.stream()
                                .map(FleetVehicle::getDailyPrice)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                double utilizationRate = allVehicles.size() > 0
                                ? (bookedCount * 100.0 / allVehicles.size())
                                : 0.0;

                long uniqueModels = allVehicles.stream()
                                .map(v -> v.getCarModel().getId())
                                .distinct()
                                .count();

                com.exploresg.fleetservice.dto.FleetStatistics statistics = com.exploresg.fleetservice.dto.FleetStatistics
                                .builder()
                                .totalVehicles(allVehicles.size())
                                .totalModels(uniqueModels)
                                .averageMileage(averageMileage)
                                .totalMileage(totalMileage)
                                .totalPotentialDailyRevenue(totalPotentialRevenue)
                                .totalRevenue(totalPotentialRevenue) // Same as potential for now
                                .utilizationRate(utilizationRate)
                                .build();

                // 4. Group vehicles by model and calculate breakdowns
                Map<CarModel, List<FleetVehicle>> vehiclesByModel = allVehicles.stream()
                                .collect(Collectors.groupingBy(FleetVehicle::getCarModel));

                List<com.exploresg.fleetservice.dto.FleetModelBreakdown> fleetByModel = vehiclesByModel.entrySet()
                                .stream()
                                .map(entry -> {
                                        CarModel model = entry.getKey();
                                        List<FleetVehicle> vehicles = entry.getValue();

                                        long modelAvailableCount = vehicles.stream()
                                                        .filter(v -> v.getStatus() == VehicleStatus.AVAILABLE)
                                                        .count();
                                        long modelBookedCount = vehicles.stream()
                                                        .filter(v -> v.getStatus() == VehicleStatus.BOOKED)
                                                        .count();
                                        long modelMaintenanceCount = vehicles.stream()
                                                        .filter(v -> v.getStatus() == VehicleStatus.UNDER_MAINTENANCE)
                                                        .count();

                                        Double modelAvgMileage = vehicles.stream()
                                                        .filter(v -> v.getMileageKm() != null)
                                                        .mapToInt(FleetVehicle::getMileageKm)
                                                        .average()
                                                        .orElse(0.0);

                                        BigDecimal modelAvgPrice = vehicles.stream()
                                                        .map(FleetVehicle::getDailyPrice)
                                                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                                                        .divide(BigDecimal.valueOf(vehicles.size()), 2,
                                                                        java.math.RoundingMode.HALF_UP);

                                        return com.exploresg.fleetservice.dto.FleetModelBreakdown.builder()
                                                        .manufacturer(model.getManufacturer())
                                                        .model(model.getModel())
                                                        .imageUrl(model.getImageUrl())
                                                        .totalCount(vehicles.size())
                                                        .availableCount(modelAvailableCount)
                                                        .bookedCount(modelBookedCount)
                                                        .underMaintenanceCount(modelMaintenanceCount)
                                                        .averageMileage(modelAvgMileage)
                                                        .averageDailyPrice(modelAvgPrice)
                                                        .build();
                                })
                                .collect(Collectors.toList());

                // 7. Build and return the complete dashboard
                return com.exploresg.fleetservice.dto.FleetDashboardDto.builder()
                                .vehicleStatus(vehicleStatus)
                                .serviceReminders(serviceReminders)
                                .workOrders(workOrders)
                                .vehicleAssignments(vehicleAssignments)
                                .statistics(statistics)
                                .fleetByModel(fleetByModel)
                                .build();
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

        /**
         * Customer: Set vehicle status to BOOKED.
         */
        @Transactional
        public boolean updateFleetVehicleStatusToBooked(UUID id) {
                java.util.Optional<FleetVehicle> opt = fleetVehicleRepository.findById(id);
                if (opt.isEmpty())
                        return false;
                FleetVehicle vehicle = opt.get();
                vehicle.setStatus(VehicleStatus.BOOKED);
                fleetVehicleRepository.save(vehicle);
                return true;
        }

        /**
         * Fleet Manager: Set vehicle status to any valid value.
         */
        @Transactional
        public boolean updateFleetVehicleStatus(UUID id, VehicleStatus status) {
                java.util.Optional<FleetVehicle> opt = fleetVehicleRepository.findById(id);
                if (opt.isEmpty())
                        return false;
                FleetVehicle vehicle = opt.get();
                vehicle.setStatus(status);
                fleetVehicleRepository.save(vehicle);
                return true;
        }

        /**
         * Customer: Set vehicle status to BOOKED and return updated vehicle.
         */
        @Transactional
        public FleetVehicle updateFleetVehicleStatusToBookedWithDetails(UUID id) {
                java.util.Optional<FleetVehicle> opt = fleetVehicleRepository.findById(id);
                if (opt.isEmpty())
                        return null;
                FleetVehicle vehicle = opt.get();
                vehicle.setStatus(VehicleStatus.BOOKED);
                fleetVehicleRepository.save(vehicle);
                return vehicle;
        }

        /**
         * Fleet Manager: Set vehicle status to any valid value and return updated
         * vehicle.
         */
        @Transactional
        public FleetVehicle updateFleetVehicleStatusWithDetails(UUID id, VehicleStatus status) {
                java.util.Optional<FleetVehicle> opt = fleetVehicleRepository.findById(id);
                if (opt.isEmpty())
                        return null;
                FleetVehicle vehicle = opt.get();
                vehicle.setStatus(status);
                fleetVehicleRepository.save(vehicle);
                return vehicle;
        }
}