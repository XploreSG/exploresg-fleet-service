# 📘 ExploreSG Fleet Service - API Reference

Complete REST API documentation for the ExploreSG Fleet Service.

## Base URL

**Local Development:**

```
http://localhost:8080/api/v1/fleet
```

**Docker Development:**

```
http://localhost:8081/api/v1/fleet
```

**Production:**

```
https://api.exploresg.com/api/v1/fleet
```

---

## Table of Contents

1. [Authentication](#authentication)
2. [Public Endpoints](#public-endpoints)
3. [Customer Endpoints](#customer-endpoints)
4. [Fleet Manager Endpoints](#fleet-manager-endpoints)
5. [Admin Endpoints](#admin-endpoints)
6. [Error Responses](#error-responses)
7. [Rate Limiting](#rate-limiting)

---

## Authentication

### JWT Token Format

All authenticated endpoints require a JWT token in the Authorization header:

```http
Authorization: Bearer <your-jwt-token>
```

### Token Structure

```json
{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "email": "user@example.com",
  "roles": ["CUSTOMER"],
  "iss": "https://accounts.google.com",
  "aud": "your-client-id",
  "exp": 1735689600,
  "iat": 1735603200
}
```

### Supported Roles

| Role            | Description          | Access Level                     |
| --------------- | -------------------- | -------------------------------- |
| `ADMIN`         | System administrator | Full access to all endpoints     |
| `FLEET_MANAGER` | Fleet operator       | Manage own fleet vehicles        |
| `CUSTOMER`      | End user             | Create bookings and reservations |

---

## Public Endpoints

### 1. Get Available Car Models

Returns all car models with at least one available vehicle.

**Endpoint:** `GET /models`

**Authentication:** Not required

**Response:**

```json
[
  {
    "modelId": "550e8400-e29b-41d4-a716-446655440000",
    "operatorId": "660e8400-e29b-41d4-a716-446655440000",
    "modelName": "Toyota Camry",
    "manufacturer": "Toyota",
    "year": 2024,
    "seatingCapacity": 5,
    "fuelType": "PETROL",
    "transmissionType": "AUTOMATIC",
    "dailyRate": 85.0,
    "imageUrl": "https://example.com/camry.jpg",
    "availableCount": 3
  }
]
```

**Example Request:**

```bash
curl -X GET http://localhost:8080/api/v1/fleet/models
```

---

### 2. Get Models by Operator

Returns available car models for a specific fleet operator.

**Endpoint:** `GET /operators/{operatorId}/models`

**Authentication:** Not required

**Path Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `operatorId` | UUID | Yes | Fleet operator's unique ID |

**Response:**

```json
[
  {
    "modelId": "550e8400-e29b-41d4-a716-446655440000",
    "operatorId": "660e8400-e29b-41d4-a716-446655440000",
    "modelName": "Honda Civic",
    "manufacturer": "Honda",
    "year": 2024,
    "seatingCapacity": 5,
    "fuelType": "PETROL",
    "transmissionType": "AUTOMATIC",
    "dailyRate": 75.0,
    "availableCount": 5
  }
]
```

**Example Request:**

```bash
curl -X GET http://localhost:8080/api/v1/fleet/operators/660e8400-e29b-41d4-a716-446655440000/models
```

**Status Codes:**

- `200 OK` - Success
- `204 No Content` - No models found for this operator
- `400 Bad Request` - Invalid operator ID format

---

### 3. Check Vehicle Availability

Check availability count for a specific car model within a date range.

**Endpoint:** `GET /models/{modelPublicId}/availability-count`

**Authentication:** Not required

**Path Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `modelPublicId` | UUID | Yes | Car model's unique ID |

**Query Parameters:**
| Parameter | Type | Required | Format | Description |
|-----------|------|----------|--------|-------------|
| `startDate` | DateTime | Yes | ISO 8601 | Booking start date/time |
| `endDate` | DateTime | Yes | ISO 8601 | Booking end date/time |

**Response:**

```json
{
  "modelPublicId": "550e8400-e29b-41d4-a716-446655440000",
  "startDate": "2025-01-01T10:00:00",
  "endDate": "2025-01-05T10:00:00",
  "availableCount": 3,
  "isAvailable": true
}
```

**Example Request:**

```bash
curl -X GET "http://localhost:8080/api/v1/fleet/models/550e8400-e29b-41d4-a716-446655440000/availability-count?startDate=2025-01-01T10:00:00&endDate=2025-01-05T10:00:00"
```

**Status Codes:**

- `200 OK` - Success
- `400 Bad Request` - Invalid date range (end before start, past dates, etc.)
- `404 Not Found` - Model not found

---

## Customer Endpoints

### 4. Create Temporary Reservation

Creates a temporary reservation that locks a vehicle for 30 seconds before payment.

**Endpoint:** `POST /reservations/temporary`

**Authentication:** Required (CUSTOMER role)

**Request Body:**

```json
{
  "modelPublicId": "550e8400-e29b-41d4-a716-446655440000",
  "bookingId": "booking-12345",
  "startDate": "2025-01-01T10:00:00",
  "endDate": "2025-01-05T10:00:00"
}
```

**Field Descriptions:**
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `modelPublicId` | UUID | Yes | Car model to reserve |
| `bookingId` | String | Yes | Booking service reference ID |
| `startDate` | DateTime | Yes | Rental start date/time (ISO 8601) |
| `endDate` | DateTime | Yes | Rental end date/time (ISO 8601) |

**Response:**

```json
{
  "reservationId": "770e8400-e29b-41d4-a716-446655440000",
  "vehicleId": "880e8400-e29b-41d4-a716-446655440000",
  "modelPublicId": "550e8400-e29b-41d4-a716-446655440000",
  "bookingId": "booking-12345",
  "status": "PENDING",
  "startDate": "2025-01-01T10:00:00",
  "endDate": "2025-01-05T10:00:00",
  "expiresAt": "2025-10-18T10:05:00",
  "createdAt": "2025-10-18T10:04:30"
}
```

**Example Request:**

```bash
curl -X POST http://localhost:8080/api/v1/fleet/reservations/temporary \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "modelPublicId": "550e8400-e29b-41d4-a716-446655440000",
    "bookingId": "booking-12345",
    "startDate": "2025-01-01T10:00:00",
    "endDate": "2025-01-05T10:00:00"
  }'
```

**Status Codes:**

- `201 Created` - Reservation created successfully
- `400 Bad Request` - Invalid request data
- `401 Unauthorized` - Missing or invalid token
- `409 Conflict` - No vehicles available

**Important Notes:**

- Reservation expires after 30 seconds
- User must confirm within expiry time
- Vehicle is locked from other reservations during this time

---

### 5. Confirm Reservation

Confirms a temporary reservation after successful payment.

**Endpoint:** `POST /reservations/{reservationId}/confirm`

**Authentication:** Required (CUSTOMER role)

**Path Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `reservationId` | UUID | Yes | Reservation ID from temporary reservation |

**Request Body:**

```json
{
  "paymentReference": "PAY-67890"
}
```

**Response:**

```json
{
  "reservationId": "770e8400-e29b-41d4-a716-446655440000",
  "vehicleId": "880e8400-e29b-41d4-a716-446655440000",
  "status": "CONFIRMED",
  "paymentReference": "PAY-67890",
  "confirmedAt": "2025-10-18T10:05:00"
}
```

**Example Request:**

```bash
curl -X POST http://localhost:8080/api/v1/fleet/reservations/770e8400-e29b-41d4-a716-446655440000/confirm \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"paymentReference": "PAY-67890"}'
```

**Status Codes:**

- `200 OK` - Reservation confirmed
- `400 Bad Request` - Reservation not in PENDING status
- `401 Unauthorized` - Missing or invalid token
- `404 Not Found` - Reservation doesn't exist
- `410 Gone` - Reservation expired

---

### 6. Cancel Reservation

Cancels a pending reservation.

**Endpoint:** `DELETE /reservations/{reservationId}`

**Authentication:** Required (CUSTOMER role)

**Path Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `reservationId` | UUID | Yes | Reservation ID to cancel |

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `reason` | String | No | Cancellation reason |

**Response:** `204 No Content`

**Example Request:**

```bash
curl -X DELETE "http://localhost:8080/api/v1/fleet/reservations/770e8400-e29b-41d4-a716-446655440000?reason=payment_failed" \
  -H "Authorization: Bearer <token>"
```

**Status Codes:**

- `204 No Content` - Cancelled successfully
- `400 Bad Request` - Reservation not in PENDING status
- `401 Unauthorized` - Missing or invalid token
- `404 Not Found` - Reservation doesn't exist

---

### 7. Get Reservation Details

Retrieves details of an existing reservation.

**Endpoint:** `GET /reservations/{reservationId}`

**Authentication:** Required (CUSTOMER role)

**Path Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `reservationId` | UUID | Yes | Reservation ID |

**Example Request:**

```bash
curl -X GET http://localhost:8080/api/v1/fleet/reservations/770e8400-e29b-41d4-a716-446655440000 \
  -H "Authorization: Bearer <token>"
```

**Status Codes:**

- `200 OK` - Success
- `401 Unauthorized` - Missing or invalid token
- `404 Not Found` - Reservation doesn't exist

---

## Fleet Manager Endpoints

### 8. Get My Fleet Models

Returns car models in the authenticated fleet manager's fleet.

**Endpoint:** `GET /operators/fleet`

**Authentication:** Required (FLEET_MANAGER role)

**Response:**

```json
[
  {
    "modelId": "550e8400-e29b-41d4-a716-446655440000",
    "operatorId": "660e8400-e29b-41d4-a716-446655440000",
    "modelName": "Tesla Model 3",
    "manufacturer": "Tesla",
    "year": 2024,
    "seatingCapacity": 5,
    "fuelType": "ELECTRIC",
    "transmissionType": "AUTOMATIC",
    "dailyRate": 120.0,
    "availableCount": 2
  }
]
```

**Example Request:**

```bash
curl -X GET http://localhost:8080/api/v1/fleet/operators/fleet \
  -H "Authorization: Bearer <fleet-manager-token>"
```

**Status Codes:**

- `200 OK` - Success
- `204 No Content` - No models in fleet
- `401 Unauthorized` - Missing or invalid token
- `403 Forbidden` - User doesn't have FLEET_MANAGER role

---

### 9. Get All My Vehicles (Paginated)

Returns all vehicles in the fleet manager's fleet with pagination and filtering.

**Endpoint:** `GET /operators/fleet/all/paginated`

**Authentication:** Required (FLEET_MANAGER role)

**Query Parameters:**
| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `page` | Integer | No | 0 | Page number (0-indexed) |
| `size` | Integer | No | 10 | Page size |
| `sortBy` | String | No | licensePlate | Field to sort by |
| `sortDirection` | String | No | asc | Sort direction (asc/desc) |
| `licensePlate` | String | No | - | Filter by license plate |
| `status` | String | No | - | Filter by status |
| `model` | String | No | - | Filter by model name |
| `manufacturer` | String | No | - | Filter by manufacturer |
| `location` | String | No | - | Filter by location |

**Vehicle Status Values:**

- `AVAILABLE`
- `BOOKED`
- `UNDER_MAINTENANCE`

**Response:**

```json
{
  "content": [
    {
      "id": "880e8400-e29b-41d4-a716-446655440000",
      "licensePlate": "SGX1234A",
      "carModel": {
        "id": "550e8400-e29b-41d4-a716-446655440000",
        "modelName": "Toyota Camry",
        "manufacturer": "Toyota"
      },
      "status": "AVAILABLE",
      "currentLocation": "Marina Bay Sands",
      "lastMaintenanceDate": "2024-10-01"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10,
    "sort": {
      "sorted": true,
      "unsorted": false
    }
  },
  "totalElements": 25,
  "totalPages": 3,
  "last": false
}
```

**Example Request:**

```bash
curl -X GET "http://localhost:8080/api/v1/fleet/operators/fleet/all/paginated?page=0&size=10&status=AVAILABLE" \
  -H "Authorization: Bearer <fleet-manager-token>"
```

---

### 10. Get Fleet Dashboard

Returns comprehensive fleet statistics and metrics.

**Endpoint:** `GET /operators/dashboard`

**Authentication:** Required (FLEET_MANAGER role)

**Response:**

```json
{
  "totalVehicles": 25,
  "availableVehicles": 15,
  "bookedVehicles": 8,
  "maintenanceVehicles": 2,
  "utilizationRate": 60.0,
  "modelBreakdown": [
    {
      "modelName": "Toyota Camry",
      "totalCount": 10,
      "availableCount": 6
    }
  ],
  "recentBookings": 45,
  "monthlyRevenue": 35000.0
}
```

**Example Request:**

```bash
curl -X GET http://localhost:8080/api/v1/fleet/operators/dashboard \
  -H "Authorization: Bearer <fleet-manager-token>"
```

---

### 11. Update Vehicle Status

Updates the status of a specific vehicle in the fleet.

**Endpoint:** `PATCH /operators/fleet/{id}/status`

**Authentication:** Required (FLEET_MANAGER role)

**Path Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | UUID | Yes | Vehicle ID |

**Request Body:**

```json
{
  "status": "UNDER_MAINTENANCE"
}
```

**Valid Status Values:**

- `AVAILABLE`
- `BOOKED`
- `UNDER_MAINTENANCE`

**Response:**

```json
{
  "id": "880e8400-e29b-41d4-a716-446655440000",
  "licensePlate": "SGX1234A",
  "status": "UNDER_MAINTENANCE",
  "updatedAt": "2025-10-18T10:00:00"
}
```

**Example Request:**

```bash
curl -X PATCH http://localhost:8080/api/v1/fleet/operators/fleet/880e8400-e29b-41d4-a716-446655440000/status \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <fleet-manager-token>" \
  -d '{"status": "UNDER_MAINTENANCE"}'
```

**Status Codes:**

- `200 OK` - Status updated
- `400 Bad Request` - Invalid status value
- `401 Unauthorized` - Missing or invalid token
- `403 Forbidden` - Not authorized for this vehicle
- `404 Not Found` - Vehicle doesn't exist

---

## Admin Endpoints

### 12. Create Car Model

Creates a new car model in the master catalog.

**Endpoint:** `POST /models`

**Authentication:** Required (ADMIN role)

**Request Body:**

```json
{
  "modelName": "Toyota Camry",
  "manufacturer": "Toyota",
  "year": 2024,
  "seatingCapacity": 5,
  "fuelType": "PETROL",
  "transmissionType": "AUTOMATIC",
  "dailyRate": 85.0,
  "description": "Comfortable sedan for business travel",
  "features": ["GPS", "Bluetooth", "Backup Camera"],
  "imageUrl": "https://example.com/camry.jpg"
}
```

**Field Validations:**
| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| `modelName` | String | Yes | 2-100 characters |
| `manufacturer` | String | Yes | 2-50 characters |
| `year` | Integer | Yes | 1900-2100 |
| `seatingCapacity` | Integer | Yes | 1-50 |
| `fuelType` | Enum | Yes | PETROL, DIESEL, ELECTRIC, HYBRID |
| `transmissionType` | Enum | Yes | MANUAL, AUTOMATIC |
| `dailyRate` | Decimal | Yes | > 0 |

**Response:**

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "modelName": "Toyota Camry",
  "manufacturer": "Toyota",
  "year": 2024,
  "seatingCapacity": 5,
  "fuelType": "PETROL",
  "transmissionType": "AUTOMATIC",
  "dailyRate": 85.0,
  "createdAt": "2025-10-18T10:00:00"
}
```

**Example Request:**

```bash
curl -X POST http://localhost:8080/api/v1/fleet/models \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <admin-token>" \
  -d '{
    "modelName": "Toyota Camry",
    "manufacturer": "Toyota",
    "year": 2024,
    "seatingCapacity": 5,
    "fuelType": "PETROL",
    "transmissionType": "AUTOMATIC",
    "dailyRate": 85.00
  }'
```

**Status Codes:**

- `201 Created` - Model created successfully
- `400 Bad Request` - Validation errors
- `401 Unauthorized` - Missing or invalid token
- `403 Forbidden` - User doesn't have ADMIN role

---

### 13. Get All Car Models

Returns all car models in the master catalog (admin view).

**Endpoint:** `GET /models/all`

**Authentication:** Required (ADMIN role)

**Response:**

```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "modelName": "Toyota Camry",
    "manufacturer": "Toyota",
    "year": 2024,
    "seatingCapacity": 5,
    "fuelType": "PETROL",
    "transmissionType": "AUTOMATIC",
    "dailyRate": 85.0,
    "totalFleetVehicles": 15,
    "createdAt": "2025-10-01T10:00:00"
  }
]
```

**Example Request:**

```bash
curl -X GET http://localhost:8080/api/v1/fleet/models/all \
  -H "Authorization: Bearer <admin-token>"
```

---

## Error Responses

### Standard Error Format

```json
{
  "timestamp": "2025-10-18T10:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid date range: end date must be after start date",
  "path": "/api/v1/fleet/reservations/temporary",
  "correlationId": "abc-123-def"
}
```

### Common HTTP Status Codes

| Code  | Meaning               | Description                                     |
| ----- | --------------------- | ----------------------------------------------- |
| `200` | OK                    | Request succeeded                               |
| `201` | Created               | Resource created successfully                   |
| `204` | No Content            | Success with no response body                   |
| `400` | Bad Request           | Invalid request data                            |
| `401` | Unauthorized          | Missing or invalid authentication               |
| `403` | Forbidden             | Insufficient permissions                        |
| `404` | Not Found             | Resource doesn't exist                          |
| `409` | Conflict              | Business logic conflict (e.g., no availability) |
| `410` | Gone                  | Resource expired                                |
| `500` | Internal Server Error | Server error                                    |

### Validation Errors

```json
{
  "timestamp": "2025-10-18T10:00:00",
  "status": 400,
  "error": "Validation Failed",
  "errors": [
    {
      "field": "modelName",
      "message": "Model name must be between 2 and 100 characters"
    },
    {
      "field": "dailyRate",
      "message": "Daily rate must be greater than 0"
    }
  ]
}
```

---

## Rate Limiting

### Default Limits

| User Type     | Requests per Minute | Burst Capacity |
| ------------- | ------------------- | -------------- |
| Public        | 60                  | 100            |
| Authenticated | 100                 | 200            |
| Admin         | Unlimited           | Unlimited      |

### Rate Limit Headers

```http
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1735689600
```

### Rate Limit Exceeded Response

```json
{
  "timestamp": "2025-10-18T10:00:00",
  "status": 429,
  "error": "Too Many Requests",
  "message": "Rate limit exceeded. Please retry after 60 seconds",
  "retryAfter": 60
}
```

---

## Pagination

### Pagination Parameters

| Parameter | Type    | Default | Max | Description              |
| --------- | ------- | ------- | --- | ------------------------ |
| `page`    | Integer | 0       | -   | Page number (0-indexed)  |
| `size`    | Integer | 10      | 100 | Items per page           |
| `sort`    | String  | id,asc  | -   | Sort field and direction |

### Pagination Response

```json
{
  "content": [...],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10,
    "offset": 0,
    "sort": {
      "sorted": true,
      "unsorted": false
    }
  },
  "totalElements": 150,
  "totalPages": 15,
  "last": false,
  "first": true,
  "numberOfElements": 10,
  "size": 10,
  "number": 0
}
```

---

## Best Practices

### 1. Always Use HTTPS in Production

```
https://api.exploresg.com/api/v1/fleet/...
```

### 2. Include Correlation ID for Debugging

```http
X-Correlation-ID: your-correlation-id
```

### 3. Handle Token Expiration

- Check token expiry before making requests
- Implement token refresh logic
- Handle 401 responses gracefully

### 4. Implement Exponential Backoff

- Retry failed requests with exponential backoff
- Max retries: 3
- Initial delay: 1 second

### 5. Use Idempotency Keys

```http
Idempotency-Key: unique-operation-id
```

---

## Swagger UI

Interactive API documentation is available at:

🔗 **http://localhost:8080/swagger-ui.html**

Features:

- Try out API calls directly
- View request/response schemas
- See all available endpoints
- Authentication support

---

## Change Log

| Version | Date       | Changes             |
| ------- | ---------- | ------------------- |
| 1.0.0   | 2025-10-18 | Initial API release |

---

**For additional support, visit our [GitHub repository](https://github.com/XploreSG/exploresg-fleet-service).**
