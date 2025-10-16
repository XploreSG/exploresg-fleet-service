package com.exploresg.fleetservice.service;

import com.exploresg.fleetservice.controller.FleetController;
import com.exploresg.fleetservice.dto.CreateCarModelRequest;
import com.exploresg.fleetservice.model.CarModel;
import com.exploresg.fleetservice.repository.CarModelRepository;
import com.exploresg.fleetservice.repository.FleetVehicleRepository;
import com.exploresg.fleetservice.repository.VehicleBookingRecordRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class FleetServiceUnitTest {
    @Mock
    private CarModelRepository carModelRepository;

    @Mock
    private FleetVehicleRepository fleetVehicleRepository;

    @Mock
    private VehicleBookingRecordRepository bookingRecordRepository;

    @InjectMocks
    private CarModelService carModelService;

    @Test
    void shouldCreateCarModel() {
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

        CarModel carModel = new CarModel();
        carModel.setModel("model1");
        carModel.setId(1L);
        carModel.setCategory("category1");
        carModel.setModelYear(2019);
        carModel.setManufacturer("manufacturer1");
        carModel.setEngineCapacityCc(1231);
        carModel.setHasAirConditioning(true);
        carModel.setImageUrl("imageUrl1");
        carModel.setZeroToHundredSec(9.00);
        carModel.setFuelType("fuelType1");
        carModel.setLuggage(1);
        carModel.setSafetyRating("SafetyRating1");

        when(carModelRepository.save(any(CarModel.class))).thenReturn(carModel);

        CarModel result = carModelService.createCarModel(request);

        assert(result.getModel()).equals("model1");
        assert(result.getCategory().equals("category1"));
        assertThat(result.getManufacturer().equals("manufacturer1"));
        assertThat(result.getModelYear().equals(2019));
        assertThat(result.getEngineCapacityCc().equals(1231));
        assertThat(result.getImageUrl().equals("imageUrl1"));
        Mockito.verify(carModelRepository, Mockito.times(1)).save(any(CarModel.class));
    }
}
