#!/bin/bash

# Ethereum Blockchain Historical Data Backfill Script
# This script helps you backfill historical Ethereum data using the blockchain-sync-service

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default values
DEFAULT_START_BLOCK=18000000
DEFAULT_END_BLOCK=18001000
DEFAULT_BATCH_SIZE=100
DEFAULT_RPC_URL="https://mainnet.infura.io/v3/YOUR_PROJECT_ID"

# Function to print colored output
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

# Function to check if required services are running
check_services() {
    print_info "Checking required services..."
    
    # Check if MongoDB is running
    if ! pgrep -x "mongod" > /dev/null; then
        print_warning "MongoDB is not running. Starting with Docker..."
        docker-compose up -d mongodb
        sleep 10
    fi
    
    # Check if Kafka is running
    if ! pgrep -x "kafka" > /dev/null; then
        print_warning "Kafka is not running. Starting with Docker..."
        docker-compose up -d zookeeper kafka
        sleep 15
    fi
    
    print_success "Required services are running"
}

# Function to validate RPC endpoint
validate_rpc() {
    local rpc_url=$1
    print_info "Validating RPC endpoint: $rpc_url"
    
    # Test RPC endpoint
    response=$(curl -s -X POST -H "Content-Type: application/json" \
        --data '{"jsonrpc":"2.0","method":"eth_blockNumber","params":[],"id":1}' \
        "$rpc_url" 2>/dev/null || echo "error")
    
    if [[ "$response" == "error" ]] || [[ "$response" == *"error"* ]]; then
        print_error "RPC endpoint validation failed. Please check your ETH_RPC_URL"
        exit 1
    fi
    
    latest_block=$(echo "$response" | grep -o '"result":"[^"]*"' | cut -d'"' -f4)
    latest_block_decimal=$((16#${latest_block#0x}))
    
    print_success "RPC endpoint is valid. Latest block: $latest_block_decimal"
    echo "$latest_block_decimal"
}

# Function to get user input
get_user_input() {
    local prompt=$1
    local default=$2
    local value
    
    read -p "$prompt [$default]: " value
    echo "${value:-$default}"
}

# Function to estimate backfill time
estimate_time() {
    local start_block=$1
    local end_block=$2
    local batch_size=$3
    
    local total_blocks=$((end_block - start_block + 1))
    local total_batches=$((total_blocks / batch_size))
    
    # Estimate 2 seconds per batch (conservative estimate)
    local estimated_seconds=$((total_batches * 2))
    local estimated_minutes=$((estimated_seconds / 60))
    local estimated_hours=$((estimated_minutes / 60))
    
    if [ $estimated_hours -gt 0 ]; then
        echo "~${estimated_hours}h ${estimated_minutes}m"
    else
        echo "~${estimated_minutes}m"
    fi
}

# Function to run backfill
run_backfill() {
    local start_block=$1
    local end_block=$2
    local batch_size=$3
    local rpc_url=$4
    
    print_info "Starting backfill process..."
    print_info "Start block: $start_block"
    print_info "End block: $end_block"
    print_info "Batch size: $batch_size"
    print_info "Estimated time: $(estimate_time $start_block $end_block $batch_size)"
    
    # Set environment variables
    export ETH_RPC_URL="$rpc_url"
    export MONGODB_URI="mongodb://localhost:27017/ethereum_chaindata"
    export KAFKA_BROKERS="localhost:9092"
    export RUST_LOG="info"
    
    # Create logs directory
    mkdir -p logs
    
    # Run the backfill command
    print_info "Executing backfill command..."
    
    if command -v cargo &> /dev/null; then
        # Run with cargo if available
        cargo run -- backfill \
            --start-block "$start_block" \
            --end-block "$end_block" \
            --batch-size "$batch_size" 2>&1 | tee "logs/backfill_${start_block}_${end_block}.log"
    else
        # Run with docker if cargo is not available
        print_warning "Cargo not found. Using Docker instead..."
        docker-compose run --rm blockchain-sync backfill \
            --start-block "$start_block" \
            --end-block "$end_block" \
            --batch-size "$batch_size" 2>&1 | tee "logs/backfill_${start_block}_${end_block}.log"
    fi
    
    print_success "Backfill completed!"
}

# Function to show backfill status
show_status() {
    print_info "Checking backfill status..."
    
    if command -v cargo &> /dev/null; then
        cargo run -- status
    else
        docker-compose run --rm blockchain-sync status
    fi
}

# Function to show help
show_help() {
    echo "Ethereum Blockchain Historical Data Backfill Script"
    echo ""
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "Options:"
    echo "  -s, --start-block BLOCK    Start block number (default: $DEFAULT_START_BLOCK)"
    echo "  -e, --end-block BLOCK      End block number (default: $DEFAULT_END_BLOCK)"
    echo "  -b, --batch-size SIZE      Batch size for processing (default: $DEFAULT_BATCH_SIZE)"
    echo "  -r, --rpc-url URL          Ethereum RPC URL"
    echo "  -c, --check                Check service status only"
    echo "  -h, --help                 Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0                                    # Interactive mode"
    echo "  $0 -s 18000000 -e 18001000          # Backfill specific range"
    echo "  $0 -s 18000000 -e 18001000 -b 50    # Backfill with custom batch size"
    echo "  $0 -c                                # Check status only"
    echo ""
    echo "Environment Variables:"
    echo "  ETH_RPC_URL                          Ethereum RPC endpoint"
    echo "  MONGODB_URI                          MongoDB connection string"
    echo "  KAFKA_BROKERS                        Kafka broker addresses"
}

# Main function
main() {
    local start_block=""
    local end_block=""
    local batch_size=""
    local rpc_url=""
    local check_only=false
    
    # Parse command line arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            -s|--start-block)
                start_block="$2"
                shift 2
                ;;
            -e|--end-block)
                end_block="$2"
                shift 2
                ;;
            -b|--batch-size)
                batch_size="$2"
                shift 2
                ;;
            -r|--rpc-url)
                rpc_url="$2"
                shift 2
                ;;
            -c|--check)
                check_only=true
                shift
                ;;
            -h|--help)
                show_help
                exit 0
                ;;
            *)
                print_error "Unknown option: $1"
                show_help
                exit 1
                ;;
        esac
    done
    
    # Check services first
    check_services
    
    # If check only, show status and exit
    if [ "$check_only" = true ]; then
        show_status
        exit 0
    fi
    
    # Get RPC URL
    if [ -z "$rpc_url" ]; then
        rpc_url=$(get_user_input "Enter Ethereum RPC URL" "$DEFAULT_RPC_URL")
    fi
    
    # Validate RPC endpoint
    latest_block=$(validate_rpc "$rpc_url")
    
    # Get backfill parameters
    if [ -z "$start_block" ]; then
        start_block=$(get_user_input "Enter start block number" "$DEFAULT_START_BLOCK")
    fi
    
    if [ -z "$end_block" ]; then
        end_block=$(get_user_input "Enter end block number" "$DEFAULT_END_BLOCK")
    fi
    
    if [ -z "$batch_size" ]; then
        batch_size=$(get_user_input "Enter batch size" "$DEFAULT_BATCH_SIZE")
    fi
    
    # Validate inputs
    if [ "$start_block" -gt "$end_block" ]; then
        print_error "Start block cannot be greater than end block"
        exit 1
    fi
    
    if [ "$end_block" -gt "$latest_block" ]; then
        print_warning "End block ($end_block) is greater than latest block ($latest_block)"
        print_warning "This will only process up to the latest available block"
    fi
    
    # Confirm backfill
    echo ""
    print_info "Backfill Configuration:"
    print_info "  Start block: $start_block"
    print_info "  End block: $end_block"
    print_info "  Batch size: $batch_size"
    print_info "  RPC URL: $rpc_url"
    print_info "  Estimated time: $(estimate_time $start_block $end_block $batch_size)"
    echo ""
    
    read -p "Do you want to proceed with the backfill? (y/N): " confirm
    if [[ ! "$confirm" =~ ^[Yy]$ ]]; then
        print_info "Backfill cancelled"
        exit 0
    fi
    
    # Run backfill
    run_backfill "$start_block" "$end_block" "$batch_size" "$rpc_url"
    
    # Show final status
    echo ""
    show_status
}

# Run main function
main "$@"
