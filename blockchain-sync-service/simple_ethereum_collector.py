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
