# Swagger/OpenAPI Documentation

This service has been configured with Swagger/OpenAPI documentation for easy API exploration and testing.

## Accessing Swagger UI

Once the application is running, you can access the Swagger UI at:

**Swagger UI**: [http://localhost:8081/swagger-ui.html](http://localhost:8081/swagger-ui.html)

## API Documentation JSON

The OpenAPI specification in JSON format is available at:

**API Docs**: [http://localhost:8081/v3/api-docs](http://localhost:8081/v3/api-docs)

## Using Swagger UI

### For Public Endpoints

Simply select the endpoint and click "Try it out" to test it.

### For Protected Endpoints (Requires JWT)

1. **Obtain a JWT Token**: First, authenticate with the auth service to get a JWT token
2. **Authorize in Swagger**:
   - Click the "Authorize" button at the top right of the Swagger UI
   - Enter your JWT token in the format: `Bearer <your-token-here>` or just paste the token directly
   - Click "Authorize"
   - Click "Close"
3. **Test Endpoints**: Now you can test protected endpoints with your authentication

## Features

- **Interactive API Documentation**: All endpoints are documented with request/response examples
- **Try It Out**: Test API endpoints directly from the browser
- **JWT Authentication**: Built-in support for Bearer token authentication
- **Schema Definitions**: View all DTO and model schemas
- **Response Codes**: See all possible response codes and their meanings

## Configuration

Swagger configuration can be found in:

- **Java Config**: `src/main/java/com/exploresg/fleetservice/config/OpenApiConfig.java`
- **Properties**: `src/main/resources/application.properties` (Swagger section)

## Development

To modify the API documentation:

- Use OpenAPI annotations in your controllers (e.g., `@Operation`, `@ApiResponse`, `@Parameter`)
- Update the `OpenApiConfig.java` for global API information
- Add schema descriptions to your DTOs using `@Schema` annotation

## Dependencies

This service uses SpringDoc OpenAPI:

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.7.0</version>
</dependency>
```

## Security Note

The following Swagger endpoints are publicly accessible (no authentication required):

- `/v3/api-docs/**`
- `/swagger-ui/**`
- `/swagger-ui.html`

This is configured in `SecurityConfig.java` for easy API exploration during development.
