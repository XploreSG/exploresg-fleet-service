🚗 Fleet Service - AI Work Instructions
📋 Context & Objective
You are completing the Fleet Service for the ExploreSG fleet booking system. Most of the code is already implemented - you just need to verify everything works together.
Your Role: Fleet workspace AI assistant
Service Name: exploresg-fleet-service
Port: 8081
Technology: Java 17, Spring Boot 3.2.0, PostgreSQL

✅ What's ALREADY DONE (98% Complete)
Entities:
✅ FleetVehicle - Vehicle instances
✅ VehicleBookingRecord - Reservation records with indexes
✅ CarModel - Vehicle models
Repositories:
✅ FleetVehicleRepository with pessimistic locking query
✅ VehicleBookingRecordRepository with all queries
Services:
✅ ReservationService - Complete with all 4 methods
✅ ReservationCleanupScheduler - Auto-expires PENDING reservations every 10s
Controllers:
✅ ReservationController - All 4 endpoints with Swagger docs
DTOs:
✅ All request/response DTOs
✅ All custom exceptions
✅ GlobalExceptionHandler

🎯 What You Need to VERIFY
Your job is to:
✅ Verify all code compiles
✅ Run the service successfully on port 8081
✅ Test the 4 reservation endpoints
✅ Verify cleanup scheduler runs every 10 seconds
✅ Verify pessimistic locking prevents race conditions

📊 API Contract (What Booking Service Will Call)
Endpoint 1: Create Temporary Reservation ⭐ CRITICAL
POST /api/v1/fleet/reservations/temporary
Content-Type: application/json

Request Body:
{
"modelPublicId": "uuid",
"bookingId": "uuid",
"startDate": "2025-01-15T10:00:00",
"endDate": "2025-01-20T10:00:00"
}

Response 201 Created (Success):
{
"success": true,
"message": "Reservation created",
"reservationId": "uuid",
"vehicleId": "uuid",
"bookingId": "uuid",
"status": "PENDING",
"expiresAt": "2025-01-15T10:05:00"
}

Response 409 Conflict (No vehicles available):
{
"success": false,
"message": "No vehicles available for the requested dates",
"reservationId": null
}

Response 400 Bad Request (Invalid dates):
{
"success": false,
"message": "End date must be after start date",
"reservationId": null
}

Endpoint 2: Confirm Reservation ⭐ CRITICAL
POST /api/v1/fleet/reservations/{reservationId}/confirm
Content-Type: application/json

Request Body:
{
"paymentReference": "mock_stripe_pi_abc123",
"notes": "Booking confirmed after payment"
}

Response 200 OK (Success):
{
"success": true,
"message": "Reservation confirmed",
"reservationId": "uuid",
"vehicleId": "uuid",
"status": "CONFIRMED",
"confirmedAt": "2025-01-15T10:04:30"
}

Response 410 Gone (Expired):
{
"success": false,
"message": "Reservation has expired"
}

Response 404 Not Found:
{
"success": false,
"message": "Reservation not found"
}

Response 400 Bad Request:
{
"success": false,
"message": "Reservation is not in PENDING status"
}

Endpoint 3: Cancel Reservation
DELETE /api/v1/fleet/reservations/{reservationId}?reason=Payment+failed

Response 204 No Content (Success - no body)

Response 404 Not Found:
{
"success": false,
"message": "Reservation not found"
}

Endpoint 4: Check Availability
GET /api/v1/fleet/models/{modelPublicId}/availability-count
?startDate=2025-01-15T10:00:00
&endDate=2025-01-20T10:00:00

Response 200 OK:
{
"modelPublicId": "uuid",
"availableCount": 5,
"available": true,
"startDate": "2025-01-15T10:00:00",
"endDate": "2025-01-20T10:00:00"
}

🗄️ Database Schema (Already Created)
-- Vehicle booking records table
CREATE TABLE vehicle_booking_records (
id UUID PRIMARY KEY,
vehicle_id UUID NOT NULL REFERENCES fleet_vehicles(id),
booking_id UUID NOT NULL,
booking_start_date TIMESTAMP NOT NULL,
booking_end_date TIMESTAMP NOT NULL,
reservation_status VARCHAR(20) NOT NULL, -- PENDING/CONFIRMED/CANCELLED/EXPIRED
expires_at TIMESTAMP,
payment_reference VARCHAR(255),
notes TEXT,
created_at TIMESTAMP NOT NULL,
confirmed_at TIMESTAMP,
cancelled_at TIMESTAMP,
last_updated_at TIMESTAMP NOT NULL,

    CONSTRAINT uk_vehicle_booking UNIQUE (vehicle_id, booking_id)

);

