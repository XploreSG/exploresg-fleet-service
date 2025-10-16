# Kubernetes Deployment Guide - Fleet Service

## Overview

This guide covers deploying the Fleet Service to Kubernetes with proper health checks and CORS configuration for production at `api.xplore.town/fleet`.

## Prerequisites

- Kubernetes cluster configured
- `kubectl` CLI installed and configured
- Docker image built and pushed to registry
- SSL/TLS certificate for `api.xplore.town`

## Architecture

```
https://www.xplore.town (Frontend)
           ↓
https://api.xplore.town/fleet (Ingress)
           ↓
exploresg-fleet-service (Service)
           ↓
exploresg-fleet-service pods (Deployment)
```

## Configuration Files

### 1. Deployment Configuration

**File:** `kubernetes/deployment.yaml`

Key features:

- **Health Probes:** Startup, Liveness, and Readiness probes
- **Resource Limits:** CPU and memory constraints
- **ConfigMap & Secrets:** Environment variable injection

### 2. Ingress Configuration

**File:** `kubernetes/ingress.yaml`

Key features:

- **Host:** `api.xplore.town`
- **Path:** `/fleet` routes to the service
- **CORS:** Configured for `https://www.xplore.town` and `https://xplore.town`
- **TLS:** SSL/TLS termination
- **Path Rewriting:** Removes `/fleet` prefix before forwarding to backend

## Deployment Steps

### Step 1: Create Namespace (if not exists)

```bash
kubectl create namespace exploresg
```

### Step 2: Create ConfigMap

```bash
kubectl create configmap exploresg-config -n exploresg \
  --from-literal=SPRING_PROFILES_ACTIVE=prod \
  --from-literal=SERVER_PORT=8080 \
  --from-literal=LOGGING_LEVEL_ROOT=INFO \
  --from-literal=LOGGING_LEVEL_COM_EXPLORESG=INFO \
  --from-literal=SPRING_JPA_SHOW_SQL=false \
  --from-literal=SPRING_JPA_HIBERNATE_DDL_AUTO=validate \
  --from-literal=BOOKING_RESERVATION_EXPIRY_SECONDS=300 \
  --from-literal=MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,info,metrics,prometheus \
  --from-literal=MANAGEMENT_ENDPOINT_HEALTH_SHOW_DETAILS=when-authorized
```

### Step 3: Create Secrets

```bash
kubectl create secret generic exploresg-secrets -n exploresg \
  --from-literal=SPRING_DATASOURCE_URL="jdbc:postgresql://your-db-host:5432/fleetdb" \
  --from-literal=SPRING_DATASOURCE_USERNAME="your-username" \
  --from-literal=SPRING_DATASOURCE_PASSWORD="your-password" \
  --from-literal=JWT_SECRET_KEY="your-base64-encoded-secret-key" \
  --from-literal=JWT_EXPIRATION=86400000 \
  --from-literal=JWT_REFRESH_EXPIRATION=604800000 \
  --from-literal=OAUTH2_JWT_ISSUER_URI="https://accounts.google.com" \
  --from-literal=OAUTH2_JWT_AUDIENCES="your-client-id" \
  --from-literal=CORS_ALLOWED_ORIGINS="https://www.xplore.town,https://xplore.town"
```

### Step 4: Create TLS Secret

```bash
kubectl create secret tls exploresg-tls -n exploresg \
  --cert=path/to/tls.crt \
  --key=path/to/tls.key
```

Or use cert-manager for automatic certificate management:

```yaml
apiVersion: cert-manager.io/v1
kind: Certificate
metadata:
  name: exploresg-tls
  namespace: exploresg
spec:
  secretName: exploresg-tls
  issuerRef:
    name: letsencrypt-prod
    kind: ClusterIssuer
  dnsNames:
    - api.xplore.town
```

### Step 5: Build and Push Docker Image

```bash
# Build the image
docker build -t your-registry/exploresg-fleet-service:latest .

# Push to registry
docker push your-registry/exploresg-fleet-service:latest
```

### Step 6: Update Deployment Image

Update `kubernetes/deployment.yaml` with your actual registry:

```yaml
image: your-registry/exploresg-fleet-service:latest
```

