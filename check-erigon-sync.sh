#!/bin/bash

# ERIGON Ethereum Mainnet Sync Status Checker
# This script checks if ERIGON is synced with Ethereum mainnet

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to make RPC call
make_rpc_call() {
    local method=$1
    local params=$2
    local url=${3:-"http://localhost:8545"}
    
    curl -s -X POST -H "Content-Type: application/json" \
        --data "{\"jsonrpc\":\"2.0\",\"method\":\"$method\",\"params\":$params,\"id\":1}" \
        "$url" 2>/dev/null
}

# Function to convert hex to decimal
hex_to_decimal() {
    local hex=$1
    python3 -c "print(int('$hex', 16))"
}

# Function to check if ERIGON container is running
check_erigon_container() {
    print_info "Checking ERIGON container status..."
    
    if ! docker ps | grep -q "erigon"; then
        print_error "ERIGON container is not running!"
        return 1
    fi
    
    local container_status=$(docker ps --format "table {{.Names}}\t{{.Status}}" | grep erigon)
    print_success "ERIGON container is running: $container_status"
    return 0
}

# Function to check ERIGON RPC connectivity
check_erigon_rpc() {
    print_info "Checking ERIGON RPC connectivity..."
    
    local response=$(make_rpc_call "eth_blockNumber" "[]")
    
    if [[ -z "$response" ]] || [[ "$response" == *"error"* ]]; then
        print_error "Failed to connect to ERIGON RPC endpoint"
        return 1
    fi
    
    local block_hex=$(echo "$response" | grep -o '"result":"[^"]*"' | cut -d'"' -f4)
    local block_decimal=$(hex_to_decimal "$block_hex")
    
    print_success "ERIGON RPC is accessible. Current block: $block_decimal"
    echo "$block_decimal"
}

# Function to get public node block number
get_public_block() {
    print_info "Getting latest block from public node..."
    
    local response=$(make_rpc_call "eth_blockNumber" "[]" "https://ethereum.publicnode.com")
    
    if [[ -z "$response" ]] || [[ "$response" == *"error"* ]]; then
        print_warning "Failed to get public node block, trying alternative..."
        response=$(make_rpc_call "eth_blockNumber" "[]" "https://rpc.ankr.com/eth")
    fi
    
    if [[ -z "$response" ]] || [[ "$response" == *"error"* ]]; then
        print_error "Failed to get public node block number"
        return 1
    fi
    
    local block_hex=$(echo "$response" | grep -o '"result":"[^"]*"' | cut -d'"' -f4)
    local block_decimal=$(hex_to_decimal "$block_hex")
    
    print_success "Public node block: $block_decimal"
    echo "$block_decimal"
}

# Function to check sync status
check_sync_status() {
    print_info "Checking ERIGON sync status..."
    
    local response=$(make_rpc_call "eth_syncing" "[]")
    
    if [[ -z "$response" ]]; then
        print_error "Failed to get sync status"
        return 1
    fi
    
    echo "$response"
}

# Function to get detailed sync information
get_detailed_sync_info() {
    print_info "Getting detailed sync information..."
    
    local sync_response=$(check_sync_status)
    
    if [[ "$sync_response" == *"false"* ]]; then
        print_success "ERIGON is not actively syncing (likely fully synced)"
        return 0
    fi
    
    # Parse sync response
    local current_block=$(echo "$sync_response" | grep -o '"currentBlock":"[^"]*"' | cut -d'"' -f4)
    local highest_block=$(echo "$sync_response" | grep -o '"highestBlock":"[^"]*"' | cut -d'"' -f4)
    
    if [[ -n "$current_block" ]] && [[ -n "$highest_block" ]]; then
        local current_decimal=$(hex_to_decimal "$current_block")
        local highest_decimal=$(hex_to_decimal "$highest_block")
        
        print_info "Sync Progress:"
        print_info "  Current Block: $current_decimal"
        print_info "  Highest Block: $highest_decimal"
        print_info "  Remaining: $((highest_decimal - current_decimal)) blocks"
        
        if [[ $highest_decimal -gt 0 ]]; then
            local progress=$((current_decimal * 100 / highest_decimal))
            print_info "  Progress: $progress%"
        fi
    fi
}

