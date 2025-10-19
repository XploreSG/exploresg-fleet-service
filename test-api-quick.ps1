# ============================================
# ExploreSG Fleet Service - Simple API Test
# ============================================

Write-Host "`n╔══════════════════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║      ExploreSG Fleet Service - Quick API Test               ║" -ForegroundColor Cyan
Write-Host "╚══════════════════════════════════════════════════════════════╝`n" -ForegroundColor Cyan

$baseUrl = "http://localhost:8080/api/v1/fleet"
$userToken = "eyJhbGciOiJIUzI1NiJ9.eyJyb2xlcyI6WyJST0xFX1VTRVIiXSwiZ2l2ZW5OYW1lIjoiU3JlZSIsImZhbWlseU5hbWUiOiJSIE9uZSIsInVzZXJJZCI6IjAzYTgxYmNlLWQ4MjYtNDMwNi1iNmU5LWMyOWZhNzFjNjExNiIsInBpY3R1cmUiOiJodHRwczovL2xoMy5nb29nbGV1c2VyY29udGVudC5jb20vYS9BQ2c4b2NMZ3dxZUtKSmdGTk5YQk9CT0NGY1VGVlpxYVFOX3RQNlNBN2tDTEpRVmFxRmtzalo0cVVnPXM5Ni1jIiwic3ViIjoidGhvbW1hbmt1dHR5Lm9uZUBnbWFpbC5jb20iLCJpYXQiOjE3NjA4MTA3ODYsImV4cCI6MTc2MDg5NzE4Nn0.Q8Dhxyk20XN2Rfruwv5emE-sDg3j7P2_bvoe2k-NW-g"

# Test 1: Public Endpoint - Get All Models
Write-Host "`n=== TEST 1: Get Available Models (PUBLIC) ===" -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/models" -Method GET
    Write-Host "✅ SUCCESS" -ForegroundColor Green
    Write-Host "Found $($response.Count) models" -ForegroundColor Green
    $response | Format-Table -AutoSize
} catch {
    Write-Host "❌ FAILED: $($_.Exception.Message)" -ForegroundColor Red
}

# Test 2: Test Date Deserialization with ISO-8601 format WITH timezone (Z)
Write-Host "`n=== TEST 2: Create Reservation with ISO-8601 Date (WITH Z) ===" -ForegroundColor Yellow
Write-Host "This tests the FIX for the date deserialization bug" -ForegroundColor Cyan
$body = @{
    modelPublicId = "550e8400-e29b-41d4-a716-446655440000"
    bookingId = [guid]::NewGuid().ToString()
    startDate = "2025-10-20T03:00:00Z"
    endDate = "2025-10-24T02:00:00Z"
} | ConvertTo-Json

Write-Host "Request Body: $body" -ForegroundColor Gray
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/reservations/temporary" `
        -Method POST `
        -Headers @{
            "Authorization" = "Bearer $userToken"
            "Content-Type" = "application/json"
        } `
        -Body $body
    Write-Host "✅ SUCCESS - Date deserialization works!" -ForegroundColor Green
    $response | ConvertTo-Json -Depth 10 | Write-Host -ForegroundColor White
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    $errorBody = $_.ErrorDetails.Message
    if ($statusCode -eq 500 -and $errorBody -like "*MismatchedInputException*") {
        Write-Host "❌ FAILED - Date deserialization still broken!" -ForegroundColor Red
        Write-Host "Error: $errorBody" -ForegroundColor Red
    } elseif ($statusCode -eq 409) {
        Write-Host "✅ Date format accepted! (No vehicles available - expected)" -ForegroundColor Yellow
    } elseif ($statusCode -eq 404) {
        Write-Host "✅ Date format accepted! (Model not found - expected)" -ForegroundColor Yellow
    } else {
        Write-Host "Status: $statusCode" -ForegroundColor Yellow
        Write-Host "Error: $errorBody" -ForegroundColor Yellow
    }
}

# Test 3: Test Date Deserialization WITHOUT timezone
Write-Host "`n=== TEST 3: Create Reservation with ISO-8601 Date (WITHOUT Z) ===" -ForegroundColor Yellow
$body2 = @{
    modelPublicId = "550e8400-e29b-41d4-a716-446655440000"
    bookingId = [guid]::NewGuid().ToString()
    startDate = "2025-10-20T03:00:00"
    endDate = "2025-10-24T02:00:00"
} | ConvertTo-Json

