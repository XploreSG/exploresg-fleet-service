# 🚨 CRITICAL: Pod Restart Issue - Root Cause & Fix

**Date**: October 19, 2025  
**Severity**: HIGH  
**Status**: IDENTIFIED - FIX READY  
**Affected Version**: v1.2.7.2

## 🔴 Critical Findings

### Exit Code 143 Analysis

```
Last State:     Terminated
  Reason:       Error
  Exit Code:    143
  Started:      Today at 12:38 PM
  Finished:     Today at 12:38 PM
```

**What Exit Code 143 Means:**

- **143 = 128 + 15 (SIGTERM)**
- The container received a **SIGTERM** signal and was forcefully terminated
- **SIGTERM** is sent by Kubernetes when **liveness probe fails**
- The application is being **killed before it can fully start**

### Timeline of Failure (from your events)

```
12:33 PM - Pod Scheduled
12:33 PM - Image Pulled (1.792s)
12:33 PM - Container Created
12:33 PM - Container Started
12:34 PM - Readiness probe FAILED (connection refused) - 17 times!
12:34 PM - Liveness probe FAILED (connection refused) - 8 times!
12:34 PM - Container KILLED (SIGTERM - exit 143)
12:34 PM - New attempt started
[CYCLE REPEATS 4 MORE TIMES]
```

### The Problem

**The application takes ~40 seconds to start, but Kubernetes kills it before it's ready!**

**Probe Configuration Issue:**

```
Connection refused on port 8080
↓
Application not ready yet
↓
Liveness probe fails (too aggressive)
↓
Kubernetes sends SIGTERM (exit 143)
↓
Container dies before startup completes
↓
Infinite restart loop
```

## 🔍 Root Causes Identified in Your Code

### 1. ⚠️ **MISSING: Graceful Shutdown Configuration**

**Current Issue:** Your application doesn't handle SIGTERM properly

**File:** `src/main/resources/application.properties`  
**Missing Configuration:**

```properties
# Graceful shutdown NOT configured
# Application gets killed immediately on SIGTERM
```

**Impact:** When Kubernetes sends SIGTERM, the app is killed abruptly, leading to:

- Incomplete database transactions
- Lost in-flight requests
- Corrupted state

### 2. ⚠️ **ISSUE: spring.jpa.open-in-view Enabled**

**Current:** Enabled by default (causes memory leaks in production)  
**File:** `application.properties` - Missing explicit setting

### 3. ⚠️ **DOCKERFILE ISSUE: Health Check Start Period Too Short**

**File:** `Dockerfile` line 79-83

```dockerfile
HEALTHCHECK --interval=30s \
            --timeout=5s \
            --start-period=60s \    # ❌ TOO SHORT!
            --retries=3 \
            CMD curl -f http://localhost:8080/actuator/health || exit 1
```

**Problem:**

- Application takes **~40 seconds** to start
- Docker health check `start-period=60s` is borderline
- Kubernetes liveness probe is MORE aggressive than Docker health check
- No grace period for slow startups

### 4. ⚠️ **POTENTIAL ISSUE: Scheduling on Startup**

**File:** `ReservationCleanupScheduler.java` line 54

```java
@Scheduled(fixedDelay = 300000, initialDelay = 30000)
```

**Issue:** Scheduled task starts after 30 seconds, during the critical startup window  
**Impact:** Additional database queries during startup → slower initialization

### 5. ✅ **GOOD: Kubernetes Deployment Has Proper Probes**

Your `kubernetes/deployment.yaml` looks correct with startup probes, BUT Kubernetes might be using different settings from a ConfigMap or deployment that overrides these.

## 🔧 FIXES REQUIRED

### Fix 1: Add Graceful Shutdown (CRITICAL)

**File:** `src/main/resources/application.properties`

Add these lines:

```properties
# ============================================
# Graceful Shutdown Configuration
# ============================================
server.shutdown=graceful
spring.lifecycle.timeout-per-shutdown-phase=30s

# Disable JPA open-in-view (production best practice)
spring.jpa.open-in-view=false
```

**Why this fixes exit 143:**

- Application will complete in-flight requests before shutting down
- Proper cleanup of resources
- Reduces chance of corrupted state on restart

### Fix 2: Optimize Dockerfile Health Check

**File:** `Dockerfile`

Replace lines 79-83:

