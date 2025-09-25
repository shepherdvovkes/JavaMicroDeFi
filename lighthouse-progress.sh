#!/bin/bash

# Lighthouse Sync Progress Monitor
# Focused on showing Lighthouse consensus layer sync progress

set -e

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

echo -e "${BLUE}🏮 Lighthouse Consensus Layer Sync Progress${NC}"
echo "=============================================="

# Function to get sync status
get_lighthouse_sync() {
    curl -s http://localhost:5052/eth/v1/node/syncing 2>/dev/null || echo "{}"
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

# Function to calculate progress percentage
calculate_progress() {
    local head_slot=$1
    local sync_distance=$2
    
    if [ $sync_distance -gt 0 ]; then
        local total_slots=$((head_slot + sync_distance))
        local progress=$((head_slot * 100 / total_slots))
        echo $progress
    else
        echo 100
    fi
}

# Function to estimate time remaining
estimate_time() {
    local sync_distance=$1
    local head_slot=$2
    
    if [ $sync_distance -gt 0 ] && [ $head_slot -gt 0 ]; then
        # Rough estimate: Lighthouse typically syncs at ~1000-2000 slots per hour
        local slots_per_hour=1500
        local hours_remaining=$((sync_distance / slots_per_hour))
        local days=$((hours_remaining / 24))
        local hours=$((hours_remaining % 24))
        
        if [ $days -gt 0 ]; then
            echo "${days}d ${hours}h"
        else
            echo "${hours}h"
        fi
    else
        echo "Calculating..."
    fi
}

# Get current sync status
SYNC_DATA=$(get_lighthouse_sync)

if [ "$SYNC_DATA" = "{}" ]; then
    echo -e "${RED}❌ Could not connect to Lighthouse API${NC}"
    exit 1
fi

# Parse sync data
IS_SYNCING=$(echo "$SYNC_DATA" | grep -o '"is_syncing":[^,]*' | cut -d':' -f2 | tr -d ' ')
HEAD_SLOT=$(echo "$SYNC_DATA" | grep -o '"head_slot":"[^"]*"' | cut -d'"' -f4)
SYNC_DISTANCE=$(echo "$SYNC_DATA" | grep -o '"sync_distance":"[^"]*"' | cut -d'"' -f4)

echo -e "${CYAN}📊 Current Status:$(date '+%Y-%m-%d %H:%M:%S')${NC}"
echo ""

if [ "$IS_SYNCING" = "false" ]; then
    echo -e "${GREEN}✅ Lighthouse is fully synced!${NC}"
    echo -e "${GREEN}🎉 Your Ethereum node is complete and ready!${NC}"
else
    echo -e "${YELLOW}🔄 Lighthouse is syncing...${NC}"
    echo ""
    
    if [ ! -z "$HEAD_SLOT" ] && [ ! -z "$SYNC_DISTANCE" ]; then
        # Calculate progress
        PROGRESS=$(calculate_progress $HEAD_SLOT $SYNC_DISTANCE)
        TOTAL_SLOTS=$((HEAD_SLOT + SYNC_DISTANCE))
        
        echo -e "${YELLOW}📊 Sync Progress:${NC}"
        echo -e "   Head Slot: $(format_number $HEAD_SLOT)"
        echo -e "   Sync Distance: $(format_number $SYNC_DISTANCE) slots"
        echo -e "   Total Slots: $(format_number $TOTAL_SLOTS)"
        echo -e "   Progress: ${PROGRESS}%"
        echo ""
        
        # Show progress bar
        BAR_LENGTH=50
        FILLED_LENGTH=$((PROGRESS * BAR_LENGTH / 100))
        BAR=""
        for ((i=0; i<FILLED_LENGTH; i++)); do
            BAR+="█"
        done
        for ((i=FILLED_LENGTH; i<BAR_LENGTH; i++)); do
            BAR+="░"
        done
        echo -e "${YELLOW}   Progress: [${BAR}] ${PROGRESS}%${NC}"
        echo ""
        
        # Time estimates
        ETA=$(estimate_time $SYNC_DISTANCE $HEAD_SLOT)
        echo -e "${CYAN}⏱️  Estimated Time Remaining: ${ETA}${NC}"
        
        # Convert sync distance to weeks/days for context
        SLOTS_PER_EPOCH=32
        SLOTS_PER_DAY=$((SLOTS_PER_EPOCH * 225))  # ~225 epochs per day
        DAYS_REMAINING=$((SYNC_DISTANCE / SLOTS_PER_DAY))
        WEEKS_REMAINING=$((DAYS_REMAINING / 7))
        
        if [ $WEEKS_REMAINING -gt 0 ]; then
            echo -e "${CYAN}📅 Approximately ${WEEKS_REMAINING} weeks remaining${NC}"
        elif [ $DAYS_REMAINING -gt 0 ]; then
            echo -e "${CYAN}📅 Approximately ${DAYS_REMAINING} days remaining${NC}"
        fi
    fi
fi

echo ""

# Show network status
echo -e "${BLUE}🌐 Network Status:${NC}"
if netstat -tuln 2>/dev/null | grep -q ":9000"; then
    echo -e "${GREEN}✅ Lighthouse P2P (9000): Listening${NC}"
else
    echo -e "${YELLOW}⏳ Lighthouse P2P (9000): Will activate when sync completes${NC}"
fi

# Show peer count
PEER_COUNT=$(docker logs lighthouse-consensus-node 2>&1 | grep "peers:" | tail -1 | grep -o "peers:[0-9]*" | cut -d: -f2 || echo "N/A")
echo -e "${CYAN}🔗 Connected Peers: ${PEER_COUNT}${NC}"

echo ""
echo -e "${BLUE}💡 Tips:${NC}"
echo "- Lighthouse syncs much faster than ERIGON (checkpoint sync enabled)"
echo "- P2P ports will activate once sync completes"
echo "- Monitor with: watch -n 30 './lighthouse-progress.sh'"
echo "- Check logs: docker logs lighthouse-consensus-node"
