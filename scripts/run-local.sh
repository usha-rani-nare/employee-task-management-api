#!/bin/bash
# quick script so I don't have to remember the docker-compose flags every time
set -e

echo "Building and starting containers..."
docker-compose up --build -d

echo "Waiting for the app to be healthy..."
sleep 15

STATUS=$(curl -s http://localhost:8080/actuator/health | grep -o '"status":"[A-Z]*"' || echo "not reachable")
echo "Health check: $STATUS"

echo "App running at http://localhost:8080"
echo "Logs: docker logs -f taskmanager-app"