```dockerfile
# OLD - Too aggressive
HEALTHCHECK --interval=30s \
            --timeout=5s \
            --start-period=60s \
            --retries=3 \
            CMD curl -f http://localhost:8080/actuator/health || exit 1

# NEW - More lenient for slow startups
HEALTHCHECK --interval=30s \
            --timeout=10s \
            --start-period=120s \
            --retries=3 \
            CMD curl -f http://localhost:8080/actuator/health/liveness || exit 1
```

**Changes:**

- `start-period`: 60s → **120s** (2 minutes for startup)
- `timeout`: 5s → **10s** (more generous timeout)
- Endpoint: `/actuator/health` → **/actuator/health/liveness** (Kubernetes-specific)

### Fix 3: Delay Scheduled Tasks Until After Startup

**File:** `src/main/java/com/exploresg/fleetservice/service/ReservationCleanupScheduler.java`

Change line 54:

```java
// OLD - Starts 30s after app launch
@Scheduled(fixedDelay = 300000, initialDelay = 30000)

// NEW - Starts after 2 minutes (after startup stabilizes)
@Scheduled(fixedDelay = 300000, initialDelay = 120000)
```

### Fix 4: Add Production Logging Configuration

**File:** `src/main/resources/application-prod.properties`

Add:

```properties
# Reduce startup logging overhead
logging.level.org.hibernate.SQL=WARN
logging.level.org.springframework.boot=WARN

# Async logging for better performance
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%X{correlationId:-NO_CORRELATION_ID}] %logger{36} - %msg%n
```

### Fix 5: Optimize JVM for Kubernetes

**File:** `Dockerfile` line 91-102

Replace JAVA_OPTS:

```dockerfile
# OLD - May consume too much memory on startup
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:InitialRAMPercentage=50.0 \
               ...

# NEW - Faster startup, more conservative memory
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=70.0 \
               -XX:InitialRAMPercentage=30.0 \
               -XX:+TieredCompilation \
               -XX:TieredStopAtLevel=1 \
               -XX:+UseG1GC \
               -XX:MaxGCPauseMillis=200 \
               -XX:+HeapDumpOnOutOfMemoryError \
               -XX:HeapDumpPath=/tmp/heapdumps/heapdump.hprof \
               -XX:+ExitOnOutOfMemoryError \
               -XX:+UseStringDeduplication \
               -Djava.security.egd=file:/dev/./urandom \
               -Dfile.encoding=UTF-8 \
               -Dsun.net.inetaddr.ttl=60 \
               -Duser.timezone=UTC \
               -Dspring.backgroundpreinitializer.ignore=true"
```

**Key Changes:**

- `InitialRAMPercentage`: 50% → **30%** (faster startup)
- Added `-XX:+TieredCompilation` and `-XX:TieredStopAtLevel=1` (faster JIT)
- Added `-Dspring.backgroundpreinitializer.ignore=true` (skip background init)

## 📋 Implementation Checklist

### Immediate Actions (Do These First)

- [ ] **1. Add graceful shutdown to `application.properties`**

  ```properties
  server.shutdown=graceful
  spring.lifecycle.timeout-per-shutdown-phase=30s
  spring.jpa.open-in-view=false
  ```

- [ ] **2. Update Dockerfile health check**

  - Change `start-period` to 120s
  - Change timeout to 10s
  - Use `/actuator/health/liveness` endpoint

- [ ] **3. Delay scheduled tasks**

  - Change `initialDelay` from 30000 to 120000

- [ ] **4. Rebuild Docker image**

  ```bash
  docker build -t sreerajrone/exploresg-fleet-service:v1.2.7.3 .
  docker push sreerajrone/exploresg-fleet-service:v1.2.7.3
  ```

- [ ] **5. Update Kubernetes deployment**
  ```bash
  kubectl set image deployment/exploresg-fleet-service \
    exploresg-fleet-service=sreerajrone/exploresg-fleet-service:v1.2.7.3 \
    -n exploresg
  ```

### Verify Kubernetes Probe Settings

Run this command to check current probe configuration:

```bash
kubectl get deployment exploresg-fleet-service -n exploresg -o yaml | grep -A 20 "livenessProbe:"
```

**Expected Configuration:**

```yaml
startupProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 10
  periodSeconds: 10
  timeoutSeconds: 5
  failureThreshold: 30 # 30 × 10s = 5 minutes max startup time

livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 0 # Startup probe handles this
  periodSeconds: 10
  timeoutSeconds: 5
  failureThreshold: 3

readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 0
  periodSeconds: 5
  timeoutSeconds: 3
  failureThreshold: 3
```

