package com.exploresg.fleetservice.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
public class HelloWorldController {

    @GetMapping("/hello")
    public Map<String, String> sayHello() {
        return Map.of("message", "Hello from the Fleet Service!");
    }

    // --- ADD THIS NEW METHOD ---
    @GetMapping("/secure/hello")
    public Map<String, String> saySecureHello() {
        // This endpoint is automatically protected by our SecurityConfig
        return Map.of("message", "This is a SECURE message from the Fleet Service!");
    }
}