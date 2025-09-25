#!/bin/bash

# Start Ethereum Mainnet Full Stack
# Deploys the complete Ethereum node stack with monitoring

set -e

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${BLUE}🚀 Starting Ethereum Mainnet Full Stack${NC}"
echo "=============================================="

# Check if Docker is running
if ! docker info >/dev/null 2>&1; then
    echo -e "${RED}❌ Docker is not running. Please start Docker first.${NC}"
    exit 1
fi

# Check if data directories exist
echo -e "${YELLOW}📁 Checking data directories...${NC}"
if [ ! -d "/mnt/sata18tb/erigon-hot" ]; then
    echo -e "${YELLOW}⚠️  ERIGON data directory not found. Creating...${NC}"
    sudo mkdir -p /mnt/sata18tb/erigon-hot
    sudo chown -R 1000:1000 /mnt/sata18tb/erigon-hot
fi

if [ ! -d "/mnt/sata18tb/lighthouse-data" ]; then
    echo -e "${YELLOW}⚠️  Lighthouse data directory not found. Creating...${NC}"
    sudo mkdir -p /mnt/sata18tb/lighthouse-data
    sudo chown -R 1000:1000 /mnt/sata18tb/lighthouse-data
fi

# Create config directories if they don't exist
mkdir -p ./ethereum-full-archive/erigon-config
mkdir -p ./ethereum-full-archive/lighthouse-config
mkdir -p ./ethereum-full-archive/prometheus
mkdir -p ./ethereum-full-archive/grafana/provisioning
mkdir -p ./ethereum-full-archive/grafana/dashboards

# Generate JWT secret if it doesn't exist
if [ ! -f "./ethereum-full-archive/erigon-config/jwt.hex" ]; then
    echo -e "${YELLOW}🔐 Generating JWT secret...${NC}"
    openssl rand -hex 32 > ./ethereum-full-archive/erigon-config/jwt.hex
    chmod 600 ./ethereum-full-archive/erigon-config/jwt.hex
fi

# Start the stack
echo -e "${BLUE}🐳 Starting Docker containers...${NC}"
docker-compose -f ethereum-mainnet-full-stack.yml -p ETHEREUM_mainnet_full up -d

# Wait for services to start
echo -e "${YELLOW}⏳ Waiting for services to start...${NC}"
sleep 30

# Check service status
echo -e "${BLUE}📊 Service Status:${NC}"
echo "=================="

services=("ETHEREUM_mainnet_full-erigon" "ETHEREUM_mainnet_full-lighthouse" "ETHEREUM_mainnet_full-prometheus" "ETHEREUM_mainnet_full-grafana")

for service in "${services[@]}"; do
    if docker ps --format "table {{.Names}}" | grep -q "^${service}$"; then
        echo -e "${GREEN}✅ ${service}: Running${NC}"
    else
        echo -e "${RED}❌ ${service}: Not running${NC}"
    fi
done

echo ""
echo -e "${GREEN}🎉 Ethereum Mainnet Full Stack Started!${NC}"
echo ""
echo -e "${BLUE}📋 Access Points:${NC}"
echo "=================="
echo -e "🌐 ERIGON RPC: http://localhost:8545"
echo -e "🌐 ERIGON WebSocket: ws://localhost:8546"
echo -e "🌐 Lighthouse API: http://localhost:5052"
echo -e "🌐 Prometheus: http://localhost:9091"
echo -e "🌐 Grafana: http://localhost:3001 (admin/admin123)"
echo ""
echo -e "${BLUE}🔧 Management Commands:${NC}"
echo "========================"
echo -e "📊 Check status: ./check-ethereum-stack.sh"
echo -e "📈 View logs: ./logs-ethereum-stack.sh"
echo -e "🛑 Stop stack: ./stop-ethereum-stack.sh"
echo -e "🔄 Restart stack: ./restart-ethereum-stack.sh"
echo ""
echo -e "${YELLOW}💡 Tips:${NC}"
echo "- Monitor sync progress with: ./lighthouse-progress.sh"
echo "- Check node health with: ./verify-node-status.sh"
echo "- View Grafana dashboards for detailed metrics"
