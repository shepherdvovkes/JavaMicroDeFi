# DEFIMON API Documentation

## Overview

DEFIMON is a comprehensive DeFi monitoring and trading platform built with a microservices architecture. The system consists of multiple services that communicate through Apache Kafka and provide REST APIs for external access.

## Architecture

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   API Gateway   │────│  Apache Kafka    │────│   MongoDB       │
│  (Spring Boot)  │    │   (Messages)     │    │ (Chain Data)    │
└─────────────────┘    └──────────────────┘    └─────────────────┘
         │
         ├─── Blockchain Sync Service (Rust)
         ├─── Transaction Signing Service (Rust)
         ├─── Math Computing Service (Rust)
         └─── Data Aggregation Service (Rust)
```

## Base URL

All API requests should be made to: `http://localhost:8080`

## Services

### 1. API Gateway (Port 8080)
**Technology**: Java 8 + Spring Cloud Gateway  
**Purpose**: Route requests to appropriate microservices

### 2. Blockchain Sync Service (Port 8081)
**Technology**: Rust  
**Purpose**: Synchronize blockchain data and index events

### 3. Transaction Signing Service (Port 8082)
**Technology**: Rust  
**Purpose**: Secure transaction signing with private key management

### 4. Math Computing Service (Port 8083)
**Technology**: Rust  
**Purpose**: High-speed financial calculations and analytics

### 5. Data Aggregation Service (Port 8084)
**Technology**: Rust  
**Purpose**: Real-time data processing and aggregation

## API Endpoints

### Health Checks

#### Check API Gateway Health
```http
GET /health
```

#### Check Individual Service Health
```http
GET /api/blockchain/health
GET /api/transactions/health
GET /api/calculations/health
GET /api/data/health
```

### Blockchain Data

#### Get Block Information
```http
GET /api/blockchain/blocks/{blockNumber}
```

#### Get Transaction Details
```http
GET /api/blockchain/transactions/{txHash}
```

### Transaction Management

#### Create Wallet
```http
POST /api/transactions/create-wallet
Content-Type: application/json

{
  "password": "secure_password",
  "name": "My Wallet"
}
```

**Response:**
```json
{
  "wallet_id": "uuid-string",
  "address": "0x...",
  "mnemonic": "word1 word2 ... word12"
}
```

#### Sign Transaction
```http
POST /api/transactions/sign
Content-Type: application/json

{
  "wallet_id": "uuid-string",
  "to": "0x...",
  "value": "1000000000000000000",
  "gas_limit": "21000",
  "gas_price": "20000000000",
  "nonce": 42,
  "chain_id": 1
}
```

**Response:**
```json
{
  "transaction_hash": "0x...",
  "signed_transaction": "0x...",
  "signature": {
    "r": "0x...",
    "s": "0x...",
    "v": 27
  }
}
```

### Mathematical Calculations

#### Calculate Option Price (Black-Scholes)
```http
POST /api/calculations/option-price
Content-Type: application/json

{
  "option_type": "Call",
  "spot_price": 100.0,
  "strike_price": 105.0,
  "time_to_expiry": 0.25,
  "risk_free_rate": 0.05,
  "volatility": 0.2,
  "dividend_yield": 0.0
}
```

**Response:**
```json
{
  "price": 2.13,
  "delta": 0.45,
  "gamma": 0.03,
  "theta": -0.02,
  "vega": 0.08,
  "rho": 0.05,
  "calculation_time_ms": 15
}
```

#### Find Arbitrage Opportunities
```http
POST /api/calculations/arbitrage
Content-Type: application/json

{
  "exchanges": [
    {
      "name": "Uniswap",
      "price": 100.0,
      "liquidity": 1000000.0,
      "fee": 0.003
    },
    {
      "name": "SushiSwap", 
      "price": 102.0,
      "liquidity": 500000.0,
      "fee": 0.003
    }
  ],
  "token_pair": {
    "base": "ETH",
    "quote": "USDC"
  },
  "amount": 1000.0,
  "max_slippage": 0.01,
  "gas_costs": [0.01, 0.01]
}
```

**Response:**
```json
{
  "profitable": true,
  "profit_amount": 15.5,
  "profit_percentage": 1.55,
  "optimal_path": [
    {
      "exchange": "Uniswap",
      "action": "buy",
      "amount": 1000.0,
      "price": 100.3
    },
    {
      "exchange": "SushiSwap",
      "action": "sell", 
      "amount": 1000.0,
      "price": 101.7
    }
  ],
  "estimated_gas_cost": 0.02,
  "net_profit": 15.48,
  "calculation_time_ms": 25
}
```

