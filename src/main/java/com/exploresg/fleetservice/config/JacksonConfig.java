package com.exploresg.fleetservice.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Jackson Configuration for JSON Serialization/Deserialization
 * 
 * Configures ObjectMapper to:
 * - Handle Java 8 Date/Time types (LocalDateTime, Instant, etc.)
 * - Accept ISO-8601 date formats with and without timezone
 * - Write dates as strings (not timestamps)
 * - Be lenient with unknown properties
 */
@Configuration
public class JacksonConfig {

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // Register JavaTimeModule to handle Java 8 date/time types
        mapper.registerModule(new JavaTimeModule());

        // Write dates as ISO-8601 strings, not as timestamps
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Be lenient with unknown properties (don't fail on extra fields)
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // Allow accepting single values as arrays (helps with flexibility)
        mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);

        // Allow coercion of empty strings to null for better handling
        mapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);

        return mapper;
    }
}
