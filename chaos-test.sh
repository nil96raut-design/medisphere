#!/bin/bash

echo "========================================="
echo "   HEALTH TRACK - CHAOS TEST SCRIPT      "
echo "========================================="

BASE_URL="http://localhost:8080/api"

echo "Please ensure you have a valid token (requires pharmacist or admin role) and set it via: export TOKEN=your_token_here"
if [ -z "$TOKEN" ]; then
  echo "Error: TOKEN environment variable is not set."
  echo "You can get one by calling POST /api/auth/login"
  exit 1
fi

if [ -z "$PATIENT_ID" ]; then
  echo "Warning: PATIENT_ID is not set, using 1"
  PATIENT_ID=1
fi

MEDICINE_NAME="Aspirin"

echo "Attempting concurrent dispensation for $MEDICINE_NAME..."
echo "Simulating 5 simultaneous requests..."

# We fire off 5 curl requests in the background
for i in {1..5}
do
  curl -s -X POST "$BASE_URL/pharmacy/dispense" \
       -H "Authorization: Bearer $TOKEN" \
       -H "Content-Type: application/json" \
       -d "{\"patientId\": $PATIENT_ID, \"medicineName\": \"$MEDICINE_NAME\", \"quantity\": 2}" &
done

wait
echo "Done."
