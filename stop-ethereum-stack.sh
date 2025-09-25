#!/bin/bash

# Stop Ethereum Mainnet Full Stack
# Gracefully stops all Ethereum node services

set -e

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}🛑 Stopping Ethereum Mainnet Full Stack${NC}"
echo "=============================================="

# Stop the stack
echo -e "${YELLOW}🐳 Stopping Docker containers...${NC}"
docker-compose -f ethereum-mainnet-full-stack.yml -p ETHEREUM_mainnet_full down

# Wait for graceful shutdown
echo -e "${YELLOW}⏳ Waiting for graceful shutdown...${NC}"
sleep 10

# Check if any containers are still running
echo -e "${BLUE}📊 Checking remaining containers...${NC}"
REMAINING=$(docker ps --format "table {{.Names}}" | grep "ETHEREUM_mainnet_full" | wc -l)

if [ $REMAINING -eq 0 ]; then
    echo -e "${GREEN}✅ All containers stopped successfully${NC}"
else
    echo -e "${YELLOW}⚠️  ${REMAINING} containers still running${NC}"
    echo "Remaining containers:"
    docker ps --format "table {{.Names}}" | grep "ETHEREUM_mainnet_full"
fi

echo ""
echo -e "${GREEN}🎉 Ethereum Mainnet Full Stack Stopped${NC}"
echo ""
echo -e "${BLUE}💡 To restart the stack:${NC}"
echo "========================"
echo -e "🚀 Start stack: ./start-ethereum-stack.sh"
echo -e "🔄 Restart stack: ./restart-ethereum-stack.sh"
