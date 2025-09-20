#!/bin/bash

# DEFIMON Services Stop Script

set -e

echo "🛑 Stopping DEFIMON microservices..."

# Stop all services
docker-compose down

# Optional: Remove volumes (uncomment if you want to clear all data)
# echo "🗑️  Removing volumes..."
# docker-compose down -v

# Optional: Remove images (uncomment if you want to remove built images)
# echo "🗑️  Removing images..."
# docker-compose down --rmi all

echo "✅ All services stopped successfully!"
echo ""
echo "💡 To start services again: ./start-services.sh"
echo "🔧 To rebuild services: docker-compose build"
echo "📊 To check status: docker-compose ps"