#### Portfolio Optimization
```http
POST /api/calculations/portfolio-optimization
Content-Type: application/json

{
  "assets": [
    {
      "symbol": "ETH",
      "current_price": 2000.0,
      "market_cap": 240000000000.0,
      "daily_volume": 10000000000.0
    },
    {
      "symbol": "BTC",
      "current_price": 45000.0,
      "market_cap": 850000000000.0,
      "daily_volume": 15000000000.0
    }
  ],
  "expected_returns": [0.12, 0.08],
  "covariance_matrix": [
    [0.04, 0.02],
    [0.02, 0.03]
  ],
  "risk_tolerance": 0.5,
  "constraints": {
    "min_weight": 0.1,
    "max_weight": 0.6,
    "max_assets": 10
  }
}
```

### Data Analytics

#### Get Price History
```http
GET /api/data/price-history?symbol=ETH&timeframe=1h&limit=100
```

**Response:**
```json
{
  "symbol": "ETH",
  "timeframe": "1h",
  "total_points": 100,
  "data": [
    {
      "timestamp": 1640995200,
      "symbol": "ETH",
      "price": 2000.0,
      "volume": 1000000.0,
      "source": "blockchain"
    }
  ]
}
```

#### Get OHLCV Data
```http
GET /api/data/aggregated-ohlcv?symbol=ETH&timeframe=1h&limit=24
```

**Response:**
```json
{
  "symbol": "ETH",
  "timeframe": "1h",
  "total_candles": 24,
  "data": [
    {
      "timestamp": 1640995200,
      "symbol": "ETH",
      "open": 1995.0,
      "high": 2005.0,
      "low": 1990.0,
      "close": 2000.0,
      "volume": 1000000.0,
      "timeframe": "1h"
    }
  ]
}
```

#### Get Market Summary
```http
GET /api/data/market-summary
```

**Response:**
```json
{
  "total_market_cap": 1000000000.0,
  "total_volume_24h": 10000000.0,
  "active_pairs": 100,
  "top_gainers": [
    {
      "symbol": "ETH",
      "current_price": 2000.0,
      "price_change_24h": 50.0,
      "price_change_percentage_24h": 2.5
    }
  ],
  "top_losers": [],
  "most_active": [],
  "timestamp": "2024-01-01T00:00:00Z"
}
```

#### Calculate Correlation
```http
POST /api/analytics/correlation
Content-Type: application/json

{
  "symbols": ["ETH", "BTC"],
  "timeframe": "1d",
  "period_days": 30
}
```

**Response:**
```json
{
  "correlation_matrix": [
    [1.0, 0.75],
    [0.75, 1.0]
  ],
  "symbols": ["ETH", "BTC"],
  "period_days": 30,
  "calculation_timestamp": "2024-01-01T00:00:00Z"
}
```

## Error Responses

All endpoints return standard HTTP status codes. Error responses follow this format:

```json
{
  "error": "Error description",
  "code": "ERROR_CODE",
  "timestamp": "2024-01-01T00:00:00Z"
}
```

Common status codes:
- `200` - Success
- `400` - Bad Request
- `401` - Unauthorized
- `404` - Not Found
- `500` - Internal Server Error

## Rate Limiting

API endpoints are rate-limited to prevent abuse:
- Public endpoints: 100 requests per minute
- Calculation endpoints: 50 requests per minute
- Transaction endpoints: 20 requests per minute

## Authentication

Currently, the API does not require authentication for most endpoints. In production, implement JWT-based authentication for sensitive operations like transaction signing.

## Kafka Topics

The system uses the following Kafka topics for inter-service communication:

- `blockchain-events` - Block and transaction events
- `transaction-signing-requests` - Transaction signing requests
- `math-computation-requests` - Mathematical calculation requests
- `aggregated-data` - Processed and aggregated data
- `service-status` - Service health and status updates

## Development

### Running Locally
```bash
# Start all services
./start-services.sh

# Stop all services
./stop-services.sh

# View logs
docker-compose logs -f [service-name]
```

### Environment Variables
See `.env.example` for configuration options.

### Building Services
```bash
# Build all services
docker-compose build

# Build specific service
docker-compose build [service-name]
```
