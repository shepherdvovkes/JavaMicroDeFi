# Multi-Chain Service

A plugin-based architecture for supporting multiple blockchain networks with Java 21, Spring Boot 3.2, and virtual threads.

## 🎯 Overview

The Multi-Chain Service provides a unified interface for blockchain operations across different networks, enabling your DeFi platform to support multiple chains without rewriting core logic for each chain.

## 🏗️ Architecture

### Plugin-Based Design
- **Modular Architecture**: Each blockchain is implemented as a plugin
- **Hot-Swappable**: Enable/disable chains dynamically
- **Technology-Optimized**: Use the best technology for each chain (Rust for high-volume, Java for business logic)
- **Unified API**: Single interface for all blockchain operations

### Supported Chains
- **Ethereum**: Rust + Java hybrid for high-performance data processing
- **Bitcoin**: Java 21 with virtual threads for efficient RPC operations
- **Polygon**: EVM-compatible Java implementation
- **Extensible**: Easy to add new chains (BSC, Arbitrum, Solana, etc.)

## 🚀 Features

### Core Features
- **Multi-Chain Support**: Unified API for multiple blockchain networks
- **Plugin Architecture**: Dynamic plugin loading and lifecycle management
- **Virtual Threads**: High concurrency with Java 21 virtual threads
- **Reactive Streams**: Non-blocking I/O with WebFlux
- **Real-time Sync**: Live blockchain data synchronization
- **Transaction Management**: Send, track, and manage transactions
- **Wallet Operations**: Address generation, balance checking, and more

### Advanced Features
- **Health Monitoring**: Comprehensive health checks and metrics
- **Configuration Management**: Chain-specific settings and environment variables
- **Error Handling**: Robust error handling and retry mechanisms
- **Metrics & Observability**: Prometheus metrics and distributed tracing
- **Docker Support**: Containerized deployment with Docker Compose

## 📋 Prerequisites

- **Java 21+**: Required for virtual threads and latest language features
- **Maven 3.9+**: For building the project
- **Docker & Docker Compose**: For containerized deployment
- **MongoDB**: For blockchain data storage
- **Redis**: For caching and session management
- **Apache Kafka**: For event streaming

## 🛠️ Installation

### 1. Clone and Build
```bash
cd multi-chain-service
mvn clean package
```

### 2. Configuration
Create a `.env` file with your API keys:
```bash
# Ethereum
INFURA_PROJECT_ID=your_infura_project_id
ALCHEMY_API_KEY=your_alchemy_api_key

# Bitcoin
BITCOIN_RPC_USERNAME=your_bitcoin_rpc_username
BITCOIN_RPC_PASSWORD=your_bitcoin_rpc_password
```

### 3. Run with Docker Compose
```bash
# Start all services
docker-compose up -d

# Check service status
docker-compose ps

# View logs
docker-compose logs -f multi-chain-service
```

### 4. Verify Installation
```bash
# Check service health
curl http://localhost:8085/multichain/actuator/health

# Get chain information
curl http://localhost:8085/multichain/api/v1/chains/info
```

## 🔧 Configuration

### Application Configuration
The service uses Spring Boot's configuration system with profiles:

- **Development**: `application.yml` (default)
- **Docker**: `application-docker.yml`
- **Production**: `application-production.yml`

### Chain-Specific Settings
Each chain plugin has its own configuration section:

```yaml
multichain:
  plugins:
    ethereum:
      enabled: true
      rpc-url: "https://mainnet.infura.io/v3/${INFURA_PROJECT_ID}"
      block-time: 12000
      sync-strategy: "realtime"
      technology: "rust-java-hybrid"
      
    bitcoin:
      enabled: true
      rpc-url: "http://localhost:8332"
      block-time: 600000
      sync-strategy: "batch"
      technology: "java21"
      
    polygon:
      enabled: true
      rpc-url: "https://polygon-rpc.com"
      block-time: 2000
      sync-strategy: "realtime"
      technology: "java21"
```

## 📚 API Usage

### Unified REST API
All blockchain operations are available through a unified REST API:

#### Get Chain Information
```bash
GET /api/v1/chains/info
```

#### Get Latest Block
```bash
GET /api/v1/chains/{chainId}/blocks/latest
```

#### Get Block by Number
```bash
GET /api/v1/chains/{chainId}/blocks/{blockNumber}
```

#### Get Transaction
```bash
GET /api/v1/chains/{chainId}/transactions/{txHash}
```

#### Send Transaction
```bash
POST /api/v1/chains/{chainId}/transactions
Content-Type: application/json

{
  "to": "0x...",
  "value": "1000000000000000000",
  "gas": "21000",
  "gasPrice": "20000000000",
  "privateKey": "0x...",
  "data": "0x"
}
```

#### Get Balance
```bash
GET /api/v1/chains/{chainId}/addresses/{address}/balance
```

#### Get Sync Status
```bash
GET /api/v1/chains/{chainId}/sync/status
```

#### Enable/Disable Plugin
```bash
POST /api/v1/chains/{chainId}/enable
POST /api/v1/chains/{chainId}/disable
```

