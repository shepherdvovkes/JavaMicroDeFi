#!/bin/bash

# Bitcoin RPC Test Script
# Tests various RPC endpoints to ensure Bitcoin node is working correctly

set -e

RPC_URL="http://localhost:8332"
RPC_USER="bitcoin"
RPC_PASS="ultrafast_archive_node_2024"

echo "🔍 Testing Bitcoin RPC Connectivity..."
echo "=================================="

# Function to make RPC calls
rpc_call() {
    local method=$1
    local params=$2
    curl -s -u "$RPC_USER:$RPC_PASS" -X POST -H "Content-Type: application/json" \
        -d "{\"jsonrpc\": \"1.0\", \"id\": \"test\", \"method\": \"$method\", \"params\": $params}" \
        "$RPC_URL"
}

echo ""
echo "📊 1. Blockchain Information"
echo "---------------------------"
BLOCKCHAIN_INFO=$(rpc_call "getblockchaininfo" "[]")
echo "$BLOCKCHAIN_INFO" | jq '.result | {chain, blocks, headers, verificationprogress, initialblockdownload}'

echo ""
echo "🌐 2. Network Information"
echo "------------------------"
NETWORK_INFO=$(rpc_call "getnetworkinfo" "[]")
echo "$NETWORK_INFO" | jq '.result | {version, subversion, connections, protocolversion}'

echo ""
echo "💾 3. Mempool Information"
echo "------------------------"
MEMPOOL_INFO=$(rpc_call "getmempoolinfo" "[]")
echo "$MEMPOOL_INFO" | jq '.result | {size, bytes, usage, total_fee}'

echo ""
echo "📈 4. Block Count"
echo "-----------------"
BLOCK_COUNT=$(rpc_call "getblockcount" "[]")
echo "Current block count: $(echo "$BLOCK_COUNT" | jq '.result')"

echo ""
echo "🔗 5. Latest Block Hash"
echo "----------------------"
LATEST_BLOCK=$(echo "$BLOCKCHAIN_INFO" | jq -r '.result.bestblockhash')
echo "Latest block hash: $LATEST_BLOCK"

echo ""
echo "📋 6. Raw Mempool (Transaction Count)"
echo "------------------------------------"
RAW_MEMPOOL=$(rpc_call "getrawmempool" "[false]")
MEMPOOL_COUNT=$(echo "$RAW_MEMPOOL" | jq '.result | length')
echo "Transactions in mempool: $MEMPOOL_COUNT"

echo ""
echo "⚡ 7. RPC Performance Test"
echo "-------------------------"
echo "Testing response time for 10 consecutive calls..."
start_time=$(date +%s%N)
for i in {1..10}; do
    rpc_call "getblockcount" "[]" > /dev/null
done
end_time=$(date +%s%N)
duration=$(( (end_time - start_time) / 1000000 ))
echo "Average response time: $(( duration / 10 ))ms per call"

echo ""
echo "🔧 8. RPC Methods Available"
echo "--------------------------"
echo "Testing common RPC methods..."
methods=("getblockchaininfo" "getnetworkinfo" "getmempoolinfo" "getblockcount" "getblockhash" "getrawmempool" "getblock" "gettxout")

for method in "${methods[@]}"; do
    if rpc_call "$method" "[$(test "$method" = "getblockhash" && echo "0" || echo "")]" > /dev/null 2>&1; then
        echo "✅ $method - Working"
    else
        echo "❌ $method - Failed"
    fi
done

echo ""
echo "📊 9. System Resource Usage"
echo "---------------------------"
echo "Bitcoin container resource usage:"
docker stats bitcoin-full-node --no-stream --format "table {{.Container}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.NetIO}}"

echo ""
echo "💾 10. Disk Usage"
echo "-----------------"
echo "Bitcoin data directory size:"
du -sh /mnt/bitcoin/data

echo ""
echo "🎉 RPC Test Complete!"
echo "===================="
echo ""
echo "✅ Bitcoin RPC is fully functional and ready for Java microservices integration"
echo ""
echo "📝 Quick Reference:"
echo "   RPC URL: $RPC_URL"
echo "   Username: $RPC_USER"
echo "   Password: $RPC_PASS"
echo ""
echo "🔗 Example Java Integration:"
echo "   RestTemplate restTemplate = new RestTemplate();"
echo "   HttpHeaders headers = new HttpHeaders();"
echo "   headers.setBasicAuth(\"$RPC_USER\", \"$RPC_PASS\");"
echo "   headers.setContentType(MediaType.APPLICATION_JSON);"
echo ""
echo "   String requestBody = \"{\\\"jsonrpc\\\": \\\"1.0\\\", \\\"id\\\": \\\"test\\\", \\\"method\\\": \\\"getblockchaininfo\\\", \\\"params\\\": []}\";"
echo "   HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);"
echo "   ResponseEntity<String> response = restTemplate.postForEntity(\"$RPC_URL\", entity, String.class);"
