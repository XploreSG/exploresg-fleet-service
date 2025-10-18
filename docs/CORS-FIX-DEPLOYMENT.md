# CORS Configuration Fix - Deployment Guide

## Problem

Frontend at `https://www.xplore.town` was blocked by CORS when calling `https://api.xplore.town/auth/api/v1/auth/google`.

Error:

```
Access to XMLHttpRequest at 'https://api.xplore.town/auth/api/v1/auth/google' from origin 'https://www.xplore.town'
has been blocked by CORS policy: Response to preflight request doesn't pass access control check:
No 'Access-Control-Allow-Origin' header is present on the requested resource.
```

## Root Cause

The production environment variables were not configured with the correct CORS origins. The `.env.production` file had placeholder values.

## Changes Made

### 1. Updated `.env.production`

```bash
# BEFORE
CORS_ALLOWED_ORIGINS=https://your-production-domain.com

# AFTER
CORS_ALLOWED_ORIGINS=https://www.xplore.town,https://xplore.town
```

### 2. Enhanced `SecurityConfig.java`

Added explicit exposed headers and preflight cache:

- `setExposedHeaders(List.of("Authorization", "Content-Type"))`
- `setMaxAge(3600L)` - Cache preflight requests for 1 hour

## Deployment Steps

### Option 1: Deploy to Cloud (Recommended)

#### If using Kubernetes:

1. Update your ConfigMap or Secret with production environment variables:

```bash
kubectl create secret generic auth-service-env \
  --from-literal=CORS_ALLOWED_ORIGINS="https://www.xplore.town,https://xplore.town" \
  --from-literal=SPRING_PROFILES_ACTIVE=prod \
  --dry-run=client -o yaml | kubectl apply -f -
```

2. Rebuild and deploy:

```bash
# Build the Docker image
docker build -t exploresg-auth-service:latest .

# Push to registry
docker push your-registry/exploresg-auth-service:latest

# Apply Kubernetes deployment
kubectl apply -f kubernetes/deployment.yaml

# Restart the pods to pick up new config
kubectl rollout restart deployment/exploresg-auth-service
```

#### If using Docker Compose:

1. Ensure `.env.production` is loaded:

```bash
docker-compose --env-file .env.production up -d --build
```

#### If using Cloud Provider (AWS/Azure/GCP):

1. Update environment variables in your cloud service:
   - **AWS ECS/Fargate**: Update Task Definition
   - **Azure App Service**: Update Application Settings
   - **GCP Cloud Run**: Update environment variables
2. Set:

```
CORS_ALLOWED_ORIGINS=https://www.xplore.town,https://xplore.town
SPRING_PROFILES_ACTIVE=prod
```

### Option 2: Quick Test Locally

1. Start with production profile:

```bash
# Windows PowerShell
$env:CORS_ALLOWED_ORIGINS="https://www.xplore.town,https://xplore.town"
$env:SPRING_PROFILES_ACTIVE="prod"
./mvnw spring-boot:run
```

2. Or update `.env` temporarily for testing:

```bash
CORS_ALLOWED_ORIGINS=https://www.xplore.town,https://xplore.town
```

## Verification Steps

### 1. Check Backend Logs

After deployment, check if CORS headers are being sent:

```bash
# Kubernetes
kubectl logs -f deployment/exploresg-auth-service

# Docker
docker logs -f exploresg-auth-service
```

### 2. Test CORS with curl

```bash
curl -X OPTIONS https://api.xplore.town/auth/api/v1/auth/google \
  -H "Origin: https://www.xplore.town" \
  -H "Access-Control-Request-Method: POST" \
  -H "Access-Control-Request-Headers: Content-Type" \
  -v
```

Expected response headers:

```
Access-Control-Allow-Origin: https://www.xplore.town
Access-Control-Allow-Methods: GET,POST,PUT,DELETE,OPTIONS
Access-Control-Allow-Headers: *
Access-Control-Allow-Credentials: true
```

### 3. Test from Frontend

Open browser console at `https://www.xplore.town` and try the Google login again.

## Current Configuration

### application.properties

```properties
cors.allowed-origins=${CORS_ALLOWED_ORIGINS:http://localhost:3000}
cors.allowed-methods=${CORS_ALLOWED_METHODS:GET,POST,PUT,DELETE,OPTIONS}
cors.allowed-headers=${CORS_ALLOWED_HEADERS:*}
cors.allow-credentials=${CORS_ALLOW_CREDENTIALS:true}
```

### SecurityConfig.java

```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
    configuration.setAllowedMethods(Arrays.asList(allowedMethods.split(",")));
    configuration.setAllowedHeaders(Arrays.asList(allowedHeaders.split(",")));
    configuration.setAllowCredentials(allowCredentials);
    configuration.setExposedHeaders(List.of("Authorization", "Content-Type"));
    configuration.setMaxAge(3600L); // Cache preflight for 1 hour
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
}
```

## Important Notes

1. **Environment Variables**: Ensure your production deployment uses the correct environment variables
2. **HTTPS Only**: Both frontend and backend are using HTTPS, which is correct
3. **Credentials**: `allowCredentials=true` is set, which is needed for cookies/auth headers
4. **Wildcard Origins**: Never use `*` for origins when `allowCredentials=true` - it won't work

## Troubleshooting

### Issue: Still getting CORS errors

- Verify the environment variable is actually set in production
- Check if there's a reverse proxy (nginx, CloudFlare, etc.) stripping CORS headers
- Ensure the backend is actually restarted/redeployed

### Issue: Works locally but not in production

- Check if your cloud provider has additional CORS configuration
- Verify DNS is pointing to the correct backend
- Check if there's a CDN or WAF that might be blocking

### Issue: Preflight (OPTIONS) request failing

- Ensure OPTIONS method is allowed in CORS configuration ✅ (already set)
- Check if there's authentication required for OPTIONS requests (should be permitAll)

## Next Steps

1. ✅ Update `.env.production` with correct CORS origins
2. ✅ Update `SecurityConfig.java` with exposed headers
3. 🔄 Build and deploy to production
4. 🔄 Test from frontend
5. 🔄 Monitor logs for any issues

## Related Files

- `.env.production` - Production environment variables
- `src/main/java/com/exploresg/authservice/config/SecurityConfig.java` - CORS configuration
- `src/main/resources/application.properties` - CORS property mappings
