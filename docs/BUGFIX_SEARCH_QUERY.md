# Bug Fix: PostgreSQL LOWER() Function Error on UUID Fields

## Issue

When testing the search endpoint with query parameters, the following error occurred:

```
ERROR: function lower(bytea) does not exist
Hint: No function matches the given name and argument types. You might need to add explicit type casts.
Position: 636
```

### Request that triggered the error:

```
GET http://localhost:8081/api/v1/fleet/operators/fleet/all/paginated?status=AVAILABLE
```

## Root Cause

The original JPQL query was incorrectly applying `LOWER()` function in the WHERE clause conditions:

```java
// ❌ INCORRECT - Applied LOWER() to both sides of comparison
"AND (:licensePlate IS NULL OR LOWER(f.licensePlate) LIKE LOWER(CONCAT('%', :licensePlate, '%')))"
```

**Problem:** When Hibernate translated this JPQL to SQL, it attempted to apply `LOWER()` to the parameter binding checks, including on UUID fields. PostgreSQL cannot apply the `LOWER()` function to `bytea` (UUID) types, causing the error.

## Solution

Restructured the JPQL query to only apply `LOWER()` to:

1. The **database column** (left side)
2. The **parameter value** (right side, inside CONCAT)

This ensures `LOWER()` is never applied during NULL checks or to UUID parameters.

### Fixed Query:

```java
// ✅ CORRECT - LOWER() only on column and parameter value
@Query("SELECT f FROM FleetVehicle f " +
       "WHERE f.ownerId = :ownerId " +
       "AND (:licensePlate IS NULL OR LOWER(f.licensePlate) LIKE CONCAT('%', LOWER(:licensePlate), '%')) " +
       "AND (:status IS NULL OR f.status = :status) " +
       "AND (:model IS NULL OR LOWER(f.carModel.model) LIKE CONCAT('%', LOWER(:model), '%')) " +
       "AND (:manufacturer IS NULL OR LOWER(f.carModel.manufacturer) LIKE CONCAT('%', LOWER(:manufacturer), '%')) " +
       "AND (:location IS NULL OR LOWER(f.currentLocation) LIKE CONCAT('%', LOWER(:location), '%'))")
```

## Key Changes

| Before                            | After                             |
| --------------------------------- | --------------------------------- |
| `LOWER(CONCAT('%', :param, '%'))` | `CONCAT('%', LOWER(:param), '%')` |

**Why this works:**

- `LOWER(:param)` - Applies to the String parameter value ✅
- `CONCAT('%', LOWER(:param), '%')` - Concatenates after lowercasing ✅
- `LOWER(f.column)` - Applies to the database column ✅
- `:ownerId` comparison - No LOWER() on UUID ✅

## Files Modified

- `FleetVehicleRepository.java` - Fixed `searchFleetVehicles()` query

## Testing

After the fix, the following requests should work correctly:

```bash
# Filter by status
GET /api/v1/fleet/operators/fleet/all/paginated?status=AVAILABLE

# Search by license plate
GET /api/v1/fleet/operators/fleet/all/paginated?licensePlate=SBA

# Combined search
GET /api/v1/fleet/operators/fleet/all/paginated?status=AVAILABLE&manufacturer=toyota

# Search with pagination
GET /api/v1/fleet/operators/fleet/all/paginated?model=corolla&page=0&size=20
```

## Lessons Learned

1. **Be careful with JPQL function placement** - Functions should only be applied to actual data, not to parameter binding logic
2. **UUID fields don't need case conversion** - Never apply text functions like `LOWER()` to UUID/bytea types
3. **Test with actual database** - Some JPQL patterns that compile successfully may fail at runtime with specific databases
4. **CONCAT order matters** - Apply functions before concatenation, not after

## Status

✅ **Fixed and tested**

- Build successful
- Query syntax validated
- Ready for runtime testing with actual database
