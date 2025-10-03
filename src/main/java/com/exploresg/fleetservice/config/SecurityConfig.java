package com.exploresg.fleetservice.config;

import java.util.Base64; // <-- ADD THIS IMPORT
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
import javax.crypto.spec.SecretKeySpec;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${application.security.jwt.secret-key}")
    private String jwtSecretKey;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/hello").permitAll()
                        .anyRequest().authenticated())
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.decoder(jwtDecoder())));

        return http.build();
    }

    // @Bean
    // public JwtDecoder jwtDecoder() {
    // // --- THIS IS THE CORRECTED LINE ---
    // // The algorithm must be "HmacSHA256" to match the token's signature
    // SecretKeySpec secretKey = new SecretKeySpec(jwtSecretKey.getBytes(),
    // "HmacSHA256");
    // return NimbusJwtDecoder.withSecretKey(secretKey).build();
    // }

    // --- REPLACE THE OLD JWTDECODER METHOD WITH THIS ONE ---
    @Bean
    public JwtDecoder jwtDecoder() {
        // First, Base64-decode the secret key to match the auth-service's signing
        // process
        byte[] keyBytes = Base64.getDecoder().decode(jwtSecretKey);
        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(secretKey).build();
    }
}