### Step 7: Apply Kubernetes Resources

```bash
# Apply deployment
kubectl apply -f kubernetes/deployment.yaml -n exploresg

# Apply ingress
kubectl apply -f kubernetes/ingress.yaml -n exploresg
```

### Step 8: Verify Deployment

```bash
# Check pods status
kubectl get pods -n exploresg -l app=exploresg-fleet-service

# Check deployment status
kubectl rollout status deployment/exploresg-fleet-service -n exploresg

# Check services
kubectl get svc -n exploresg

# Check ingress
kubectl get ingress -n exploresg
```

## Health Check Verification

### 1. Port Forward to Test Locally

```bash
kubectl port-forward -n exploresg deployment/exploresg-fleet-service 8080:8080
```

### 2. Test Health Endpoints

```bash
# Overall health
curl http://localhost:8080/actuator/health

# Liveness probe
curl http://localhost:8080/actuator/health/liveness

# Readiness probe
curl http://localhost:8080/actuator/health/readiness
```

### 3. Test from Ingress

```bash
# Health check through ingress
curl https://api.xplore.town/fleet/actuator/health

# Test API endpoint
curl https://api.xplore.town/fleet/api/v1/fleet/models
```

## CORS Verification

### Test CORS with curl

```bash
# Preflight request
curl -X OPTIONS https://api.xplore.town/fleet/api/v1/fleet/models \
  -H "Origin: https://www.xplore.town" \
  -H "Access-Control-Request-Method: GET" \
  -H "Access-Control-Request-Headers: Authorization,Content-Type" \
  -v

# Expected response headers:
# Access-Control-Allow-Origin: https://www.xplore.town
# Access-Control-Allow-Methods: GET,POST,PUT,DELETE,OPTIONS
# Access-Control-Allow-Headers: Authorization,Content-Type
# Access-Control-Allow-Credentials: true
# Access-Control-Max-Age: 3600
```

### Test from Browser Console

Open browser console at `https://www.xplore.town` and run:

```javascript
fetch("https://api.xplore.town/fleet/api/v1/fleet/models", {
  method: "GET",
  headers: {
    "Content-Type": "application/json",
  },
  credentials: "include",
})
  .then((response) => response.json())
  .then((data) => console.log("Success:", data))
  .catch((error) => console.error("Error:", error));
```

## Monitoring and Troubleshooting

### View Logs

```bash
# All pods
kubectl logs -n exploresg -l app=exploresg-fleet-service --tail=100 -f

# Specific pod
kubectl logs -n exploresg <pod-name> --tail=100 -f

# Previous pod (if restarted)
kubectl logs -n exploresg <pod-name> --previous
```

### Describe Resources

```bash
# Pod details
kubectl describe pod -n exploresg <pod-name>

# Deployment details
kubectl describe deployment -n exploresg exploresg-fleet-service

# Service details
kubectl describe svc -n exploresg exploresg-fleet-service

# Ingress details
kubectl describe ingress -n exploresg exploresg-fleet-ingress
```

### Common Issues

#### 1. Pod Not Starting

**Symptoms:** Pod stuck in `Pending`, `CrashLoopBackOff`, or `Error` state

**Check:**

```bash
kubectl describe pod -n exploresg <pod-name>
kubectl logs -n exploresg <pod-name>
```

**Common causes:**

- ConfigMap or Secret not found
- Image pull error
- Resource constraints
- Database connection failure

#### 2. Health Check Failing

**Symptoms:** Pod restarts frequently

**Check:**

```bash
kubectl logs -n exploresg <pod-name> --previous
```

**Solutions:**

- Increase `initialDelaySeconds` if app takes longer to start
- Verify database connectivity
- Check application logs for startup errors

#### 3. CORS Errors

**Symptoms:** Frontend can't access API due to CORS

**Check:**

```bash
# Verify CORS environment variable
kubectl exec -n exploresg <pod-name> -- env | grep CORS

# Test CORS headers
curl -I https://api.xplore.town/fleet/api/v1/fleet/models \
  -H "Origin: https://www.xplore.town"
```

**Solutions:**

