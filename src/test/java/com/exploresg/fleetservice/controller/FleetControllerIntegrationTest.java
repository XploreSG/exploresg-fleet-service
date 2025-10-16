package com.exploresg.fleetservice.controller;

import com.exploresg.fleetservice.model.CarModel;
import com.exploresg.fleetservice.repository.CarModelRepository;
import com.exploresg.fleetservice.service.CarModelService;
import com.exploresg.fleetservice.utils.JwtTestHelper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.util.Arrays;
import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class FleetControllerIntegrationTest {
    private TestRestTemplate restTemplate = new TestRestTemplate();
    @Autowired
    private CarModelRepository carModelRepository;
    @Autowired
    private JwtTestHelper jwtTestHelper;
    @LocalServerPort
    private int port;
    private String baseUrl() {
        return "http://localhost:" + port;
    }
    @BeforeAll
    public void setup() {
        carModelRepository.deleteAll();
        CarModel carModel = new CarModel();
        carModel.setModel("model1");
        //carModel.setId(1L);
        carModel.setSeats(4);
        carModel.setTransmission("transmission1");
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
        carModelRepository.save(carModel);
    }
    @Test
    @DisplayName("Should display all car models if user has admin role")
    public void testGetAllCarModels() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        String jwtToken = jwtTestHelper.generateAdminToken("chowjss@gmail.com");
        headers.setBearerAuth(jwtToken);
        HttpEntity<String> reqEntity = new HttpEntity<>(null, headers);
        /*ResponseEntity<String> response = restTemplate.exchange(
                baseUrl()+ "/api/v1/fleet/models/all",
                HttpMethod.GET,
                reqEntity,
                String.class
        );

        System.out.println("Status: " + response.getStatusCode());
        System.out.println("Body: " + response.getBody());*/
        ResponseEntity<List<CarModel>> resp = restTemplate.exchange(
                baseUrl()+"/api/v1/fleet/models/all",
                HttpMethod.GET,
                reqEntity,
                new ParameterizedTypeReference<List<CarModel>>() {}
        );
        Assertions.assertEquals(HttpStatus.OK, resp.getStatusCode());
        Assertions.assertNotNull(resp.getBody());
        Assertions.assertArrayEquals(carModelRepository.findAll().toArray(), resp.getBody().toArray());
    }
    @AfterAll
    public void tearDown() {
        carModelRepository.deleteAll();
    }
}