CREATE INDEX idx_vehicle_dates ON vehicle_booking_records
(vehicle_id, booking_start_date, booking_end_date);
CREATE INDEX idx_booking_id ON vehicle_booking_records (booking_id);
CREATE INDEX idx_status_expires ON vehicle_booking_records
(reservation_status, expires_at);

🔐 Critical: Pessimistic Locking Query
This query MUST be in FleetVehicleRepository:
@Query(value = """
SELECT fv.\*
FROM fleet_vehicles fv
WHERE fv.car_model_id = (
SELECT id FROM car_models WHERE public_id = :modelPublicId
)
AND fv.status = 'AVAILABLE'
AND NOT EXISTS (
SELECT 1
FROM vehicle_booking_records vbr
WHERE vbr.vehicle_id = fv.id
AND vbr.reservation_status IN ('CONFIRMED', 'PENDING')
AND vbr.booking_start_date < :endDate
AND vbr.booking_end_date > :startDate
)
ORDER BY fv.mileage_km ASC, fv.id ASC
LIMIT 1
FOR UPDATE SKIP LOCKED
""", nativeQuery = true)
Optional<FleetVehicle> findOneAvailableVehicleForBooking(
@Param("modelPublicId") UUID modelPublicId,
@Param("startDate") LocalDateTime startDate,
@Param("endDate") LocalDateTime endDate
);

Why this is critical:
FOR UPDATE locks the selected row
SKIP LOCKED makes concurrent transactions skip already-locked rows
This prevents two users from booking the same vehicle

🧹 Cleanup Scheduler (Already Implemented)
Verify this runs every 10 seconds:
@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationCleanupScheduler {

    private final VehicleBookingRecordRepository bookingRecordRepository;

    @Scheduled(fixedDelay = 10000, initialDelay = 5000)
    @Transactional
    public void cleanupExpiredReservations() {
        LocalDateTime now = LocalDateTime.now();
        int expiredCount = bookingRecordRepository.expirePendingReservations(now);

        if (expiredCount > 0) {
            log.info("Expired {} PENDING reservation(s) at {}", expiredCount, now);
        }
    }

}

Make sure main application has:
@SpringBootApplication
@EnableScheduling // ← This is required!
public class FleetServiceApplication {
// ...
}

⚙️ Configuration
application.properties:

# Application

spring.application.name=exploresg-fleet-service
server.port=8081

# Reservation expiry time (5 minutes for testing, 30s for production)

booking.reservation.expiry-seconds=300

# Database (PostgreSQL)

spring.datasource.url=jdbc:postgresql://localhost:5433/exploresg-fleet-db
spring.datasource.username=exploresguser
spring.datasource.password=exploresgpass

# JPA

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

# Enable scheduling

spring.task.scheduling.pool.size=2

# Logging

logging.level.com.exploresg.fleetservice=DEBUG

✅ Testing Checklist
Test 1: Create Temporary Reservation (Happy Path)

# Prerequisite: Have at least 1 vehicle in database with status='AVAILABLE'

curl -X POST http://localhost:8081/api/v1/fleet/reservations/temporary \
 -H "Content-Type: application/json" \
 -d '{
"modelPublicId": "your-model-uuid",
"bookingId": "123e4567-e89b-12d3-a456-426614174000",
"startDate": "2025-01-15T10:00:00",
"endDate": "2025-01-20T10:00:00"
}'

# Expected: 201 Created with reservationId and expiresAt

# Save the reservationId for next tests

Test 2: Confirm Reservation
curl -X POST http://localhost:8081/api/v1/fleet/reservations/{reservationId}/confirm \
 -H "Content-Type: application/json" \
 -d '{
"paymentReference": "mock_stripe_pi_test123"
}'

# Expected: 200 OK with confirmedAt timestamp

Test 3: Check Expiration (Wait >5 minutes)

# 1. Create reservation

RESERVATION_ID=$(curl -s -X POST http://localhost:8081/api/v1/fleet/reservations/temporary \
 -H "Content-Type: application/json" \
 -d '{"modelPublicId":"uuid", "bookingId":"uuid", "startDate":"2025-01-15T10:00:00", "endDate":"2025-01-20T10:00:00"}' \
 | jq -r '.reservationId')

# 2. Wait 6 minutes (or change expiry-seconds to 10 for faster testing)

# 3. Try to confirm

