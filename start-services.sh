#!/bin/bash

# DEFIMON Services Startup Script

set -e

echo "🚀 Starting DEFIMON microservices architecture..."

# Check if Docker and Docker Compose are installed
if ! command -v docker &> /dev/null; then
    echo "❌ Docker is not installed. Please install Docker first."
    exit 1
fi

if ! command -v docker-compose &> /dev/null; then
    echo "❌ Docker Compose is not installed. Please install Docker Compose first."
    exit 1
fi

# Create environment file if it doesn't exist
if [ ! -f .env ]; then
    echo "📝 Creating .env file from template..."
    cp .env.example .env
    echo "⚠️  Please update the .env file with your configuration before running the services."
    echo "   Especially update the ETH_RPC_URL with your Infura project ID."
fi

# Build all services
echo "🔨 Building all services..."
docker-compose build

# Start infrastructure services first
echo "🗄️  Starting infrastructure services (Kafka, MongoDB)..."
docker-compose up -d zookeeper kafka mongodb

# Wait for Kafka to be ready
echo "⏳ Waiting for Kafka to be ready..."
sleep 30

# Start application services
echo "🎯 Starting application services..."
docker-compose up -d api-gateway blockchain-sync transaction-signing math-computing data-aggregation

# Show service status
echo "📊 Service Status:"
docker-compose ps

echo ""
echo "✅ All services started successfully!"
echo ""
echo "🌐 API Gateway: http://localhost:8080"
echo "📈 Health checks:"
echo "   - API Gateway: http://localhost:8080/health"
echo "   - Blockchain Sync: http://localhost:8081/health"
echo "   - Transaction Signing: http://localhost:8082/health"
echo "   - Math Computing: http://localhost:8083/health"
echo "   - Data Aggregation: http://localhost:8084/health"
echo ""
echo "📝 To view logs: docker-compose logs -f [service-name]"
echo "🛑 To stop services: docker-compose down"
echo ""
echo "🔧 Available API endpoints:"
echo "   - POST /api/transactions/sign - Sign transactions"
echo "   - POST /api/calculations/option-price - Calculate option prices"
echo "   - POST /api/calculations/arbitrage - Find arbitrage opportunities"
echo "   - GET /api/data/price-history?symbol=ETH&timeframe=1h - Get price history"
echo "   - GET /api/data/market-summary - Get market summary"