- Verify `CORS_ALLOWED_ORIGINS` secret is set correctly
- Check ingress CORS annotations
- Ensure frontend origin matches exactly (including protocol and www)

#### 4. 404 Not Found

**Symptoms:** API endpoints return 404

**Check:**

```bash
# Verify ingress rules
kubectl get ingress -n exploresg exploresg-fleet-ingress -o yaml

# Test directly to pod (bypass ingress)
kubectl port-forward -n exploresg deployment/exploresg-fleet-service 8080:8080
curl http://localhost:8080/api/v1/fleet/models
```

**Solutions:**

- Verify ingress path matches: `/fleet(/|$)(.*)`
- Check rewrite-target annotation
- Ensure service is correctly routing to pods

## Scaling

### Manual Scaling

```bash
# Scale to 3 replicas
kubectl scale deployment/exploresg-fleet-service -n exploresg --replicas=3

# Verify scaling
kubectl get pods -n exploresg -l app=exploresg-fleet-service
```

### Horizontal Pod Autoscaler

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: exploresg-fleet-service-hpa
  namespace: exploresg
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: exploresg-fleet-service
  minReplicas: 2
  maxReplicas: 10
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 70
    - type: Resource
      resource:
        name: memory
        target:
          type: Utilization
          averageUtilization: 80
```

Apply:

```bash
kubectl apply -f hpa.yaml -n exploresg
```

## Rolling Updates

```bash
# Update image
kubectl set image deployment/exploresg-fleet-service \
  exploresg-fleet-service=your-registry/exploresg-fleet-service:v2 \
  -n exploresg

# Monitor rollout
kubectl rollout status deployment/exploresg-fleet-service -n exploresg

# Rollback if needed
kubectl rollout undo deployment/exploresg-fleet-service -n exploresg
```

## Security Best Practices

✅ **Use Secrets** for sensitive data (DB passwords, JWT keys)  
✅ **Enable RBAC** for service accounts  
✅ **Set Resource Limits** to prevent resource exhaustion  
✅ **Use TLS** for all external communication  
✅ **Restrict Ingress** to specific origins  
✅ **Enable Network Policies** to control pod-to-pod communication  
✅ **Regular Security Scans** of container images  
✅ **Keep Dependencies Updated** to patch vulnerabilities

## Production Checklist

- [ ] ConfigMap created with correct values
- [ ] Secrets created with production credentials
- [ ] TLS certificate configured
- [ ] Docker image built and pushed to registry
- [ ] Deployment image reference updated
- [ ] Health probes tested and working
- [ ] CORS tested from frontend domain
- [ ] Database connection verified
- [ ] Ingress routing verified
- [ ] Monitoring and alerts configured
- [ ] Backup strategy in place
- [ ] Disaster recovery plan documented

## Environment Variables Reference

| Variable                     | Description              | Example                                       |
| ---------------------------- | ------------------------ | --------------------------------------------- |
| `SPRING_PROFILES_ACTIVE`     | Active Spring profile    | `prod`                                        |
| `SPRING_DATASOURCE_URL`      | Database connection URL  | `jdbc:postgresql://db:5432/fleetdb`           |
| `SPRING_DATASOURCE_USERNAME` | Database username        | `fleetuser`                                   |
| `SPRING_DATASOURCE_PASSWORD` | Database password        | `<secret>`                                    |
| `JWT_SECRET_KEY`             | JWT signing key (Base64) | `<base64-secret>`                             |
| `JWT_EXPIRATION`             | JWT expiration (ms)      | `86400000` (24h)                              |
| `OAUTH2_JWT_ISSUER_URI`      | OAuth2 issuer            | `https://accounts.google.com`                 |
| `OAUTH2_JWT_AUDIENCES`       | OAuth2 audience          | `your-client-id`                              |
| `CORS_ALLOWED_ORIGINS`       | Allowed CORS origins     | `https://www.xplore.town,https://xplore.town` |

## References

- [Health Check Guide](./HEALTH-CHECK-GUIDE.md)
- [CORS Fix Deployment](./CORS-FIX-DEPLOYMENT.md)
- [Kubernetes Documentation](https://kubernetes.io/docs/)
- [NGINX Ingress Controller](https://kubernetes.github.io/ingress-nginx/)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
