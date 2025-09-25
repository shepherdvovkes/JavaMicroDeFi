#!/bin/bash

# Check Ethereum Mainnet Full Stack Status
# Comprehensive status check for the entire Ethereum node stack

set -e

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

echo -e "${BLUE}🔍 Ethereum Mainnet Full Stack Status${NC}"
echo "=============================================="
echo -e "${CYAN}Timestamp: $(date '+%Y-%m-%d %H:%M:%S')${NC}"
echo ""

# Function to check service status
check_service() {
    local service_name=$1
    local container_name=$2
    local port=$3
    local health_url=$4
    
    echo -n "Checking ${service_name}... "
    
    # Check if container is running
    if docker ps --format "table {{.Names}}" | grep -q "^${container_name}$"; then
        echo -e "${GREEN}✅ Running${NC}"
        
        # Check if port is accessible
        if [ ! -z "$port" ]; then
            if ss -tuln | grep -q ":${port} "; then
                echo -e "  📡 Port ${port}: ${GREEN}Listening${NC}"
            else
                echo -e "  📡 Port ${port}: ${RED}Not listening${NC}"
            fi
        fi
        
        # Check health endpoint if provided
        if [ ! -z "$health_url" ]; then
            if curl -s "$health_url" >/dev/null 2>&1; then
                echo -e "  🏥 Health: ${GREEN}OK${NC}"
            else
                echo -e "  🏥 Health: ${RED}Failed${NC}"
            fi
        fi
    else
        echo -e "${RED}❌ Not running${NC}"
    fi
    echo ""
}

# Check all services
echo -e "${YELLOW}🐳 Container Status:${NC}"
echo "========================"

check_service "ERIGON" "ETHEREUM_mainnet_full-erigon" "8545" "http://localhost:8545"
check_service "Lighthouse" "ETHEREUM_mainnet_full-lighthouse" "5052" "http://localhost:5052"
check_service "Prometheus" "ETHEREUM_mainnet_full-prometheus" "9091" "http://localhost:9091"
check_service "Grafana" "ETHEREUM_mainnet_full-grafana" "3001" "http://localhost:3001"
check_service "Nginx" "ETHEREUM_mainnet_full-nginx" "80" "http://localhost"
check_service "Monitor" "ETHEREUM_mainnet_full-monitor" "" ""

# Check sync status
echo -e "${YELLOW}🔄 Sync Status:${NC}"
echo "=================="

# ERIGON sync status
echo "ERIGON Sync Status:"
ERIGON_SYNC=$(curl -s -X POST -H "Content-Type: application/json" \
    -d '{"jsonrpc":"2.0","method":"eth_syncing","params":[],"id":1}' \
    http://localhost:8545 2>/dev/null || echo "{}")

if echo "$ERIGON_SYNC" | grep -q '"result":false'; then
    echo -e "${GREEN}✅ ERIGON: Fully synced${NC}"
    
    # Get latest block
    LATEST_BLOCK=$(curl -s -X POST -H "Content-Type: application/json" \
        -d '{"jsonrpc":"2.0","method":"eth_blockNumber","params":[],"id":1}' \
        http://localhost:8545 2>/dev/null | grep -o '"result":"[^"]*"' | cut -d'"' -f4)
    
    if [ ! -z "$LATEST_BLOCK" ]; then
        BLOCK_NUM=$((16#${LATEST_BLOCK#0x}))
        echo -e "${CYAN}📦 Latest Block: ${BLOCK_NUM}${NC}"
    fi
elif echo "$ERIGON_SYNC" | grep -q '"result":{'; then
    echo -e "${YELLOW}🔄 ERIGON: Syncing...${NC}"
else
    echo -e "${RED}❌ ERIGON: Status unknown${NC}"
fi

# Lighthouse sync status
echo ""
echo "Lighthouse Sync Status:"
LIGHTHOUSE_SYNC=$(curl -s http://localhost:5052/eth/v1/node/syncing 2>/dev/null || echo "{}")

if echo "$LIGHTHOUSE_SYNC" | grep -q '"is_syncing":false'; then
    echo -e "${GREEN}✅ Lighthouse: Fully synced${NC}"
elif echo "$LIGHTHOUSE_SYNC" | grep -q '"is_syncing":true'; then
    echo -e "${YELLOW}🔄 Lighthouse: Syncing...${NC}"
else
    echo -e "${RED}❌ Lighthouse: Status unknown${NC}"
fi

echo ""

# Check network connectivity
echo -e "${YELLOW}🌐 Network Status:${NC}"
echo "===================="

# Check P2P ports
if ss -tuln | grep -q ":30303"; then
    echo -e "${GREEN}✅ ERIGON P2P (30303): Listening${NC}"
else
    echo -e "${RED}❌ ERIGON P2P (30303): Not listening${NC}"
fi

if ss -tuln | grep -q ":9000"; then
    echo -e "${GREEN}✅ Lighthouse P2P (9000): Listening${NC}"
else
    echo -e "${RED}❌ Lighthouse P2P (9000): Not listening${NC}"
fi

echo ""

# Check disk usage
echo -e "${YELLOW}💾 Disk Usage:${NC}"
echo "=============="

if [ -d "/mnt/sata18tb/erigon-hot" ]; then
    ERIGON_SIZE=$(du -sh /mnt/sata18tb/erigon-hot 2>/dev/null | cut -f1 || echo "N/A")
    echo -e "${CYAN}📁 ERIGON Data: ${ERIGON_SIZE}${NC}"
fi

if [ -d "/mnt/sata18tb/lighthouse-data" ]; then
    LIGHTHOUSE_SIZE=$(du -sh /mnt/sata18tb/lighthouse-data 2>/dev/null | cut -f1 || echo "N/A")
    echo -e "${CYAN}📁 Lighthouse Data: ${LIGHTHOUSE_SIZE}${NC}"
fi

echo ""

# Summary
echo -e "${BLUE}📋 Summary:${NC}"
echo "=============="

# Count running services
RUNNING_SERVICES=$(docker ps --format "table {{.Names}}" | grep "ETHEREUM_mainnet_full" | wc -l)
TOTAL_SERVICES=6

echo -e "${CYAN}Running Services: ${RUNNING_SERVICES}/${TOTAL_SERVICES}${NC}"

if [ $RUNNING_SERVICES -eq $TOTAL_SERVICES ]; then
    echo -e "${GREEN}🎉 All services are running!${NC}"
elif [ $RUNNING_SERVICES -gt 0 ]; then
    echo -e "${YELLOW}⚠️  Some services are not running${NC}"
else
    echo -e "${RED}❌ No services are running${NC}"
fi

echo ""
echo -e "${BLUE}💡 Management Commands:${NC}"
echo "========================"
echo -e "📈 View logs: ./logs-ethereum-stack.sh"
echo -e "🛑 Stop stack: ./stop-ethereum-stack.sh"
echo -e "🔄 Restart stack: ./restart-ethereum-stack.sh"
echo -e "📊 Detailed status: ./verify-node-status.sh"
