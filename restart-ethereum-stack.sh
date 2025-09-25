#!/bin/bash

# Restart Ethereum Mainnet Full Stack
# Stops and starts the complete Ethereum node stack

set -e

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${BLUE}🔄 Restarting Ethereum Mainnet Full Stack${NC}"
echo "=============================================="

# Stop the stack
echo -e "${YELLOW}🛑 Stopping current stack...${NC}"
./stop-ethereum-stack.sh

# Wait a moment
echo -e "${YELLOW}⏳ Waiting 5 seconds...${NC}"
sleep 5

# Start the stack
echo -e "${YELLOW}🚀 Starting stack...${NC}"
./start-ethereum-stack.sh

echo ""
echo -e "${GREEN}🎉 Ethereum Mainnet Full Stack Restarted${NC}"
