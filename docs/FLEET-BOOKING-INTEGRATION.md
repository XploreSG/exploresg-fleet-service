# Fleet Service Booking Integration - Complete Implementation Guide

## 📋 Executive Summary

### Current Plan Issues

1. ❌ Race condition still exists between availability check and payment
2. ❌ No date range validation in availability checks
3. ❌ Poor UX: User pays before vehicle is secured
4. ❌ Unnecessary refunds when last car is taken during payment

### Recommended Solution: Two-Phase Reservation

✅ Phase 1: Create temporary 30-second hold BEFORE payment
✅ Phase 2: Confirm reservation AFTER successful payment
✅ Phase 3: Auto-cleanup expired temporary holds

## 🔄 Revised Booking Flow

┌────────────────────────────────────────────────────────────────┐
│ BEFORE PAYMENT: Secure Vehicle First │
└────────────────────────────────────────────────────────────────┘

1. User selects: Toyota Camry, Jan 1-5, 2025
2. Frontend → Booking Service: Create booking request
3. Booking Service → Fleet Service: POST /reservations/temporary
   {
   "modelPublicId": "uuid-123",
   "bookingId": "uuid-456",
   "startDate": "2025-01-01T10:00:00",
   "endDate": "2025-01-05T18:00:00"
   }

4. Fleet Service (ATOMIC TRANSACTION):
   a. SELECT FOR UPDATE: Find ONE available vehicle
   b. Create VehicleBookingRecord (status: PENDING, expires: now+30s)
   c. COMMIT
   d. Return: {
   "reservationId": "uuid-789",
   "vehicleId": "uuid-xyz",
   "expiresAt": "2025-01-01T09:30:30"
   }
5. Booking Service → Frontend: Show payment screen with countdown timer
   "Your vehicle is reserved for 30 seconds. Complete payment to confirm."

┌────────────────────────────────────────────────────────────────┐
│ DURING PAYMENT: Vehicle is Locked for 30 Seconds │
└────────────────────────────────────────────────────────────────┘

6. User enters payment details (must complete within 30 seconds)
7. Booking Service → Payment Service: Process payment
8. Payment Service → Booking Service: Payment successful

┌────────────────────────────────────────────────────────────────┐
│ AFTER PAYMENT: Confirm Reservation │
└────────────────────────────────────────────────────────────────┘

9. Booking Service → Fleet Service: POST /reservations/{id}/confirm
   {
   "paymentReference": "stripe_pi_123"
   }

10. Fleet Service:
    a. Find reservation (check not expired)
    b. Update: PENDING → CONFIRMED
    c. Return confirmation

11. Booking Service → Frontend: "Booking Confirmed!"
12. Booking Service → RabbitMQ: Publish BookingFinalizedEvent
13. Email/SMS services consume event → send confirmations

┌────────────────────────────────────────────────────────────────┐
│ FAILURE SCENARIOS │
└────────────────────────────────────────────────────────────────┘

Scenario A: User takes >30 seconds to pay
→ Reservation expires automatically (background job)
→ Vehicle becomes available again
→ Frontend shows: "Reservation expired. Please try again."
→ NO PAYMENT was processed ✓

Scenario B: Payment fails
→ Booking Service calls: DELETE /reservations/{id}
→ Vehicle becomes available again
→ NO REFUND needed ✓

Scenario C: Payment succeeds but reservation expired
→ Confirm endpoint returns: 410 GONE
→ Booking Service triggers automatic refund
→ User notified: "Booking failed, refund processed"

## 🗄️ Database Schema

VehicleBookingRecord Table
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

    -- Indexes for performance
    CONSTRAINT uk_vehicle_booking UNIQUE (vehicle_id, booking_id),
    CHECK (booking_end_date > booking_start_date)

);
CREATE INDEX idx_vehicle_dates ON vehicle_booking_records
(vehicle_id, booking_start_date, booking_end_date);
CREATE INDEX idx_booking_id ON vehicle_booking_records (booking_id);
CREATE INDEX idx_status_expires ON vehicle_booking_records
(reservation_status, expires_at);

## 🔌 API Endpoints

1. Check Availability (Optional Pre-Check)
   GET /api/v1/fleet/models/{modelPublicId}/availability-count
   ?startDate=2025-01-01T10:00:00
   &endDate=2025-01-05T18:00:00
   Response 200 OK:
   {
   "modelPublicId": "uuid-123",
   "availableCount": 7,
   "available": true,
   "startDate": "2025-01-01T10:00:00",
   "endDate": "2025-01-05T18:00:00"
   }
2. Create Temporary Reservation ⭐ CRITICAL
   POST /api/v1/fleet/reservations/temporary
   Request Body:
   {
   "modelPublicId": "uuid-123",
   "bookingId": "uuid-456",
   "startDate": "2025-01-01T10:00:00",
   "endDate": "2025-01-05T18:00:00"
   }
   Response 201 Created:
   {
   "success": true,
   "message": "Reservation created",
   "reservationId": "uuid-789",
   "vehicleId": "uuid-xyz",
   "bookingId": "uuid-456",
   "status": "PENDING",
   "expiresAt": "2025-01-01T09:30:30"
   }
   Response 409 Conflict (No vehicles available):
   {
   "success": false,
   "message": "No vehicles available for the requested dates",
   "reservationId": null
   }
3. Confirm Reservation ⭐ CRITICAL
   POST /api/v1/fleet/reservations/{reservationId}/confirm
   Request Body:
   {
   "paymentReference": "stripe_pi_123"
   }
   Response 200 OK:
   {
   "success": true,
   "message": "Reservation confirmed",
   "reservationId": "uuid-789",
   "vehicleId": "uuid-xyz",
   "status": "CONFIRMED"
   }
   Response 410 Gone (Expired):
   {
   "success": false,
   "message": "Reservation has expired. Please create a new reservation."
   }
