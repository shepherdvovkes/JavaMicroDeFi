# Ethereum Prediction Service

A comprehensive microservice for Ethereum (ETH) price prediction using multiple data sources and machine learning models.

## 🎯 Overview

The Ethereum Prediction Service collects data from various sources that affect ETH price and provides intelligent price predictions using ensemble machine learning models.

## 🏗️ Architecture

### Data Sources
- **Market Data**: CoinGecko, CoinMarketCap
- **On-Chain Metrics**: Etherscan, blockchain nodes
- **Sentiment Analysis**: Twitter, Reddit, News APIs
- **Technical Analysis**: RSI, MACD, Bollinger Bands, Moving Averages
- **Macro-Economic**: FRED API, Alpha Vantage (DXY, VIX, Interest Rates)
- **DeFi Analytics**: DeFiLlama, Dune Analytics

### Prediction Models
- **Technical Analysis Model**: Based on price indicators and patterns
- **Sentiment Model**: Social media and news sentiment analysis
- **Macro-Economic Model**: Economic indicators correlation
- **DeFi Model**: DeFi metrics and protocol activity
- **Ensemble Model**: Weighted combination of all models

## 🚀 Features

### Core Functionality
- Real-time price predictions for multiple time horizons (1h, 24h, 7d, 30d)
- Comprehensive factor analysis
- Risk metrics calculation (VaR, Expected Shortfall, Volatility)
- Market condition assessment
- Confidence scoring for predictions

### Data Collection
- Multi-source data aggregation
- Real-time streaming updates
- Historical data analysis
- Error handling and fallback mechanisms

### API Endpoints
- RESTful API with comprehensive endpoints
- Server-Sent Events for real-time predictions
- Health monitoring and metrics
- Factor analysis and risk assessment

## 📊 Data Sources & APIs

### Market Data APIs
| Service | Purpose | Rate Limit | Key Data |
|---------|---------|------------|----------|
| **CoinGecko** | Price, volume, market cap | 50/min | Real-time ETH data |
| **CoinMarketCap** | Professional market data | 100/month | Institutional metrics |
| **Binance** | Trading data, order book | 1200/min | Exchange metrics |

### On-Chain Data APIs
| Service | Purpose | Rate Limit | Key Data |
|---------|---------|------------|----------|
| **Etherscan** | Blockchain metrics | 5/sec | Gas, transactions, blocks |
| **Infura** | Network data | 100k/day | Block data, network stats |
| **Alchemy** | Enhanced blockchain data | 300M/month | Smart contract analytics |

### Sentiment Analysis APIs
| Service | Purpose | Rate Limit | Key Data |
|---------|---------|------------|----------|
| **Twitter API v2** | Social sentiment | 300/15min | Tweet sentiment analysis |
| **Reddit API** | Community sentiment | 60/min | Discussion sentiment |
| **NewsAPI** | News sentiment | 1000/day | News article analysis |
| **CryptoPanic** | Crypto news aggregation | 100/min | Crypto-specific news |

### Macro-Economic APIs
| Service | Purpose | Rate Limit | Key Data |
|---------|---------|------------|----------|
| **FRED API** | Economic indicators | 120/min | Interest rates, inflation |
| **Alpha Vantage** | Market indicators | 5/min | DXY, VIX, commodities |
| **World Bank API** | Global economic data | 1000/day | GDP, unemployment |

### DeFi Analytics APIs
| Service | Purpose | Rate Limit | Key Data |
|---------|---------|------------|----------|
| **DeFiLlama** | DeFi TVL and protocols | 100/min | Total Value Locked |
| **Dune Analytics** | Custom on-chain queries | 1000/hour | Protocol metrics |
| **DeFi Pulse** | DeFi protocol rankings | 1000/day | Protocol analytics |

### Technical Analysis APIs
| Service | Purpose | Rate Limit | Key Data |
|---------|---------|------------|----------|
| **TradingView** | Charting and indicators | 1000/day | Technical indicators |
| **TA-Lib** | Technical analysis library | N/A | Custom indicators |
| **Polygon.io** | Market data | 5/min | OHLCV data |

## 🔧 Configuration

### Environment Variables
```bash
# Database
MONGODB_URI=mongodb://admin:defimon123@localhost:27017/prediction?authSource=admin
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=defimon123

# Kafka
KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# API Keys
COINGECKO_API_KEY=your_coingecko_key
COINMARKETCAP_API_KEY=your_cmc_key
ETHERSCAN_API_KEY=your_etherscan_key
TWITTER_API_KEY=your_twitter_key
REDDIT_API_KEY=your_reddit_key
NEWS_API_KEY=your_news_key
FRED_API_KEY=your_fred_key
ALPHAVANTAGE_API_KEY=your_alpha_vantage_key
DEFILLAMA_API_KEY=your_defillama_key
DUNE_API_KEY=your_dune_key
```

## 🚀 Quick Start

### Local Development
```bash
# Clone the repository
git clone <repository-url>
cd ethereum-prediction-service

# Build the application
mvn clean package

# Run the service
java -jar target/ethereum-prediction-service-*.jar
```

### Docker
```bash
# Build Docker image
docker build -t ethereum-prediction-service .

# Run with Docker Compose
docker-compose up ethereum-prediction-service
```

### API Usage Examples

#### Get Price Prediction
```bash
# 1-hour prediction
curl http://localhost:8087/api/v1/prediction/price/1h

# 24-hour prediction
curl http://localhost:8087/api/v1/prediction/price/24h

# All time horizons
curl http://localhost:8087/api/v1/prediction/price/all-horizons
```

#### Get Factor Analysis
```bash
curl http://localhost:8087/api/v1/prediction/factors
```

#### Get Risk Metrics
```bash
curl http://localhost:8087/api/v1/prediction/risk
```

