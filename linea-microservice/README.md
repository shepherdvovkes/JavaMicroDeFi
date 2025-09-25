# Linea Microservice - DEFIMON Blockchain Data Collection

A comprehensive Java 21 microservice for collecting, storing, and serving Linea blockchain data with real-time monitoring and archive collection capabilities.

## 🚀 Features

- **Real-time Data Collection**: 10 concurrent workers collecting blocks, transactions, accounts, contracts, tokens, and DeFi data
- **Archive Collection**: Complete blockchain history from genesis to current block
- **REST API Server**: Spring Boot-based metrics server with comprehensive endpoints
- **Database Storage**: SQLite databases for both real-time and archive data
- **Progress Tracking**: Real-time progress monitoring and statistics
- **Error Handling**: Robust error handling with retry logic and rate limiting

## 🏗️ Architecture

### Core Components

1. **LineaSyncService**: Main synchronization service with 10 concurrent workers
2. **LineaConfiguration**: Comprehensive configuration management
3. **JPA Entities**: Database models for blocks, transactions, accounts, and metrics
4. **Repositories**: Spring Data repositories for data access
5. **REST API**: Comprehensive API endpoints for data access

### Worker Distribution

- **Workers 1-3**: Block data collection
- **Workers 4-6**: Account data collection  
- **Workers 7-8**: Token data collection
- **Workers 9-10**: DeFi protocol data collection

## 🛠️ Technology Stack

- **Java 21**: Latest LTS with virtual threads for high-performance I/O
- **Spring Boot 3.2**: Modern Spring framework with reactive programming
- **SQLite**: Lightweight database for data storage
- **Web3J**: Ethereum/Linea blockchain integration
- **Docker**: Containerization for easy deployment
- **Maven**: Build and dependency management

## 📋 Prerequisites

- **Java 21+**: Required for virtual threads and latest language features
- **Maven 3.9+**: For building the project
- **Docker**: For containerized deployment
- **Disk Space**: At least 100GB for data storage on `/mnt/sata18tb`

## 🚀 Quick Start

### 1. Clone and Build

```bash
git clone <repository-url>
cd linea-microservice
mvn clean package -DskipTests
```

### 2. Configure Environment

Update `src/main/resources/application.yml` with your Linea RPC endpoints:

```yaml
linea:
  rpc-url: https://your-linea-rpc-endpoint/
  wss-url: wss://your-linea-wss-endpoint/
  database-path: /mnt/sata18tb/linea_data.db
  archive-database-path: /mnt/sata18tb/linea_archive_data.db
```

### 3. Run with Docker

```bash
# Build and run with Docker Compose
docker-compose up -d

# Check logs
docker-compose logs -f linea-microservice

# Check health
curl http://localhost:8008/api/actuator/health
```

### 4. Run Locally

```bash
# Run the application
java --enable-preview -jar target/linea-microservice-1.0.0.jar

# Or with Maven
mvn spring-boot:run
```

## 📊 API Endpoints

### Base URL: `http://localhost:8008/api`

#### System Endpoints
- `GET /actuator/health` - Health check
- `GET /actuator/metrics` - Application metrics
- `GET /actuator/info` - Application information

#### Block Endpoints
- `GET /blocks/latest` - Latest block information
- `GET /blocks/{blockNumber}` - Get specific block
- `GET /blocks` - List blocks with pagination

#### Transaction Endpoints
- `GET /transactions/{txHash}` - Get transaction details
- `GET /transactions` - List transactions with filters

#### Account Endpoints
- `GET /accounts/{address}` - Get account information
- `GET /accounts` - List accounts with filters

#### Metrics Endpoints
- `GET /metrics/network` - Network metrics
- `GET /metrics/summary` - Metrics summary
- `GET /stats` - Collection statistics

## ⚙️ Configuration

