package com.exploresg.fleetservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.crypto.spec.SecretKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // Enables method-level security like @PreAuthorize
public class SecurityConfig {

    @Value("${application.security.jwt.secret-key}")
    private String jwtSecretKey;

    @Value("${cors.allowed-origins:http://localhost:3000}")
    private String allowedOrigins;

    @Value("${cors.allowed-methods:GET,POST,PUT,DELETE,OPTIONS}")
    private String allowedMethods;

    @Value("${cors.allowed-headers:*}")
    private String allowedHeaders;

    @Value("${cors.allow-credentials:true}")
    private boolean allowCredentials;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. CORS Configuration (Aligned with fleet-service)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // 2. CSRF Disabled for stateless APIs (Aligned with fleet-service)
                .csrf(csrf -> csrf.disable())
                // 3. Stateless Session Management (Aligned with fleet-service)
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 4. Route Permissions (Adapted for fleet-service)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/",
                                "/error",
                                "/hello",
                                "/api/v1/fleet/health", // Health check for service monitoring
                                "/api/v1/fleet/models", // Public endpoint for browsing all cars
                                "/api/v1/fleet/models/*/availability-count", // Public endpoint for checking
                                                                             // availability
                                "/api/v1/fleet/operators/*/models", // Public endpoint for browsing cars by operator
                                "/api/v1/fleet/bookings/**", // Allow booking service to access without auth (dev only)
                                "/api/v1/fleet/reservations/**", // Allow booking service reservation endpoints (dev
                                                                 // only)
                                // Actuator endpoints for Kubernetes health probes
                                "/actuator/health",
                                "/actuator/health/liveness",
                                "/actuator/health/readiness",
                                "/actuator/info",
                                "/actuator/prometheus",
                                // Swagger/OpenAPI endpoints
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/openapi/**")
                        .permitAll()
                        // All other requests require authentication
                        .anyRequest().authenticated())
                // 5. JWT Validation (The Resource Server's primary job)
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt
                        .decoder(jwtDecoder())
                        .jwtAuthenticationConverter(jwtAuthenticationConverter())));

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        // Configure how to extract authorities from the JWT
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();

        // Tell Spring Security to look for roles in the "roles" claim
        grantedAuthoritiesConverter.setAuthoritiesClaimName("roles");

        // Don't add any prefix (since your JWT already has "ROLE_" prefix)
        grantedAuthoritiesConverter.setAuthorityPrefix("");

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);

        return jwtAuthenticationConverter;
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        // This bean tells the resource server how to validate the JWT signature
        byte[] keyBytes = Base64.getDecoder().decode(jwtSecretKey);
        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(secretKey).build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        // This configuration is aligned with our other services to ensure
        // our frontend can communicate with both services.
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        configuration.setAllowedMethods(Arrays.asList(allowedMethods.split(",")));
        configuration.setAllowedHeaders(Arrays.asList(allowedHeaders.split(",")));
        configuration.setAllowCredentials(allowCredentials);
        // Expose headers that frontend needs to read
        configuration.setExposedHeaders(List.of("Authorization", "Content-Type"));
        // Cache preflight requests for 1 hour to reduce overhead
        configuration.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
