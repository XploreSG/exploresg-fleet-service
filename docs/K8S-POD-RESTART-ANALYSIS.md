# Kubernetes Pod Restart Analysis & Resolution

**Date**: October 19, 2025  
**Service**: exploresg-fleet-service  
**Namespace**: exploresg

## Problem Summary

### Current State

- **Deployment Target**: 1 replica
- **Active Pods**: 2 (from different ReplicaSets)
- **Healthy Pod**: `exploresg-fleet-service-644754d84b-jzkrp` (v1.2.4.2) - Running 1/1
- **Problematic Pod**: `exploresg-fleet-service-785db7bfd9-6zjzk` (v1.2.7.2) - CrashLoopBackOff (5 restarts)

### Cleaned Up Pods

✅ Successfully removed:

- 3 **Evicted** pods (memory pressure)
- 3 **ContainerStatusUnknown** pods
- 1 **Error** pod

## Root Cause Analysis

### 1. Memory Pressure Issues

**Problem**: Pods were evicted due to insufficient memory on nodes

```
Reason: 2 Insufficient memory
```

**Impact**:

- Kubernetes evicted pods to free up memory
- Application couldn't run reliably

**Solution Applied**: Current deployment has proper resource limits:

```yaml
resources:
  requests:
    memory: "512Mi"
  limits:
    memory: "768Mi"
```

### 2. Slow Application Startup

**Problem**: Application takes ~40 seconds to fully start

```
Started ExploresgFleetServiceApplication in 16.385 seconds (process running for 19.188)
```

**Impact**:

- Liveness probe kills pod before fully ready
- Continuous restart cycle on newer version

**Current Configuration**: ✅ Well configured

```yaml
startupProbe:
  initialDelaySeconds: 0
  periodSeconds: 10
  failureThreshold: 30 # Allows up to 5 minutes
```

### 3. Multiple ReplicaSets Running

**Problem**: Two different versions are active simultaneously

| ReplicaSet                           | Image Version | Status     |
| ------------------------------------ | ------------- | ---------- |
| `exploresg-fleet-service-644754d84b` | v1.2.4.2      | ✅ Working |
| `exploresg-fleet-service-785db7bfd9` | v1.2.7.2      | ⚠️ Failing |

**Why this happens**:

- A deployment rollout was initiated to v1.2.7.2
- New version fails liveness checks
- Old ReplicaSet remains active as fallback
- Both are trying to maintain pods

### 4. Liveness Probe Configuration

**Analysis**: Based on your Kubernetes output, the deployed version might have different probe settings than your local `deployment.yaml`

**Potential Issues**:

- `initialDelaySeconds: 60` may be too aggressive for a 40-second startup
- App might be ready earlier but probe kills it during initialization
- Version v1.2.7.2 might have additional startup requirements

## Immediate Actions Required

### Option 1: Quick Fix - Use Stable Version

Delete the problematic pod and rollback to stable version:

```bash
# 1. Delete the failing pod
kubectl delete pod exploresg-fleet-service-785db7bfd9-6zjzk -n exploresg

# 2. Check deployment rollout status
kubectl rollout status deployment/exploresg-fleet-service -n exploresg

# 3. If needed, rollback to previous version
kubectl rollout undo deployment/exploresg-fleet-service -n exploresg

# 4. Scale down old ReplicaSets
kubectl scale rs exploresg-fleet-service-785db7bfd9 --replicas=0 -n exploresg
```

### Option 2: Fix v1.2.7.2 and Re-deploy

If v1.2.7.2 contains important fixes, investigate and fix:

```bash
# 1. Check what changed in v1.2.7.2
kubectl get deployment exploresg-fleet-service -n exploresg -o yaml > current-deployment.yaml

# 2. Compare probe configurations
kubectl describe deployment exploresg-fleet-service -n exploresg | grep -A 20 "Liveness"

# 3. Get logs from failed pod
kubectl logs exploresg-fleet-service-785db7bfd9-6zjzk -n exploresg --previous --tail=200 > v1.2.7.2-failure.log

# 4. Check for memory/CPU issues
kubectl top pod exploresg-fleet-service-785db7bfd9-6zjzk -n exploresg
```

## Long-Term Solutions

### 1. Optimize Application Startup Time

**Current**: ~40 seconds  
**Target**: <30 seconds

**Actions**:

- Profile application startup
- Lazy-load non-critical components
- Optimize database connection pool initialization
- Consider using Spring Boot's lazy initialization:
  ```properties
  spring.main.lazy-initialization=true
  ```

