# Changelog

All notable changes to the ExploreSG Fleet Service will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Planned Features

- [ ] Vehicle recommendations based on user preferences
- [ ] Real-time vehicle tracking integration
- [ ] Advanced analytics dashboard for fleet managers
- [ ] Mobile app support endpoints
- [ ] Multi-language support
- [ ] Rate limiting per user/API key

---

## [1.0.0] - 2025-10-18

### 🎉 Initial Release

The first production-ready release of the ExploreSG Fleet Service.

### Added

#### Core Features

- **Fleet Management System**

  - Car model master catalog management
  - Individual vehicle fleet management
  - Vehicle status tracking (AVAILABLE, BOOKED, UNDER_MAINTENANCE)
  - Multi-tenant support for fleet operators

- **Two-Phase Reservation System**

  - Temporary reservation creation (30-second lock)
  - Reservation confirmation with payment reference
  - Automatic reservation expiration
  - Reservation cancellation support
  - Pessimistic locking to prevent double-booking

- **Availability Management**

  - Real-time vehicle availability checking
  - Date range-based availability queries
  - Operator-specific availability filtering

- **Fleet Manager Dashboard**

  - Comprehensive fleet statistics
  - Vehicle status breakdown
  - Model-wise fleet distribution
  - Utilization metrics

- **Advanced Search & Pagination**
  - Vehicle search with multiple filters
  - Pagination support for large datasets
  - Sorting by multiple fields
  - License plate, status, model, and location filtering

#### Security Features

- **Authentication & Authorization**

  - OAuth2 Resource Server integration
  - JWT token validation (Google OAuth2)
  - Custom JWT generation for service-to-service communication
  - Role-Based Access Control (ADMIN, FLEET_MANAGER, CUSTOMER)
  - Method-level security with `@PreAuthorize`

- **CORS Configuration**
  - Configurable allowed origins
  - Support for credentials
  - Customizable HTTP methods and headers

#### API Documentation

- **OpenAPI 3.0 / Swagger UI**
  - Interactive API documentation
  - Request/response schema visualization
  - Authentication testing support
  - Available at `/swagger-ui.html`

#### Observability

- **Structured Logging**

  - JSON-formatted logs with Logstash encoder
  - Correlation ID tracking across requests
  - MDC (Mapped Diagnostic Context) support
  - Configurable log levels per package

- **Health Checks**

  - Spring Boot Actuator endpoints
  - Database health monitoring
  - Disk space monitoring
  - Custom health indicators

- **Metrics & Monitoring**
  - Prometheus metrics endpoint
  - JVM metrics (memory, threads, GC)
  - HTTP request metrics
  - Database connection pool metrics
  - Custom business metrics

#### Testing Infrastructure

- **Comprehensive Test Suite**

  - Unit tests with Mockito
  - Integration tests with Spring Boot Test
  - Security tests with Spring Security Test
  - Repository tests with H2 in-memory database
  - Maven profiles for different test scenarios

- **Code Coverage**
  - JaCoCo integration
  - Minimum 80% coverage requirement
  - Coverage reports in HTML format
  - CI/CD integration for coverage tracking

#### DevOps & Deployment

- **Containerization**

  - Dockerfile for application
  - Docker Compose setup with PostgreSQL
  - Multi-stage build support
  - External network configuration

- **Kubernetes Support**

  - Deployment manifests
  - Service definitions
  - Ingress configuration
  - Health check probes

- **CI/CD Pipeline**
  - GitHub Actions workflow
  - Automated build and test
  - Security scanning
  - Code quality checks
  - Artifact publishing

#### Configuration Management

- **Environment Profiles**

  - Development profile (`application-dev.properties`)
  - Staging profile (`application-staging.properties`)
  - Production profile (`application-prod.properties`)
  - Environment variable support

- **Feature Configurations**
  - Database connection pooling
  - Transaction timeout settings
  - Reservation expiry configuration
  - Scheduled task configuration
  - Actuator endpoint exposure

### Database Schema

- `car_models` - Master catalog of car models
- `fleet_vehicles` - Individual vehicles in operator fleets
- `reservations` - Booking reservations with status tracking

### API Endpoints

#### Public Endpoints

- `GET /api/v1/fleet/models` - List available car models
- `GET /api/v1/fleet/operators/{operatorId}/models` - Get models by operator
- `GET /api/v1/fleet/models/{modelId}/availability-count` - Check availability

