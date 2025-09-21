#!/bin/bash

# Blockchain Sync Service Monitoring Startup Script
# This script starts the complete monitoring stack for the blockchain-sync-service

set -e

echo "🚀 Starting Blockchain Sync Service Monitoring Stack..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_header() {
    echo -e "${BLUE}================================${NC}"
    echo -e "${BLUE} $1${NC}"
    echo -e "${BLUE}================================${NC}"
}

# Check if docker-compose is available
if ! command -v docker-compose &> /dev/null; then
    print_error "docker-compose is not installed. Please install docker-compose first."
    exit 1
fi

# Check if Docker is running
if ! docker info &> /dev/null; then
    print_error "Docker is not running. Please start Docker first."
    exit 1
fi

print_header "BLOCKCHAIN SYNC SERVICE MONITORING SETUP"

# Create necessary directories
print_status "Creating monitoring directories..."
mkdir -p blockchain-sync-service/grafana/dashboards
mkdir -p blockchain-sync-service/grafana/provisioning/datasources
mkdir -p blockchain-sync-service/grafana/provisioning/dashboards
mkdir -p blockchain-sync-service/prometheus

# Check if .env file exists
if [ ! -f "blockchain-sync-service/.env" ]; then
    print_warning ".env file not found. Creating from env.example..."
    if [ -f "blockchain-sync-service/env.example" ]; then
        cp blockchain-sync-service/env.example blockchain-sync-service/.env
        print_warning "Please edit blockchain-sync-service/.env with your actual values before starting the services."
    else
        print_error "env.example file not found. Please create a .env file with required environment variables."
        exit 1
    fi
fi

# Start the monitoring stack
print_status "Starting monitoring services..."

# Start Prometheus and Grafana first
print_status "Starting Prometheus and Grafana..."
docker-compose up -d prometheus grafana

# Wait for services to be ready
print_status "Waiting for services to be ready..."
sleep 10

# Check if services are running
if docker-compose ps prometheus | grep -q "Up"; then
    print_status "✅ Prometheus is running on http://localhost:9091"
else
    print_error "❌ Prometheus failed to start"
    exit 1
fi

if docker-compose ps grafana | grep -q "Up"; then
    print_status "✅ Grafana is running on http://localhost:3000"
else
    print_error "❌ Grafana failed to start"
    exit 1
fi

# Start the blockchain sync service
print_status "Building and starting blockchain-sync-service..."
docker-compose up -d blockchain-sync

# Wait for blockchain sync service
sleep 5

if docker-compose ps blockchain-sync | grep -q "Up"; then
    print_status "✅ Blockchain Sync Service is running on port 9090 (metrics)"
else
    print_warning "⚠️  Blockchain Sync Service may not be fully started yet. Check logs with: docker-compose logs blockchain-sync"
fi

print_header "MONITORING STACK STARTED SUCCESSFULLY"

echo ""
print_status "🎉 Monitoring stack is now running!"
echo ""
echo -e "${GREEN}📊 Access URLs:${NC}"
echo -e "  • Grafana Dashboard: ${BLUE}http://localhost:3000${NC}"
echo -e "    Username: admin"
echo -e "    Password: defimon123"
echo ""
echo -e "  • Prometheus: ${BLUE}http://localhost:9091${NC}"
echo ""
echo -e "  • Blockchain Sync Metrics: ${BLUE}http://localhost:9090/metrics${NC}"
echo ""

echo -e "${GREEN}🔍 Useful Commands:${NC}"
echo -e "  • View logs: ${YELLOW}docker-compose logs -f blockchain-sync${NC}"
echo -e "  • Check service status: ${YELLOW}docker-compose ps${NC}"
echo -e "  • Stop all services: ${YELLOW}docker-compose down${NC}"
echo -e "  • Restart services: ${YELLOW}docker-compose restart${NC}"
echo ""

echo -e "${GREEN}📈 Dashboard Features:${NC}"
echo -e "  • Real-time block processing rate"
echo -e "  • Sync lag monitoring"
echo -e "  • Ethereum RPC performance"
echo -e "  • MongoDB operation metrics"
echo -e "  • Kafka message throughput"
echo -e "  • Service health status"
echo -e "  • DeFi protocol interactions"
echo -e "  • Gas price monitoring"
echo ""

echo -e "${GREEN}⚠️  Important Notes:${NC}"
echo -e "  • Make sure to configure your ETH_RPC_URL in the .env file"
echo -e "  • The dashboard will show data once the sync service starts processing blocks"
echo -e "  • Check Prometheus targets at http://localhost:9091/targets to ensure metrics are being collected"
echo ""

print_status "Happy monitoring! 🚀"
