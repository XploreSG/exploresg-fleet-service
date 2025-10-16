package com.exploresg.fleetservice.controller;

import com.exploresg.fleetservice.model.CarModel;
import com.exploresg.fleetservice.service.CarModelService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@WebMvcTest(controllers = FleetController.class, excludeAutoConfiguration =  {SecurityAutoConfiguration.class, OAuth2ResourceServerAutoConfiguration.class,
        OAuth2ClientAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
public class FleetControllerUnitTest {
    @MockitoBean
    CarModelService carModelService;
    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("Should return car model entity with same details as provided input")
    public void testGetAllCarModels() throws Exception {
        RequestBuilder req = MockMvcRequestBuilders.get("/api/v1/fleet/models/all").accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON);
        CarModel carModel = getCarModel();
        when(carModelService.getAllCarModels()).thenReturn(new ArrayList<CarModel>(){
            {
                add(carModel);
            }
        });
        MvcResult results = mockMvc.perform(req).andReturn();
        String jsonResponse = results.getResponse().getContentAsString();
        List<CarModel> content = new ObjectMapper().readValue(jsonResponse,new TypeReference<List<CarModel>>() {});
        assertEquals(carModel, content.get(0), "Should return created car type with similar details as input");

        Mockito.verify(carModelService, Mockito.times(1)).getAllCarModels();
    }

    private static CarModel getCarModel() {
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
        return carModel;
    }

    @Test
    @DisplayName("Should return 500 response status code if car service returns exception")
    public void testGetAllCarModelsReturnsException() throws Exception {
        RequestBuilder req = MockMvcRequestBuilders.get("/api/v1/fleet/models/all").accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON);
        when(carModelService.getAllCarModels()).thenThrow(RuntimeException.class);
        MvcResult results = mockMvc.perform(req).andReturn();
        int statusCode = results.getResponse().getStatus();
        //List<CarModel> content = new ObjectMapper().readValue(jsonResponse,new TypeReference<List<CarModel>>() {});
        assertEquals(500, statusCode, "Should return status code 500");

        Mockito.verify(carModelService, Mockito.times(1)).getAllCarModels();
    }
}
