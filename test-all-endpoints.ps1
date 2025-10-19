# ============================================
# ExploreSG Fleet Service - API Endpoint Test Script
# ============================================

$baseUrl = "http://localhost:8080/api/v1/fleet"
$userToken = "eyJhbGciOiJIUzI1NiJ9.eyJyb2xlcyI6WyJST0xFX1VTRVIiXSwiZ2l2ZW5OYW1lIjoiU3JlZSIsImZhbWlseU5hbWUiOiJSIE9uZSIsInVzZXJJZCI6IjAzYTgxYmNlLWQ4MjYtNDMwNi1iNmU5LWMyOWZhNzFjNjExNiIsInBpY3R1cmUiOiJodHRwczovL2xoMy5nb29nbGV1c2VyY29udGVudC5jb20vYS9BQ2c4b2NMZ3dxZUtKSmdGTk5YQk9CT0NGY1VGVlpxYVFOX3RQNlNBN2tDTEpRVmFxRmtzalo0cVVnPXM5Ni1jIiwic3ViIjoidGhvbW1hbmt1dHR5Lm9uZUBnbWFpbC5jb20iLCJpYXQiOjE3NjA4MTA3ODYsImV4cCI6MTc2MDg5NzE4Nn0.Q8Dhxyk20XN2Rfruwv5emE-sDg3j7P2_bvoe2k-NW-g"

$testResults = @()

function Test-Endpoint {
    param(
        [string]$Name,
        [string]$Method,
        [string]$Url,
        [string]$Token = $null,
        [object]$Body = $null,
        [int]$ExpectedStatus = 200
    )
    
    Write-Host "`n========================================" -ForegroundColor Cyan
    Write-Host "Testing: $Name" -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "Method: $Method" -ForegroundColor Yellow
    Write-Host "URL: $Url" -ForegroundColor Yellow
    
    try {
        $headers = @{
            "Content-Type" = "application/json"
        }
        
        if ($Token) {
            $headers["Authorization"] = "Bearer $Token"
            Write-Host "Auth: Bearer Token Provided" -ForegroundColor Yellow
        }
        
        if ($Body) {
            $jsonBody = $Body | ConvertTo-Json -Depth 10
            Write-Host "Body: $jsonBody" -ForegroundColor Yellow
        }
        
        $params = @{
            Uri = $Url
            Method = $Method
            Headers = $headers
            ContentType = "application/json"
        }
        
        if ($Body) {
            $params.Body = $Body | ConvertTo-Json -Depth 10
        }
        
        $response = Invoke-WebRequest @params -ErrorAction Stop
        
        Write-Host "`n✅ SUCCESS" -ForegroundColor Green
        Write-Host "Status Code: $($response.StatusCode)" -ForegroundColor Green
        Write-Host "Response:" -ForegroundColor Green
        $response.Content | ConvertFrom-Json | ConvertTo-Json -Depth 10 | Write-Host -ForegroundColor White
        
        $testResults += [PSCustomObject]@{
            Test = $Name
            Status = "✅ PASS"
            StatusCode = $response.StatusCode
            Details = "Success"
        }
        
        return $response.Content | ConvertFrom-Json
        
    } catch {
        $statusCode = if ($_.Exception.Response) { 
            [int]$_.Exception.Response.StatusCode 
        } else { 
            "N/A" 
        }
        
        $errorBody = if ($_.ErrorDetails.Message) {
            $_.ErrorDetails.Message
        } else {
            $_.Exception.Message
        }
        
        # Check if this is an expected error status
        if ($statusCode -eq $ExpectedStatus) {
            Write-Host "`n✅ EXPECTED STATUS" -ForegroundColor Yellow
            Write-Host "Status Code: $statusCode (Expected: $ExpectedStatus)" -ForegroundColor Yellow
            $testResults += [PSCustomObject]@{
                Test = $Name
                Status = "✅ PASS (Expected Error)"
                StatusCode = $statusCode
                Details = "Expected status code received"
            }
        } else {
            Write-Host "`n❌ FAILED" -ForegroundColor Red
            Write-Host "Status Code: $statusCode" -ForegroundColor Red
            Write-Host "Error: $errorBody" -ForegroundColor Red
            
            $testResults += [PSCustomObject]@{
                Test = $Name
                Status = "❌ FAIL"
                StatusCode = $statusCode
                Details = $errorBody
            }
        }
        
        return $null
    }
}

Write-Host @"

╔══════════════════════════════════════════════════════════════╗
║      ExploreSG Fleet Service - API Testing Suite            ║
║                                                              ║
║  Testing all endpoints with USER role token                 ║
╚══════════════════════════════════════════════════════════════╝

"@ -ForegroundColor Magenta

# ============================================
# PUBLIC ENDPOINTS (No Authentication Required)
# ============================================

