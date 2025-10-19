# ConfigMap Environment Variables for Production

**Important:** If you're using a ConfigMap for environment variables in Kubernetes, ensure these settings are configured correctly to prevent memory leaks.

## Required ConfigMap Settings

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: exploresg-config
  namespace: exploresg
data:
  # ============================================
  # Spring Profile
  # ============================================
  SPRING_PROFILES_ACTIVE: "prod"

  # ============================================
  # Logging Configuration (CRITICAL)
  # ============================================
  LOGGING_LEVEL_ROOT: "INFO"
  LOGGING_LEVEL_COM_EXPLORESG: "INFO" # ✅ Changed from DEBUG
  LOGGING_LEVEL_SPRING_SECURITY: "WARN"

  # ============================================
  # JPA/Hibernate Configuration (CRITICAL)
  # ============================================
  SPRING_JPA_SHOW_SQL: "false" # ✅ Must be false in prod
  SPRING_JPA_HIBERNATE_DDL_AUTO: "validate" # ✅ Never use "update" in prod

  # ============================================
  # Database Connection Pool
  # ============================================
  SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE: "10"
  SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE: "5"

  # ============================================
  # Actuator
  # ============================================
  MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE: "health,info,metrics,prometheus"

  # ============================================
  # Server
  # ============================================
  SERVER_PORT: "8080"
```

## Apply ConfigMap Changes

```bash
# Create or update the ConfigMap
kubectl apply -f k8s/configmap.yaml -n exploresg

# Restart pods to pick up new ConfigMap values
kubectl rollout restart deployment/exploresg-fleet-service -n exploresg

# Monitor the restart
kubectl rollout status deployment/exploresg-fleet-service -n exploresg
```

## Verify ConfigMap Settings

```bash
# Check current ConfigMap
kubectl get configmap exploresg-config -n exploresg -o yaml

# Verify pod environment variables
kubectl exec -it deployment/exploresg-fleet-service -n exploresg -- env | grep -E "LOGGING|JPA|SQL"

# Should show:
# LOGGING_LEVEL_COM_EXPLORESG=INFO (not DEBUG)
# SPRING_JPA_SHOW_SQL=false (not true)
```

## ⚠️ Common Mistakes to Avoid

1. **DEBUG logging in production**

   ```yaml
   LOGGING_LEVEL_COM_EXPLORESG: "DEBUG" # ❌ NEVER in production
   ```

2. **SQL logging enabled**

   ```yaml
   SPRING_JPA_SHOW_SQL: "true" # ❌ Causes massive log volume
   ```

3. **DDL auto-update in production**
   ```yaml
   SPRING_JPA_HIBERNATE_DDL_AUTO: "update" # ❌ Use "validate" or "none"
   ```

## Memory-Safe Production Settings Summary

| Setting                         | Development | Production   | Reason                |
| ------------------------------- | ----------- | ------------ | --------------------- |
| `LOGGING_LEVEL`                 | DEBUG       | INFO         | Reduce log volume     |
| `SPRING_JPA_SHOW_SQL`           | true        | **false**    | Prevent SQL log flood |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | update      | **validate** | Safety + performance  |
| Memory Limit                    | 512Mi       | **768Mi**    | Prevent OOM evictions |
| Scheduler Interval              | 10s         | **300s**     | Reduce overhead       |

---

**Note:** The application already uses `application-prod.properties` which has the correct settings. The ConfigMap values (if used) should align with or override these for production.