### Collection Intervals
- Block collection: 2 seconds
- Transaction collection: 1 second
- Account collection: 5 seconds
- Contract collection: 10 seconds
- Token collection: 15 seconds
- DeFi collection: 30 seconds

### Rate Limiting
- RPC rate limit: 100 requests/second
- WebSocket rate limit: 50 requests/second

### Archive Settings
- Batch size: 1000 blocks
- Concurrent workers: 10
- Max retries: 3
- Retry delay: 5 seconds

## 📈 Data Collection

### Real-time Collection (10 Workers)
1. **Workers 1-3**: Block data collection
2. **Workers 4-6**: Account data collection
3. **Workers 7-8**: Token data collection
4. **Workers 9-10**: DeFi protocol data collection

### Archive Collection
- Collects complete blockchain history from genesis
- Uses 10 concurrent workers for parallel processing
- Progress tracking and error handling
- Batch processing with configurable batch sizes

## 🗄️ Database Schema

### Real-time Tables
- `linea_blocks` - Block data
- `linea_transactions` - Transaction data
- `linea_transaction_receipts` - Transaction receipts
- `linea_network_metrics` - Network metrics
- `linea_accounts` - Account data
- `linea_contracts` - Contract data
- `linea_tokens` - Token data
- `linea_defi_protocols` - DeFi protocol data
- `linea_bridge_transactions` - Bridge transactions

### Archive Tables
- Same structure with `linea_archive_` prefix
- `linea_archive_progress` - Collection progress tracking

## 🔧 Monitoring

### Logs
- Application logs: `/mnt/sata18tb/logs/linea_collector.log`
- Log rotation: 100MB per file, 30 days retention

### Metrics
- Prometheus metrics: `http://localhost:8008/api/actuator/prometheus`
- Health check: `http://localhost:8008/api/actuator/health`
- Application info: `http://localhost:8008/api/actuator/info`

### Statistics
Real-time statistics available via API:
- Blocks collected
- Transactions collected
- Accounts collected
- Contracts collected
- Tokens collected
- DeFi protocols collected
- Error counts
- Progress percentage

## 🚀 Performance

### Expected Performance
- **Real-time Collection**: ~2-5 blocks per second
- **Archive Collection**: ~100-500 blocks per second
- **API Response Time**: <100ms for most endpoints
- **Database Size**: ~1-10GB per million blocks

### Optimization
- Virtual threads for high-concurrency I/O operations
- Connection pooling for database access
- Caching for frequently accessed data
- Batch processing for archive collection

## 🔒 Security

- Rate limiting to prevent API abuse
- Input validation on all endpoints
- Error handling without sensitive data exposure
- CORS configuration for web access

## 📞 Support

For issues or questions:
1. Check logs for error messages
2. Verify configuration settings
3. Monitor system resources
4. Review API documentation

## 🎯 Getting Started

1. **Quick Start**
   ```bash
   # Build and run
   mvn clean package
   java --enable-preview -jar target/linea-microservice-1.0.0.jar
   ```

2. **Access API**
   - Open browser to `http://localhost:8008/api`
   - View API docs at `http://localhost:8008/api/swagger-ui.html`

3. **Monitor Progress**
   - Check logs for collection progress
   - Use API endpoints for real-time statistics

## 🔄 System Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   LINEA RPC     │    │  Data Collector │    │   SQLite DB     │
│   Endpoints     │◄───┤   (10 Workers)  │◄───┤  (Real-time)    │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                │
                                ▼
                       ┌─────────────────┐    ┌─────────────────┐
                       │ Archive Collector│    │   SQLite DB     │
                       │   (10 Workers)  │◄───┤   (Archive)     │
                       └─────────────────┘    └─────────────────┘
                                │
                                ▼
                       ┌─────────────────┐
                       │  REST API       │
                       │  (Port 8008)    │
                       └─────────────────┘
```

This system provides a complete solution for Linea blockchain data collection, storage, and serving with high performance and reliability.