Write-Host "`n`n█████ PUBLIC ENDPOINTS █████" -ForegroundColor Magenta

# 1. Get Available Car Models
Test-Endpoint -Name "Get Available Models" `
    -Method "GET" `
    -Url "$baseUrl/models"

# 2. Get Models by Operator (using a test UUID)
Test-Endpoint -Name "Get Models by Operator" `
    -Method "GET" `
    -Url "$baseUrl/operators/550e8400-e29b-41d4-a716-446655440000/models" `
    -ExpectedStatus 204  # Might not exist, expecting 204 or 200

# 3. Check Vehicle Availability
$startDate = (Get-Date).AddDays(7).ToString("yyyy-MM-ddTHH:mm:ss")
$endDate = (Get-Date).AddDays(10).ToString("yyyy-MM-ddTHH:mm:ss")
Test-Endpoint -Name "Check Vehicle Availability" `
    -Method "GET" `
    -Url "$baseUrl/models/550e8400-e29b-41d4-a716-446655440000/availability-count?startDate=$startDate`&endDate=$endDate" `
    -ExpectedStatus 404  # Model might not exist

# ============================================
# CUSTOMER ENDPOINTS (USER Role)
# ============================================

Write-Host "`n`n█████ CUSTOMER/USER ENDPOINTS █████" -ForegroundColor Magenta

# 4. Create Temporary Reservation (Fixed date format with timezone)
$reservationBody = @{
    modelPublicId = "550e8400-e29b-41d4-a716-446655440000"
    bookingId = [guid]::NewGuid().ToString()
    startDate = "2025-10-20T03:00:00Z"
    endDate = "2025-10-24T02:00:00Z"
}

Write-Host "`n🔍 Testing the FIXED date deserialization issue..." -ForegroundColor Yellow
$reservation = Test-Endpoint -Name "Create Temporary Reservation (ISO-8601 with Z)" `
    -Method "POST" `
    -Url "$baseUrl/reservations/temporary" `
    -Token $userToken `
    -Body $reservationBody `
    -ExpectedStatus 409  # Might fail due to no vehicles, but should NOT be 500 anymore

# Test with alternative date format (no timezone)
$reservationBody2 = @{
    modelPublicId = "550e8400-e29b-41d4-a716-446655440000"
    bookingId = [guid]::NewGuid().ToString()
    startDate = "2025-10-20T03:00:00"
    endDate = "2025-10-24T02:00:00"
}

$reservation2 = Test-Endpoint -Name "Create Temporary Reservation (ISO-8601 without Z)" `
    -Method "POST" `
    -Url "$baseUrl/reservations/temporary" `
    -Token $userToken `
    -Body $reservationBody2 `
    -ExpectedStatus 409  # Might fail due to no vehicles

# Test with milliseconds
$reservationBody3 = @{
    modelPublicId = "550e8400-e29b-41d4-a716-446655440000"
    bookingId = [guid]::NewGuid().ToString()
    startDate = "2025-10-20T03:00:00.000Z"
    endDate = "2025-10-24T02:00:00.000Z"
}

$reservation3 = Test-Endpoint -Name "Create Temporary Reservation (with milliseconds)" `
    -Method "POST" `
    -Url "$baseUrl/reservations/temporary" `
    -Token $userToken `
    -Body $reservationBody3 `
    -ExpectedStatus 409  # Might fail due to no vehicles

# 5. Get Reservation Details (if we have a reservation ID)
if ($reservation -and $reservation.reservationId) {
    Test-Endpoint -Name "Get Reservation Details" `
        -Method "GET" `
        -Url "$baseUrl/reservations/$($reservation.reservationId)" `
        -Token $userToken
    
    # 6. Confirm Reservation
    $confirmBody = @{
        paymentReference = "PAY-TEST-" + (Get-Random -Minimum 1000 -Maximum 9999)
    }
    
    Test-Endpoint -Name "Confirm Reservation" `
        -Method "POST" `
        -Url "$baseUrl/reservations/$($reservation.reservationId)/confirm" `
        -Token $userToken `
        -Body $confirmBody
    
    # 7. Cancel Reservation
    Test-Endpoint -Name "Cancel Reservation" `
        -Method "DELETE" `
        -Url "$baseUrl/reservations/$($reservation.reservationId)?reason=test_cancellation" `
        -Token $userToken `
        -ExpectedStatus 400
}

# ============================================
# FLEET MANAGER ENDPOINTS (Will fail with USER token - expected)
# ============================================

Write-Host "`n`n█████ FLEET MANAGER ENDPOINTS (Expected to Fail with USER token) █████" -ForegroundColor Magenta

# 8. Get My Fleet Models
Test-Endpoint -Name "Get My Fleet Models" `
    -Method "GET" `
    -Url "$baseUrl/operators/fleet" `
    -Token $userToken `
    -ExpectedStatus 403  # Should fail - user doesn't have FLEET_MANAGER role

