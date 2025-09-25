#!/bin/bash

# Ethereum Node Verification Script
# Comprehensive check of sync status and RPC endpoints

set -e

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

echo -e "${BLUE}🔍 Ethereum Node Verification Report${NC}"
echo "=============================================="
echo -e "${CYAN}Timestamp: $(date '+%Y-%m-%d %H:%M:%S')${NC}"
echo ""

# Function to test RPC endpoint
test_rpc() {
    local name=$1
    local url=$2
    local method=$3
    local data=$4
    
    echo -n "Testing ${name}... "
    
    if [ ! -z "$data" ]; then
        response=$(curl -s -X POST -H "Content-Type: application/json" -d "$data" "$url" 2>/dev/null)
    else
        response=$(curl -s "$url" 2>/dev/null)
    fi
    
    if [ $? -eq 0 ] && [ ! -z "$response" ]; then
        echo -e "${GREEN}✅ OK${NC}"
        return 0
    else
        echo -e "${RED}❌ FAILED${NC}"
        return 1
    fi
}

# Function to get block number
get_block_number() {
    local response=$(curl -s -X POST -H "Content-Type: application/json" \
        -d '{"jsonrpc":"2.0","method":"eth_blockNumber","params":[],"id":1}' \
        http://localhost:8545 2>/dev/null)
    
    if echo "$response" | grep -q '"result"'; then
        local hex_block=$(echo "$response" | grep -o '"result":"[^"]*"' | cut -d'"' -f4)
        local dec_block=$((16#${hex_block#0x}))
        echo $dec_block
    else
        echo "0"
    fi
}

# Function to get lighthouse head slot
get_head_slot() {
    local response=$(curl -s http://localhost:5052/eth/v1/node/syncing 2>/dev/null)
    
    if echo "$response" | grep -q '"head_slot"'; then
        echo "$response" | grep -o '"head_slot":"[^"]*"' | cut -d'"' -f4
    else
        echo "0"
    fi
}

echo -e "${YELLOW}📦 Container Status:${NC}"
echo "----------------------------------------"

# Check containers
if docker ps --format "table {{.Names}}" | grep -q "erigon-archive-node"; then
    echo -e "${GREEN}✅ ERIGON: Running${NC}"
else
    echo -e "${RED}❌ ERIGON: Not running${NC}"
fi

if docker ps --format "table {{.Names}}" | grep -q "lighthouse-consensus-node"; then
    echo -e "${GREEN}✅ Lighthouse: Running${NC}"
else
    echo -e "${RED}❌ Lighthouse: Not running${NC}"
fi

echo ""

echo -e "${YELLOW}🔄 Sync Status:${NC}"
echo "----------------------------------------"

# Check ERIGON sync
echo "Checking ERIGON sync status..."
ERIGON_SYNC=$(curl -s -X POST -H "Content-Type: application/json" \
    -d '{"jsonrpc":"2.0","method":"eth_syncing","params":[],"id":1}' \
    http://localhost:8545 2>/dev/null)

if echo "$ERIGON_SYNC" | grep -q '"result":false'; then
    echo -e "${GREEN}✅ ERIGON: Fully synced${NC}"
    LATEST_BLOCK=$(get_block_number)
    echo -e "${CYAN}📦 Latest Block: ${LATEST_BLOCK}${NC}"
elif echo "$ERIGON_SYNC" | grep -q '"result":{'; then
    echo -e "${YELLOW}🔄 ERIGON: Still syncing${NC}"
else
    echo -e "${RED}❌ ERIGON: Sync status unknown${NC}"
fi

# Check Lighthouse sync
echo "Checking Lighthouse sync status..."
LIGHTHOUSE_SYNC=$(curl -s http://localhost:5052/eth/v1/node/syncing 2>/dev/null)

if echo "$LIGHTHOUSE_SYNC" | grep -q '"is_syncing":false'; then
    echo -e "${GREEN}✅ Lighthouse: Fully synced${NC}"
    HEAD_SLOT=$(get_head_slot)
    echo -e "${CYAN}📦 Head Slot: ${HEAD_SLOT}${NC}"
elif echo "$LIGHTHOUSE_SYNC" | grep -q '"is_syncing":true'; then
    echo -e "${YELLOW}🔄 Lighthouse: Still syncing${NC}"
else
    echo -e "${RED}❌ Lighthouse: Sync status unknown${NC}"
fi

echo ""

echo -e "${YELLOW}🌐 Network Ports:${NC}"
echo "----------------------------------------"

# Check listening ports
echo "Checking listening ports..."

PORTS=("8545:ERIGON HTTP RPC" "8546:ERIGON WebSocket" "8551:ERIGON Engine API" "5052:Lighthouse HTTP API" "5054:Lighthouse Metrics" "6060:ERIGON Metrics")

for port_info in "${PORTS[@]}"; do
    port=$(echo "$port_info" | cut -d: -f1)
    name=$(echo "$port_info" | cut -d: -f2-)
    
    if ss -tuln | grep -q ":${port} "; then
        echo -e "${GREEN}✅ Port ${port} (${name}): Listening${NC}"
    else
        echo -e "${RED}❌ Port ${port} (${name}): Not listening${NC}"
    fi
done

echo ""

echo -e "${YELLOW}🔌 RPC Endpoint Tests:${NC}"
echo "----------------------------------------"

# Test ERIGON HTTP RPC
test_rpc "ERIGON HTTP RPC" "http://localhost:8545" "POST" '{"jsonrpc":"2.0","method":"eth_blockNumber","params":[],"id":1}'

# Test ERIGON WebSocket (just check if port is open)
echo -n "Testing ERIGON WebSocket... "
if ss -tuln | grep -q ":8546 "; then
    echo -e "${GREEN}✅ OK${NC}"
else
    echo -e "${RED}❌ FAILED${NC}"
fi

# Test ERIGON Engine API
test_rpc "ERIGON Engine API" "http://localhost:8551" "POST" '{"jsonrpc":"2.0","method":"eth_blockNumber","params":[],"id":1}'

# Test Lighthouse HTTP API
test_rpc "Lighthouse HTTP API" "http://localhost:5052/eth/v1/node/health" "GET" ""

# Test Lighthouse Metrics
test_rpc "Lighthouse Metrics" "http://localhost:5054/metrics" "GET" ""

echo ""

echo -e "${YELLOW}📊 Node Information:${NC}"
echo "----------------------------------------"

# Get current block number
CURRENT_BLOCK=$(get_block_number)
echo -e "${CYAN}📦 Current Block: ${CURRENT_BLOCK}${NC}"

# Get head slot
HEAD_SLOT=$(get_head_slot)
echo -e "${CYAN}📦 Head Slot: ${HEAD_SLOT}${NC}"

# Calculate epoch
if [ "$HEAD_SLOT" != "0" ]; then
    EPOCH=$((HEAD_SLOT / 32))
    echo -e "${CYAN}📦 Current Epoch: ${EPOCH}${NC}"
fi

echo ""

echo -e "${YELLOW}💾 Disk Usage:${NC}"
echo "----------------------------------------"

# Check disk usage
if [ -d "/mnt/sata18tb/erigon-hot" ]; then
    ERIGON_SIZE=$(du -sh /mnt/sata18tb/erigon-hot 2>/dev/null | cut -f1 || echo "N/A")
    echo -e "${CYAN}📁 ERIGON Data: ${ERIGON_SIZE}${NC}"
fi

if [ -d "/mnt/sata18tb/lighthouse-data" ]; then
    LIGHTHOUSE_SIZE=$(du -sh /mnt/sata18tb/lighthouse-data 2>/dev/null | cut -f1 || echo "N/A")
    echo -e "${CYAN}📁 Lighthouse Data: ${LIGHTHOUSE_SIZE}${NC}"
fi

echo ""

echo -e "${YELLOW}🎯 Summary:${NC}"
echo "----------------------------------------"

# Determine overall status
ERIGON_SYNCED=false
LIGHTHOUSE_SYNCED=false
RPC_WORKING=false

if echo "$ERIGON_SYNC" | grep -q '"result":false'; then
    ERIGON_SYNCED=true
fi

if echo "$LIGHTHOUSE_SYNC" | grep -q '"is_syncing":false'; then
    LIGHTHOUSE_SYNCED=true
fi

if ss -tuln | grep -q ":8545 "; then
    RPC_WORKING=true
fi

if [ "$ERIGON_SYNCED" = true ] && [ "$LIGHTHOUSE_SYNCED" = true ] && [ "$RPC_WORKING" = true ]; then
    echo -e "${GREEN}🎉 Your Ethereum node is FULLY OPERATIONAL!${NC}"
    echo -e "${GREEN}✅ Both execution and consensus layers are synced${NC}"
    echo -e "${GREEN}✅ RPC endpoints are working${NC}"
    echo -e "${GREEN}✅ Ready to serve requests${NC}"
elif [ "$ERIGON_SYNCED" = true ] && [ "$LIGHTHOUSE_SYNCED" = true ]; then
    echo -e "${YELLOW}⚠️  Node is synced but RPC may have issues${NC}"
else
    echo -e "${RED}❌ Node is not fully operational${NC}"
fi

echo ""
echo -e "${BLUE}💡 Usage Examples:${NC}"
echo "- Query latest block: curl -X POST -H 'Content-Type: application/json' -d '{\"jsonrpc\":\"2.0\",\"method\":\"eth_blockNumber\",\"params\":[],\"id\":1}' http://localhost:8545"
echo "- Check node health: curl http://localhost:5052/eth/v1/node/health"
echo "- View metrics: curl http://localhost:5054/metrics"
