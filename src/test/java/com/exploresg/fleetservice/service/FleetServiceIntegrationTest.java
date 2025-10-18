package com.exploresg.fleetservice.service;

import com.exploresg.fleetservice.dto.CreateCarModelRequest;
import com.exploresg.fleetservice.model.CarModel;
import com.exploresg.fleetservice.repository.CarModelRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class FleetServiceIntegrationTest {
    @Autowired
    private CarModelService carModelService;
    @Autowired
    private CarModelRepository carModelRepository;

    @BeforeAll
    @AfterAll
    public void deleteAll() {
        carModelRepository.deleteAll();
    }

    @Test
    @Order(1)
    public void testCreateCarModel() {
        CreateCarModelRequest request = new CreateCarModelRequest();
        request.setModel("model1");
        request.setCategory("category1");
        request.setModelYear(2019);
        request.setManufacturer("manufacturer1");
        request.setEngineCapacityCc(1231);
        request.setHasAirConditioning(true);
        request.setImageUrl("imageUrl1");
        request.setZeroToHundredSec(9.00);
        request.setFuelType("fuelType1");
        request.setLuggage(1);
        request.setSafetyRating("SafetyRating1");
        request.setSeats(3);
        request.setTransmission("transmission1");
        CarModel createdCar = carModelService.createCarModel(request);
        assertEquals(createdCar.getModel(), request.getModel());
        assertEquals(createdCar.getCategory(), request.getCategory());
        assertEquals(createdCar.getModelYear(), request.getModelYear());
        assertEquals(createdCar.getManufacturer(), request.getManufacturer());
        assertEquals(createdCar.getEngineCapacityCc(), request.getEngineCapacityCc());
        assertEquals(createdCar.isHasAirConditioning(), request.isHasAirConditioning());
        assertEquals(createdCar.getImageUrl(), request.getImageUrl());
        assertEquals(createdCar.getZeroToHundredSec(), request.getZeroToHundredSec());
        assertEquals(createdCar.getFuelType(), request.getFuelType());
        assertEquals(createdCar.getLuggage(), request.getLuggage());
        assertEquals(createdCar.getSafetyRating(), request.getSafetyRating());
        assertEquals(createdCar.getSeats(), request.getSeats());
    }

    @Test
    @Order(2)
    public void shouldReturnAllCarModels() {
        List<CarModel> carmodels = carModelService.getAllCarModels();
        assertTrue(!carmodels.isEmpty());
    }

}
