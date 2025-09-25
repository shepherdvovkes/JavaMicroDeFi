#!/bin/bash

# Ethereum Node Sync Status Checker
# Monitors ERIGON and Lighthouse sync progress

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Configuration
ERIGON_RPC_URL="http://localhost:8545"
LIGHTHOUSE_API_URL="http://localhost:5052"
LIGHTHOUSE_METRICS_URL="http://localhost:5054"
ERIGON_METRICS_URL="http://localhost:6060"

echo -e "${BLUE}🔍 Ethereum Node Sync Status Checker${NC}"
echo "=============================================="
echo ""

# Function to check if service is running
check_service() {
    local service_name=$1
    local container_name=$2
    
    if docker ps --format "table {{.Names}}" | grep -q "^${container_name}$"; then
        echo -e "${GREEN}✅ ${service_name} is running${NC}"
        return 0
    else
        echo -e "${RED}❌ ${service_name} is not running${NC}"
        return 1
    fi
}

# Function to make JSON-RPC request
make_rpc_request() {
    local url=$1
    local method=$2
    local params=$3
    
    curl -s -X POST \
        -H "Content-Type: application/json" \
        -d "{\"jsonrpc\":\"2.0\",\"method\":\"${method}\",\"params\":${params},\"id\":1}" \
        "${url}" 2>/dev/null || echo "{}"
}

# Function to get current timestamp
get_timestamp() {
    date '+%Y-%m-%d %H:%M:%S'
}

echo -e "${CYAN}📊 Sync Status Report - $(get_timestamp)${NC}"
echo ""

# Check if containers are running
echo -e "${PURPLE}🐳 Container Status:${NC}"
check_service "ERIGON" "erigon-archive-node"
check_service "Lighthouse" "lighthouse-consensus-node"
echo ""

# ERIGON Sync Status
echo -e "${YELLOW}⚡ ERIGON Execution Client Status:${NC}"
echo "----------------------------------------"

# Check ERIGON sync status
echo "Checking ERIGON sync status..."
ERIGON_SYNC_RESPONSE=$(make_rpc_request "${ERIGON_RPC_URL}" "eth_syncing" "[]")
echo "Raw response: ${ERIGON_SYNC_RESPONSE}"