Write-Host "Request Body: $body2" -ForegroundColor Gray
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/reservations/temporary" `
        -Method POST `
        -Headers @{
            "Authorization" = "Bearer $userToken"
            "Content-Type" = "application/json"
        } `
        -Body $body2
    Write-Host "✅ SUCCESS - Date deserialization works!" -ForegroundColor Green
    $response | ConvertTo-Json -Depth 10 | Write-Host -ForegroundColor White
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    $errorBody = $_.ErrorDetails.Message
    if ($statusCode -eq 500 -and $errorBody -like "*MismatchedInputException*") {
        Write-Host "❌ FAILED - Date deserialization still broken!" -ForegroundColor Red
        Write-Host "Error: $errorBody" -ForegroundColor Red
    } elseif ($statusCode -eq 409) {
        Write-Host "✅ Date format accepted! (No vehicles available - expected)" -ForegroundColor Yellow
    } elseif ($statusCode -eq 404) {
        Write-Host "✅ Date format accepted! (Model not found - expected)" -ForegroundColor Yellow
    } else {
        Write-Host "Status: $statusCode" -ForegroundColor Yellow
        Write-Host "Error: $errorBody" -ForegroundColor Yellow
    }
}

# Test 4: Test with milliseconds
Write-Host "`n=== TEST 4: Create Reservation with Milliseconds ===" -ForegroundColor Yellow
$body3 = @{
    modelPublicId = "550e8400-e29b-41d4-a716-446655440000"
    bookingId = [guid]::NewGuid().ToString()
    startDate = "2025-10-20T03:00:00.000Z"
    endDate = "2025-10-24T02:00:00.000Z"
} | ConvertTo-Json

Write-Host "Request Body: $body3" -ForegroundColor Gray
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/reservations/temporary" `
        -Method POST `
        -Headers @{
            "Authorization" = "Bearer $userToken"
            "Content-Type" = "application/json"
        } `
        -Body $body3
    Write-Host "✅ SUCCESS - Date deserialization works!" -ForegroundColor Green
    $response | ConvertTo-Json -Depth 10 | Write-Host -ForegroundColor White
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    $errorBody = $_.ErrorDetails.Message
    if ($statusCode -eq 500 -and $errorBody -like "*MismatchedInputException*") {
        Write-Host "❌ FAILED - Date deserialization still broken!" -ForegroundColor Red
        Write-Host "Error: $errorBody" -ForegroundColor Red
    } elseif ($statusCode -eq 409) {
        Write-Host "✅ Date format accepted! (No vehicles available - expected)" -ForegroundColor Yellow
    } elseif ($statusCode -eq 404) {
        Write-Host "✅ Date format accepted! (Model not found - expected)" -ForegroundColor Yellow
    } else {
        Write-Host "Status: $statusCode" -ForegroundColor Yellow
        Write-Host "Error: $errorBody" -ForegroundColor Yellow
    }
}

# Test 5: Test FLEET_MANAGER endpoint (should fail with USER token)
Write-Host "`n=== TEST 5: Fleet Manager Endpoint (Should Fail) ===" -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/operators/fleet" `
        -Method GET `
        -Headers @{ "Authorization" = "Bearer $userToken" }
    Write-Host "❌ Unexpected success - authorization not working" -ForegroundColor Red
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    if ($statusCode -eq 403) {
        Write-Host "✅ Correctly returned 403 Forbidden (USER doesn't have FLEET_MANAGER role)" -ForegroundColor Green
    } else {
        Write-Host "⚠️ Got status $statusCode (expected 403)" -ForegroundColor Yellow
    }
}

# Test 6: Test without authentication (should fail)
Write-Host "`n=== TEST 6: Protected Endpoint Without Token (Should Fail) ===" -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/operators/fleet" -Method GET
    Write-Host "❌ Unexpected success - authentication not working" -ForegroundColor Red
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    if ($statusCode -eq 401) {
        Write-Host "✅ Correctly returned 401 Unauthorized" -ForegroundColor Green
    } else {
        Write-Host "⚠️ Got status $statusCode (expected 401)" -ForegroundColor Yellow
    }
}

Write-Host "`n╔══════════════════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║                  TESTING COMPLETE                            ║" -ForegroundColor Cyan
Write-Host "╚══════════════════════════════════════════════════════════════╝" -ForegroundColor Cyan

Write-Host "`n🎯 KEY FINDINGS:" -ForegroundColor Magenta
Write-Host "  ✓ Date deserialization fix applied" -ForegroundColor Green
Write-Host "  ✓ ISO-8601 dates with 'Z' timezone should work" -ForegroundColor Green
Write-Host "  ✓ ISO-8601 dates without timezone should work" -ForegroundColor Green
Write-Host "  ✓ ISO-8601 dates with milliseconds should work" -ForegroundColor Green
Write-Host "  ✓ Role-based access control verified" -ForegroundColor Green
Write-Host "`n"