curl -X POST http://localhost:8081/api/v1/fleet/reservations/$RESERVATION_ID/confirm \
 -H "Content-Type: application/json" \
 -d '{"paymentReference": "test"}'

# Expected: 410 Gone (reservation expired)

Test 4: Cancel Reservation
curl -X DELETE http://localhost:8081/api/v1/fleet/reservations/{reservationId}?reason=User+cancelled

# Expected: 204 No Content

Test 5: No Vehicles Available

# Set all vehicles to status='BOOKED' in database, then:

curl -X POST http://localhost:8081/api/v1/fleet/reservations/temporary \
 -H "Content-Type: application/json" \
 -d '{
"modelPublicId": "uuid",
"bookingId": "uuid",
"startDate": "2025-01-15T10:00:00",
"endDate": "2025-01-20T10:00:00"
}'

# Expected: 409 Conflict

Test 6: Race Condition Prevention

# Run two concurrent requests trying to book the same vehicle

# (Use ApacheBench or similar tool)

ab -n 2 -c 2 -p reservation-request.json -T "application/json" \
 http://localhost:8081/api/v1/fleet/reservations/temporary

# Expected: 1 request succeeds (201), 1 fails (409)

🐛 Common Issues & Solutions
Issue: @EnableScheduling missing
Symptom: Cleanup job never runs
Solution: Add @EnableScheduling to main application class
Issue: FOR UPDATE SKIP LOCKED not working
Symptom: Race conditions occurring
Solution:
Verify PostgreSQL version ≥ 9.5 (older versions don't support SKIP LOCKED)
Verify transaction isolation level is READ_COMMITTED or higher
Issue: Reservations not expiring
Symptom: PENDING reservations stay forever
Solution:
Check cleanup scheduler logs
Verify expirePendingReservations query is correct
Check database for PENDING records with expires_at in the past
Issue: Port 8081 already in use
Solution: Change port in application.properties or stop conflicting service

📊 Database Queries for Debugging
-- Check current reservations
SELECT
id,
vehicle_id,
booking_id,
reservation_status,
expires_at,
created_at
FROM vehicle_booking_records
ORDER BY created_at DESC
LIMIT 10;

-- Check expired PENDING reservations
SELECT \*
FROM vehicle_booking_records
WHERE reservation_status = 'PENDING'
AND expires_at < NOW();

-- Manually expire a reservation (for testing)
UPDATE vehicle_booking_records
SET reservation_status = 'EXPIRED'
WHERE id = 'your-reservation-uuid';

-- Check available vehicles for a date range
SELECT fv.\*
FROM fleet_vehicles fv
WHERE fv.status = 'AVAILABLE'
AND NOT EXISTS (
SELECT 1
FROM vehicle_booking_records vbr
WHERE vbr.vehicle_id = fv.id
AND vbr.reservation_status IN ('CONFIRMED', 'PENDING')
AND vbr.booking_start_date < '2025-01-20T10:00:00'
AND vbr.booking_end_date > '2025-01-15T10:00:00'
);

📦 Deliverables
Verify these exist:
✅ All entities (FleetVehicle, VehicleBookingRecord, CarModel)
✅ All repositories with custom queries
✅ ReservationService with 4 methods
✅ ReservationCleanupScheduler
✅ ReservationController with 4 endpoints
✅ All DTOs and exceptions
✅ GlobalExceptionHandler
Test Results to Provide:
✅ Screenshot of service starting successfully
✅ Postman/curl test showing all 4 endpoints working
✅ Logs showing cleanup job running every 10 seconds
✅ Database screenshot showing PENDING → EXPIRED transition

🔗 Integration with Other Services
Who calls you: Booking Service (port 8082)
You call: Nobody (you are the resource owner)
Critical: Booking Service depends on your API contract being exactly as specified. Don't change response formats without coordinating.

⏱️ Estimated Time
Total: 30 minutes - 1 hour
Verification: 15 mins
Testing all endpoints: 30 mins
Fix any issues: 15 mins

🎯 Final Checklist
Before marking complete:
[ ] Service starts without errors on port 8081
[ ] All 4 endpoints tested and working
[ ] Cleanup scheduler logs appear every 10 seconds
[ ] Created a PENDING reservation and verified it expires after 5 mins
[ ] Tested race condition (2 concurrent requests, only 1 succeeds)
[ ] Database has correct schema with all indexes
[ ] @EnableScheduling is present in main application
[ ] Pessimistic locking query is in FleetVehicleRepository

Your service is 98% done! Just verify everything works! 🚀