if echo "${ERIGON_SYNC_RESPONSE}" | grep -q '"result":false'; then
    echo -e "${GREEN}✅ ERIGON is fully synced${NC}"
    
    # Get latest block number
    LATEST_BLOCK_RESPONSE=$(make_rpc_request "${ERIGON_RPC_URL}" "eth_blockNumber" "[]")
    LATEST_BLOCK_HEX=$(echo "${LATEST_BLOCK_RESPONSE}" | grep -o '"result":"[^"]*"' | cut -d'"' -f4)
    if [ ! -z "${LATEST_BLOCK_HEX}" ]; then
        LATEST_BLOCK_DEC=$((16#${LATEST_BLOCK_HEX#0x}))
        echo -e "${GREEN}📦 Latest Block: ${LATEST_BLOCK_DEC}${NC}"
    fi
elif echo "${ERIGON_SYNC_RESPONSE}" | grep -q '"result":{'; then
    echo -e "${YELLOW}🔄 ERIGON is syncing...${NC}"
    
    # Parse sync progress
    CURRENT_BLOCK=$(echo "${ERIGON_SYNC_RESPONSE}" | grep -o '"currentBlock":"[^"]*"' | cut -d'"' -f4)
    HIGHEST_BLOCK=$(echo "${ERIGON_SYNC_RESPONSE}" | grep -o '"highestBlock":"[^"]*"' | cut -d'"' -f4)
    
    if [ ! -z "${CURRENT_BLOCK}" ] && [ ! -z "${HIGHEST_BLOCK}" ]; then
        CURRENT_DEC=$((16#${CURRENT_BLOCK#0x}))
        HIGHEST_DEC=$((16#${HIGHEST_BLOCK#0x}))
        PROGRESS=$((CURRENT_DEC * 100 / HIGHEST_DEC))
        
        echo -e "${YELLOW}📊 Sync Progress: ${CURRENT_DEC} / ${HIGHEST_DEC} (${PROGRESS}%)${NC}"
        
        # Calculate ETA
        if [ ${PROGRESS} -gt 0 ]; then
            REMAINING=$((HIGHEST_DEC - CURRENT_DEC))
            echo -e "${YELLOW}⏱️  Remaining blocks: ${REMAINING}${NC}"
        fi
    fi
else
    echo -e "${RED}❌ Could not determine ERIGON sync status${NC}"
fi

echo ""

# ERIGON Metrics
echo -e "${CYAN}📈 ERIGON Metrics:${NC}"
echo "Checking ERIGON metrics..."
ERIGON_METRICS=$(curl -s "${ERIGON_METRICS_URL}/metrics" 2>/dev/null || echo "")

if [ ! -z "${ERIGON_METRICS}" ]; then
    # Extract key metrics
    PEER_COUNT=$(echo "${ERIGON_METRICS}" | grep "p2p_peers" | head -1 | awk '{print $2}' || echo "N/A")
    BLOCK_HEIGHT=$(echo "${ERIGON_METRICS}" | grep "erigon_head_block" | head -1 | awk '{print $2}' || echo "N/A")
    
    echo -e "${CYAN}🔗 Connected Peers: ${PEER_COUNT}${NC}"
    echo -e "${CYAN}📦 Block Height: ${BLOCK_HEIGHT}${NC}"
else
    echo -e "${YELLOW}⚠️  ERIGON metrics not available${NC}"
fi

echo ""

# Lighthouse Sync Status
echo -e "${YELLOW}🏮 Lighthouse Consensus Client Status:${NC}"
echo "----------------------------------------"

# Check Lighthouse health
echo "Checking Lighthouse health..."
LIGHTHOUSE_HEALTH=$(curl -s "${LIGHTHOUSE_API_URL}/eth/v1/node/health" 2>/dev/null || echo "")

if [ ! -z "${LIGHTHOUSE_HEALTH}" ]; then
    echo -e "${GREEN}✅ Lighthouse is healthy${NC}"
else
    echo -e "${RED}❌ Lighthouse health check failed${NC}"
fi

# Check Lighthouse sync status
echo "Checking Lighthouse sync status..."
LIGHTHOUSE_SYNC_RESPONSE=$(curl -s "${LIGHTHOUSE_API_URL}/eth/v1/node/syncing" 2>/dev/null || echo "{}")

if echo "${LIGHTHOUSE_SYNC_RESPONSE}" | grep -q '"is_syncing":false'; then
    echo -e "${GREEN}✅ Lighthouse is fully synced${NC}"
elif echo "${LIGHTHOUSE_SYNC_RESPONSE}" | grep -q '"is_syncing":true'; then
    echo -e "${YELLOW}🔄 Lighthouse is syncing...${NC}"
    
    # Parse sync progress
    CURRENT_SLOT=$(echo "${LIGHTHOUSE_SYNC_RESPONSE}" | grep -o '"head_slot":"[^"]*"' | cut -d'"' -f4)
    SYNC_DISTANCE=$(echo "${LIGHTHOUSE_SYNC_RESPONSE}" | grep -o '"sync_distance":"[^"]*"' | cut -d'"' -f4)
    
    if [ ! -z "${CURRENT_SLOT}" ] && [ ! -z "${SYNC_DISTANCE}" ]; then
        echo -e "${YELLOW}📊 Current Slot: ${CURRENT_SLOT}${NC}"
        echo -e "${YELLOW}📊 Sync Distance: ${SYNC_DISTANCE}${NC}"
    fi
else
    echo -e "${RED}❌ Could not determine Lighthouse sync status${NC}"
fi

echo ""

# Lighthouse Metrics
echo -e "${CYAN}📈 Lighthouse Metrics:${NC}"
echo "Checking Lighthouse metrics..."
LIGHTHOUSE_METRICS=$(curl -s "${LIGHTHOUSE_METRICS_URL}/metrics" 2>/dev/null || echo "")

if [ ! -z "${LIGHTHOUSE_METRICS}" ]; then
    # Extract key metrics
    BEACON_PEERS=$(echo "${LIGHTHOUSE_METRICS}" | grep "beacon_network_peers" | head -1 | awk '{print $2}' || echo "N/A")
    BEACON_HEAD_SLOT=$(echo "${LIGHTHOUSE_METRICS}" | grep "beacon_head_slot" | head -1 | awk '{print $2}' || echo "N/A")
    
    echo -e "${CYAN}🔗 Connected Peers: ${BEACON_PEERS}${NC}"
    echo -e "${CYAN}📦 Head Slot: ${BEACON_HEAD_SLOT}${NC}"
else
    echo -e "${YELLOW}⚠️  Lighthouse metrics not available${NC}"
fi

echo ""

# Network Status
echo -e "${PURPLE}🌐 Network Status:${NC}"
echo "----------------------------------------"

# Check P2P connections
echo "Checking P2P port status..."
if netstat -tuln 2>/dev/null | grep -q ":30303"; then
    echo -e "${GREEN}✅ ERIGON P2P port 30303 is listening${NC}"
else
    echo -e "${RED}❌ ERIGON P2P port 30303 is not listening${NC}"
fi

if netstat -tuln 2>/dev/null | grep -q ":9000"; then
    echo -e "${GREEN}✅ Lighthouse P2P port 9000 is listening${NC}"
else
    echo -e "${RED}❌ Lighthouse P2P port 9000 is not listening${NC}"
fi

echo ""

# Disk Usage
echo -e "${PURPLE}💾 Disk Usage:${NC}"
echo "----------------------------------------"

# Check ERIGON data directory
ERIGON_DATA_DIR="/mnt/sata18tb/erigon-hot"
if [ -d "${ERIGON_DATA_DIR}" ]; then
    ERIGON_SIZE=$(du -sh "${ERIGON_DATA_DIR}" 2>/dev/null | cut -f1 || echo "N/A")
    echo -e "${CYAN}📁 ERIGON Data Size: ${ERIGON_SIZE}${NC}"
else
    echo -e "${YELLOW}⚠️  ERIGON data directory not found${NC}"
fi

# Check Lighthouse data directory
LIGHTHOUSE_DATA_DIR="/mnt/sata18tb/lighthouse-data"
if [ -d "${LIGHTHOUSE_DATA_DIR}" ]; then
    LIGHTHOUSE_SIZE=$(du -sh "${LIGHTHOUSE_DATA_DIR}" 2>/dev/null | cut -f1 || echo "N/A")
    echo -e "${CYAN}📁 Lighthouse Data Size: ${LIGHTHOUSE_SIZE}${NC}"
else
    echo -e "${YELLOW}⚠️  Lighthouse data directory not found${NC}"
fi

echo ""

# Summary
echo -e "${BLUE}📋 Summary:${NC}"
echo "=============================================="

# Check if both services are synced
ERIGON_SYNCED=false
LIGHTHOUSE_SYNCED=false

if echo "${ERIGON_SYNC_RESPONSE}" | grep -q '"result":false'; then
    ERIGON_SYNCED=true
fi

if echo "${LIGHTHOUSE_SYNC_RESPONSE}" | grep -q '"is_syncing":false'; then
    LIGHTHOUSE_SYNCED=true
fi

if [ "${ERIGON_SYNCED}" = true ] && [ "${LIGHTHOUSE_SYNCED}" = true ]; then
    echo -e "${GREEN}🎉 Both ERIGON and Lighthouse are fully synced!${NC}"
elif [ "${ERIGON_SYNCED}" = true ]; then
    echo -e "${YELLOW}⚠️  ERIGON is synced, but Lighthouse is still syncing${NC}"
elif [ "${LIGHTHOUSE_SYNCED}" = true ]; then
    echo -e "${YELLOW}⚠️  Lighthouse is synced, but ERIGON is still syncing${NC}"
else
    echo -e "${RED}🔄 Both ERIGON and Lighthouse are still syncing${NC}"
fi

echo ""
echo -e "${BLUE}💡 Tips:${NC}"
echo "- Use 'docker logs erigon-archive-node' to check ERIGON logs"
echo "- Use 'docker logs lighthouse-consensus-node' to check Lighthouse logs"
echo "- Monitor metrics at http://localhost:6060 (ERIGON) and http://localhost:5054 (Lighthouse)"
echo "- Check Grafana dashboard at http://localhost:3001 for detailed metrics"

echo ""
echo -e "${GREEN}✅ Sync status check completed!${NC}"



