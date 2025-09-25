#!/bin/bash

# View Ethereum Mainnet Full Stack Logs
# Shows logs for all services in the Ethereum node stack

set -e

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${BLUE}📋 Ethereum Mainnet Full Stack Logs${NC}"
echo "=============================================="

# Function to show logs for a service
show_logs() {
    local service_name=$1
    local container_name=$2
    local lines=${3:-50}
    
    echo -e "${YELLOW}📄 ${service_name} Logs (last ${lines} lines):${NC}"
    echo "=============================================="
    
    if docker ps --format "table {{.Names}}" | grep -q "^${container_name}$"; then
        docker logs --tail $lines $container_name
    else
        echo -e "${RED}❌ Container ${container_name} is not running${NC}"
    fi
    echo ""
}

# Show logs for each service
show_logs "ERIGON" "ETHEREUM_mainnet_full-erigon" 20
show_logs "Lighthouse" "ETHEREUM_mainnet_full-lighthouse" 20
show_logs "Prometheus" "ETHEREUM_mainnet_full-prometheus" 10
show_logs "Grafana" "ETHEREUM_mainnet_full-grafana" 10
show_logs "Nginx" "ETHEREUM_mainnet_full-nginx" 10
show_logs "Monitor" "ETHEREUM_mainnet_full-monitor" 10

echo -e "${BLUE}💡 Log Commands:${NC}"
echo "=================="
echo -e "📄 Follow ERIGON logs: docker logs -f ETHEREUM_mainnet_full-erigon"
echo -e "📄 Follow Lighthouse logs: docker logs -f ETHEREUM_mainnet_full-lighthouse"
echo -e "📄 Follow all logs: docker-compose -f ethereum-mainnet-full-stack.yml -p ETHEREUM_mainnet_full logs -f"
echo -e "📄 Specific service: docker logs -f ETHEREUM_mainnet_full-<service>"
