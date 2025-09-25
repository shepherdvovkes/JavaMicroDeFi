#!/bin/bash

# Simple Ethereum Backfill Script
# This script provides instructions and a simple way to run the backfill

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

print_info "Ethereum Blockchain Historical Data Backfill"
print_info "=============================================="

# Check if we have the required tools
check_requirements() {
    print_info "Checking requirements..."
    
    if ! command -v docker &> /dev/null; then
        print_error "Docker is not installed. Please install Docker first."
        exit 1
    fi
    
    if ! command -v docker-compose &> /dev/null; then
        print_error "Docker Compose is not installed. Please install Docker Compose first."
        exit 1
    fi
    
    print_success "Docker and Docker Compose are available"
}

# Show setup instructions
show_setup_instructions() {
    print_info "Setup Instructions:"
    echo ""
    echo "1. Configure your Ethereum RPC endpoint:"
    echo "   Edit the .env file and set your ETH_RPC_URL:"
    echo "   ETH_RPC_URL=https://mainnet.infura.io/v3/YOUR_PROJECT_ID"
    echo "   OR"
    echo "   ETH_RPC_URL=https://eth-mainnet.alchemyapi.io/v2/YOUR_API_KEY"
    echo ""
    echo "2. Start the required services:"
    echo "   docker-compose up -d mongodb zookeeper kafka"
    echo ""
    echo "3. Wait for services to be ready (about 30 seconds)"
    echo ""
    echo "4. Run the backfill:"
    echo "   ./simple-backfill.sh run --start-block 18000000 --end-block 18000100"
    echo ""
}

# Check if services are running
check_services() {
    print_info "Checking if required services are running..."
    
    if ! docker-compose ps | grep -q "ethereum-mongodb.*Up"; then
        print_warning "MongoDB is not running. Starting it..."
        docker-compose up -d mongodb
        sleep 10
    fi
    
    if ! docker-compose ps | grep -q "ethereum-kafka.*Up"; then
        print_warning "Kafka is not running. Starting it..."
        docker-compose up -d zookeeper kafka
        sleep 15
    fi
    
    print_success "Required services are running"
}

