#!/bin/bash

# Ethereum Full Node Real-time Synchronization Monitor
# This script monitors both Erigon (execution) and Lighthouse (consensus) clients

echo "🚀 Ethereum Mainnet Real-time Synchronization Monitor"
echo "=================================================="
echo ""

# Function to get current timestamp
get_timestamp() {
    date '+%Y-%m-%d %H:%M:%S'
}

# Function to convert hex to decimal
hex_to_decimal() {
    python3 -c "print(int('$1', 16))"
}

# Function to format numbers with commas
format_number() {
    printf "%'d" $1
}

# Function to get Erigon sync status
get_erigon_status() {
    local response=$(curl -s http://localhost:8545 -X POST -H "Content-Type: application/json" -d '{"jsonrpc":"2.0","method":"eth_syncing","params":[],"id":1}')
    
    if echo "$response" | jq -e '.result == false' > /dev/null; then
        echo "✅ FULLY SYNCED"
        return 0
    fi
    
    local current_block=$(echo "$response" | jq -r '.result.currentBlock')
    local highest_block=$(echo "$response" | jq -r '.result.highestBlock')
    
    if [ "$current_block" != "null" ] && [ "$highest_block" != "null" ]; then
        local current_dec=$(hex_to_decimal "$current_block")
        local highest_dec=$(hex_to_decimal "$highest_block")
        local behind=$((highest_dec - current_dec))
        local progress=$(echo "scale=2; ($current_dec / $highest_dec) * 100" | bc)
        
        echo "🔄 SYNCING - Block $(format_number $current_dec) / $(format_number $highest_dec) ($(printf "%.2f" $progress)%)"
        echo "   Behind: $(format_number $behind) blocks"
        return 1
    else
        echo "❓ UNKNOWN STATUS"
        return 2
    fi
}

# Function to get Lighthouse sync status
get_lighthouse_status() {
    local response=$(curl -s http://localhost:5052/eth/v1/node/syncing 2>/dev/null)
    
    if [ -z "$response" ]; then
        echo "❌ API NOT AVAILABLE"
        return 2
    fi
    
    local is_syncing=$(echo "$response" | jq -r '.data.is_syncing')
    local head_slot=$(echo "$response" | jq -r '.data.head_slot')
    local sync_distance=$(echo "$response" | jq -r '.data.sync_distance')
    
    if [ "$is_syncing" = "false" ]; then
        echo "✅ FULLY SYNCED - Slot $(format_number $head_slot)"
        return 0
    else
        echo "🔄 SYNCING - Slot $(format_number $head_slot), Distance: $(format_number $sync_distance)"
        return 1
    fi
}

# Function to get peer count
get_peer_count() {
    local erigon_peers=$(docker logs erigon --tail 100 2>/dev/null | grep -o "peers=[0-9]*" | tail -1 | cut -d= -f2)
    local lighthouse_peers=$(docker logs lighthouse --tail 100 2>/dev/null | grep -o "peers: \"[0-9]*\"" | tail -1 | cut -d'"' -f2)
    
    echo "Erigon peers: ${erigon_peers:-0}"
    echo "Lighthouse peers: ${lighthouse_peers:-0}"
}

# Main monitoring loop
while true; do
    clear
    echo "🚀 Ethereum Mainnet Real-time Synchronization Monitor"
    echo "=================================================="
    echo "Last updated: $(get_timestamp)"
    echo ""
    
    echo "📊 EXECUTION CLIENT (Erigon):"
    get_erigon_status
    echo ""
    
    echo "📊 CONSENSUS CLIENT (Lighthouse):"
    get_lighthouse_status
    echo ""
    
    echo "🌐 NETWORK STATUS:"
    get_peer_count
    echo ""
    
    echo "📈 CONTAINER STATUS:"
    docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" | grep -E "(erigon|lighthouse)"
    echo ""
    
    echo "Press Ctrl+C to stop monitoring"
    echo "Refreshing in 10 seconds..."
    
    sleep 10
done