#### Customer Endpoints

- `POST /api/v1/fleet/reservations/temporary` - Create temporary reservation
- `POST /api/v1/fleet/reservations/{id}/confirm` - Confirm reservation
- `DELETE /api/v1/fleet/reservations/{id}` - Cancel reservation
- `GET /api/v1/fleet/reservations/{id}` - Get reservation details

#### Fleet Manager Endpoints

- `GET /api/v1/fleet/operators/fleet` - Get my fleet models
- `GET /api/v1/fleet/operators/fleet/all` - Get all my vehicles
- `GET /api/v1/fleet/operators/fleet/all/paginated` - Search vehicles
- `GET /api/v1/fleet/operators/dashboard` - Fleet dashboard
- `PATCH /api/v1/fleet/operators/fleet/{id}/status` - Update vehicle status

#### Admin Endpoints

- `POST /api/v1/fleet/models` - Create car model
- `GET /api/v1/fleet/models/all` - View all car models

### Dependencies

- Spring Boot 3.5.6
- Java 17
- PostgreSQL 15
- Spring Security OAuth2 Resource Server
- JJWT 0.11.5
- SpringDoc OpenAPI 2.7.0
- Logstash Logback Encoder 7.4
- JaCoCo 0.8.13
- Lombok 1.18.36
- H2 Database (test scope)

### Documentation

- Comprehensive README with setup instructions
- API Reference documentation
- Contributing guidelines
- Architecture diagrams
- Deployment guides
- Health check documentation
- Logging guide
- Swagger documentation guide

### Infrastructure

- Docker Compose configuration
- Kubernetes deployment manifests
- GitHub Actions CI/CD pipeline
- Environment variable configuration
- Database migration scripts

---

## Version History

### Version Numbering

This project uses [Semantic Versioning](https://semver.org/):

- **MAJOR** version for incompatible API changes
- **MINOR** version for new functionality (backward-compatible)
- **PATCH** version for backward-compatible bug fixes

---

## Migration Guide

### From Pre-Release to 1.0.0

This is the first official release. No migration required.

---

## Deprecation Notices

No deprecations in this release.

---

## Breaking Changes

No breaking changes in this release.

---

## Security Updates

### Initial Security Features (v1.0.0)

- OAuth2 Resource Server implementation
- JWT token validation
- Role-based access control
- CORS configuration
- Secure password handling
- Environment variable-based secrets

---

## Performance Improvements

### Database Optimization (v1.0.0)

- Pessimistic locking for reservation concurrency
- Connection pooling configuration
- Transaction timeout settings
- Query optimization for availability checks

---

## Known Issues

### Current Limitations

1. **Reservation Cleanup**: Expired reservations are cleaned up by scheduled task (5-minute interval)

   - **Impact**: Slight delay in freeing up vehicles from expired reservations
   - **Workaround**: None required - will be freed automatically
   - **Fix Planned**: v1.1.0

2. **Rate Limiting**: Not yet implemented at application level
   - **Impact**: Potential for API abuse
   - **Workaround**: Use API Gateway or reverse proxy rate limiting
   - **Fix Planned**: v1.1.0

---

## Contributors

### Core Team

- Development Team @ ExploreSG

### Special Thanks

- Spring Boot Team
- PostgreSQL Community
- Docker Community

---

## Release Notes

### How to Upgrade

#### From Source

```bash
git checkout v1.0.0
./mvnw clean package -DskipTests
java -jar target/fleet-service-0.0.1-SNAPSHOT.jar
```

#### Using Docker

```bash
docker pull exploresg/fleet-service:1.0.0
docker run -d -p 8080:8080 --env-file .env exploresg/fleet-service:1.0.0
```

#### Using Kubernetes

```bash
kubectl apply -f kubernetes/deployment.yaml
kubectl set image deployment/fleet-service fleet-service=exploresg/fleet-service:1.0.0
```

---

## Support

For questions about this release:

- **GitHub Issues**: https://github.com/XploreSG/exploresg-fleet-service/issues
- **Documentation**: See `docs/` folder
- **Email**: dev@exploresg.com

---

[Unreleased]: https://github.com/XploreSG/exploresg-fleet-service/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/XploreSG/exploresg-fleet-service/releases/tag/v1.0.0
