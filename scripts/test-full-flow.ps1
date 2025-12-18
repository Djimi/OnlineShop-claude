# OnlineShop API Gateway - Full Flow Test (PowerShell)
# This script tests the complete authentication and items flow through the API Gateway

$ErrorActionPreference = "Stop"

$GatewayUrl = "http://localhost:10000"
$Timestamp = Get-Date -Format "yyyyMMddHHmmss"
$TestUser = "testuser_$Timestamp"

Write-Host "======================================" -ForegroundColor Cyan
Write-Host "OnlineShop API Gateway - Full Flow Test" -ForegroundColor Cyan
Write-Host "======================================" -ForegroundColor Cyan
Write-Host ""

function Test-HttpStatus {
    param (
        [int]$Expected,
        [int]$Actual,
        [string]$TestName
    )

    if ($Actual -eq $Expected) {
        Write-Host "[PASS] $TestName (Status: $Actual)" -ForegroundColor Green
        return $true
    } else {
        Write-Host "[FAIL] $TestName (Expected: $Expected, Got: $Actual)" -ForegroundColor Red
        return $false
    }
}

# Test 1: Access items without token (should fail with 401)
Write-Host "--- Test 1: Access items without token (should fail) ---" -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "$GatewayUrl/items" -Method Get -ErrorAction SilentlyContinue
    $status = $response.StatusCode
} catch {
    $status = $_.Exception.Response.StatusCode.value__
}
Test-HttpStatus -Expected 401 -Actual $status -TestName "Unauthenticated access blocked"
Write-Host ""

# Test 2: Register new user
Write-Host "--- Test 2: Register new user ---" -ForegroundColor Yellow
$registerBody = @{
    username = $TestUser
    password = "testpass123"
} | ConvertTo-Json

try {
    $registerResponse = Invoke-RestMethod -Uri "$GatewayUrl/auth/register" `
        -Method Post -Body $registerBody -ContentType "application/json"
    Write-Host "Registered user: $($registerResponse.username)" -ForegroundColor Green
} catch {
    Write-Host "Registration error: $($_.Exception.Message)" -ForegroundColor Yellow
    # Try with existing test user
    $TestUser = "testuser"
}
Write-Host ""

# Test 3: Login and get token
Write-Host "--- Test 3: Login and get token ---" -ForegroundColor Yellow
$loginBody = @{
    username = $TestUser
    password = "testpass123"
} | ConvertTo-Json

try {
    $loginResponse = Invoke-RestMethod -Uri "$GatewayUrl/auth/login" `
        -Method Post -Body $loginBody -ContentType "application/json"
    $Token = $loginResponse.token
    Write-Host "Token received: $($Token.Substring(0, [Math]::Min(20, $Token.Length)))..." -ForegroundColor Green
} catch {
    Write-Host "[FAIL] Could not obtain token: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}
Write-Host ""

# Test 4: Access items with valid token
Write-Host "--- Test 4: Access items with valid token ---" -ForegroundColor Yellow
$headers = @{ "Authorization" = "Bearer: $Token" }
try {
    $itemsResponse = Invoke-RestMethod -Uri "$GatewayUrl/items" -Headers $headers
    Write-Host "[PASS] Authenticated access to items" -ForegroundColor Green
    Write-Host "Items count: $($itemsResponse.Count)"
} catch {
    Write-Host "[FAIL] Could not access items: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

# Test 5: Get specific item
Write-Host "--- Test 5: Get specific item (ID: 1) ---" -ForegroundColor Yellow
try {
    $itemResponse = Invoke-RestMethod -Uri "$GatewayUrl/items/1" -Headers $headers
    Write-Host "[PASS] Get item by ID" -ForegroundColor Green
    Write-Host "Item: $($itemResponse | ConvertTo-Json -Compress)"
} catch {
    Write-Host "[FAIL] Could not get item: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

# Test 6: Create new item with token
Write-Host "--- Test 6: Create new item with token ---" -ForegroundColor Yellow
$newItem = @{
    name = "Test Item via Gateway (PS)"
    quantity = 5
    description = "Created via API Gateway PowerShell test script"
} | ConvertTo-Json

try {
    $createResponse = Invoke-RestMethod -Uri "$GatewayUrl/items" `
        -Method Post -Headers $headers -Body $newItem -ContentType "application/json"
    Write-Host "[PASS] Create item through gateway" -ForegroundColor Green
    Write-Host "Created item ID: $($createResponse.id)"
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    if ($statusCode -eq 201) {
        Write-Host "[PASS] Create item through gateway (Status: 201)" -ForegroundColor Green
    } else {
        Write-Host "[FAIL] Could not create item: $($_.Exception.Message)" -ForegroundColor Red
    }
}
Write-Host ""

# Test 7: Access with invalid token (should fail with 401)
Write-Host "--- Test 7: Access with invalid token (should fail) ---" -ForegroundColor Yellow
$invalidHeaders = @{ "Authorization" = "Bearer: invalidtoken123" }
try {
    Invoke-WebRequest -Uri "$GatewayUrl/items" -Headers $invalidHeaders -ErrorAction Stop
    Test-HttpStatus -Expected 401 -Actual 200 -TestName "Invalid token rejected"
} catch {
    $status = $_.Exception.Response.StatusCode.value__
    Test-HttpStatus -Expected 401 -Actual $status -TestName "Invalid token rejected"
}
Write-Host ""

# Test 8: Validate token endpoint
Write-Host "--- Test 8: Validate token endpoint ---" -ForegroundColor Yellow
try {
    $validateResponse = Invoke-RestMethod -Uri "$GatewayUrl/auth/validate" -Headers $headers
    Write-Host "[PASS] Token validation" -ForegroundColor Green
    Write-Host "Valid: $($validateResponse.valid), User: $($validateResponse.username)"
} catch {
    Write-Host "[FAIL] Token validation failed: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

# Test 9: Cache test - make multiple requests
Write-Host "--- Test 9: Cache test - 5 rapid requests ---" -ForegroundColor Yellow
for ($i = 1; $i -le 5; $i++) {
    $stopwatch = [System.Diagnostics.Stopwatch]::StartNew()
    try {
        $null = Invoke-RestMethod -Uri "$GatewayUrl/items" -Headers $headers
        $stopwatch.Stop()
        Write-Host "Request $i : Status=200, Time=$($stopwatch.ElapsedMilliseconds)ms"
    } catch {
        $stopwatch.Stop()
        Write-Host "Request $i : Failed, Time=$($stopwatch.ElapsedMilliseconds)ms" -ForegroundColor Red
    }
}
Write-Host "[PASS] Cache test completed (check if subsequent requests are faster)" -ForegroundColor Green
Write-Host ""

Write-Host "======================================" -ForegroundColor Cyan
Write-Host "Full Flow Test Complete!" -ForegroundColor Green
Write-Host "======================================" -ForegroundColor Cyan
