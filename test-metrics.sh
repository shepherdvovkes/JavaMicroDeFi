#!/bin/bash

# Test script to demonstrate the monitoring setup

echo "🚀 Testing Blockchain Sync Service Monitoring Setup"
echo "=================================================="

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${BLUE}📊 Checking Monitoring Services...${NC}"

# Check Prometheus
echo -e "\n${YELLOW}1. Prometheus Status:${NC}"
if curl -s -o /dev/null -w "%{http_code}" http://localhost:9091 | grep -q "200\|302"; then
    echo -e "   ✅ Prometheus is running on http://localhost:9091"
else
    echo -e "   ❌ Prometheus is not accessible"
fi

# Check Grafana
echo -e "\n${YELLOW}2. Grafana Status:${NC}"
if curl -s -o /dev/null -w "%{http_code}" http://localhost:3000 | grep -q "200\|302"; then
    echo -e "   ✅ Grafana is running on http://localhost:3000"
    echo -e "   📝 Login: admin / defimon123"
else
    echo -e "   ❌ Grafana is not accessible"
fi

# Check Docker services
echo -e "\n${YELLOW}3. Docker Services Status:${NC}"
docker-compose ps --format "table {{.Name}}\t{{.State}}\t{{.Ports}}"

# Check if metrics endpoint would be available
echo -e "\n${YELLOW}4. Expected Metrics Endpoint:${NC}"
echo -e "   📡 Blockchain Sync Service: http://localhost:9090/metrics"
echo -e "   📡 Prometheus: http://localhost:9091"
echo -e "   📡 Grafana: http://localhost:3000"

echo -e "\n${GREEN}🎉 Monitoring Setup Complete!${NC}"
echo -e "\n${BLUE}📈 Access Your Dashboards:${NC}"
echo -e "   • Grafana: http://localhost:3000 (admin/defimon123)"
echo -e "   • Prometheus: http://localhost:9091"
echo -e "   • Pre-configured dashboard: 'Blockchain Sync Service Dashboard'"

echo -e "\n${BLUE}📋 Next Steps:${NC}"
echo -e "   1. Configure your ETH_RPC_URL in blockchain-sync-service/.env"
echo -e "   2. Start the blockchain-sync-service with: docker-compose up -d blockchain-sync"
echo -e "   3. View real-time metrics in Grafana"
echo -e "   4. Monitor sync progress, errors, and performance"

echo -e "\n${YELLOW}💡 Tip: The blockchain-sync-service will start collecting metrics once it begins processing blocks!${NC}"
