#!/bin/bash

# Bitcoin Metrics Pipeline Startup Script
# Starts Bitcoin metrics collection from node to Prometheus/Grafana

set -e

echo "🚀 Starting Bitcoin Metrics Pipeline..."

# Check if Bitcoin node is running
if ! docker ps | grep -q "bitcoin-full-node"; then
    echo "❌ Bitcoin node is not running. Please start it first:"
    echo "   cd /home/vovkes/JavaMicroDeFi"
    echo "   docker-compose -f bitcoin-simple-docker-compose.yml up -d"
    exit 1
fi

# Check if main DeFi services are running
if ! docker ps | grep -q "prometheus"; then
    echo "❌ Prometheus is not running. Please start the main DeFi services first:"
    echo "   cd /home/vovkes/JavaMicroDeFi"
    echo "   docker-compose up -d"
    exit 1
fi

# Build Bitcoin metrics service
echo "🔨 Building Bitcoin metrics service..."
cd /home/vovkes/JavaMicroDeFi/bitcoin-metrics-service
mvn clean package -DskipTests

# Check if build was successful
if [ ! -f "target/bitcoin-metrics-service-1.0.0.jar" ]; then
    echo "❌ Failed to build Bitcoin metrics service"
    exit 1
fi

echo "✅ Bitcoin metrics service built successfully"

# Start Bitcoin metrics service
echo "🚀 Starting Bitcoin metrics service..."
cd /home/vovkes/JavaMicroDeFi
docker-compose -f bitcoin-metrics-docker-compose.yml up -d

# Wait for service to start
echo "⏳ Waiting for Bitcoin metrics service to start..."
sleep 30

# Check service health
echo "🔍 Checking Bitcoin metrics service health..."
if curl -s http://localhost:8082/bitcoin-metrics/actuator/health | grep -q "UP"; then
    echo "✅ Bitcoin metrics service is healthy"
else
    echo "❌ Bitcoin metrics service is not healthy"
    docker logs bitcoin-metrics-service
    exit 1
fi

# Check metrics endpoint
echo "📊 Checking metrics endpoint..."
if curl -s http://localhost:8082/bitcoin-metrics/actuator/prometheus | grep -q "bitcoin_"; then
    echo "✅ Bitcoin metrics are being collected"
else
    echo "❌ Bitcoin metrics are not being collected"
    exit 1
fi

# Restart Prometheus to pick up new configuration
echo "🔄 Restarting Prometheus to pick up Bitcoin metrics..."
docker restart prometheus

# Wait for Prometheus to restart
echo "⏳ Waiting for Prometheus to restart..."
sleep 20

# Check if Prometheus is scraping Bitcoin metrics
echo "🔍 Checking Prometheus targets..."
sleep 10
if curl -s http://localhost:9090/api/v1/targets | grep -q "bitcoin-metrics"; then
    echo "✅ Prometheus is configured to scrape Bitcoin metrics"
else
    echo "❌ Prometheus is not configured to scrape Bitcoin metrics"
    echo "Please check the Prometheus configuration"
fi

# Import Grafana dashboard
echo "📊 Setting up Grafana dashboard..."
if docker ps | grep -q "grafana"; then
    echo "✅ Grafana is running"
    echo "📋 To import the Bitcoin dashboard:"
    echo "   1. Go to http://localhost:3000"
    echo "   2. Login with admin/admin"
    echo "   3. Go to + > Import"
    echo "   4. Upload /home/vovkes/JavaMicroDeFi/bitcoin-grafana-dashboard.json"
    echo "   5. Select Prometheus as data source"
    echo "   6. Click Import"
else
    echo "❌ Grafana is not running"
    echo "Please start Grafana first"
fi

echo ""
echo "🎉 Bitcoin Metrics Pipeline Setup Complete!"
echo "=========================================="
echo ""
echo "📊 Services Status:"
echo "   Bitcoin Node: $(docker ps --filter name=bitcoin-full-node --format '{{.Status}}')"
echo "   Bitcoin Metrics: $(docker ps --filter name=bitcoin-metrics-service --format '{{.Status}}')"
echo "   Prometheus: $(docker ps --filter name=prometheus --format '{{.Status}}')"
echo "   Grafana: $(docker ps --filter name=grafana --format '{{.Status}}')"
echo ""
echo "🌐 Access URLs:"
echo "   Bitcoin RPC: http://localhost:8332"
echo "   Bitcoin Metrics: http://localhost:8082/bitcoin-metrics/actuator/prometheus"
echo "   Prometheus: http://localhost:9090"
echo "   Grafana: http://localhost:3000"
echo ""
echo "📈 Available Metrics:"
echo "   - bitcoin_blockchain_blocks: Current block count"
echo "   - bitcoin_blockchain_verification_progress: Sync progress"
echo "   - bitcoin_network_connections: Network connections"
echo "   - bitcoin_mempool_size: Mempool transaction count"
echo "   - bitcoin_mempool_bytes: Mempool size in bytes"
echo "   - bitcoin_mempool_total_fee: Total fees in mempool"
echo "   - bitcoin_blockchain_difficulty: Mining difficulty"
echo "   - bitcoin_rpc_call_duration_seconds: RPC response time"
echo ""
echo "🔧 Management Commands:"
echo "   View metrics: curl http://localhost:8082/bitcoin-metrics/actuator/prometheus"
echo "   Check health: curl http://localhost:8082/bitcoin-metrics/actuator/health"
echo "   View logs: docker logs bitcoin-metrics-service"
echo "   Restart service: docker restart bitcoin-metrics-service"
echo ""
echo "✅ Your Bitcoin metrics pipeline is ready for monitoring!"