### Test Locally First

Before deploying to Kubernetes:

```bash
# Build new image
docker build -t exploresg-fleet-service:test .

# Run container and watch startup
docker run -p 8080:8080 --name fleet-test \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e SPRING_DATASOURCE_URL="jdbc:postgresql://host.docker.internal:5433/exploresg-fleet-service-db" \
  -e SPRING_DATASOURCE_USERNAME=exploresguser \
  -e SPRING_DATASOURCE_PASSWORD=exploresgpass \
  -e JWT_SECRET_KEY=test-secret-key-change-in-prod \
  -e OAUTH2_JWT_AUDIENCES=test \
  exploresg-fleet-service:test

# In another terminal, monitor health
watch -n 1 'curl -s http://localhost:8080/actuator/health/liveness | jq'

# Check startup time
docker logs -f fleet-test | grep "Started ExploresgFleetServiceApplication"
```

## 🎯 Expected Results After Fix

### Before (Current Behavior)

```
00:00 - Container starts
00:10 - Liveness probe starts checking
00:20 - Connection refused (app not ready)
00:30 - Connection refused (still starting)
00:40 - App ALMOST ready
00:50 - Liveness probe FAILS (3rd failure)
00:51 - SIGTERM sent (exit 143)
00:52 - Container killed
→ RESTART LOOP
```

### After (Expected Behavior)

```
00:00 - Container starts
00:10 - Startup probe begins (lenient)
00:30 - Still starting (no kill)
00:40 - App ready!
00:41 - Liveness probe passes
00:42 - Readiness probe passes
00:43 - Pod marked READY
→ STABLE
```

## 🔬 Verification Commands

After deploying the fix:

```bash
# Watch pod status
kubectl get pods -n exploresg -w | grep fleet

# Monitor events
kubectl get events -n exploresg --sort-by='.lastTimestamp' | grep fleet

# Check if startup is successful
kubectl logs -f deployment/exploresg-fleet-service -n exploresg

# Verify health endpoints
kubectl port-forward deployment/exploresg-fleet-service 8080:8080 -n exploresg
curl http://localhost:8080/actuator/health/liveness
curl http://localhost:8080/actuator/health/readiness
```

## 📊 Monitoring After Deployment

Watch for these metrics:

1. **Pod Restart Count**: Should be 0
2. **Startup Time**: Should complete within 60 seconds
3. **Memory Usage**: Should stabilize around 512-600 MB
4. **CPU Usage**: Should drop after startup

```bash
# Check restart count
kubectl get pods -n exploresg | grep fleet

# Monitor resources
kubectl top pods -n exploresg | grep fleet

# View detailed pod status
kubectl describe pod <pod-name> -n exploresg
```

## 🚨 If Issues Persist

If the pod still restarts after implementing these fixes:

1. **Check for missing environment variables**

   ```bash
   kubectl get deployment exploresg-fleet-service -n exploresg -o yaml | grep -A 30 "env:"
   ```

2. **Review pod logs for actual errors**

   ```bash
   kubectl logs <pod-name> -n exploresg --previous --tail=200
   ```

3. **Check resource constraints**

   ```bash
   kubectl describe node <node-name> | grep -A 5 "Allocated resources"
   ```

4. **Verify database connectivity**
   ```bash
   kubectl exec -it <pod-name> -n exploresg -- curl http://localhost:8080/actuator/health
   ```

## 📚 Related Documentation

- [Kubernetes Liveness/Readiness Probes](https://kubernetes.io/docs/tasks/configure-pod-container/configure-liveness-readiness-startup-probes/)
- [Spring Boot Graceful Shutdown](https://docs.spring.io/spring-boot/docs/current/reference/html/web.html#web.graceful-shutdown)
- [Docker Health Checks](https://docs.docker.com/engine/reference/builder/#healthcheck)
- [Exit Codes](https://tldp.org/LDP/abs/html/exitcodes.html)

---

## ⚡ Quick Fix Summary

**The pod is being killed (exit 143) because:**

1. App takes 40s to start
2. Liveness probe fails after ~30-50s
3. Kubernetes sends SIGTERM
4. App has no graceful shutdown configured

**Three critical fixes:**

1. Add `server.shutdown=graceful`
2. Increase Docker health check `start-period` to 120s
3. Ensure Kubernetes startup probe allows 5 minutes (already in your deployment.yaml)

**Then rebuild and redeploy v1.2.7.3**
