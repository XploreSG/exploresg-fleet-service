# ⚡ Quick Start Guide

Get the ExploreSG Fleet Service up and running in 5 minutes!

---

## 🚀 Quick Setup (Docker - Recommended)

The fastest way to get started is using Docker Compose.

### 1. Prerequisites

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) installed
- [Git](https://git-scm.com/) installed

### 2. Clone Repository

```bash
git clone https://github.com/XploreSG/exploresg-fleet-service.git
cd exploresg-fleet-service
```

### 3. Create Environment File

Create a `.env` file in the project root:

```bash
# Database
POSTGRES_DB=exploresg-fleet-service-db
POSTGRES_USER=exploresguser
POSTGRES_PASSWORD=exploresgpass

SPRING_DATASOURCE_URL=jdbc:postgresql://fleet-db:5432/exploresg-fleet-service-db
SPRING_DATASOURCE_USERNAME=exploresguser
SPRING_DATASOURCE_PASSWORD=exploresgpass

# JWT (use strong keys in production!)
JWT_SECRET_KEY=your-super-secret-jwt-key-change-this-in-production-min-256-bits
JWT_EXPIRATION=86400000
JWT_REFRESH_EXPIRATION=604800000

# OAuth2
OAUTH2_JWT_ISSUER_URI=https://accounts.google.com
OAUTH2_JWT_AUDIENCES=your-google-client-id

# CORS
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:8082
```

### 4. Start Services

```bash
# Create network (first time only)
docker network create exploresg-net

# Start everything
docker-compose up -d

# Check logs
docker-compose logs -f backend-fleet-dev
```

### 5. Verify Installation

Open your browser and visit:

✅ **Application Health Check**  
http://localhost:8081/actuator/health

You should see:

```json
{ "status": "UP" }
```

✅ **Swagger UI (API Documentation)**  
http://localhost:8081/swagger-ui.html

✅ **Public API Endpoint**  
http://localhost:8081/api/v1/fleet/models

---

## 💻 Local Development Setup (Without Docker)

### 1. Prerequisites

- Java 17 or higher
- Maven 3.8+
- PostgreSQL 15

### 2. Install PostgreSQL

**Windows:**
Download from [PostgreSQL.org](https://www.postgresql.org/download/windows/)

**Mac:**

```bash
brew install postgresql@15
brew services start postgresql@15
```

**Linux:**

```bash
sudo apt-get install postgresql-15
```

### 3. Create Database

```bash
# Login to PostgreSQL
psql -U postgres

# Create database and user
CREATE DATABASE exploresg_fleet_service_db;
CREATE USER exploresguser WITH PASSWORD 'exploresgpass';
GRANT ALL PRIVILEGES ON DATABASE exploresg_fleet_service_db TO exploresguser;
\q
```

### 4. Clone & Configure

```bash
git clone https://github.com/XploreSG/exploresg-fleet-service.git
cd exploresg-fleet-service
```

Create `.env` file:

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/exploresg_fleet_service_db
SPRING_DATASOURCE_USERNAME=exploresguser
SPRING_DATASOURCE_PASSWORD=exploresgpass

JWT_SECRET_KEY=your-super-secret-jwt-key-change-this-in-production-min-256-bits
JWT_EXPIRATION=86400000

OAUTH2_JWT_ISSUER_URI=https://accounts.google.com
OAUTH2_JWT_AUDIENCES=your-google-client-id
```

### 5. Run Application

```bash
# Using Maven wrapper
./mvnw spring-boot:run

# Or build and run JAR
./mvnw clean package -DskipTests
java -jar target/fleet-service-0.0.1-SNAPSHOT.jar
```

### 6. Verify

Visit http://localhost:8080/swagger-ui.html

---

## 🧪 Test the API

### Using cURL

**1. Get Available Car Models (Public endpoint)**

```bash
curl http://localhost:8081/api/v1/fleet/models
```

**2. Check Health**

```bash
curl http://localhost:8081/actuator/health
```

**3. Check Availability for a Model**

```bash
curl "http://localhost:8081/api/v1/fleet/models/YOUR-MODEL-ID/availability-count?startDate=2025-01-01T10:00:00&endDate=2025-01-05T10:00:00"
```

### Using Swagger UI

1. Go to http://localhost:8081/swagger-ui.html
2. Expand any endpoint
3. Click "Try it out"
4. Fill in parameters
5. Click "Execute"

---

## 🔐 Authentication Setup

### For Testing Authenticated Endpoints

You'll need a JWT token. For development, you can:

**Option 1: Use Swagger UI**

1. Click the "Authorize" button
2. Enter: `Bearer YOUR_JWT_TOKEN`
3. Try authenticated endpoints

**Option 2: Use cURL with Token**

```bash
curl -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  http://localhost:8081/api/v1/fleet/operators/fleet
```

### Getting a Test JWT Token

For local testing, you can generate a token using online tools:

- [JWT.io](https://jwt.io/) - Create and decode JWT tokens

Sample payload for testing:

```json
{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "email": "test@example.com",
  "roles": ["CUSTOMER"],
  "iss": "https://accounts.google.com",
  "exp": 1735689600
}
```

---

## 📊 Next Steps

### 1. Explore the API

- Visit Swagger UI: http://localhost:8081/swagger-ui.html
- Check out the [API Reference](docs/API-REFERENCE.md)

### 2. Run Tests

```bash
# Run all tests
./mvnw test

# Run with coverage
./mvnw clean verify -P ci

# View coverage report
open target/site/jacoco/index.html
```

### 3. Read Documentation

- [Full README](README.md) - Complete documentation
- [Contributing Guide](CONTRIBUTING.md) - How to contribute
- [API Reference](docs/API-REFERENCE.md) - Detailed API docs
- [Deployment Guide](docs/KUBERNETES-DEPLOYMENT-GUIDE.md) - Production deployment

### 4. Set Up Development Environment

- Configure your IDE (IntelliJ IDEA recommended)
- Install Lombok plugin
- Configure code formatter (Google Java Style)
- Set up Git hooks for code quality

---

## 🛠 Troubleshooting

### Docker Issues

**Problem: Port already in use**

```
Error: bind: address already in use
```

**Solution:**

```bash
# Stop conflicting services
docker ps
docker stop <container-id>

# Or change ports in docker-compose.yaml
```

**Problem: Database connection failed**

```
org.postgresql.util.PSQLException: Connection refused
```

**Solution:**

```bash
# Check if database is running
docker-compose ps

# Restart database
docker-compose restart fleet-db

# Check logs
docker-compose logs fleet-db
```

### Local Development Issues

**Problem: Java version mismatch**

```
Unsupported class file major version
```

**Solution:**

```bash
# Check Java version (must be 17+)
java -version

# Set JAVA_HOME
export JAVA_HOME=/path/to/java17
```

**Problem: Maven build fails**

```
Cannot resolve dependencies
```

**Solution:**

```bash
# Clean and rebuild
./mvnw clean install -U

# Skip tests if needed
./mvnw clean install -DskipTests
```

**Problem: Database connection timeout**

```
Connection to localhost:5432 refused
```

**Solution:**

- Ensure PostgreSQL is running
- Check connection string in `.env`
- Verify firewall settings

---

## 🔧 Configuration Tips

### Development Profile

Create `application-dev.properties` with:

```properties
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
logging.level.com.exploresg=DEBUG
```

Run with dev profile:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### Disable Security for Testing

In `application-dev.properties`:

```properties
# WARNING: Only for local testing!
spring.security.oauth2.resourceserver.jwt.issuer-uri=
```

---

## 📞 Get Help

### Resources

- 📖 [Full Documentation](README.md)
- 🐛 [Report Issues](https://github.com/XploreSG/exploresg-fleet-service/issues)
- 💬 [GitHub Discussions](https://github.com/XploreSG/exploresg-fleet-service/discussions)

### Common Questions

**Q: How do I seed test data?**  
A: Use the SQL scripts in `data/` folder or create via API endpoints.

**Q: Can I use MySQL instead of PostgreSQL?**  
A: Not recommended. The app uses PostgreSQL-specific features.

**Q: How do I enable HTTPS locally?**  
A: See [Security Configuration Guide](docs/SECURITY-CONFIG-ENHANCEMENT.md)

**Q: How do I deploy to cloud?**  
A: See [Kubernetes Deployment Guide](docs/KUBERNETES-DEPLOYMENT-GUIDE.md)

---

## ✅ Verification Checklist

- [ ] Application starts without errors
- [ ] Health check returns `{"status":"UP"}`
- [ ] Swagger UI is accessible
- [ ] Can fetch car models from API
- [ ] Database is connected
- [ ] Tests pass successfully

---

## 🎉 Success!

You're all set! The Fleet Service is now running.

**What's next?**

- Explore the API endpoints in Swagger UI
- Read the [Contributing Guide](CONTRIBUTING.md) to start developing
- Check out the [API Reference](docs/API-REFERENCE.md) for detailed documentation

---

**Happy Coding! 🚗💨**