### Example Usage
```bash
# Get Ethereum latest block
curl http://localhost:8085/multichain/api/v1/chains/1/blocks/latest

# Get Bitcoin balance
curl http://localhost:8085/multichain/api/v1/chains/bitcoin/addresses/bc1.../balance

# Get Polygon transaction
curl http://localhost:8085/multichain/api/v1/chains/137/transactions/0x...
```

## 🔌 Plugin Development

### Creating a New Plugin
1. Implement the `BlockchainPlugin` interface
2. Create chain-specific configuration class
3. Implement required service interfaces
4. Register the plugin in `PluginManager`

### Example Plugin Structure
```java
@Component
public class CustomChainPlugin implements BlockchainPlugin<CustomChainConfiguration> {
    
    @Override
    public String getChainId() {
        return "custom";
    }
    
    @Override
    public ChainType getChainType() {
        return ChainType.EVM;
    }
    
    @Override
    public void initialize(PluginContext context, CustomChainConfiguration config) {
        // Initialize plugin
    }
    
    // Implement other required methods...
}
```

## 📊 Monitoring & Observability

### Metrics
The service exposes comprehensive metrics via Prometheus:

- **Plugin Metrics**: Plugin health, status, and performance
- **API Metrics**: Request counts, response times, error rates
- **Blockchain Metrics**: Block processing, transaction counts
- **System Metrics**: JVM, memory, virtual thread usage

### Health Checks
- **Plugin Health**: Individual plugin status
- **Service Health**: Overall service health
- **Dependencies**: MongoDB, Redis, Kafka connectivity

### Accessing Metrics
```bash
# Prometheus metrics
curl http://localhost:8085/multichain/actuator/prometheus

# Health status
curl http://localhost:8085/multichain/actuator/health

# Plugin information
curl http://localhost:8085/multichain/actuator/plugins
```

## 🐳 Docker Deployment

### Build Docker Image
```bash
docker build -t multichain-service:latest .
```

### Run with Docker Compose
```bash
# Start all services
docker-compose up -d

# Scale the service
docker-compose up -d --scale multi-chain-service=3

# View logs
docker-compose logs -f multi-chain-service
```

### Environment Variables
```bash
# Required
INFURA_PROJECT_ID=your_infura_project_id
BITCOIN_RPC_USERNAME=your_username
BITCOIN_RPC_PASSWORD=your_password

# Optional
ALCHEMY_API_KEY=your_alchemy_key
POLYGON_INFURA_PROJECT_ID=your_polygon_infura_id
```

## 🔧 Development

### Running Locally
```bash
# Start dependencies
docker-compose up -d mongodb redis kafka

# Run the application
mvn spring-boot:run

# Or with specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=development
```

### Testing
```bash
# Run unit tests
mvn test

# Run integration tests
mvn verify

# Run with coverage
mvn test jacoco:report
```

### Code Quality
```bash
# Check code style
mvn checkstyle:check

# Run static analysis
mvn spotbugs:check

# Format code
mvn spring-javaformat:apply
```

## 🚀 Performance

### Virtual Threads
The service leverages Java 21 virtual threads for high concurrency:
- **Lightweight**: Millions of concurrent connections
- **Efficient**: Optimal for I/O-bound operations
- **Scalable**: Automatic load balancing

### Technology Selection
Each chain uses the optimal technology:
- **Ethereum**: Rust for high-volume data processing
- **Bitcoin**: Java 21 for batch processing
- **Polygon**: Java 21 for EVM compatibility

### Expected Performance
- **Concurrent Requests**: 100K+ with virtual threads
- **Block Processing**: Real-time for fast chains, batch for slow chains
- **Memory Usage**: Optimized with native image support
- **Startup Time**: <5 seconds with GraalVM

## 🔒 Security

### API Security
- **Input Validation**: Comprehensive request validation
- **Rate Limiting**: Built-in rate limiting per endpoint
- **Error Handling**: Secure error responses without sensitive data

### Data Protection
- **Encryption**: TLS for all communications
- **Secrets Management**: Environment variable configuration
- **Access Control**: Plugin-based access control

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

### Development Guidelines
- Follow Java 21 best practices
- Use virtual threads for I/O operations
- Implement comprehensive tests
- Document new plugins and APIs

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🆘 Support

- **Documentation**: Check the `/docs` directory
- **Issues**: Report bugs via GitHub Issues
- **Discussions**: Join GitHub Discussions for questions
- **Email**: Contact the development team

## 🗺️ Roadmap

### Phase 1: Core Infrastructure ✅
- [x] Plugin architecture
- [x] Ethereum, Bitcoin, Polygon plugins
- [x] Unified REST API
- [x] Docker deployment

### Phase 2: Advanced Features
- [ ] BSC, Arbitrum, Avalanche plugins
- [ ] Cross-chain functionality
- [ ] Advanced analytics
- [ ] WebSocket support

### Phase 3: Enterprise Features
- [ ] Solana, Cardano, Polkadot plugins
- [ ] Multi-signature wallets
- [ ] Advanced monitoring
- [ ] High availability

---

**Multi-Chain Service** - Transform your DeFi platform from a dual-chain system into a truly multi-chain powerhouse! 🎯