# Run a simple backfill using curl to test RPC
test_rpc() {
    local rpc_url=$1
    print_info "Testing RPC endpoint: $rpc_url"
    
    response=$(curl -s -X POST -H "Content-Type: application/json" \
        --data '{"jsonrpc":"2.0","method":"eth_blockNumber","params":[],"id":1}' \
        "$rpc_url" 2>/dev/null || echo "error")
    
    if [[ "$response" == "error" ]] || [[ "$response" == *"error"* ]]; then
        print_error "RPC endpoint test failed. Please check your ETH_RPC_URL"
        return 1
    fi
    
    latest_block=$(echo "$response" | grep -o '"result":"[^"]*"' | cut -d'"' -f4)
    latest_block_decimal=$((16#${latest_block#0x}))
    
    print_success "RPC endpoint is working. Latest block: $latest_block_decimal"
    return 0
}

# Create a simple data collection script
create_simple_collector() {
    print_info "Creating a simple data collection script..."
    
    cat > simple_ethereum_collector.py << 'EOF'
#!/usr/bin/env python3
"""
Simple Ethereum Data Collector
This script collects basic Ethereum data for the specified block range
"""

import json
import requests
import time
import sys
from datetime import datetime

class EthereumCollector:
    def __init__(self, rpc_url):
        self.rpc_url = rpc_url
        self.session = requests.Session()
    
    def make_request(self, method, params=None):
        """Make a JSON-RPC request to Ethereum node"""
        payload = {
            "jsonrpc": "2.0",
            "method": method,
            "params": params or [],
            "id": 1
        }
        
        try:
            response = self.session.post(self.rpc_url, json=payload, timeout=30)
            response.raise_for_status()
            result = response.json()
            
            if "error" in result:
                raise Exception(f"RPC Error: {result['error']}")
            
            return result["result"]
        except Exception as e:
            print(f"Request failed: {e}")
            return None
    
    def get_block_number(self):
        """Get the latest block number"""
        result = self.make_request("eth_blockNumber")
        if result:
            return int(result, 16)
        return None
    
    def get_block(self, block_number):
        """Get block data by number"""
        block_hex = hex(block_number)
        result = self.make_request("eth_getBlockByNumber", [block_hex, True])
        return result
    
    def collect_block_range(self, start_block, end_block, delay=0.1):
        """Collect data for a range of blocks"""
        print(f"Collecting data from block {start_block} to {end_block}")
        
        collected_data = {
            "start_block": start_block,
            "end_block": end_block,
            "collection_time": datetime.now().isoformat(),
            "blocks": []
        }
        
        for block_num in range(start_block, end_block + 1):
            print(f"Processing block {block_num}...")
            
            block_data = self.get_block(block_num)
            if block_data:
                # Extract key information
                block_info = {
                    "number": int(block_data["number"], 16),
                    "hash": block_data["hash"],
                    "timestamp": int(block_data["timestamp"], 16),
                    "gas_used": int(block_data["gasUsed"], 16),
                    "gas_limit": int(block_data["gasLimit"], 16),
                    "transaction_count": len(block_data["transactions"]),
                    "miner": block_data["miner"],
                    "transactions": []
                }
                
                # Collect transaction data
                for tx in block_data["transactions"]:
                    tx_info = {
                        "hash": tx["hash"],
                        "from": tx.get("from", ""),
                        "to": tx.get("to", ""),
                        "value": int(tx["value"], 16),
                        "gas": int(tx["gas"], 16),
                        "gas_price": int(tx.get("gasPrice", "0x0"), 16),
                        "input_length": len(tx["input"]) - 2  # Remove 0x prefix
                    }
                    block_info["transactions"].append(tx_info)
                
                collected_data["blocks"].append(block_info)
                print(f"  - Collected {len(block_info['transactions'])} transactions")
            else:
                print(f"  - Failed to get block {block_num}")
            
            # Rate limiting
            if delay > 0:
                time.sleep(delay)
        
        return collected_data
    
    def save_data(self, data, filename):
        """Save collected data to JSON file"""
        with open(filename, 'w') as f:
            json.dump(data, f, indent=2)
        print(f"Data saved to {filename}")

def main():
    if len(sys.argv) < 4:
        print("Usage: python3 simple_ethereum_collector.py <RPC_URL> <START_BLOCK> <END_BLOCK>")
        print("Example: python3 simple_ethereum_collector.py https://mainnet.infura.io/v3/YOUR_PROJECT_ID 18000000 18000100")
        sys.exit(1)
    
    rpc_url = sys.argv[1]
    start_block = int(sys.argv[2])
    end_block = int(sys.argv[3])
    
    collector = EthereumCollector(rpc_url)
    
    # Test connection
    print("Testing RPC connection...")
    latest_block = collector.get_block_number()
    if latest_block is None:
        print("Failed to connect to RPC endpoint")
        sys.exit(1)
    
    print(f"Connected! Latest block: {latest_block}")
    
    if end_block > latest_block:
        print(f"Warning: End block {end_block} is greater than latest block {latest_block}")
        end_block = latest_block
    
    # Collect data
    data = collector.collect_block_range(start_block, end_block)
    
    # Save data
    filename = f"ethereum_data_{start_block}_{end_block}.json"
    collector.save_data(data, filename)
    
    print(f"\nCollection complete!")
    print(f"Blocks collected: {len(data['blocks'])}")
    print(f"Total transactions: {sum(len(block['transactions']) for block in data['blocks'])}")
    print(f"Data saved to: {filename}")

if __name__ == "__main__":
    main()
EOF

    chmod +x simple_ethereum_collector.py
    print_success "Created simple_ethereum_collector.py"
}

# Run the simple collector
run_simple_collector() {
    local start_block=$1
    local end_block=$2
    local rpc_url=$3
    
    print_info "Running simple Ethereum data collector..."
    
    if ! command -v python3 &> /dev/null; then
        print_error "Python3 is not installed. Please install Python3 first."
        return 1
    fi
    
    # Test RPC first
    if ! test_rpc "$rpc_url"; then
        return 1
    fi
    
    # Run the collector
    python3 simple_ethereum_collector.py "$rpc_url" "$start_block" "$end_block"
}

# Main function
main() {
    case "${1:-help}" in
        "setup")
            check_requirements
            show_setup_instructions
            ;;
        "run")
            if [ $# -lt 4 ]; then
                print_error "Usage: $0 run --start-block <START> --end-block <END> [--rpc-url <URL>]"
                exit 1
            fi
            
            local start_block=""
            local end_block=""
            local rpc_url=""
            
            # Parse arguments
            shift
            while [[ $# -gt 0 ]]; do
                case $1 in
                    --start-block)
                        start_block="$2"
                        shift 2
                        ;;
                    --end-block)
                        end_block="$2"
                        shift 2
                        ;;
                    --rpc-url)
                        rpc_url="$2"
                        shift 2
                        ;;
                    *)
                        shift
                        ;;
                esac
            done
            
            if [ -z "$start_block" ] || [ -z "$end_block" ]; then
                print_error "Start block and end block are required"
                exit 1
            fi
            
            if [ -z "$rpc_url" ]; then
                # Try to get from .env file
                if [ -f ".env" ]; then
                    rpc_url=$(grep "^ETH_RPC_URL=" .env | cut -d'=' -f2- | tr -d '"' | tr -d "'")
                fi
                
                if [ -z "$rpc_url" ]; then
                    print_error "RPC URL not provided. Use --rpc-url or set ETH_RPC_URL in .env file"
                    exit 1
                fi
            fi
            
            check_requirements
            create_simple_collector
            run_simple_collector "$start_block" "$end_block" "$rpc_url"
            ;;
        "help"|*)
            echo "Ethereum Blockchain Historical Data Backfill"
            echo ""
            echo "Usage:"
            echo "  $0 setup                    - Show setup instructions"
            echo "  $0 run --start-block <N> --end-block <N> [--rpc-url <URL>] - Run backfill"
            echo ""
            echo "Examples:"
            echo "  $0 setup"
            echo "  $0 run --start-block 18000000 --end-block 18000100"
            echo "  $0 run --start-block 18000000 --end-block 18000100 --rpc-url https://mainnet.infura.io/v3/YOUR_PROJECT_ID"
            ;;
    esac
}

main "$@"