4. Cancel Reservation
   DELETE /api/v1/fleet/reservations/{reservationId}?reason=Payment+failed
   Response 204 No Content

## 🔐 Critical SQL Query: Pessimistic Locking

-- This query is THE MOST IMPORTANT part of preventing race conditions
-- It uses SELECT FOR UPDATE to lock the row until transaction commits
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
FOR UPDATE SKIP LOCKED; -- PostgreSQL syntax
Why this works:
• FOR UPDATE: Locks the selected row
• SKIP LOCKED: If row already locked by another transaction, skip it and try next
• This ensures only ONE transaction can reserve each vehicle
• Transaction isolation: READ_COMMITTED or higher

🧹 Background Cleanup Job
@Scheduled(fixedDelay = 10000) // Every 10 seconds
public void cleanupExpiredReservations() {
// Find all PENDING reservations where expires_at < NOW()
// Set their status to EXPIRED
// These vehicles automatically become available again
}

⚙️ Configuration
application.properties

# Transaction timeout for reservation operations

spring.transaction.default-timeout=10

# JPA settings for pessimistic locking

spring.jpa.properties.jakarta.persistence.lock.timeout=3000

# Scheduled job settings

spring.task.scheduling.pool.size=2
Spring Boot Application
@SpringBootApplication
@EnableScheduling // Required for cleanup job
public class FleetServiceApplication {
// ...
}

🧪 Testing Strategy
Unit Tests
@Test
void testTemporaryReservation_Success() {
// Given: 1 vehicle available
// When: Create temporary reservation
// Then: Reservation created with PENDING status, expires in 30s
}
@Test
void testTemporaryReservation_NoVehicleAvailable() {
// Given: 0 vehicles available
// When: Create temporary reservation
// Then: NoVehicleAvailableException thrown
}
@Test
void testConfirmReservation_Expired() {
// Given: Reservation expired 1 second ago
// When: Attempt to confirm
// Then: ReservationExpiredException thrown
}
Integration Tests
@Test
@Transactional
void testConcurrentReservations_PreventRaceCondition() {
// Given: 1 vehicle available
// When: 2 users try to reserve simultaneously
// Then: Only 1 succeeds, other gets 409 Conflict
}

📊 Monitoring & Metrics
Track these metrics:

# Reservations

- reservations_created_total (counter)
- reservations_confirmed_total (counter)
- reservations_expired_total (counter)
- reservations_cancelled_total (counter)
- reservation_confirmation_time_seconds (histogram)

# Availability

- availability_check_duration_seconds (histogram)
- vehicles_locked_gauge (gauge)

# Cleanup

- expired_reservations_cleaned_total (counter)

🚨 Edge Cases to Handle 1. Network failure during confirmation
○ Solution: Implement idempotency with bookingId
○ Allow retry of confirm request with same bookingId 2. User refreshes page during payment
○ Solution: Frontend stores reservationId in sessionStorage
○ Backend allows querying reservation status by bookingId 3. Clock skew between services
○ Solution: Use database NOW() for all timestamp comparisons
○ Never compare client-side timestamps 4. Database deadlocks
○ Solution: Always acquire locks in consistent order (by vehicle.id ASC)
○ Set appropriate deadlock timeout

🎯 Success Criteria
✅ Zero double-bookings (two users cannot book same vehicle for overlapping dates)
✅ <1% reservation expiration rate (most users complete payment within 30s)
✅ <200ms p95 latency for temporary reservation creation
✅ 100% of expired reservations cleaned up within 30 seconds
✅ Zero manual intervention required for stuck reservations

📝 Checklist
Backend Implementation:
• [ ] Create VehicleBookingRecord entity
• [ ] Create VehicleBookingRecordRepository with pessimistic lock query
• [ ] Update FleetVehicleRepository with findOneAvailableVehicleForBooking
• [ ] Implement ReservationService with temporary/confirm methods
• [ ] Create ReservationController with 3 endpoints
• [ ] Add @Scheduled cleanup job
• [ ] Add @EnableScheduling to main application
• [ ] Write unit tests for all reservation scenarios
• [ ] Write integration tests for race conditions
• [ ] Add database indexes for performance
Frontend Implementation:
• [ ] Show countdown timer during payment (30 seconds)
• [ ] Handle reservation expiration gracefully
• [ ] Store reservationId in sessionStorage for recovery
• [ ] Show clear error messages for each failure scenario
DevOps:
• [ ] Add monitoring for reservation metrics
• [ ] Set up alerts for high expiration rates
• [ ] Configure database connection pool appropriately
• [ ] Load test with concurrent users

🆚 Comparison: Original vs. Recommended Plan
Aspect Original Plan Recommended Plan
When vehicle secured After payment Before payment
Race condition risk High None
Refunds needed Yes (frequent) Rare
User experience ❌ Pay then find out car unavailable ✅ Know car secured before paying
Implementation complexity Medium Medium-High
Database load Lower Slightly higher
Reliability ⚠️ Prone to conflicts ✅ Guaranteed atomicity

🎓 Key Learnings 1. Pessimistic locking is essential for preventing race conditions in booking systems 2. Two-phase commit with temporary holds provides better UX than post-payment reservation 3. Automatic cleanup jobs prevent resource leaks from abandoned reservations 4. Date range overlap detection requires careful SQL logic: (start1 < end2) AND (end1 > start2) 5. Transaction isolation must be READ_COMMITTED or higher for SELECT FOR UPDATE to work

From <https://claude.ai/chat/ec01dfc1-73db-46a1-b967-1ab6ff7a5e62>
