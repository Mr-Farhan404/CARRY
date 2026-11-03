#!/bin/bash
# ==============================================================================
# CARRY - One-Command Local Startup Script
# ==============================================================================

set -e

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
export JAVA_HOME="/opt/homebrew/opt/openjdk@21"
export PATH="$JAVA_HOME/bin:$PATH"

echo "=========================================================="
echo "🚀 Starting CARRY Platform Locally..."
echo "=========================================================="

# 1. Ensure MySQL is running via Homebrew
echo "📦 [1/3] Checking MySQL service..."
if brew services list | grep -q "mysql.*started"; then
  echo "✅ MySQL is already running."
else
  echo "⚙️ Starting MySQL..."
  brew services start mysql
  sleep 2
fi

# 2. Check and start Spring Boot backend on port 8080
echo "☕ [2/3] Checking Backend (port 8080)..."
if lsof -i :8080 > /dev/null 2>&1; then
  echo "✅ Backend is already running on http://localhost:8080"
else
  echo "⚙️ Starting Backend (Spring Boot)..."
  cd "$PROJECT_DIR/backend"
  if [ -f "target/carry-backend-0.0.1-SNAPSHOT.jar" ]; then
    nohup java -jar target/carry-backend-0.0.1-SNAPSHOT.jar > "$PROJECT_DIR/backend.log" 2>&1 &
  else
    nohup ./mvnw spring-boot:run > "$PROJECT_DIR/backend.log" 2>&1 &
  fi
  echo "⏳ Waiting for Backend to be ready..."
  for i in {1..30}; do
    if curl -s http://localhost:8080/api/health | grep -q "ok"; then
      echo "✅ Backend is live on http://localhost:8080"
      break
    fi
    sleep 1
  done
fi

# 3. Start Frontend on port 5173
echo "⚛️ [3/3] Checking Frontend (port 5173)..."
cd "$PROJECT_DIR/frontend"

# Ensure frontend .env points to local backend
echo "VITE_API_BASE_URL=http://localhost:8080" > .env

if lsof -i :5173 > /dev/null 2>&1; then
  echo "✅ Frontend is already running on http://localhost:5173"
else
  echo "⚙️ Starting Frontend..."
  nohup npm run dev > "$PROJECT_DIR/frontend.log" 2>&1 &
  sleep 2
  echo "✅ Frontend is live on http://localhost:5173"
fi

echo "=========================================================="
echo "🎉 CARRY Platform is running locally!"
echo "👉 App URL:     http://localhost:5173"
echo "👉 Shop URL:    http://localhost:5173/shop"
echo "👉 Backend API: http://localhost:8080/api/health"
echo "=========================================================="
echo "To stop them later, run:"
echo "kill \$(lsof -t -i:8080) \$(lsof -t -i:5173)"
echo "=========================================================="
