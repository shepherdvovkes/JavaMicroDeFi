#!/bin/bash

# Test script to debug blockchain-sync-service

echo "🔍 Testing Blockchain Sync Service"
echo "=================================="

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}1. Checking Erigon RPC endpoint...${NC}"
if curl -s -X POST -H "Content-Type: application/json" --data '{"jsonrpc":"2.0","method":"eth_blockNumber","params":[],"id":1}' http://localhost:8545 | grep -q "result"; then
    echo -e "   ✅ Erigon RPC is working"
    BLOCK_NUM=$(curl -s -X POST -H "Content-Type: application/json" --data '{"jsonrpc":"2.0","method":"eth_blockNumber","params":[],"id":1}' http://localhost:8545 | jq -r '.result' | sed 's/0x//' | xargs printf "%d")
    echo -e "   📊 Current block: $BLOCK_NUM"
else
    echo -e "   ❌ Erigon RPC is not accessible"
    exit 1
fi

echo -e "\n${BLUE}2. Testing blockchain-sync-service binary...${NC}"

# Clean up any existing containers
docker rm -f test-blockchain-sync 2>/dev/null || true

# Run the service in a test container
echo -e "${YELLOW}   Running service in test container...${NC}"
docker run --rm --name test-blockchain-sync \
    -e RUST_LOG=debug \
    -e ETH_RPC_URL=http://host.docker.internal:8545 \
    -e KAFKA_BROKERS=localhost:9092 \
    -e MONGODB_URI=mongodb://admin:defimon123@localhost:27017/chaindata?authSource=admin \
    -e METRICS_ADDR=0.0.0.0:9090 \
    -p 9090:9090 \
    --network host \
    javamicrodefi_blockchain-sync:latest \
    timeout 10s ./blockchain-sync-service sync 2>&1 || echo "Service completed or timed out"

echo -e "\n${BLUE}3. Checking if metrics endpoint is available...${NC}"
sleep 2
if curl -s http://localhost:9090/metrics | head -5 | grep -q "blockchain"; then
    echo -e "   ✅ Metrics endpoint is working"
    echo -e "   📊 Sample metrics:"
    curl -s http://localhost:9090/metrics | head -10
else
    echo -e "   ⚠️  Metrics endpoint not yet available"
fi

echo -e "\n${BLUE}4. Checking Prometheus targets...${NC}"
if curl -s http://localhost:9091/api/v1/targets | jq -r '.data.activeTargets[] | select(.job=="blockchain-sync-service") | .health' | grep -q "up"; then
    echo -e "   ✅ Prometheus is scraping blockchain-sync-service"
else
    echo -e "   ⚠️  Prometheus is not scraping blockchain-sync-service"
fi

echo -e "\n${GREEN}🎯 Test completed!${NC}"
echo -e "\n${YELLOW}💡 Next steps:${NC}"
echo -e "   1. If metrics are working, your Grafana dashboards should show data"
echo -e "   2. If not, check the service logs for errors"
echo -e "   3. Access Grafana at http://localhost:3000 (admin/defimon123)"
