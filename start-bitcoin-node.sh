#!/bin/bash

# Bitcoin Node Startup Script for Java MicroDeFi
# This script starts the Bitcoin full node with proper configuration

set -e

echo "🚀 Starting Bitcoin Full Node for Java MicroDeFi..."

# Check if Bitcoin data exists
if [ ! -d "/mnt/bitcoin/data" ]; then
    echo "❌ Bitcoin data directory not found at /mnt/bitcoin/data"
    exit 1
fi

# Check if Bitcoin configuration exists
if [ ! -f "/mnt/bitcoin/bitcoin.conf" ]; then
    echo "❌ Bitcoin configuration not found at /mnt/bitcoin/bitcoin.conf"
    exit 1
fi

# Create logs directory if it doesn't exist
mkdir -p /mnt/bitcoin/logs

# Check disk space
AVAILABLE_SPACE=$(df /mnt | awk 'NR==2 {print $4}')
if [ "$AVAILABLE_SPACE" -lt 100000000 ]; then  # Less than 100GB
    echo "⚠️  Warning: Low disk space on /mnt. Available: $(df -h /mnt | awk 'NR==2 {print $4}')"
fi

echo "📊 Bitcoin Data Status:"
echo "   Data directory: /mnt/bitcoin/data"
echo "   Data size: $(du -sh /mnt/bitcoin/data | cut -f1)"
echo "   Available space: $(df -h /mnt | awk 'NR==2 {print $4}')"

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker is not running. Please start Docker first."
    exit 1
fi

# Check if the main DeFi network exists
if ! docker network ls | grep -q "javamicrodefi_default"; then
    echo "🔧 Creating DeFi network..."
    cd /home/vovkes/JavaMicroDeFi
    docker network create javamicrodefi_default
fi

# Start Bitcoin node
echo "🔄 Starting Bitcoin containers..."
cd /home/vovkes/JavaMicroDeFi

# Stop existing containers if running
docker-compose -f bitcoin-node-docker-compose.yml down 2>/dev/null || true

# Start Bitcoin node
docker-compose -f bitcoin-node-docker-compose.yml up -d

echo "⏳ Waiting for Bitcoin node to initialize..."
sleep 10

# Check Bitcoin node status
echo "🔍 Checking Bitcoin node status..."
if docker-compose -f bitcoin-node-docker-compose.yml ps | grep -q "Up"; then
    echo "✅ Bitcoin node started successfully!"
    
    # Show container status
    echo ""
    echo "📋 Container Status:"
    docker-compose -f bitcoin-node-docker-compose.yml ps
    
    echo ""
    echo "🌐 Bitcoin RPC Endpoints:"
    echo "   Direct RPC: http://localhost:8332"
    echo "   HTTP Proxy: http://localhost:8080"
    echo "   Health Check: http://localhost:8080/health"
    
    echo ""
    echo "🔧 RPC Configuration:"
    echo "   Username: bitcoin"
    echo "   Password: ultrafast_archive_node_2024"
    
    echo ""
    echo "📊 Monitoring:"
    echo "   Node Exporter: http://localhost:9100"
    
    # Wait for Bitcoin to be ready
    echo ""
    echo "⏳ Waiting for Bitcoin node to be ready..."
    for i in {1..30}; do
        if curl -s -u bitcoin:ultrafast_archive_node_2024 http://localhost:8332 > /dev/null 2>&1; then
            echo "✅ Bitcoin RPC is ready!"
            break
        fi
        echo "   Attempt $i/30: Bitcoin node still initializing..."
        sleep 10
    done
    
    echo ""
    echo "🎉 Bitcoin Full Node is ready for Java Microservices!"
    echo ""
    echo "📝 Next steps:"
    echo "   1. Configure your Java services using bitcoin-service-config.yml"
    echo "   2. Use the RPC endpoints for blockchain data access"
    echo "   3. Monitor the node health at http://localhost:8080/health"
    
else
    echo "❌ Failed to start Bitcoin node. Check logs:"
    docker-compose -f bitcoin-node-docker-compose.yml logs
    exit 1
fi
