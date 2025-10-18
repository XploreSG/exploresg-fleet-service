# Logging Implementation Guide - Fleet Service

## 📚 Complete Guide to Logging

### Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Configuration](#configuration)
4. [Usage Examples](#usage-examples)
5. [CloudWatch Integration](#cloudwatch-integration)
6. [Troubleshooting](#troubleshooting)
7. [Best Practices](#best-practices)

---

## Overview

The Fleet Service uses structured logging with correlation IDs, user context, and multi-environment support for comprehensive observability in cloud environments.

### Features

✅ **Correlation IDs** - Track requests across microservices  
✅ **User Context** - Audit trail with user information  
✅ **Structured JSON** - Machine-readable logs for production  
✅ **Request Tracking** - HTTP method, path, duration, status  
✅ **IP Detection** - Client IP with proxy support  
✅ **Slow Request Detection** - Automatic warnings for >2s requests  
✅ **Multi-Environment** - Dev, Staging, Production, Test profiles  
✅ **Cloud-Native** - Kubernetes, CloudWatch, ELK ready

---

## Architecture

### Logging Flow

```
┌────────────────────────────────────────────────────────────┐
│                    Incoming HTTP Request                    │
└──────────────────────────┬─────────────────────────────────┘
                           │
                           ▼
┌──────────────────────────────────────────────────────────────┐
│          RequestCorrelationFilter (HIGHEST_PRECEDENCE)       │
│  - Generate/extract correlation ID                           │
│  - Add to MDC: correlationId                                 │
│  - Add to response header                                    │
└──────────────────────────┬───────────────────────────────────┘
                           │
                           ▼
┌──────────────────────────────────────────────────────────────┐
│              Spring Security Filter Chain                     │
│  - JWT validation                                            │
│  - User authentication                                       │
└──────────────────────────┬───────────────────────────────────┘
                           │
                           ▼
┌──────────────────────────────────────────────────────────────┐
│       UserContextLoggingFilter (LOWEST_PRECEDENCE - 1)       │
│  - Extract authenticated user                                │
│  - Add to MDC: userId, userEmail                             │
└──────────────────────────┬───────────────────────────────────┘
                           │
                           ▼
┌──────────────────────────────────────────────────────────────┐
│              RequestLoggingInterceptor.preHandle             │
│  - Record start time                                         │
│  - Add to MDC: requestMethod, requestPath, clientIp          │
└──────────────────────────┬───────────────────────────────────┘
                           │
                           ▼
┌──────────────────────────────────────────────────────────────┐
│                    Controller Execution                       │
│  - Your business logic                                       │
│  - All logs include MDC context                              │
└──────────────────────────┬───────────────────────────────────┘
                           │
                           ▼
┌──────────────────────────────────────────────────────────────┐
│         RequestLoggingInterceptor.afterCompletion            │
│  - Calculate duration                                        │
│  - Log request completion with status                        │
│  - Check for slow requests                                   │
│  - Clean up MDC                                              │
└──────────────────────────┬───────────────────────────────────┘
                           │
                           ▼
┌────────────────────────────────────────────────────────────┐
│                     HTTP Response                           │
│  - Includes X-Correlation-ID header                        │
└────────────────────────────────────────────────────────────┘
```

### MDC (Mapped Diagnostic Context) Fields

| Field           | Source                    | Example                  | Available When         |
| --------------- | ------------------------- | ------------------------ | ---------------------- |
| `correlationId` | RequestCorrelationFilter  | `abc-123-def-456`        | All requests           |
| `userId`        | UserContextLoggingFilter  | `user-42`                | Authenticated requests |
| `userEmail`     | UserContextLoggingFilter  | `user@example.com`       | Authenticated requests |
| `requestMethod` | RequestLoggingInterceptor | `GET`                    | All requests           |
| `requestPath`   | RequestLoggingInterceptor | `/api/v1/fleet/vehicles` | All requests           |
| `clientIp`      | RequestLoggingInterceptor | `192.168.1.100`          | All requests           |

---

## Configuration

### Environment Profiles

#### Development (`dev` or `local`)

- Human-readable logs with colors
- DEBUG level for your packages
- Full stack traces
- No JSON formatting

#### Staging (`staging`)

- Pretty-printed JSON (readable in console)
- DEBUG level for debugging
- Full context information
- Ideal for pre-production testing

#### Production (`prod`)

- Compact JSON for performance
- INFO level (WARN for frameworks)
- Shortened stack traces
- Optimized for log aggregation

#### Test (`test`)

- Minimal logging
- WARN level to reduce noise
- Simple pattern

### logback-spring.xml

```xml
<!-- Located at: src/main/resources/logback-spring.xml -->
<configuration>
    <!-- Development Profile -->
    <springProfile name="dev,local">
        <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
            <encoder>
                <pattern>%clr(%d{yyyy-MM-dd HH:mm:ss.SSS}){faint} %clr(%-5level) %clr([%X{correlationId:-NO_CORRELATION_ID}]){magenta} %clr([%thread]){faint} %clr(%logger{36}){cyan} - %msg%n</pattern>
            </encoder>
        </appender>
    </springProfile>

    <!-- Production Profile -->
    <springProfile name="prod">
        <appender name="JSON_CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
            <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
                <providers>
                    <timestamp><timeZone>UTC</timeZone></timestamp>
                    <message/>
                    <loggerName/>
                    <threadName/>
                    <logLevel/>
                    <mdc/>
                    <stackTrace/>
                </providers>
            </encoder>
        </appender>
    </springProfile>
</configuration>
```

### application.properties

```properties
# Application name (used in logs)
spring.application.name=exploresg-fleet-service

# Logging configuration
logging.level.root=${LOGGING_LEVEL_ROOT:INFO}
logging.level.com.exploresg=${LOGGING_LEVEL_COM_EXPLORESG:INFO}
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss} [%X{correlationId:-NO_CORRELATION_ID}] - %msg%n
```

### application-prod.properties

```properties
# Production logging
logging.level.root=INFO
logging.level.com.exploresg=INFO
logging.level.org.springframework=WARN
logging.level.org.hibernate=WARN
```

---

## Usage Examples

### Basic Logging in Controllers

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/v1/fleet")
public class FleetController {

    private static final Logger log = LoggerFactory.getLogger(FleetController.class);

    @GetMapping("/vehicles")
    public ResponseEntity<List<Vehicle>> getAllVehicles() {
        log.info("Fetching all available vehicles");

        List<Vehicle> vehicles = vehicleService.findAllAvailable();

        log.info("Found {} available vehicles", vehicles.size());
        return ResponseEntity.ok(vehicles);
    }
}
```

### Logging with Context

```java
@GetMapping("/vehicles/{id}")
public ResponseEntity<Vehicle> getVehicle(@PathVariable Long id) {
    log.info("Fetching vehicle with ID: {}", id);

    Optional<Vehicle> vehicle = vehicleService.findById(id);

    if (vehicle.isEmpty()) {
        log.warn("Vehicle not found with ID: {}", id);
        return ResponseEntity.notFound().build();
    }

    log.debug("Vehicle details: make={}, model={}, year={}",
        vehicle.get().getMake(),
        vehicle.get().getModel(),
        vehicle.get().getYear());

    return ResponseEntity.ok(vehicle.get());
}
```

### Error Logging

```java
@PostMapping("/vehicles")
public ResponseEntity<Vehicle> createVehicle(@Valid @RequestBody VehicleDto dto) {
    log.info("Creating new vehicle: {} {} {}",
        dto.getMake(), dto.getModel(), dto.getYear());

    try {
        Vehicle created = vehicleService.create(dto);
        log.info("Successfully created vehicle with ID: {}", created.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    } catch (IllegalArgumentException e) {
        log.warn("Invalid vehicle data: {}", e.getMessage());
        throw e;
    } catch (Exception e) {
        log.error("Failed to create vehicle: {}", e.getMessage(), e);
        throw e;
    }
}
```

### Security Audit Logging

```java
@PostMapping("/vehicles/{id}/reserve")
@PreAuthorize("hasRole('USER')")
public ResponseEntity<?> reserveVehicle(
        @PathVariable Long id,
        @RequestBody ReservationDto dto,
        Authentication auth) {

    String userId = auth.getName();

    log.info("User {} attempting to reserve vehicle {} for {} days",
        userId, id, dto.getDays());

    Reservation reservation = reservationService.create(id, userId, dto);

    log.info("Vehicle {} successfully reserved by user {}. Reservation ID: {}",
        id, userId, reservation.getId());

    return ResponseEntity.ok(reservation);
}
```

### Performance Logging

```java
@GetMapping("/search")
public ResponseEntity<List<Vehicle>> searchVehicles(@RequestParam String query) {
    log.info("Searching vehicles with query: {}", query);

    long startTime = System.currentTimeMillis();
    List<Vehicle> results = vehicleService.search(query);
    long duration = System.currentTimeMillis() - startTime;

    log.info("Search completed in {}ms, found {} results", duration, results.size());

    if (duration > 1000) {
        log.warn("Slow search query detected: '{}' took {}ms", query, duration);
    }

    return ResponseEntity.ok(results);
}
```

---

## CloudWatch Integration

### Automatic Log Capture

When deployed to Kubernetes/EKS, logs are automatically captured and sent to CloudWatch.

### CloudWatch Insights Queries

#### Find All Requests by Correlation ID

```cloudwatch
fields @timestamp, message, requestMethod, requestPath, userId
| filter correlationId = "abc-123-def-456"
| sort @timestamp asc
```

#### Track User Activity

```cloudwatch
fields @timestamp, message, requestMethod, requestPath
| filter userId = "user-42"
| sort @timestamp desc
| limit 100
```

#### Find Slow Requests

```cloudwatch
fields @timestamp, requestMethod, requestPath, message
| filter message like /took.*ms/
| parse message /took (?<duration>\d+)ms/
| filter duration > 2000
| sort duration desc
```

#### Find All Errors

```cloudwatch
fields @timestamp, level, message, logger, requestPath
| filter level = "ERROR"
| sort @timestamp desc
| limit 50
```

#### Track Vehicle Reservations

```cloudwatch
fields @timestamp, message, userId
| filter message like /reserve/
| sort @timestamp desc
```

#### Monitor API Health

```cloudwatch
fields @timestamp, requestPath, message
| filter message like /completed with status/
| parse message /status (?<status>\d+)/
| stats count() by status
```

---

## Troubleshooting

### Common Issues and Solutions

#### 1. Logs Not Appearing in CloudWatch

**Symptoms:** No logs in CloudWatch console

**Check:**

```bash
# Verify pod is running
kubectl get pods -n exploresg -l app=exploresg-fleet-service

# Check pod logs directly
kubectl logs -n exploresg <pod-name>
```

**Solutions:**

- Ensure CloudWatch agent/Fluent Bit is configured
- Verify IAM roles have CloudWatch Logs permissions
- Check container is writing to stdout/stderr

#### 2. Logs Not in JSON Format

**Symptoms:** Logs are human-readable instead of JSON

**Check:**

```bash
# Verify Spring profile
kubectl exec -n exploresg <pod-name> -- env | grep SPRING_PROFILES_ACTIVE
```

**Solution:**

- Set `SPRING_PROFILES_ACTIVE=prod` in ConfigMap or Secret
- Restart pods: `kubectl rollout restart deployment/exploresg-fleet-service -n exploresg`

#### 3. Correlation ID Missing

**Symptoms:** No correlationId in logs

**Check application startup logs:**

```bash
kubectl logs -n exploresg <pod-name> | grep RequestCorrelationFilter
```

**Solution:**

- Verify `RequestCorrelationFilter` is loaded (should auto-configure)
- Check for conflicting filter configurations

#### 4. User Context Not Captured

**Symptoms:** No userId/userEmail in logs

**Possible Causes:**

- Request is not authenticated (public endpoint)
- JWT validation failing
- Filter order issue

**Solution:**

```bash
# Test with valid JWT token
curl https://api.xplore.town/fleet/api/v1/fleet/vehicles \
  -H "Authorization: Bearer <valid-jwt>"

# Check logs should include userId
```

#### 5. Too Many Logs / High Volume

**Symptoms:** CloudWatch costs increasing, too much noise

**Solutions:**

```properties
# Reduce logging levels in application-prod.properties
logging.level.root=WARN
logging.level.com.exploresg.fleetservice=INFO
logging.level.org.springframework=ERROR
```

---

## Best Practices

### DO's ✅

✅ **Use appropriate log levels**

- INFO: Important business events
- WARN: Potential issues, degraded functionality
- ERROR: Failures that need attention
- DEBUG: Detailed debugging (dev only)

✅ **Include context in messages**

```java
// Good
log.info("Vehicle {} reserved by user {} for {} days", vehicleId, userId, days);

// Bad
log.info("Vehicle reserved");
```

✅ **Use correlation IDs for debugging**

```java
// When debugging multi-service issues, filter by correlation ID
log.info("Calling booking service for vehicle {}", vehicleId);
```

✅ **Log at decision points**

```java
if (vehicle.isAvailable()) {
    log.info("Vehicle {} is available for reservation", vehicleId);
} else {
    log.warn("Vehicle {} is not available, current status: {}", vehicleId, vehicle.getStatus());
}
```

✅ **Use structured logging in production**

- Always use `prod` profile for production deployments
- JSON format enables powerful querying in CloudWatch

### DON'Ts ❌

❌ **Don't log sensitive data**

```java
// BAD - Never log passwords, tokens, credit cards
log.info("User password: {}", password);
log.info("JWT token: {}", token);
log.info("Credit card: {}", creditCard);
```

❌ **Don't log in tight loops**

```java
// BAD
for (Vehicle vehicle : vehicles) {
    log.info("Processing vehicle {}", vehicle.getId());
}

// GOOD
log.info("Processing {} vehicles", vehicles.size());
```

❌ **Don't use System.out.println**

```java
// BAD
System.out.println("Debug info");

// GOOD
log.debug("Debug info");
```

❌ **Don't log exceptions without context**

```java
// BAD
catch (Exception e) {
    log.error(e.getMessage());
}

// GOOD
catch (Exception e) {
    log.error("Failed to reserve vehicle {}: {}", vehicleId, e.getMessage(), e);
}
```

---

## Testing Logging Locally

### Test Correlation ID

```powershell
# Send request with custom correlation ID
$response = curl http://localhost:8080/api/v1/fleet/models `
  -H "X-Correlation-ID: test-correlation-123" `
  -v

# Check response header includes X-Correlation-ID
# Check logs include [test-correlation-123]
```

### Test Different Profiles

```powershell
# Test development profile
$env:SPRING_PROFILES_ACTIVE="dev"
.\mvnw spring-boot:run

# Test production profile
$env:SPRING_PROFILES_ACTIVE="prod"
java -jar target/fleet-service-0.0.1-SNAPSHOT.jar
```

### Test Slow Request Warning

Create a test endpoint that sleeps:

```java
@GetMapping("/test/slow")
public String slowEndpoint() throws InterruptedException {
    Thread.sleep(3000); // 3 seconds
    return "slow response";
}
```

Request it and check for WARN log about slow request.

---

## Summary

Your Fleet Service now has:

✅ Production-grade structured logging  
✅ Correlation IDs for distributed tracing  
✅ User context for security auditing  
✅ Request tracking with duration monitoring  
✅ Multi-environment configuration  
✅ Cloud-native observability  
✅ Kubernetes and CloudWatch ready

**Status: Production Ready** 🚀

---

## Related Documentation

- [Logging Implementation Summary](./LOGGING-IMPLEMENTATION-SUMMARY.md)
- [Health Check Guide](./HEALTH-CHECK-GUIDE.md)
- [Kubernetes Deployment Guide](./KUBERNETES-DEPLOYMENT-GUIDE.md)
- [Cloud Readiness Checklist](./CLOUD-READINESS-CHECKLIST.md)