### 2. Improve Probe Configuration

Update `deployment.yaml` to be more resilient:

```yaml
startupProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 10 # Give app time to initialize JVM
  periodSeconds: 10 # Check every 10 seconds
  timeoutSeconds: 5 # Allow 5 seconds per check
  failureThreshold: 30 # Max 5 minutes (10s × 30 = 300s)

livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 0 # Startup probe handles initial delay
  periodSeconds: 10
  timeoutSeconds: 5
  failureThreshold: 3 # Restart after 3 consecutive failures

readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 0
  periodSeconds: 5 # Check more frequently
  timeoutSeconds: 3
  failureThreshold: 3
  successThreshold: 1
```

### 3. Increase Memory Resources

If memory pressure continues:

```yaml
resources:
  requests:
    cpu: "500m" # Increase from 250m
    memory: "768Mi" # Increase from 512Mi
  limits:
    cpu: "1000m" # Increase from 500m
    memory: "1Gi" # Increase from 768Mi
```

### 4. Add Pod Disruption Budget

Prevent too many pods from being down during updates:

```yaml
apiVersion: policy/v1
kind: PodDisruptionBudget
metadata:
  name: exploresg-fleet-service-pdb
  namespace: exploresg
spec:
  minAvailable: 1
  selector:
    matchLabels:
      app: exploresg-fleet-service
```

### 5. Implement Graceful Shutdown

Ensure proper cleanup on pod termination:

```properties
# application.properties
server.shutdown=graceful
spring.lifecycle.timeout-per-shutdown-phase=30s
```

## Investigation Checklist

### Before Proceeding

- [ ] Check which version should be running: `kubectl get deployment exploresg-fleet-service -n exploresg -o jsonpath='{.spec.template.spec.containers[0].image}'`
- [ ] Verify all ReplicaSets: `kubectl get rs -n exploresg | grep fleet-service`
- [ ] Check deployment rollout history: `kubectl rollout history deployment/exploresg-fleet-service -n exploresg`
- [ ] Review recent changes between v1.2.4.2 and v1.2.7.2
- [ ] Check node resources: `kubectl top nodes`
- [ ] Verify namespace resource quotas: `kubectl describe resourcequota -n exploresg`

### During Resolution

- [ ] Monitor pod events: `kubectl get events -n exploresg --sort-by='.lastTimestamp' | grep fleet`
- [ ] Watch pod status: `kubectl get pods -n exploresg -w | grep fleet`
- [ ] Collect logs from both versions for comparison
- [ ] Test health endpoints directly: `kubectl port-forward pod/exploresg-fleet-service-xxx -n exploresg 8080:8080`

## Monitoring & Alerts

### Set up alerts for:

1. **Pod restarts** > 3 in 10 minutes
2. **Memory usage** > 80% of limit
3. **CPU throttling**
4. **Liveness probe failures**
5. **Deployment rollout failures**

### Commands to monitor:

```bash
# Watch pod status
kubectl get pods -n exploresg -w | grep fleet

# Monitor events
kubectl get events -n exploresg --watch --field-selector involvedObject.name=exploresg-fleet-service

# Check resource usage
kubectl top pods -n exploresg | grep fleet

# View deployment rollout status
kubectl rollout status deployment/exploresg-fleet-service -n exploresg --watch
```

## Recommended Next Steps

1. **Immediate** (Today):

   - Delete the failing pod or rollback to v1.2.4.2
   - Ensure only target replica count is running
   - Monitor for 1 hour to confirm stability

2. **Short-term** (This Week):

   - Investigate what changed in v1.2.7.2
   - Test v1.2.7.2 in staging with proper probe configuration
   - Review and optimize application startup time
   - Implement resource monitoring

3. **Long-term** (This Month):
   - Set up proper CI/CD with canary deployments
   - Implement comprehensive health checks
   - Add application performance monitoring (APM)
   - Document deployment procedures

## Related Documentation

- [Health Check Guide](./HEALTH-CHECK-GUIDE.md)
- [Kubernetes Deployment Guide](./KUBERNETES-DEPLOYMENT-GUIDE.md)
- [Memory Leak Fix](./MEMORY-LEAK-FIX.md)

## References

- [Kubernetes Liveness/Readiness Probes](https://kubernetes.io/docs/tasks/configure-pod-container/configure-liveness-readiness-startup-probes/)
- [Spring Boot Actuator Health](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html#actuator.endpoints.health)
