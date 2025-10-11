# =============================================================================
# Multi-Stage Dockerfile for ExploreSG Fleet Service
# Security Hardened for Production Kubernetes/EKS Deployment
# =============================================================================

# ---- Build Stage ----
FROM maven:3.9.9-eclipse-temurin-17-alpine AS builder

# Security: Run as non-root user during build
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

WORKDIR /app

# Security: Set ownership to non-root user
RUN chown -R appuser:appgroup /app

# Switch to non-root user for build
USER appuser

# Copy dependency definitions first (better layer caching)
COPY --chown=appuser:appgroup pom.xml .

# Download dependencies (cached layer if pom.xml doesn't change)
RUN mvn dependency:go-offline -B

# Copy source code
COPY --chown=appuser:appgroup src ./src

# Build the application
# -DskipTests: Tests should run in CI/CD, not during image build
# -Dmaven.javadoc.skip=true: Skip javadoc generation for faster builds
# -B: Batch mode (non-interactive)
RUN mvn clean package -DskipTests -Dmaven.javadoc.skip=true -B

# ---- Runtime Stage ----
FROM eclipse-temurin:17-jre-alpine

# Metadata labels for image tracking
LABEL maintainer="ExploreSG Platform Team" \
      application="exploresg-fleet-service" \
      version="0.0.1-SNAPSHOT" \
      description="Fleet management microservice for ExploreSG platform" \
      org.opencontainers.image.source="https://github.com/XploreSG/exploresg-fleet-service"

# Security: Install only essential security updates
RUN apk upgrade --no-cache && \
    apk add --no-cache \
    # Required for health checks
    curl \
    # Security: Remove apk cache
    && rm -rf /var/cache/apk/*

# Security: Create non-root user with specific UID/GID
# Using high UID to avoid conflicts with host systems
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

# Create application directory
WORKDIR /app

# Security: Create logs directory with proper permissions
RUN mkdir -p /app/logs /tmp/heapdumps && \
    chown -R appuser:appgroup /app /tmp/heapdumps && \
    chmod 755 /app /tmp/heapdumps

# Copy JAR from builder stage with proper ownership
COPY --from=builder --chown=appuser:appgroup /app/target/*.jar app.jar

# Security: Switch to non-root user
USER appuser

# Expose application port
EXPOSE 8081

# Health check for container orchestration
# This allows Kubernetes to determine if container is healthy
HEALTHCHECK --interval=30s \
            --timeout=5s \
            --start-period=60s \
            --retries=3 \
            CMD curl -f http://localhost:8081/actuator/health || exit 1

# Environment variables with secure defaults
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:InitialRAMPercentage=50.0 \
               -XX:+UseG1GC \
               -XX:MaxGCPauseMillis=200 \
               -XX:+HeapDumpOnOutOfMemoryError \
               -XX:HeapDumpPath=/tmp/heapdumps/heapdump.hprof \
               -XX:+ExitOnOutOfMemoryError \
               -XX:+UseStringDeduplication \
               -Djava.security.egd=file:/dev/./urandom \
               -Dfile.encoding=UTF-8 \
               -Dsun.net.inetaddr.ttl=60 \
               -Duser.timezone=UTC"

# Application entrypoint
# Using exec form with shell wrapper for environment variable expansion
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]