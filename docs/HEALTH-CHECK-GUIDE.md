# Health Check Guide

## Overview

This service uses **Spring Boot Actuator** for health checks, following Kubernetes and Docker best practices.

## Available Health Endpoints

### 1. Overall Health Check

```bash
GET http://localhost:8080/actuator/health
```

**Response:**

```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "validationQuery": "isValid()"
      }
    },
    "diskSpace": {
      "status": "UP"
    },
    "ping": {
      "status": "UP"
    }
  }
}
```

### 2. Liveness Probe (Kubernetes)

```bash
GET http://localhost:8080/actuator/health/liveness
```

**Purpose:** Determines if the application is alive and should be restarted if failing.

**Response:**

```json
{
  "status": "UP"
}
```

### 3. Readiness Probe (Kubernetes)

```bash
GET http://localhost:8080/actuator/health/readiness
```

**Purpose:** Determines if the application is ready to receive traffic.

**Response:**

```json
{
  "status": "UP"
}
```

### 4. Application Info

```bash
GET http://localhost:8080/actuator/info
```

### 5. Metrics (Prometheus)

```bash
GET http://localhost:8080/actuator/prometheus
```

### 6. Simple Ping Test

```bash
GET http://localhost:8080/api/v1/check/ping
```

**Response:**

```
pong
```

## Kubernetes Configuration

### Deployment Health Probes

```yaml
containers:
  - name: auth-service
    image: exploresg-auth-service:latest

    # Startup Probe - For slow-starting applications
    startupProbe:
      httpGet:
        path: /actuator/health/liveness
        port: 8080
      initialDelaySeconds: 0
      periodSeconds: 10
      timeoutSeconds: 3
      failureThreshold: 30 # Max 5 minutes to start

    # Liveness Probe - Restarts pod if failing
    livenessProbe:
      httpGet:
        path: /actuator/health/liveness
        port: 8080
      initialDelaySeconds: 0
      periodSeconds: 10
      timeoutSeconds: 5
      failureThreshold: 3

    # Readiness Probe - Removes from service if not ready
    readinessProbe:
      httpGet:
        path: /actuator/health/readiness
        port: 8080
      initialDelaySeconds: 0
      periodSeconds: 5
      timeoutSeconds: 3
      failureThreshold: 3
```

## Docker Compose Health Check

```yaml
services:
  backend-auth-dev:
    healthcheck:
      test:
        ["CMD", "curl", "-f", "http://localhost:8080/actuator/health/liveness"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s
```

## Testing Health Endpoints

### Local Development

```bash
# Overall health
curl http://localhost:8080/actuator/health

# Liveness
curl http://localhost:8080/actuator/health/liveness

# Readiness
curl http://localhost:8080/actuator/health/readiness

# Simple ping
curl http://localhost:8080/api/v1/check/ping

# Hello endpoint
curl http://localhost:8080/api/v1/check/hello
```

### Docker Container

```bash
# Check from host
curl http://localhost:8080/actuator/health

# Check inside container
docker exec dev-exploresg-auth-service curl http://localhost:8080/actuator/health/liveness
```

### Kubernetes Pod

```bash
# Port forward
kubectl port-forward -n exploresg pod/exploresg-auth-service-xxxxx 8080:8080

# Test locally
curl http://localhost:8080/actuator/health

# Or directly from pod
kubectl exec -n exploresg exploresg-auth-service-xxxxx -- curl http://localhost:8080/actuator/health/liveness
```

## Health Check Components

The readiness probe checks:

- **Database connectivity** - PostgreSQL connection is healthy
- **Application state** - Service is ready to handle requests

The liveness probe checks:

- **Application is running** - JVM is alive and responsive

## Configuration

### application.properties

```properties
# Actuator & Monitoring
management.endpoints.web.exposure.include=health,info,metrics,prometheus
management.endpoint.health.show-details=when-authorized

# Kubernetes Health Probes
management.endpoint.health.probes.enabled=true
management.health.livenessstate.enabled=true
management.health.readinessstate.enabled=true

# Health check components
management.endpoint.health.show-components=always
management.endpoint.health.group.liveness.include=livenessState
management.endpoint.health.group.readiness.include=readinessState,db
```

### Environment Variables

```bash
MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,info,metrics,prometheus
MANAGEMENT_ENDPOINT_HEALTH_SHOW_DETAILS=when-authorized
```

## Security Configuration

Health endpoints are publicly accessible (no authentication required):

```java
.requestMatchers(
    "/actuator/health",
    "/actuator/health/liveness",
    "/actuator/health/readiness",
    "/actuator/info",
    "/actuator/prometheus"
).permitAll()
```

## Monitoring Integration

### Prometheus

The service exposes Prometheus metrics at:

```
http://localhost:8080/actuator/prometheus
```

### Grafana Dashboard

You can use these health endpoints to create:

- Uptime dashboards
- Availability metrics
- Service health status

## Troubleshooting

### Health Check Failing

1. **Check logs:**

   ```bash
   kubectl logs -n exploresg exploresg-auth-service-xxxxx
   ```

2. **Check database connection:**

   ```bash
   curl http://localhost:8080/actuator/health
   ```

3. **Verify configuration:**
   ```bash
   kubectl describe pod -n exploresg exploresg-auth-service-xxxxx
   ```

### Pod Restarting Frequently

- Check liveness probe settings (may be too aggressive)
- Increase `initialDelaySeconds` or `failureThreshold`
- Review application logs for startup issues

### Pod Not Receiving Traffic

- Check readiness probe is passing
- Verify database connectivity
- Check service and ingress configuration

## Best Practices

✅ **Use separate probes** - Liveness for restart, Readiness for traffic  
✅ **Set appropriate timeouts** - Allow enough time for checks  
✅ **Include database checks in readiness** - Don't serve traffic if DB is down  
✅ **Use startup probes** - Protect slow-starting applications  
✅ **Monitor health endpoints** - Set up alerts for failures  
✅ **Keep probes lightweight** - Fast checks reduce overhead

## References

- [Spring Boot Actuator Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [Kubernetes Liveness and Readiness Probes](https://kubernetes.io/docs/tasks/configure-pod-container/configure-liveness-readiness-startup-probes/)
- [Docker Compose Healthcheck](https://docs.docker.com/compose/compose-file/compose-file-v3/#healthcheck)