# Function to check ERIGON logs for sync information
check_erigon_logs() {
    print_info "Checking recent ERIGON logs..."
    
    local recent_logs=$(docker logs erigon --tail 20 2>&1)
    
    # Look for sync-related messages
    if echo "$recent_logs" | grep -q "sync"; then
        print_info "Sync-related log entries found:"
        echo "$recent_logs" | grep -i sync | head -5
    fi
    
    # Look for stage information
    if echo "$recent_logs" | grep -q "stage"; then
        print_info "Stage-related log entries found:"
        echo "$recent_logs" | grep -i stage | head -5
    fi
    
    # Look for error messages
    if echo "$recent_logs" | grep -q "ERROR\|error"; then
        print_warning "Error messages found in logs:"
        echo "$recent_logs" | grep -i error | head -3
    fi
}

# Function to calculate sync status
calculate_sync_status() {
    local erigon_block=$1
    local public_block=$2
    
    local blocks_behind=$((public_block - erigon_block))
    local sync_percentage=$((erigon_block * 100 / public_block))
    
    echo ""
    print_info "=== ERIGON Sync Status Summary ==="
    echo "ERIGON Block:     $erigon_block"
    echo "Public Node Block: $public_block"
    echo "Blocks Behind:    $blocks_behind"
    echo "Sync Progress:    $sync_percentage%"
    echo ""
    
    if [[ $blocks_behind -lt 10 ]]; then
        print_success "✅ ERIGON is fully synced! (within 10 blocks)"
        return 0
    elif [[ $blocks_behind -lt 100 ]]; then
        print_success "✅ ERIGON is synced (within 100 blocks)"
        return 0
    elif [[ $blocks_behind -lt 1000 ]]; then
        print_warning "⚠️  ERIGON is catching up (within 1000 blocks)"
        return 1
    else
        print_error "❌ ERIGON is significantly behind ($blocks_behind blocks)"
        return 2
    fi
}

# Function to show recommendations
show_recommendations() {
    local status_code=$1
    
    echo ""
    print_info "=== Recommendations ==="
    
    case $status_code in
        0)
            print_success "ERIGON is synced! You can now:"
            echo "  - Use it for blockchain queries"
            echo "  - Run your blockchain-sync-service against it"
            echo "  - Start collecting historical data"
            ;;
        1)
            print_warning "ERIGON is catching up. Consider:"
            echo "  - Waiting for it to fully sync"
            echo "  - Using a public RPC endpoint temporarily"
            echo "  - Monitoring sync progress"
            ;;
        2)
            print_error "ERIGON is significantly behind. Consider:"
            echo "  - Checking disk space and resources"
            echo "  - Restarting ERIGON if needed"
            echo "  - Using a public RPC endpoint for now"
            echo "  - Checking network connectivity"
            ;;
    esac
    
    echo ""
    print_info "Useful commands:"
    echo "  - Monitor logs: docker logs erigon -f"
    echo "  - Check container: docker ps | grep erigon"
    echo "  - Restart ERIGON: docker restart erigon"
    echo "  - Check disk space: df -h"
}

# Main function
main() {
    echo "=========================================="
    echo "    ERIGON Ethereum Mainnet Sync Checker"
    echo "=========================================="
    echo ""
    
    # Check if ERIGON container is running
    if ! check_erigon_container; then
        print_error "Cannot proceed without running ERIGON container"
        exit 1
    fi
    
    # Check ERIGON RPC connectivity
    local erigon_block
    if ! erigon_block=$(check_erigon_rpc); then
        print_error "Cannot connect to ERIGON RPC"
        exit 1
    fi
    
    # Get public node block
    local public_block
    if ! public_block=$(get_public_block); then
        print_error "Cannot get public node block number"
        exit 1
    fi
    
    # Get detailed sync information
    get_detailed_sync_info
    
    # Check logs
    check_erigon_logs
    
    # Calculate and display sync status
    calculate_sync_status "$erigon_block" "$public_block"
    local status_code=$?
    
    # Show recommendations
    show_recommendations $status_code
    
    exit $status_code
}

# Run main function
main "$@"