#### Stream Real-time Predictions
```bash
curl -N http://localhost:8087/api/v1/prediction/price/stream
```

## 📈 Prediction Models

### Technical Analysis Model
- **RSI (Relative Strength Index)**: Momentum oscillator
- **MACD**: Trend following indicator
- **Bollinger Bands**: Volatility indicator
- **Moving Averages**: SMA (20, 50, 200), EMA (12, 26)
- **Stochastic Oscillator**: Momentum indicator
- **Williams %R**: Momentum indicator
- **CCI**: Commodity Channel Index
- **ATR**: Average True Range
- **ADX**: Average Directional Index
- **OBV**: On-Balance Volume

### Sentiment Analysis Model
- **Twitter Sentiment**: Real-time social media sentiment
- **Reddit Sentiment**: Community discussion sentiment
- **News Sentiment**: News article sentiment analysis
- **Influencer Sentiment**: Key opinion leader analysis
- **Fear & Greed Index**: Market psychology indicator

### Macro-Economic Model
- **DXY (Dollar Index)**: Currency strength impact
- **VIX (Volatility Index)**: Market fear indicator
- **Federal Funds Rate**: Interest rate impact
- **Inflation Rate**: Economic stability indicator
- **Unemployment Rate**: Economic health indicator
- **GDP Growth**: Economic growth indicator
- **Treasury Yields**: Risk-free rate comparison
- **Commodity Prices**: Gold, Oil correlation

### DeFi Analytics Model
- **Total Value Locked (TVL)**: DeFi ecosystem health
- **DeFi Dominance**: Market share analysis
- **Lending Volume**: DeFi lending activity
- **DEX Volume**: Decentralized exchange activity
- **Staking Rewards**: Network security metrics
- **Yield Farming APY**: DeFi yield opportunities

### Ensemble Model
- **Weighted Average**: Combines all models with confidence weights
- **Confidence Scoring**: Each model contributes based on accuracy
- **Factor Analysis**: Identifies primary and secondary price drivers
- **Risk Assessment**: Calculates VaR, Expected Shortfall, Volatility

## 📊 API Response Examples

### Price Prediction Response
```json
{
  "timestamp": "2024-01-15T10:30:00.000Z",
  "current_price": 2450.50,
  "predicted_price": 2520.75,
  "price_change_percentage": 2.87,
  "confidence_score": 0.78,
  "time_horizon": "24h",
  "prediction_model": "ensemble_model_v1",
  "factor_analysis": {
    "primary_factors": [
      {
        "factor_name": "Technical Analysis",
        "impact_score": 0.8,
        "weight": 0.3,
        "description": "RSI, MACD, and Bollinger Bands indicate market momentum"
      }
    ],
    "sentiment_score": 0.65,
    "technical_indicators": {
      "rsi": 58.5,
      "macd": 12.3,
      "bollinger_position": 0.45
    }
  },
  "risk_metrics": {
    "volatility": 0.25,
    "value_at_risk": 612.63,
    "expected_shortfall": 673.89,
    "max_drawdown": 367.58,
    "sharpe_ratio": 1.2
  },
  "market_conditions": {
    "market_regime": "BULL_MARKET",
    "liquidity_score": 1.15,
    "correlation_btc": 0.85,
    "fear_greed_index": 68.5
  }
}
```

### Factor Analysis Response
```json
{
  "primary_factors": [
    {
      "factor_name": "Technical Analysis",
      "impact_score": 0.8,
      "weight": 0.3,
      "description": "Strong bullish momentum indicators"
    },
    {
      "factor_name": "DeFi Activity",
      "impact_score": 0.7,
      "weight": 0.2,
      "description": "Increasing TVL and protocol usage"
    }
  ],
  "secondary_factors": [
    {
      "factor_name": "Network Activity",
      "impact_score": 0.4,
      "weight": 0.1,
      "description": "High transaction volume and gas usage"
    }
  ],
  "sentiment_score": 0.65,
  "technical_indicators": {
    "rsi": 58.5,
    "macd": 12.3,
    "bollinger_position": 0.45
  },
  "on_chain_metrics": {
    "active_addresses": 500000,
    "transaction_count": 1000000,
    "gas_price": 25.5
  }
}
```

## 🔍 Monitoring & Observability

### Health Endpoints
- `/api/v1/prediction/health` - Service health status
- `/api/v1/prediction/metrics` - Performance metrics
- `/actuator/health` - Spring Boot health check
- `/actuator/metrics` - Micrometer metrics

### Prometheus Metrics
- `prediction_generated_total` - Total predictions generated
- `prediction_confidence_histogram` - Confidence score distribution
- `data_collection_duration` - Data collection latency
- `api_requests_total` - API request counter
- `error_rate` - Error rate metric

### Grafana Dashboards
- Prediction accuracy over time
- Data source health monitoring
- API performance metrics
- Factor analysis trends
- Risk metrics visualization

## 🛠️ Development

### Prerequisites
- Java 21+
- Maven 3.9+
- MongoDB 6.0+
- Redis 7.0+
- Apache Kafka 3.0+

### Building
```bash
mvn clean compile
mvn test
mvn package
```

### Testing
```bash
# Unit tests
mvn test

# Integration tests
mvn verify -P integration-test

# Load testing
mvn verify -P load-test
```

## 📚 Documentation

- [API Documentation](API_DOCUMENTATION.md)
- [Architecture Guide](ARCHITECTURE.md)
- [Deployment Guide](DEPLOYMENT.md)
- [Contributing Guide](CONTRIBUTING.md)

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🆘 Support

For support and questions:
- Create an issue on GitHub
- Join our Discord community
- Email: support@defimon.com

---

*Built with ❤️ for the DeFi community*
