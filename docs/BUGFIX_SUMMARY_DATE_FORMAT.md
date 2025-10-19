# Quick Fix Summary: Date Format Issues

## Problem

Booking Service ↔ Fleet Service integration was failing with date parsing errors in **both directions**.

## Root Causes Found

### Issue 1: Missing Jackson Dependency ❌

- `jackson-datatype-jsr310` was missing from `pom.xml`
- Fleet Service couldn't parse incoming dates from Booking Service
- Error: `MismatchedInputException: Expected array or string at startDate`

### Issue 2: Response Format Missing Timezone ❌

- Fleet Service response DTOs output: `"2025-10-18T20:52:47"` (no Z)
- Booking Service expects: `"2025-10-18T20:52:47Z"` (with Z for UTC)
- Error: `Text '2025-10-18T20:52:47' could not be parsed at index 19`

## Fixes Applied ✅

### Fix 1: Added Jackson JSR310 Dependency

**File:** `pom.xml`

```xml
<dependency>
    <groupId>com.fasterxml.jackson.datatype</groupId>
    <artifactId>jackson-datatype-jsr310</artifactId>
</dependency>
```

### Fix 2: Updated Response DTO Date Formats

**Files:**

- `TemporaryReservationResponse.java`
- `ConfirmReservationResponse.java`
- `AvailabilityCheckResponse.java`

**Changed:**

```java
// BEFORE
@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")

// AFTER
@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
```

## Test & Deploy

```powershell
# 1. Rebuild
mvn clean install

# 2. Test locally (optional)
mvn spring-boot:run
.\test-date-deserialization-fix.ps1

# 3. Build Docker image
docker build -t exploresg-fleet-service:latest .

# 4. Push to Azure ACR
az acr build --registry <acr-name> --image exploresg-fleet-service:latest .

# 5. Update Container App
az containerapp update \
  --name dev-exploresg-fleet-service \
  --resource-group <rg-name> \
  --image <acr-name>.azurecr.io/exploresg-fleet-service:latest
```

## Expected Results

### Before Fix

- ❌ Booking → Fleet request: 500 error
- ❌ Fleet → Booking response: Parsing fails with `could not be parsed at index 19`

### After Fix

- ✅ Booking → Fleet request: Dates parsed correctly
- ✅ Fleet → Booking response: `"2025-10-18T20:52:47Z"` format works with `Instant`
- ✅ Full booking flow completes successfully

## Verification

Check Booking Service logs after deployment:

```bash
docker compose logs --tail=50 backend-booking-dev
```

You should see:

- ✅ `Creating temporary reservation for model...` (success)
- ✅ No parsing errors
- ✅ Reservation confirmed

## Files Changed

1. ✅ `pom.xml` - Added jackson-datatype-jsr310
2. ✅ `TemporaryReservationResponse.java` - Added Z to expiresAt format
3. ✅ `ConfirmReservationResponse.java` - Added Z to confirmedAt format
4. ✅ `AvailabilityCheckResponse.java` - Added Z to date formats

---

**Status:** Ready for deployment
**Next:** Rebuild, test locally, then deploy to Azure
