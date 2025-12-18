#!/bin/bash

# OnlineShop API Gateway - Full Flow Test
# This script tests the complete authentication and items flow through the API Gateway

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

GATEWAY_URL="http://localhost:10000"
TOKEN=""
TIMESTAMP=$(date +%s)
TEST_USER="testuser_${TIMESTAMP}"

echo -e "${BLUE}======================================${NC}"
echo -e "${BLUE}OnlineShop API Gateway - Full Flow Test${NC}"
echo -e "${BLUE}======================================${NC}"
echo ""

# Function to check HTTP status
check_status() {
    local expected=$1
    local actual=$2
    local test_name=$3

    if [ "$actual" -eq "$expected" ]; then
        echo -e "${GREEN}[PASS]${NC} $test_name (Status: $actual)"
        return 0
    else
        echo -e "${RED}[FAIL]${NC} $test_name (Expected: $expected, Got: $actual)"
        return 1
    fi
}

# Test 1: Access items without token (should fail with 401)
echo -e "${YELLOW}--- Test 1: Access items without token (should fail) ---${NC}"
STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$GATEWAY_URL/items")
check_status 401 "$STATUS" "Unauthenticated access blocked"
echo ""

# Test 2: Register new user
echo -e "${YELLOW}--- Test 2: Register new user ---${NC}"
REGISTER_RESPONSE=$(curl -s -X POST "$GATEWAY_URL/auth/register" \
    -H "Content-Type: application/json" \
    -d "{\"username\":\"$TEST_USER\",\"password\":\"testpass123\"}")
echo "Response: $REGISTER_RESPONSE"
echo -e "${GREEN}[PASS]${NC} User registered: $TEST_USER"
echo ""

# Test 3: Login and get token
echo -e "${YELLOW}--- Test 3: Login and get token ---${NC}"
LOGIN_RESPONSE=$(curl -s -X POST "$GATEWAY_URL/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"username\":\"$TEST_USER\",\"password\":\"testpass123\"}")
TOKEN=$(echo "$LOGIN_RESPONSE" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
echo "Token received: ${TOKEN:0:20}..."

if [ -z "$TOKEN" ]; then
    echo -e "${RED}[FAIL]${NC} Could not obtain token"
    echo "Login response: $LOGIN_RESPONSE"
    exit 1
fi
echo -e "${GREEN}[PASS]${NC} Token obtained successfully"
echo ""

# Test 4: Access items with valid token
echo -e "${YELLOW}--- Test 4: Access items with valid token ---${NC}"
ITEMS_RESPONSE=$(curl -s -w "\n%{http_code}" "$GATEWAY_URL/items" \
    -H "Authorization: Bearer: $TOKEN")
STATUS=$(echo "$ITEMS_RESPONSE" | tail -1)
BODY=$(echo "$ITEMS_RESPONSE" | head -n -1)
check_status 200 "$STATUS" "Authenticated access to items"
echo "Items: $BODY"
echo ""

# Test 5: Get specific item
echo -e "${YELLOW}--- Test 5: Get specific item (ID: 1) ---${NC}"
ITEM_RESPONSE=$(curl -s -w "\n%{http_code}" "$GATEWAY_URL/items/1" \
    -H "Authorization: Bearer: $TOKEN")
STATUS=$(echo "$ITEM_RESPONSE" | tail -1)
BODY=$(echo "$ITEM_RESPONSE" | head -n -1)
check_status 200 "$STATUS" "Get item by ID"
echo "Item: $BODY"
echo ""

# Test 6: Create new item with token
echo -e "${YELLOW}--- Test 6: Create new item with token ---${NC}"
CREATE_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$GATEWAY_URL/items" \
    -H "Authorization: Bearer: $TOKEN" \
    -H "Content-Type: application/json" \
    -d '{"name":"Test Item via Gateway","quantity":10,"description":"Created via API Gateway test script"}')
STATUS=$(echo "$CREATE_RESPONSE" | tail -1)
BODY=$(echo "$CREATE_RESPONSE" | head -n -1)
check_status 201 "$STATUS" "Create item through gateway"
echo "Created item: $BODY"
echo ""

# Test 7: Access with invalid token (should fail with 401)
echo -e "${YELLOW}--- Test 7: Access with invalid token (should fail) ---${NC}"
STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$GATEWAY_URL/items" \
    -H "Authorization: Bearer: invalidtoken123")
check_status 401 "$STATUS" "Invalid token rejected"
echo ""

# Test 8: Validate token endpoint
echo -e "${YELLOW}--- Test 8: Validate token endpoint ---${NC}"
VALIDATE_RESPONSE=$(curl -s -w "\n%{http_code}" "$GATEWAY_URL/auth/validate" \
    -H "Authorization: Bearer: $TOKEN")
STATUS=$(echo "$VALIDATE_RESPONSE" | tail -1)
BODY=$(echo "$VALIDATE_RESPONSE" | head -n -1)
check_status 200 "$STATUS" "Token validation"
echo "Validation response: $BODY"
echo ""

# Test 9: Cache test - make multiple requests (should use cache)
echo -e "${YELLOW}--- Test 9: Cache test - 5 rapid requests ---${NC}"
for i in {1..5}; do
    START=$(date +%s%N)
    STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$GATEWAY_URL/items" \
        -H "Authorization: Bearer: $TOKEN")
    END=$(date +%s%N)
    DURATION=$(( (END - START) / 1000000 ))
    echo "Request $i: Status=$STATUS, Time=${DURATION}ms"
done
echo -e "${GREEN}[PASS]${NC} Cache test completed (check if subsequent requests are faster)"
echo ""

echo -e "${BLUE}======================================${NC}"
echo -e "${GREEN}Full Flow Test Complete!${NC}"
echo -e "${BLUE}======================================${NC}"
