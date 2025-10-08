# Fleet Vehicle Search Enhancement

## Overview

Enhanced the paginated fleet vehicles endpoint to support optional search/filter parameters while maintaining full backward compatibility.

## Endpoint

```
GET /api/v1/fleet/operators/fleet/all/paginated
```

## Authentication

- **Required Role**: `FLEET_MANAGER`
- **Auth Type**: JWT Bearer Token

## Parameters

### Pagination & Sorting (Existing)

| Parameter       | Type   | Required | Default      | Description                      |
| --------------- | ------ | -------- | ------------ | -------------------------------- |
| `page`          | int    | No       | 0            | Page number (0-indexed)          |
| `size`          | int    | No       | 10           | Number of items per page         |
| `sortBy`        | String | No       | licensePlate | Field to sort by                 |
| `sortDirection` | String | No       | asc          | Sort direction (`asc` or `desc`) |

### Search Filters (New - All Optional)

| Parameter      | Type   | Required | Description                 | Match Type                |
| -------------- | ------ | -------- | --------------------------- | ------------------------- |
| `licensePlate` | String | No       | Search by license plate     | Partial, case-insensitive |
| `status`       | Enum   | No       | Filter by vehicle status    | Exact match               |
| `model`        | String | No       | Search by car model name    | Partial, case-insensitive |
| `manufacturer` | String | No       | Search by manufacturer name | Partial, case-insensitive |
| `location`     | String | No       | Search by current location  | Partial, case-insensitive |

### Vehicle Status Enum Values

- `AVAILABLE`
- `BOOKED`
- `UNDER_MAINTENANCE`

## Examples

### 1. Basic Pagination (No Search)

```
GET /api/v1/fleet/operators/fleet/all/paginated?page=0&size=10
```

Returns all vehicles with default pagination.

### 2. Search by License Plate

```
GET /api/v1/fleet/operators/fleet/all/paginated?licensePlate=SBA
```

Returns all vehicles with "SBA" in their license plate (e.g., SBA1234A, SBA5678B).

### 3. Filter by Status

```
GET /api/v1/fleet/operators/fleet/all/paginated?status=AVAILABLE
```

Returns only available vehicles.

### 4. Search by Car Model

```
GET /api/v1/fleet/operators/fleet/all/paginated?model=corolla
```

Returns vehicles where the car model name contains "corolla".

### 5. Combined Search

```
GET /api/v1/fleet/operators/fleet/all/paginated?status=AVAILABLE&manufacturer=toyota&page=0&size=20&sortBy=licensePlate&sortDirection=asc
```

Returns available Toyota vehicles, sorted by license plate ascending, 20 per page.

### 6. Search with Location

```
GET /api/v1/fleet/operators/fleet/all/paginated?location=jurong&status=AVAILABLE
```

Returns available vehicles currently in Jurong area.

## Response Format

```json
{
  "content": [
    {
      "id": "uuid-here",
      "carModel": {
        "id": 1,
        "model": "Corolla Altis",
        "manufacturer": "Toyota",
        ...
      },
      "ownerId": "owner-uuid",
      "dailyPrice": 85.00,
      "licensePlate": "SBA1234A",
      "status": "AVAILABLE",
      "mileageKm": 45000,
      "currentLocation": "Jurong West",
      ...
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10,
    "sort": {
      "sorted": true,
      "unsorted": false
    },
    "offset": 0,
    "paged": true,
    "unpaged": false
  },
  "totalElements": 25,
  "totalPages": 3,
  "last": false,
  "size": 10,
  "number": 0,
  "sort": {
    "sorted": true,
    "unsorted": false
  },
  "numberOfElements": 10,
  "first": true,
  "empty": false
}
```

## Implementation Details

### Architecture

1. **Controller Layer** (`FleetController.java`)

   - Added 5 optional `@RequestParam` parameters
   - Intelligently routes to search or basic pagination based on whether filters are provided

2. **Service Layer** (`CarModelService.java`)

   - New method: `searchFleetVehicles()` with search logic
   - Handles empty string to null conversion for cleaner queries

3. **Repository Layer** (`FleetVehicleRepository.java`)
   - New method: `searchFleetVehicles()` with JPQL query
   - Uses `LIKE` with wildcards for partial matching
   - Case-insensitive search using `LOWER()`

### Key Features

- ✅ **Backward Compatible**: Existing API calls work unchanged
- ✅ **Optional Filters**: All search params are optional
- ✅ **Case-Insensitive**: Text searches ignore case
- ✅ **Partial Matching**: Searches for substrings (contains)
- ✅ **Combinable**: Can use multiple filters together
- ✅ **Maintains Pagination**: Search results are still paginated
- ✅ **Maintains Sorting**: Can sort search results

## Testing Suggestions

### Test Cases to Verify

1. ✅ No search params - returns all vehicles (existing behavior)
2. ✅ Search by each individual field
3. ✅ Combine multiple search filters
4. ✅ Search with pagination (page 2, 3, etc.)
5. ✅ Search with sorting (ascending/descending)
6. ✅ Empty search strings (should be treated as no filter)
7. ✅ Case-insensitive search (search "TOYOTA" finds "Toyota")
8. ✅ Partial match (search "BA" finds "SBA1234A")
9. ✅ No results found (returns empty page)
10. ✅ Status enum validation

## Future Enhancements (Optional)

- Add date range filters (e.g., `availableFrom`, `availableUntil`)
- Add numeric range filters (e.g., `minMileage`, `maxMileage`)
- Add price range filters (e.g., `minPrice`, `maxPrice`)
- Add color filters
- Add full-text search across multiple fields
- Add "starts with" vs "contains" option for text searches

## Notes

- All text searches are **partial matches** (contains), not exact matches
- Empty strings are treated as null (no filter)
- The `status` parameter uses exact enum matching
- Search filters apply **only to the authenticated user's fleet** (owner-scoped)
