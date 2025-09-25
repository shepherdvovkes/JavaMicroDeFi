#!/bin/bash

# Quick Ethereum Sync Status Check
# Simple script for quick sync status overview

echo "🔍 Quick Ethereum Sync Status"
echo "=============================="

# Check if containers are running
echo "📦 Container Status:"
if docker ps --format "table {{.Names}}" | grep -q "erigon-archive-node"; then
    echo "✅ ERIGON: Running"
else
    echo "❌ ERIGON: Not running"
fi

if docker ps --format "table {{.Names}}" | grep -q "lighthouse-consensus-node"; then
    echo "✅ Lighthouse: Running"
else
    echo "❌ Lighthouse: Not running"
fi

echo ""

# Quick ERIGON sync check
echo "⚡ ERIGON Sync Status:"
ERIGON_SYNC=$(curl -s -X POST -H "Content-Type: application/json" \
    -d '{"jsonrpc":"2.0","method":"eth_syncing","params":[],"id":1}' \
    http://localhost:8545 2>/dev/null)

if echo "$ERIGON_SYNC" | grep -q '"result":false'; then
    echo "✅ ERIGON: Fully synced"
    
    # Get latest block
    LATEST_BLOCK=$(curl -s -X POST -H "Content-Type: application/json" \
        -d '{"jsonrpc":"2.0","method":"eth_blockNumber","params":[],"id":1}' \
        http://localhost:8545 2>/dev/null | grep -o '"result":"[^"]*"' | cut -d'"' -f4)
    
    if [ ! -z "$LATEST_BLOCK" ]; then
        BLOCK_NUM=$((16#${LATEST_BLOCK#0x}))
        echo "📦 Latest Block: $BLOCK_NUM"
    fi
elif echo "$ERIGON_SYNC" | grep -q '"result":{'; then
    echo "🔄 ERIGON: Syncing..."
    
    # Parse sync progress
    CURRENT=$(echo "$ERIGON_SYNC" | grep -o '"currentBlock":"[^"]*"' | cut -d'"' -f4)
    HIGHEST=$(echo "$ERIGON_SYNC" | grep -o '"highestBlock":"[^"]*"' | cut -d'"' -f4)
    
    if [ ! -z "$CURRENT" ] && [ ! -z "$HIGHEST" ]; then
        CURRENT_DEC=$((16#${CURRENT#0x}))
        HIGHEST_DEC=$((16#${HIGHEST#0x}))
        PROGRESS=$((CURRENT_DEC * 100 / HIGHEST_DEC))
        echo "📊 Progress: $CURRENT_DEC / $HIGHEST_DEC ($PROGRESS%)"
    fi
else
    echo "❌ ERIGON: Status unknown"
fi

echo ""

# Quick Lighthouse sync check
echo "🏮 Lighthouse Sync Status:"
LIGHTHOUSE_SYNC=$(curl -s http://localhost:5052/eth/v1/node/syncing 2>/dev/null)

if echo "$LIGHTHOUSE_SYNC" | grep -q '"is_syncing":false'; then
    echo "✅ Lighthouse: Fully synced"
elif echo "$LIGHTHOUSE_SYNC" | grep -q '"is_syncing":true'; then
    echo "🔄 Lighthouse: Syncing..."
    
    # Parse sync progress
    HEAD_SLOT=$(echo "$LIGHTHOUSE_SYNC" | grep -o '"head_slot":"[^"]*"' | cut -d'"' -f4)
    SYNC_DISTANCE=$(echo "$LIGHTHOUSE_SYNC" | grep -o '"sync_distance":"[^"]*"' | cut -d'"' -f4)
    
    if [ ! -z "$HEAD_SLOT" ] && [ ! -z "$SYNC_DISTANCE" ]; then
        echo "📊 Head Slot: $HEAD_SLOT"
        echo "📊 Sync Distance: $SYNC_DISTANCE"
    fi
else
    echo "❌ Lighthouse: Status unknown"
fi

echo ""

# Network status
echo "🌐 Network Status:"
if netstat -tuln 2>/dev/null | grep -q ":30303"; then
    echo "✅ ERIGON P2P (30303): Listening"
else
    echo "❌ ERIGON P2P (30303): Not listening"
fi

if netstat -tuln 2>/dev/null | grep -q ":9000"; then
    echo "✅ Lighthouse P2P (9000): Listening"
else
    echo "❌ Lighthouse P2P (9000): Not listening"
fi

echo ""
echo "✅ Quick check completed!"



