# Ethereum Historical Data Backfill Guide

This guide will help you backfill historical Ethereum data using the blockchain-sync-service.

## Quick Start

### 1. Prerequisites

Make sure you have:
- Docker and Docker Compose installed
- An Ethereum RPC endpoint (Infura, Alchemy, or local node)
- At least 2GB of free disk space

### 2. Setup Environment

```bash
# Copy environment configuration
cp env.example .env

# Edit the .env file with your RPC endpoint
nano .env
```

Update the following in `.env`:
```bash
ETH_RPC_URL=https://mainnet.infura.io/v3/YOUR_PROJECT_ID
# OR
ETH_RPC_URL=https://eth-mainnet.alchemyapi.io/v2/YOUR_API_KEY
```

### 3. Start Required Services

```bash
# Start MongoDB and Kafka
docker-compose up -d mongodb zookeeper kafka

# Wait for services to be ready (about 30 seconds)
sleep 30
```

### 4. Run Backfill

#### Option A: Interactive Mode
```bash
./backfill.sh
```

#### Option B: Command Line Mode
```bash
# Backfill a small range (100 blocks)
./backfill.sh -s 18000000 -e 18000100 -b 50

# Backfill a larger range (1000 blocks)
./backfill.sh -s 18000000 -e 18001000 -b 100
```

#### Option C: Using Docker
```bash
# If you don't have Rust installed
docker-compose run --rm blockchain-sync backfill --start-block 18000000 --end-block 18000100 --batch-size 50
```

## Backfill Examples

### Example 1: Small Test Backfill
```bash
# Backfill 100 blocks starting from block 18,000,000
./backfill.sh -s 18000000 -e 18000100 -b 25
```

### Example 2: Medium Range Backfill
```bash
# Backfill 1000 blocks with larger batches
./backfill.sh -s 18000000 -e 18001000 -b 100
```

### Example 3: Large Historical Backfill
```bash
# Backfill 10,000 blocks (be patient!)
./backfill.sh -s 18000000 -e 18010000 -b 200
```

## Monitoring Progress

### Check Service Status
```bash
./backfill.sh -c
```

### View Logs
```bash
# View real-time logs
tail -f logs/backfill_*.log

# View specific backfill log
cat logs/backfill_18000000_18000100.log
```

### Monitor Database
```bash
# Access MongoDB Express (web interface)
open http://localhost:8081

# Or use MongoDB shell
docker exec -it ethereum-mongodb mongosh
```

### Monitor Kafka
```bash
# Access Kafka UI (web interface)
open http://localhost:8080
```

## Performance Tips

### 1. Optimize Batch Size
- **Small batches (25-50)**: More reliable, slower
- **Medium batches (100-200)**: Good balance
- **Large batches (500+)**: Faster, but may hit rate limits

### 2. Choose Appropriate Block Range
- **Recent blocks**: Higher transaction volume, more data
- **Older blocks**: Lower transaction volume, less data
- **Peak periods**: More DeFi activity, more events

### 3. RPC Endpoint Considerations
- **Free tier**: Rate limited, use smaller batches
- **Paid tier**: Higher limits, can use larger batches
- **Local node**: No rate limits, fastest option

## Data Collected

The backfill process collects:

### Blocks
- Block number, hash, timestamp
- Gas usage and limits
- Miner information
- Transaction count

### Transactions
- Transaction hash, from/to addresses
- Value in Wei and ETH
- Gas price and usage
- Input data and transaction type

### Events
- Contract events with decoded parameters
- Token transfers (ERC-20, ERC-721, ERC-1155)
- DeFi protocol events (Uniswap, Aave, etc.)

### Storage Locations
- **MongoDB**: Persistent storage in `ethereum_chaindata` database
- **Kafka**: Real-time streaming to multiple topics

## Troubleshooting

### Common Issues

#### 1. RPC Rate Limiting
```
Error: Too many requests
```
**Solution**: Reduce batch size or use a paid RPC endpoint

#### 2. MongoDB Connection Issues
```
Error: Failed to connect to MongoDB
```
**Solution**: Ensure MongoDB is running with `docker-compose up -d mongodb`

#### 3. Kafka Connection Issues
```
Error: Failed to connect to Kafka
```
**Solution**: Start Kafka with `docker-compose up -d kafka`

#### 4. Out of Memory
```
Error: Out of memory
```
**Solution**: Reduce batch size or increase Docker memory limits

### Debug Mode
```bash
# Enable debug logging
export RUST_LOG=debug
./backfill.sh -s 18000000 -e 18000100 -b 10
```

## Expected Performance

### Processing Speed
- **Small batches (25 blocks)**: ~2-3 blocks/second
- **Medium batches (100 blocks)**: ~5-10 blocks/second
- **Large batches (200+ blocks)**: ~10-20 blocks/second

### Data Volume (per 1000 blocks)
- **Blocks**: ~1MB
- **Transactions**: ~10-50MB (varies by activity)
- **Events**: ~5-20MB (varies by DeFi activity)

### Time Estimates
- **100 blocks**: ~1-2 minutes
- **1,000 blocks**: ~5-15 minutes
- **10,000 blocks**: ~1-3 hours

## Next Steps

After backfill completion:

1. **Verify Data**: Check MongoDB collections
2. **Start Real-time Sync**: `./blockchain-sync-service sync`
3. **Monitor Performance**: Use Grafana dashboards
4. **Set up Alerts**: Configure monitoring alerts

## Support

If you encounter issues:
1. Check the logs in `logs/` directory
2. Verify all services are running
3. Check your RPC endpoint configuration
4. Review the troubleshooting section above
