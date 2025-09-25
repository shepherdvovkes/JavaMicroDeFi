#!/bin/bash

# Real-time Ethereum Sync Progress Monitor
# Continuously monitors ERIGON and Lighthouse sync progress

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
UPDATE_INTERVAL=30  # seconds

echo -e "${BLUE}🔄 Real-time Ethereum Sync Progress Monitor${NC}"
echo "=================================================="
echo "Press Ctrl+C to stop monitoring"
echo ""

# Function to get current timestamp
get_timestamp() {
    date '+%Y-%m-%d %H:%M:%S'
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

# Function to clear screen and show header
clear_and_header() {
    clear
    echo -e "${BLUE}🔄 Ethereum Sync Progress Monitor${NC}"
    echo "=================================================="
    echo -e "${CYAN}Last Update: $(get_timestamp)${NC}"
    echo ""
}

# Function to format large numbers
format_number() {
    local num=$1
    if [ ${#num} -gt 6 ]; then
        echo "${num:0:$((${#num}-6))}.${num:$((${#num}-6)):2}M"
    elif [ ${#num} -gt 3 ]; then
        echo "${num:0:$((${#num}-3))}.${num:$((${#num}-3)):1}K"
    else
        echo "$num"
    fi
}

# Function to calculate ETA
calculate_eta() {
    local current=$1
    local target=$2
    local start_time=$3
    
    if [ $current -gt 0 ] && [ $target -gt $current ]; then
        local elapsed=$(($(date +%s) - start_time))
        local rate=$((current * 1000 / elapsed))  # blocks per second * 1000
        if [ $rate -gt 0 ]; then
            local remaining=$((target - current))
            local eta_seconds=$((remaining * 1000 / rate))
            local eta_hours=$((eta_seconds / 3600))
            local eta_mins=$(((eta_seconds % 3600) / 60))
            echo "${eta_hours}h ${eta_mins}m"
        else
            echo "Calculating..."
        fi
    else
        echo "N/A"
    fi
}

# Track sync start time
ERIGON_START_TIME=$(date +%s)
LIGHTHOUSE_START_TIME=$(date +%s)

# Main monitoring loop
while true; do
    clear_and_header
    
    # ERIGON Status
    echo -e "${YELLOW}⚡ ERIGON Execution Client:${NC}"
    echo "----------------------------------------"
    
    ERIGON_SYNC_RESPONSE=$(make_rpc_request "${ERIGON_RPC_URL}" "eth_syncing" "[]")
    
    if echo "${ERIGON_SYNC_RESPONSE}" | grep -q '"result":false'; then
        echo -e "${GREEN}✅ ERIGON is fully synced!${NC}"
        
        # Get latest block
        LATEST_BLOCK_RESPONSE=$(make_rpc_request "${ERIGON_RPC_URL}" "eth_blockNumber" "[]")
        LATEST_BLOCK_HEX=$(echo "${LATEST_BLOCK_RESPONSE}" | grep -o '"result":"[^"]*"' | cut -d'"' -f4)
        if [ ! -z "${LATEST_BLOCK_HEX}" ]; then
            LATEST_BLOCK_DEC=$((16#${LATEST_BLOCK_HEX#0x}))
            echo -e "${GREEN}📦 Latest Block: $(format_number ${LATEST_BLOCK_DEC})${NC}"
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
            REMAINING=$((HIGHEST_DEC - CURRENT_DEC))
            
            echo -e "${YELLOW}📊 Progress: $(format_number ${CURRENT_DEC}) / $(format_number ${HIGHEST_DEC}) (${PROGRESS}%)${NC}"
            echo -e "${YELLOW}⏱️  Remaining: $(format_number ${REMAINING}) blocks${NC}"
            
            # Calculate ETA
            ETA=$(calculate_eta $CURRENT_DEC $HIGHEST_DEC $ERIGON_START_TIME)
            echo -e "${YELLOW}🕐 ETA: ${ETA}${NC}"
        fi
    else
        echo -e "${RED}❌ Could not determine ERIGON sync status${NC}"
    fi
    
    echo ""
    
    # Lighthouse Status
    echo -e "${YELLOW}🏮 Lighthouse Consensus Client:${NC}"
    echo "----------------------------------------"
    
    LIGHTHOUSE_SYNC_RESPONSE=$(curl -s "${LIGHTHOUSE_API_URL}/eth/v1/node/syncing" 2>/dev/null || echo "{}")
    
    if echo "${LIGHTHOUSE_SYNC_RESPONSE}" | grep -q '"is_syncing":false'; then
        echo -e "${GREEN}✅ Lighthouse is fully synced!${NC}"
    elif echo "${LIGHTHOUSE_SYNC_RESPONSE}" | grep -q '"is_syncing":true'; then
        echo -e "${YELLOW}🔄 Lighthouse is syncing...${NC}"
        
        # Parse sync progress
        HEAD_SLOT=$(echo "${LIGHTHOUSE_SYNC_RESPONSE}" | grep -o '"head_slot":"[^"]*"' | cut -d'"' -f4)
        SYNC_DISTANCE=$(echo "${LIGHTHOUSE_SYNC_RESPONSE}" | grep -o '"sync_distance":"[^"]*"' | cut -d'"' -f4)
        
        if [ ! -z "${HEAD_SLOT}" ] && [ ! -z "${SYNC_DISTANCE}" ]; then
            echo -e "${YELLOW}📊 Head Slot: $(format_number ${HEAD_SLOT})${NC}"
            echo -e "${YELLOW}📊 Sync Distance: $(format_number ${SYNC_DISTANCE}) slots${NC}"
            
            # Calculate ETA for Lighthouse (rough estimate)
            if [ ${SYNC_DISTANCE} -gt 0 ]; then
                local elapsed=$(($(date +%s) - LIGHTHOUSE_START_TIME))
                if [ $elapsed -gt 0 ]; then
                    local slots_per_second=$((HEAD_SLOT * 1000 / elapsed))
                    if [ $slots_per_second -gt 0 ]; then
                        local eta_seconds=$((SYNC_DISTANCE * 1000 / slots_per_second))
                        local eta_days=$((eta_seconds / 86400))
                        local eta_hours=$(((eta_seconds % 86400) / 3600))
                        echo -e "${YELLOW}🕐 ETA: ${eta_days}d ${eta_hours}h${NC}"
                    fi
                fi
            fi
        fi
    else
        echo -e "${RED}❌ Could not determine Lighthouse sync status${NC}"
    fi
    
    echo ""
    
    # Network Status
    echo -e "${PURPLE}🌐 Network Status:${NC}"
    echo "----------------------------------------"
    
    if netstat -tuln 2>/dev/null | grep -q ":30303"; then
        echo -e "${GREEN}✅ ERIGON P2P (30303): Listening${NC}"
    else
        echo -e "${RED}❌ ERIGON P2P (30303): Not listening${NC}"
    fi
    
    if netstat -tuln 2>/dev/null | grep -q ":9000"; then
        echo -e "${GREEN}✅ Lighthouse P2P (9000): Listening${NC}"
    else
        echo -e "${RED}❌ Lighthouse P2P (9000): Not listening${NC}"
    fi
    
    echo ""
    
    # Container Status
    echo -e "${PURPLE}🐳 Container Status:${NC}"
    echo "----------------------------------------"
    
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
    echo -e "${CYAN}Next update in ${UPDATE_INTERVAL} seconds... (Press Ctrl+C to stop)${NC}"
    
    sleep $UPDATE_INTERVAL
done



