# Fleet Service - Booking Integration Test Script
# Run these commands to test the integration endpoints

## Prerequisites
# 1. Fleet service running on http://localhost:8081
# 2. Get a valid modelPublicId from your database:
#    SELECT public_id FROM car_models LIMIT 1;

## Test 1: Health Check
Write-Host "Testing Health Endpoint..." -ForegroundColor Cyan
curl http://localhost:8081/api/v1/fleet/health

Write-Host "`n`n"

## Test 2: Check Model Availability
Write-Host "Testing Model Availability..." -ForegroundColor Cyan
# Replace YOUR_MODEL_UUID with actual model UUID
$modelId = "YOUR_MODEL_UUID"
$startDate = "2025-01-15T10:00:00"
$endDate = "2025-01-20T18:00:00"

curl "http://localhost:8081/api/v1/fleet/models/$modelId/availability-count?startDate=$startDate&endDate=$endDate"

Write-Host "`n`n"

## Test 3: Create Temporary Reservation
Write-Host "Testing Temporary Reservation..." -ForegroundColor Cyan
$reservationPayload = @{
    modelPublicId = "YOUR_MODEL_UUID"
    bookingId = "550e8400-e29b-41d4-a716-446655440000"
    startDate = "2025-01-15T10:00:00"
    endDate = "2025-01-20T18:00:00"
} | ConvertTo-Json

Write-Host "Payload: $reservationPayload" -ForegroundColor Gray
curl -X POST http://localhost:8081/api/v1/fleet/reservations/temporary `
  -H "Content-Type: application/json" `
  -d $reservationPayload

Write-Host "`n`n"

## Test 4: Confirm Reservation
Write-Host "Testing Confirm Reservation..." -ForegroundColor Cyan
Write-Host "(Replace RESERVATION_UUID with the UUID from Test 3)" -ForegroundColor Yellow
$reservationId = "RESERVATION_UUID"
$confirmPayload = @{
    paymentReference = "stripe_pi_test_123456"
} | ConvertTo-Json

curl -X POST "http://localhost:8081/api/v1/fleet/reservations/$reservationId/confirm" `
  -H "Content-Type: application/json" `
  -d $confirmPayload

Write-Host "`n`n"

## Test 5: Cancel Reservation
Write-Host "Testing Cancel Reservation..." -ForegroundColor Cyan
Write-Host "(Replace RESERVATION_UUID with the UUID from Test 3)" -ForegroundColor Yellow
curl -X DELETE "http://localhost:8081/api/v1/fleet/reservations/$reservationId?reason=Payment+failed"

Write-Host "`n`n✅ All tests completed!" -ForegroundColor Green