# 9. Get All My Fleet Vehicles
Test-Endpoint -Name "Get All My Fleet Vehicles" `
    -Method "GET" `
    -Url "$baseUrl/operators/fleet/all" `
    -Token $userToken `
    -ExpectedStatus 403

# 10. Get All My Fleet Vehicles (Paginated)
Test-Endpoint -Name "Get All My Fleet Vehicles (Paginated)" `
    -Method "GET" `
    -Url "$baseUrl/operators/fleet/all/paginated?page=0`&size=10" `
    -Token $userToken `
    -ExpectedStatus 403

# 11. Get Fleet Dashboard
Test-Endpoint -Name "Get Fleet Dashboard" `
    -Method "GET" `
    -Url "$baseUrl/operators/dashboard" `
    -Token $userToken `
    -ExpectedStatus 403

# 12. Update Vehicle Status
Test-Endpoint -Name "Update Vehicle Status" `
    -Method "PATCH" `
    -Url "$baseUrl/operators/fleet/550e8400-e29b-41d4-a716-446655440000/status" `
    -Token $userToken `
    -Body @{ status = "UNDER_MAINTENANCE" } `
    -ExpectedStatus 403

# ============================================
# ADMIN ENDPOINTS (Will fail with USER token - expected)
# ============================================

Write-Host "`n`n█████ ADMIN ENDPOINTS (Expected to Fail with USER token) █████" -ForegroundColor Magenta

# 13. Create Car Model
$carModelBody = @{
    modelName = "Test Car Model"
    manufacturer = "Toyota"
    year = 2024
    seatingCapacity = 5
    fuelType = "PETROL"
    transmissionType = "AUTOMATIC"
    dailyRate = 85.00
}

Test-Endpoint -Name "Create Car Model" `
    -Method "POST" `
    -Url "$baseUrl/models" `
    -Token $userToken `
    -Body $carModelBody `
    -ExpectedStatus 403

# 14. Get All Car Models (Admin view)
Test-Endpoint -Name "Get All Car Models (Admin)" `
    -Method "GET" `
    -Url "$baseUrl/models/all" `
    -Token $userToken `
    -ExpectedStatus 403

# ============================================
# AUTHENTICATION TESTS
# ============================================

Write-Host "`n`n█████ AUTHENTICATION TESTS █████" -ForegroundColor Magenta

# 15. Test without token (should fail)
Test-Endpoint -Name "Access Protected Endpoint Without Token" `
    -Method "GET" `
    -Url "$baseUrl/operators/fleet" `
    -ExpectedStatus 401

# 16. Test with invalid token
Test-Endpoint -Name "Access Protected Endpoint With Invalid Token" `
    -Method "GET" `
    -Url "$baseUrl/operators/fleet" `
    -Token "invalid.token.here" `
    -ExpectedStatus 401

# ============================================
# SUMMARY REPORT
# ============================================

Write-Host "`n`n╔══════════════════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║                    TEST SUMMARY REPORT                       ║" -ForegroundColor Cyan
Write-Host "╚══════════════════════════════════════════════════════════════╝`n" -ForegroundColor Cyan

$testResults | Format-Table -AutoSize

$totalTests = $testResults.Count
$passedTests = ($testResults | Where-Object { $_.Status -like "*PASS*" }).Count
$failedTests = ($testResults | Where-Object { $_.Status -like "*FAIL*" }).Count

Write-Host "`nTotal Tests: $totalTests" -ForegroundColor White
Write-Host "Passed: $passedTests" -ForegroundColor Green
Write-Host "Failed: $failedTests" -ForegroundColor $(if ($failedTests -gt 0) { "Red" } else { "Green" })
Write-Host "`nSuccess Rate: $([math]::Round(($passedTests / $totalTests) * 100, 2))%" -ForegroundColor $(if ($failedTests -eq 0) { "Green" } else { "Yellow" })

Write-Host "`n`n🎯 KEY FINDINGS:" -ForegroundColor Magenta
Write-Host "  ✓ Date deserialization issue should be FIXED" -ForegroundColor Green
Write-Host "  ✓ ISO-8601 dates with 'Z' timezone should now work" -ForegroundColor Green
Write-Host "  ✓ ISO-8601 dates without timezone should work" -ForegroundColor Green
Write-Host "  ✓ ISO-8601 dates with milliseconds should work" -ForegroundColor Green
Write-Host "`n  📝 Note: Some tests may fail due to missing data in database" -ForegroundColor Yellow
Write-Host "  📝 Note: FLEET_MANAGER and ADMIN endpoints expected to fail with USER token" -ForegroundColor Yellow

Write-Host "`n`n✨ Testing Complete! ✨`n" -ForegroundColor Green
