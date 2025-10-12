# ENVIRONMENT-SETUP

This document describes how to set up local and cloud environments for ExploreSG Fleet Service.

## Local development

1. Copy `.env.example` to `.env` and adjust values.
2. Start dependencies with docker-compose:

```bash
cp .env.example .env
docker-compose up -d
```

3. Build and run the service locally (Maven):

```bash
./mvnw clean package -DskipTests
java -jar target/*.jar
```

## Kubernetes

Instructions for deploying to Kubernetes are in `kubernetes/` manifests. Replace image name and secrets with your values.
