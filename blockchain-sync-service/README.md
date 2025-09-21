# Ethereum Blockchain Sync Service

A comprehensive, production-ready Ethereum blockchain data collection service built in Rust. This service provides real-time synchronization of Ethereum blockchain data including blocks, transactions, smart contract events, token transfers, and DeFi protocol interactions.

## Features

### Core Functionality
- **Real-time Block Synchronization**: Continuously syncs Ethereum blocks with configurable delays
- **Comprehensive Transaction Processing**: Captures all transaction data including gas fees, access lists, and EIP-1559 support
- **Smart Contract Event Processing**: Decodes and processes contract events with ABI support
- **Token Transfer Detection**: Automatically detects ERC-20, ERC-721, and ERC-1155 token transfers
- **DeFi Protocol Integration**: Built-in support for major DeFi protocols (Uniswap, Aave, Compound, etc.)
- **Historical Data Backfill**: Process historical blocks with batch processing capabilities

### Data Storage & Streaming
- **MongoDB Integration**: Persistent storage with optimized indexes for fast queries
- **Kafka Streaming**: Real-time event streaming to multiple Kafka topics
- **Data Models**: Comprehensive data structures for all Ethereum entities

### Reliability & Monitoring
- **Error Handling**: Comprehensive error handling with retry logic and circuit breakers
- **Health Monitoring**: Built-in health checks and status reporting
- **Performance Metrics**: Detailed metrics for monitoring service performance
- **Graceful Degradation**: Continues operation even when individual components fail

### Advanced Features
- **Multi-RPC Support**: Support for multiple Ethereum RPC providers with failover
- **Configurable Processing**: Enable/disable specific data processing features
- **Batch Processing**: Efficient batch processing for historical data
- **Command Line Interface**: Full CLI with multiple operation modes

## Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Ethereum      │    │   Blockchain    │    │   Ethereum      │
│   RPC Node      │◄──►│   Client        │◄──►│   Processor     │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                │                        │
                                ▼                        ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   MongoDB       │◄──►│   Main Service  │◄──►│   Kafka         │
│   Database      │    │                 │    │   Producer      │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                │
                                ▼
                       ┌─────────────────┐
                       │   Error         │
                       │   Handler       │
                       └─────────────────┘
```

## Installation

### Prerequisites
- Rust 1.70+ with Cargo
- MongoDB 4.4+
- Apache Kafka 2.8+
- Access to Ethereum RPC endpoint (Infura, Alchemy, or local node)

### Build from Source

```bash
# Clone the repository
git clone <repository-url>
cd blockchain-sync-service

# Build the service
cargo build --release

# Run tests
cargo test
```

### Docker

```bash
# Build Docker image
docker build -t blockchain-sync-service .

# Run with docker-compose
docker-compose up -d
```

## Configuration

### Environment Variables

Copy `env.example` to `.env` and configure:

```bash
cp env.example .env
```

Key configuration options:

- `ETH_RPC_URL`: Ethereum RPC endpoint
- `KAFKA_BROKERS`: Kafka broker addresses
- `MONGODB_URI`: MongoDB connection string
- `LOG_LEVEL`: Logging level (error, warn, info, debug, trace)

### RPC Provider Setup

#### Infura
```bash
export ETH_RPC_URL="https://mainnet.infura.io/v3/YOUR_PROJECT_ID"
```

#### Alchemy
```bash
export ETH_RPC_URL="https://eth-mainnet.alchemyapi.io/v2/YOUR_API_KEY"
```

#### Local Node
```bash
export ETH_RPC_URL="http://localhost:8545"
```

## Usage

### Command Line Interface

The service provides a comprehensive CLI with multiple operation modes:

#### Start Real-time Sync
```bash
# Start sync from last processed block
./blockchain-sync-service sync

# Start from specific block
./blockchain-sync-service sync --start-block 18000000

# Process limited number of blocks
./blockchain-sync-service sync --max-blocks 1000

# Custom delay between blocks
./blockchain-sync-service sync --block-delay-ms 500
```

#### Historical Data Backfill
```bash
# Backfill specific block range
./blockchain-sync-service backfill --start-block 18000000 --end-block 18001000

# Backfill with custom batch size
./blockchain-sync-service backfill --start-block 18000000 --end-block 18001000 --batch-size 50
```

#### Service Management
```bash
# Check service status
./blockchain-sync-service status

