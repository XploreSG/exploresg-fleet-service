package com.exploresg.fleetservice.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.security.Principal;
import java.util.Map;

@RestController
public class HelloWorldController {

    private static final Logger log = LoggerFactory.getLogger(HelloWorldController.class);

    @GetMapping("/hello")
    public Map<String, String> sayHello(Principal principal) {
        String correlation = MDC.get("correlationId");
        log.info("Handling /hello request, principal={}, correlationId={}",
                principal == null ? "anonymous" : principal.getName(), correlation);
        return Map.of("message", "Hello from the Fleet Service!");
    }

    // --- ADD THIS NEW METHOD ---
    @GetMapping("/secure/hello")
    public Map<String, String> saySecureHello(Principal principal) {
        String correlation = MDC.get("correlationId");
        log.info("Handling /secure/hello request, principal={}, correlationId={}",
                principal == null ? "anonymous" : principal.getName(), correlation);
        // This endpoint is automatically protected by our SecurityConfig
        return Map.of("message", "This is a SECURE message from the Fleet Service!");
    }
}