# ============================================
# Fleet Service - Date Deserialization Fix Test
# ============================================
# This script tests the FIX for the date deserialization bug
# where ISO-8601 dates with timezone (Z) were causing 500 errors
# ============================================

Write-Host "`n" -NoNewline
Write-Host "╔══════════════════════════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║  Fleet Service - Date Deserialization Bug Fix Verification Test     ║" -ForegroundColor Cyan
Write-Host "║                                                                      ║" -ForegroundColor Cyan
Write-Host "║  Testing ISO-8601 date formats from Booking Service                 ║" -ForegroundColor Cyan
Write-Host "╚══════════════════════════════════════════════════════════════════════╝" -ForegroundColor Cyan
Write-Host ""

$baseUrl = "http://localhost:8080/api/v1/fleet"
$testsPassed = 0
$testsFailed = 0

# Test Function
function Test-DateFormat {
    param(
        [string]$TestName,
        [string]$StartDate,
        [string]$EndDate,
        [string]$Description
    )
    
    Write-Host "`n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Gray
    Write-Host "TEST: $TestName" -ForegroundColor Yellow
    Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Gray
    Write-Host "Description: $Description" -ForegroundColor White
    Write-Host ""
    
    $body = @{
        modelPublicId = [guid]::NewGuid().ToString()
        bookingId = [guid]::NewGuid().ToString()
        startDate = $StartDate
        endDate = $EndDate
    } | ConvertTo-Json
    
    Write-Host "Request Payload:" -ForegroundColor Cyan
    Write-Host $body -ForegroundColor Gray
    Write-Host ""
    
    try {
        $response = Invoke-WebRequest `
            -Uri "$baseUrl/reservations/temporary" `
            -Method POST `
            -Headers @{ "Content-Type" = "application/json" } `
            -Body $body `
            -ErrorAction Stop
        
        Write-Host "✅ SUCCESS - HTTP $($response.StatusCode)" -ForegroundColor Green
        Write-Host "Response:" -ForegroundColor Cyan
        $response.Content | Write-Host -ForegroundColor Gray
        $script:testsPassed++
        return $true
        
    } catch {
        $statusCode = $_.Exception.Response.StatusCode.value__
        $errorBody = $_.ErrorDetails.Message
        
        # Check if this is the date deserialization error (the bug we're fixing)
        if ($statusCode -eq 500 -and $errorBody -like "*MismatchedInputException*" -and $errorBody -like "*startDate*") {
            Write-Host "❌ BUG STILL EXISTS - Date Deserialization Failed!" -ForegroundColor Red
            Write-Host "Status Code: 500 Internal Server Error" -ForegroundColor Red
            Write-Host "Error: Date format not accepted by Jackson deserializer" -ForegroundColor Red
            Write-Host ""
            Write-Host "Error Details:" -ForegroundColor Red
            Write-Host $errorBody -ForegroundColor DarkRed
            $script:testsFailed++
            return $false
        }
        # Expected errors (business logic failures are OK - means date was parsed successfully)
        elseif ($statusCode -eq 409) {
            Write-Host "✅ Date Format ACCEPTED (No vehicles available - expected)" -ForegroundColor Yellow
            Write-Host "Status Code: 409 Conflict" -ForegroundColor Yellow
            Write-Host "This means the date was successfully deserialized!" -ForegroundColor Green
            $script:testsPassed++
            return $true
        }
        elseif ($statusCode -eq 404) {
            Write-Host "✅ Date Format ACCEPTED (Model not found - expected)" -ForegroundColor Yellow
            Write-Host "Status Code: 404 Not Found" -ForegroundColor Yellow
            Write-Host "This means the date was successfully deserialized!" -ForegroundColor Green
            $script:testsPassed++
            return $true
        }
        elseif ($statusCode -eq 400) {
            # Check if it's a date validation error vs deserialization error
            if ($errorBody -like "*date*" -and $errorBody -notlike "*MismatchedInputException*") {
                Write-Host "✅ Date Format ACCEPTED (Validation failed - expected)" -ForegroundColor Yellow
                Write-Host "Status Code: 400 Bad Request" -ForegroundColor Yellow
                Write-Host "Date was deserialized but failed business validation" -ForegroundColor Yellow
                $script:testsPassed++
                return $true
            } else {
                Write-Host "⚠️  Unexpected 400 error" -ForegroundColor DarkYellow
                Write-Host $errorBody -ForegroundColor Gray
                $script:testsFailed++
                return $false
            }
        }
        else {
            Write-Host "⚠️  Unexpected Status Code: $statusCode" -ForegroundColor DarkYellow
            Write-Host "Error: $errorBody" -ForegroundColor Gray
            $script:testsFailed++
            return $false
        }
    }
}

Write-Host "Starting Date Deserialization Tests..." -ForegroundColor Cyan
Write-Host ""
Write-Host "These tests simulate the exact payload sent by Booking Service" -ForegroundColor White
Write-Host ""

# TEST 1: ISO-8601 with timezone indicator (Z) - THE PROBLEMATIC FORMAT FROM BOOKING SERVICE
Test-DateFormat `
    -TestName "ISO-8601 WITH TIMEZONE (Z)" `
    -StartDate "2025-10-20T03:00:00Z" `
    -EndDate "2025-10-24T02:00:00Z" `
    -Description "This is the EXACT format sent by Booking Service that was causing 500 errors"

# TEST 2: ISO-8601 without timezone - Alternative format
Test-DateFormat `
    -TestName "ISO-8601 WITHOUT TIMEZONE" `
    -StartDate "2025-10-20T03:00:00" `
    -EndDate "2025-10-24T02:00:00" `
    -Description "Standard ISO-8601 format without timezone indicator"

# TEST 3: ISO-8601 with milliseconds and timezone
Test-DateFormat `
    -TestName "ISO-8601 WITH MILLISECONDS AND TIMEZONE" `
    -StartDate "2025-10-20T03:00:00.000Z" `
    -EndDate "2025-10-24T02:00:00.000Z" `
    -Description "ISO-8601 format with milliseconds and timezone"

# TEST 4: ISO-8601 with milliseconds, no timezone
Test-DateFormat `
    -TestName "ISO-8601 WITH MILLISECONDS, NO TIMEZONE" `
    -StartDate "2025-10-20T03:00:00.123" `
    -EndDate "2025-10-24T02:00:00.456" `
    -Description "ISO-8601 format with milliseconds but no timezone"

# TEST 5: ISO-8601 with offset timezone (+00:00)
Test-DateFormat `
    -TestName "ISO-8601 WITH OFFSET TIMEZONE" `
    -StartDate "2025-10-20T03:00:00+00:00" `
    -EndDate "2025-10-24T02:00:00+00:00" `
    -Description "ISO-8601 format with explicit +00:00 timezone offset"

# Summary Report
Write-Host "`n"
Write-Host "╔══════════════════════════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║                        TEST SUMMARY REPORT                           ║" -ForegroundColor Cyan
Write-Host "╚══════════════════════════════════════════════════════════════════════╝" -ForegroundColor Cyan
Write-Host ""

$totalTests = $testsPassed + $testsFailed
$successRate = if ($totalTests -gt 0) { [math]::Round(($testsPassed / $totalTests) * 100, 1) } else { 0 }

Write-Host "Total Tests Run:     $totalTests" -ForegroundColor White
Write-Host "Tests Passed:        $testsPassed" -ForegroundColor Green
Write-Host "Tests Failed:        $testsFailed" -ForegroundColor $(if ($testsFailed -gt 0) { "Red" } else { "Green" })
Write-Host "Success Rate:        $successRate%" -ForegroundColor $(if ($successRate -eq 100) { "Green" } elseif ($successRate -ge 80) { "Yellow" } else { "Red" })
Write-Host ""

if ($testsFailed -eq 0) {
    Write-Host "╔══════════════════════════════════════════════════════════════════════╗" -ForegroundColor Green
    Write-Host "║  ✅ ALL TESTS PASSED - DATE DESERIALIZATION BUG IS FIXED! ✅         ║" -ForegroundColor Green
    Write-Host "╚══════════════════════════════════════════════════════════════════════╝" -ForegroundColor Green
    Write-Host ""
    Write-Host "🎉 The Fleet Service can now accept ISO-8601 dates from Booking Service!" -ForegroundColor Green
    Write-Host "🎉 Integration between services should work correctly now!" -ForegroundColor Green
} else {
    Write-Host "╔══════════════════════════════════════════════════════════════════════╗" -ForegroundColor Red
    Write-Host "║  ❌ SOME TESTS FAILED - BUG MAY STILL EXIST ❌                       ║" -ForegroundColor Red
    Write-Host "╚══════════════════════════════════════════════════════════════════════╝" -ForegroundColor Red
    Write-Host ""
    Write-Host "⚠️  Please review the errors above and apply the recommended fixes" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Recommended Actions:" -ForegroundColor Yellow
    Write-Host "1. Ensure JacksonConfig.java registers JavaTimeModule" -ForegroundColor White
    Write-Host "2. Use LocalDateTimeDeserializer for LocalDateTime fields" -ForegroundColor White
    Write-Host "3. Consider using Instant instead of LocalDateTime for UTC timestamps" -ForegroundColor White
    Write-Host "4. Rebuild and restart the application: mvnw clean compile spring-boot:run" -ForegroundColor White
}

Write-Host ""