# Reset sync status
./blockchain-sync-service reset --block-number 18000000
```

### Programmatic Usage

```rust
use blockchain_sync_service::BlockchainSyncService;

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let mut service = BlockchainSyncService::new().await?;
    
    // Start real-time sync
    service.start_sync(None, 0, 100).await?;
    
    Ok(())
}
```

## Data Models

### Block Data
- Block number, hash, timestamp
- Gas usage and limits
- Miner information
- Transaction count
- EIP-1559 base fee information

### Transaction Data
- Transaction hash, from/to addresses
- Value in Wei and ETH
- Gas price and usage
- Input data and length
- Transaction type (legacy, EIP-1559, EIP-2930)
- Access list for EIP-2930 transactions

### Event Data
- Contract address and event signature
- Decoded event parameters
- Log index and topics
- Transaction and block references

### Token Transfer Data
- Token type (ERC-20, ERC-721, ERC-1155)
- From/to addresses
- Transfer amount or token ID
- Contract address

### DeFi Event Data
- Protocol name and event type
- User addresses and amounts
- Pool and token information
- Event-specific parameters

## Kafka Topics

The service publishes to multiple Kafka topics:

- `ethereum-blocks`: Block events
- `ethereum-transactions`: Transaction events
- `ethereum-events`: Contract events
- `ethereum-receipts`: Transaction receipts
- `ethereum-token-transfers`: Token transfer events
- `ethereum-defi-events`: DeFi protocol events
- `ethereum-sync-status`: Service status updates
- `ethereum-service-health`: Health check events

## MongoDB Collections

Data is stored in the following MongoDB collections:

- `blocks`: Block data with indexes on block number and hash
- `transactions`: Transaction data with indexes on addresses and timestamps
- `events`: Contract events with indexes on contract addresses
- `token_transfers`: Token transfer events
- `defi_events`: DeFi protocol events
- `smart_contracts`: Smart contract information
- `address_balances`: Address balance snapshots
- `sync_status`: Service synchronization status
- `processing_errors`: Error logs and retry information

## Monitoring & Observability

### Health Checks
```bash
# Check service health
curl http://localhost:8080/health

# Get detailed status
./blockchain-sync-service status
```

### Metrics
The service exposes Prometheus metrics on `/metrics` endpoint:

- `blocks_processed_total`: Total blocks processed
- `transactions_processed_total`: Total transactions processed
- `events_processed_total`: Total events processed
- `sync_lag_blocks`: Current sync lag in blocks
- `error_rate`: Current error rate
- `uptime_seconds`: Service uptime

### Logging
Structured logging with configurable levels:

```bash
# Set log level
export RUST_LOG=debug
./blockchain-sync-service sync
```

## Performance Tuning

### RPC Optimization
- Use multiple RPC endpoints for load balancing
- Implement request batching where possible
- Configure appropriate timeouts and retry policies

### Database Optimization
- Ensure proper indexes are created
- Use connection pooling
- Consider read replicas for query workloads

### Kafka Optimization
- Configure appropriate batch sizes and linger times
- Use compression for better throughput
- Monitor consumer lag

## Error Handling

The service includes comprehensive error handling:

- **Retry Logic**: Automatic retry with exponential backoff
- **Circuit Breakers**: Prevent cascading failures
- **Error Classification**: Different handling for different error types
- **Graceful Degradation**: Continue operation when possible

## Security Considerations

- Use secure RPC endpoints with API keys
- Encrypt MongoDB connections
- Secure Kafka broker access
- Implement proper authentication and authorization
- Monitor for suspicious activity

## Troubleshooting

### Common Issues

#### RPC Connection Errors
```bash
# Check RPC endpoint
curl -X POST -H "Content-Type: application/json" \
  --data '{"jsonrpc":"2.0","method":"eth_blockNumber","params":[],"id":1}' \
  $ETH_RPC_URL
```

#### MongoDB Connection Issues
```bash
# Test MongoDB connection
mongosh $MONGODB_URI
```

#### Kafka Connection Issues
```bash
# List Kafka topics
kafka-topics --bootstrap-server localhost:9092 --list
```

### Debug Mode
```bash
# Enable debug logging
export RUST_LOG=debug
./blockchain-sync-service sync
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

For support and questions:
- Create an issue in the repository
- Check the documentation
- Review the troubleshooting guide

## Roadmap

- [ ] Support for other EVM-compatible chains
- [ ] WebSocket-based real-time updates
- [ ] Advanced analytics and reporting
- [ ] Machine learning integration for anomaly detection
- [ ] GraphQL API for data access
- [ ] Multi-chain support
- [ ] Advanced caching layer
- [ ] Real-time alerting system
