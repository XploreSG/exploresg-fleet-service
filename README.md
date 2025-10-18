[![CI Backend - Build, Test & Security Scan](https://github.com/XploreSG/exploresg-fleet-service/actions/workflows/ci-java.yml/badge.svg)](https://github.com/XploreSG/exploresg-fleet-service/actions/workflows/ci-java.yml)

# 🚗 ExploreSG Fleet Service

> **Fleet Management Microservice for the ExploreSG Platform**

A production-ready Spring Boot microservice that manages vehicle fleets, car models, and booking reservations for the ExploreSG car rental platform. Built with enterprise-grade features including JWT authentication, pessimistic locking for reservations, comprehensive monitoring, and cloud-native deployment support.

---

## 📋 Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Environment Setup](#environment-setup)
  - [Running Locally](#running-locally)
  - [Running with Docker](#running-with-docker)
- [API Documentation](#api-documentation)
- [Security](#security)
- [Database](#database)
- [Testing](#testing)
- [Deployment](#deployment)
- [Monitoring & Observability](#monitoring--observability)
- [Project Structure](#project-structure)
- [Contributing](#contributing)
- [Additional Documentation](#additional-documentation)
- [License](#license)

---

## 🎯 Overview

The **ExploreSG Fleet Service** is a critical microservice in the ExploreSG ecosystem that handles:

- **Fleet Management**: CRUD operations for vehicle fleets and car models
- **Vehicle Reservations**: Two-phase reservation system with pessimistic locking
- **Availability Tracking**: Real-time vehicle availability checks
- **Multi-tenant Support**: Role-based access for admins, fleet managers, and customers
- **Integration Ready**: REST APIs for booking service integration

---

## ✨ Features

### Core Functionality
- ✅ **Car Model Management** - Create and manage master catalog of car models (Admin)
- ✅ **Fleet Operations** - Manage individual vehicles in operator fleets (Fleet Manager)
- ✅ **Two-Phase Reservations** - Temporary locks with auto-expiry for booking flow
- ✅ **Availability Checking** - Real-time availability counts by model and date range
- ✅ **Fleet Dashboard** - Comprehensive statistics for fleet managers
- ✅ **Advanced Search** - Paginated vehicle search with filtering

### Enterprise Features
- 🔐 **JWT Authentication** - OAuth2 resource server with custom JWT support
- 🔒 **Role-Based Access Control** - Admin, Fleet Manager, and Customer roles
- 🔄 **Pessimistic Locking** - Prevents double-booking during reservations
- 📊 **Structured Logging** - JSON logs with correlation IDs (Logstash format)
- 🏥 **Health Checks** - Spring Actuator with Prometheus metrics
- 📖 **API Documentation** - OpenAPI 3.0 / Swagger UI
- 🐳 **Container Ready** - Docker and Docker Compose support
- ☸️ **Kubernetes Ready** - Deployment manifests included

---

## 🛠 Tech Stack

| Category | Technologies |
|----------|-------------|
| **Framework** | Spring Boot 3.5.6, Spring Data JPA, Spring Security |
| **Language** | Java 17 |
| **Database** | PostgreSQL 15 (production), H2 (testing) |
| **Security** | OAuth2 Resource Server, JWT (jjwt 0.11.5) |
| **Documentation** | SpringDoc OpenAPI 2.7.0 |
| **Logging** | SLF4J, Logback, Logstash Encoder |
| **Monitoring** | Spring Actuator, Micrometer, Prometheus |
| **Testing** | JUnit 5, Mockito, Spring Security Test |
| **Build Tool** | Maven 3.x |
| **Containerization** | Docker, Docker Compose |
| **Orchestration** | Kubernetes |

---

## 🏗 Architecture

### System Architecture

```
┌─────────────────┐
│  Frontend App   │
└────────┬────────┘
         │ HTTPS
         ▼
┌─────────────────┐
│   API Gateway   │
└────────┬────────┘
         │
         ▼
┌─────────────────┐      ┌─────────────────┐
│  Fleet Service  │◄────►│ Booking Service │
└────────┬────────┘      └─────────────────┘
         │
         ▼
┌─────────────────┐
│   PostgreSQL    │
└─────────────────┘
```

### Service Layers

```
┌──────────────────────────────────────┐
│         REST Controllers             │
│  (FleetController, ReservationController)
├──────────────────────────────────────┤
│        Service Layer                 │
│  (Business Logic, Transaction Mgmt)  │
├──────────────────────────────────────┤
│       Repository Layer               │
│     (Spring Data JPA)                │
├──────────────────────────────────────┤
│         Database                     │
│       (PostgreSQL)                   │
└──────────────────────────────────────┘
```

---

## 🚀 Getting Started

### Prerequisites

- **Java 17** or higher
- **Maven 3.8+**
- **PostgreSQL 15** (or use Docker Compose)
- **Docker & Docker Compose** (optional, for containerized setup)
- **Git**

### Environment Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/XploreSG/exploresg-fleet-service.git
   cd exploresg-fleet-service
   ```

2. **Create `.env` file** in the project root:
   ```bash
   # Database Configuration
   POSTGRES_DB=exploresg-fleet-service-db
   POSTGRES_USER=exploresguser
   POSTGRES_PASSWORD=your_secure_password
   
   # Spring Datasource
   SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/exploresg-fleet-service-db
   SPRING_DATASOURCE_USERNAME=exploresguser
   SPRING_DATASOURCE_PASSWORD=your_secure_password
   
   # JWT Configuration
   JWT_SECRET_KEY=your_jwt_secret_key_min_256_bits
   JWT_EXPIRATION=86400000
   JWT_REFRESH_EXPIRATION=604800000
   
   # OAuth2 Configuration
   OAUTH2_JWT_ISSUER_URI=https://accounts.google.com
   OAUTH2_JWT_AUDIENCES=your_client_id
   
   # CORS Configuration
   CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:8082
   ```

   > ⚠️ **Security Note**: Never commit `.env` files to version control!

### Running Locally

#### Option 1: Using Maven (Development)

1. **Start PostgreSQL** (if not using Docker):
   ```bash
   # On Windows (if PostgreSQL is installed)
   pg_ctl -D "C:\Program Files\PostgreSQL\15\data" start
   ```

2. **Run the application**:
   ```bash
   ./mvnw spring-boot:run
   ```

3. **Access the application**:
   - Application: http://localhost:8080
   - Swagger UI: http://localhost:8080/swagger-ui.html
   - Health Check: http://localhost:8080/actuator/health

#### Option 2: Build and Run JAR

```bash
# Build the application
./mvnw clean package -DskipTests

# Run the JAR
java -jar target/fleet-service-0.0.1-SNAPSHOT.jar
```

### Running with Docker

#### Full Stack (Application + Database)

```bash
# Create external network (first time only)
docker network create exploresg-net

# Start all services
docker-compose up -d

# View logs
docker-compose logs -f backend-fleet-dev

# Stop services
docker-compose down
```

#### Database Only

```bash
docker-compose up -d fleet-db
```

**Service Endpoints with Docker:**
- Application: http://localhost:8081 (mapped from container port 8080)
- Database: localhost:5433 (mapped from container port 5432)

---

## 📚 API Documentation

### Interactive API Documentation

Once the application is running, access the Swagger UI:

🔗 **http://localhost:8080/swagger-ui.html**

### Key API Endpoints

#### Public Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/v1/fleet/models` | List all available car models |
| `GET` | `/api/v1/fleet/operators/{operatorId}/models` | Get models by operator |
| `GET` | `/api/v1/fleet/models/{modelId}/availability-count` | Check availability |

#### Customer Endpoints (Authenticated)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/v1/fleet/reservations/temporary` | Create temporary reservation |
| `POST` | `/api/v1/fleet/reservations/{id}/confirm` | Confirm reservation after payment |
| `DELETE` | `/api/v1/fleet/reservations/{id}` | Cancel reservation |
| `GET` | `/api/v1/fleet/reservations/{id}` | Get reservation details |

#### Fleet Manager Endpoints (Role: FLEET_MANAGER)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/v1/fleet/operators/fleet` | Get my fleet models |
| `GET` | `/api/v1/fleet/operators/fleet/all` | Get all my vehicles |
| `GET` | `/api/v1/fleet/operators/fleet/all/paginated` | Search vehicles with pagination |
| `GET` | `/api/v1/fleet/operators/dashboard` | Fleet dashboard statistics |
| `PATCH` | `/api/v1/fleet/operators/fleet/{id}/status` | Update vehicle status |

#### Admin Endpoints (Role: ADMIN)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/v1/fleet/models` | Create new car model |
| `GET` | `/api/v1/fleet/models/all` | View all car models |

### Example API Requests

#### Create Temporary Reservation
```bash
POST /api/v1/fleet/reservations/temporary
Content-Type: application/json

{
  "modelPublicId": "550e8400-e29b-41d4-a716-446655440000",
  "bookingId": "booking-12345",
  "startDate": "2025-01-01T10:00:00",
  "endDate": "2025-01-05T10:00:00"
}
```

#### Confirm Reservation
```bash
POST /api/v1/fleet/reservations/{reservationId}/confirm
Content-Type: application/json

{
  "paymentReference": "PAY-67890"
}
```

---

## 🔐 Security

### Authentication & Authorization

- **OAuth2 Resource Server**: Validates JWT tokens from Google OAuth2
- **Custom JWT Support**: Internal JWT generation for service-to-service communication
- **Role-Based Access Control**: Three roles supported:
  - `ADMIN`: Full system access
  - `FLEET_MANAGER`: Manage own fleet
  - `CUSTOMER`: Make bookings

### JWT Token Structure

```json
{
  "userId": "uuid-string",
  "email": "user@example.com",
  "roles": ["CUSTOMER"],
  "iss": "https://accounts.google.com",
  "exp": 1735689600
}
```

### Security Configuration

The service validates:
- JWT signature
- Token expiration
- Issuer URI
- Audience claims
- Role-based endpoint access

---

## 💾 Database

### Schema Overview

**Main Tables:**
- `car_models` - Master catalog of car models
- `fleet_vehicles` - Individual vehicles owned by operators
- `reservations` - Booking reservations with status tracking

**Key Relationships:**
- `car_models` 1:N `fleet_vehicles`
- `fleet_vehicles` 1:N `reservations`

### Running Migrations

The application uses **Hibernate DDL Auto** with `update` mode:
```properties
spring.jpa.hibernate.ddl-auto=update
```

For production, consider using **Flyway** or **Liquibase** for versioned migrations.

### Database Connection

**Local Development:**
```
Host: localhost
Port: 5433
Database: exploresg-fleet-service-db
Username: exploresguser
Password: (from .env)
```

**Docker Environment:**
```
Host: fleet-db (container name)
Port: 5432 (internal)
```

---

## 🧪 Testing

### Test Structure

```
src/test/java/
├── controller/
│   ├── FleetControllerUnitTest.java
│   └── FleetControllerIntegrationTest.java
├── service/
│   ├── FleetServiceUnitTest.java
│   └── FleetServiceIntegrationTest.java
└── ExploresgFleetServiceApplicationTests.java
```

### Running Tests

```bash
# Run all tests
./mvnw test

# Run integration tests
./mvnw verify -P integration-tests

# Run with coverage
./mvnw clean verify -P ci

# View coverage report
open target/site/jacoco/index.html
```

### Test Profiles

| Profile | Purpose | Command |
|---------|---------|---------|
| `default` | Unit tests only | `./mvnw test` |
| `integration-tests` | Integration tests | `./mvnw verify -P integration-tests` |
| `ci` | Full CI pipeline | `./mvnw verify -P ci` |

### Coverage Reports

JaCoCo coverage reports are generated in:
- `target/site/jacoco/index.html`

---

## 🚢 Deployment

### Docker Deployment

**Build Docker Image:**
```bash
docker build -t exploresg-fleet-service:latest .
```

**Run Container:**
```bash
docker run -d \
  -p 8080:8080 \
  --env-file .env \
  --name fleet-service \
  exploresg-fleet-service:latest
```

### Kubernetes Deployment

Deployment manifests are available in `kubernetes/`:

```bash
# Apply deployments
kubectl apply -f kubernetes/deployment.yaml

# Apply ingress
kubectl apply -f kubernetes/ingress.yaml

# Check status
kubectl get pods -l app=fleet-service
kubectl logs -f deployment/fleet-service
```

### Environment Profiles

| Profile | Purpose | Activation |
|---------|---------|------------|
| `dev` | Local development | `application-dev.properties` |
| `staging` | Pre-production testing | `application-staging.properties` |
| `prod` | Production deployment | `application-prod.properties` |

**Activate Profile:**
```bash
java -jar fleet-service.jar --spring.profiles.active=prod
```

---

## 📊 Monitoring & Observability

### Health Checks

**Endpoints:**
- `/actuator/health` - Application health status
- `/actuator/info` - Application information
- `/actuator/prometheus` - Prometheus metrics

**Health Check Example:**
```bash
curl http://localhost:8080/actuator/health
```

**Response:**
```json
{
  "status": "UP",
  "components": {
    "db": {"status": "UP"},
    "diskSpace": {"status": "UP"}
  }
}
```

### Logging

**Structured JSON Logging** (Production):
```json
{
  "timestamp": "2025-01-01T10:00:00.000Z",
  "level": "INFO",
  "correlationId": "abc-123-def",
  "logger": "com.exploresg.fleetservice.controller.FleetController",
  "message": "createCarModel completed",
  "context": {
    "id": "uuid",
    "operation": "createCarModel"
  }
}
```

**Console Logging** (Development):
```
2025-01-01 10:00:00 [abc-123-def] - createCarModel completed
```

### Metrics

**Prometheus Metrics Available:**
- HTTP request durations
- JVM memory usage
- Database connection pool stats
- Custom business metrics

**Scrape Config:**
```yaml
scrape_configs:
  - job_name: 'fleet-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['localhost:8080']
```

---

## 📁 Project Structure

```
exploresg-fleet-service/
├── src/
│   ├── main/
│   │   ├── java/com/exploresg/fleetservice/
│   │   │   ├── config/              # Security, CORS, OpenAPI configs
│   │   │   ├── constants/           # Application constants
│   │   │   ├── controller/          # REST controllers
│   │   │   ├── dto/                 # Data Transfer Objects
│   │   │   ├── exception/           # Custom exceptions & handlers
│   │   │   ├── filter/              # Request filters
│   │   │   ├── interceptor/         # Request interceptors
│   │   │   ├── model/               # JPA entities
│   │   │   ├── repository/          # Spring Data repositories
│   │   │   ├── scheduler/           # Scheduled tasks
│   │   │   ├── service/             # Business logic
│   │   │   └── ExploresgFleetServiceApplication.java
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── application-dev.properties
│   │       ├── application-staging.properties
│   │       ├── application-prod.properties
│   │       └── logback-spring.xml
│   └── test/                        # Test classes
├── docs/                            # Additional documentation
├── kubernetes/                      # K8s manifests
├── scripts/                         # Utility scripts
├── docker-compose.yaml             # Docker Compose config
├── Dockerfile                       # Container image definition
├── pom.xml                          # Maven configuration
└── README.md                        # This file
```

---

## 🤝 Contributing

We welcome contributions! Please follow these guidelines:

1. **Fork the repository**
2. **Create a feature branch**: `git checkout -b feature/my-feature`
3. **Commit your changes**: `git commit -m 'Add some feature'`
4. **Push to the branch**: `git push origin feature/my-feature`
5. **Open a Pull Request**

### Code Standards

- Follow Java naming conventions
- Write unit and integration tests
- Maintain test coverage above 80%
- Update documentation for API changes
- Use meaningful commit messages

---

## 📖 Additional Documentation

Detailed guides are available in the `docs/` directory:

| Document | Description |
|----------|-------------|
| [AUTH-SERVICE-COMPARISON.md](docs/AUTH-SERVICE-COMPARISON.md) | Authentication strategy comparison |
| [BOOKING-INTEGRATION-COMPLETE.md](docs/BOOKING-INTEGRATION-COMPLETE.md) | Booking service integration guide |
| [CLOUD-READINESS-CHECKLIST.md](docs/CLOUD-READINESS-CHECKLIST.md) | Cloud deployment checklist |
| [ENVIRONMENT-SETUP.md](docs/ENVIRONMENT-SETUP.md) | Detailed setup instructions |
| [GITHUB-ACTIONS-SETUP.md](docs/GITHUB-ACTIONS-SETUP.md) | CI/CD pipeline guide |
| [HEALTH-CHECK-GUIDE.md](docs/HEALTH-CHECK-GUIDE.md) | Health check configuration |
| [KUBERNETES-DEPLOYMENT-GUIDE.md](docs/KUBERNETES-DEPLOYMENT-GUIDE.md) | K8s deployment guide |
| [LOGGING-GUIDE.md](docs/LOGGING-GUIDE.md) | Logging best practices |
| [SWAGGER_DOCUMENTATION.md](docs/SWAGGER_DOCUMENTATION.md) | API documentation guide |

---

## 📄 License

This project is part of the ExploreSG Platform.  
© 2025 ExploreSG. All rights reserved.

---

## 📞 Support

For questions or issues:
- **GitHub Issues**: [Create an issue](https://github.com/XploreSG/exploresg-fleet-service/issues)
- **Documentation**: Check the `docs/` folder
- **Wiki**: Visit the project wiki (coming soon)

---

## 🎉 Acknowledgments

Built with ❤️ by the ExploreSG Team using:
- [Spring Boot](https://spring.io/projects/spring-boot)
- [Spring Security](https://spring.io/projects/spring-security)
- [PostgreSQL](https://www.postgresql.org/)
- [Docker](https://www.docker.com/)

---

**Happy Coding! 🚗💨**
