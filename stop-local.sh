#!/bin/bash
# ==============================================================================
# CARRY - Stop Local Servers
# ==============================================================================

echo "🛑 Stopping CARRY local servers..."

if lsof -t -i:8080 > /dev/null 2>&1; then
  kill $(lsof -t -i:8080)
  echo "✅ Stopped Backend on port 8080."
else
  echo "ℹ️ Backend was not running."
fi

if lsof -t -i:5173 > /dev/null 2>&1; then
  kill $(lsof -t -i:5173)
  echo "✅ Stopped Frontend on port 5173."
else
  echo "ℹ️ Frontend was not running."
fi

echo "All CARRY services stopped."
