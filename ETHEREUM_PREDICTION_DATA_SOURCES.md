# Ethereum Prediction Service - Comprehensive Data Sources

## 🎯 Overview

This document provides a comprehensive list of APIs and data sources that the Ethereum Prediction Service can use to collect maximum data affecting ETH price. The service integrates with multiple data providers to ensure robust and accurate predictions.

## 📊 Market Data APIs

### 1. CoinGecko API
**Base URL**: `https://api.coingecko.com/api/v3`
**Rate Limit**: 50 requests/minute (free), 500 requests/minute (pro)

#### Key Endpoints:
```http
# Ethereum price and market data
GET /coins/ethereum?localization=false&tickers=false&market_data=true&community_data=true&developer_data=true&sparkline=false

# Historical price data
GET /coins/ethereum/market_chart?vs_currency=usd&days=30&interval=daily

# Global cryptocurrency metrics
GET /global

# Trending cryptocurrencies
GET /search/trending

# Developer activity data
GET /coins/ethereum/developer_data

# Community metrics
GET /coins/ethereum/community_data

# Exchange data and trading pairs
GET /coins/ethereum/tickers

# Market dominance data
GET /coins/ethereum/market_chart?vs_currency=usd&days=1&interval=hourly
```

### 2. CoinMarketCap API
**Base URL**: `https://pro-api.coinmarketcap.com/v1`
**Rate Limit**: 100 requests/month (free), 10,000 requests/month (pro)

#### Key Endpoints:
```http
# Latest quotes
GET /cryptocurrency/quotes/latest?symbol=ETH&convert=USD

# Historical quotes
GET /cryptocurrency/quotes/historical?symbol=ETH&count=30&interval=daily&convert=USD

# Global metrics
GET /global-metrics/quotes/latest

# Trending cryptocurrencies
GET /cryptocurrency/trending/most-visited

# Market pairs
GET /cryptocurrency/market-pairs/latest?symbol=ETH

# Price performance statistics
GET /cryptocurrency/price-performance-stats/latest?symbol=ETH&time_period=24h,7d,30d,90d,365d

# Market dominance
GET /global-metrics/quotes/historical?count=30&interval=daily
```

### 3. Binance API
**Base URL**: `https://api.binance.com/api/v3`
**Rate Limit**: 1200 requests/minute

#### Key Endpoints:
```http
# 24hr ticker price change statistics
GET /ticker/24hr?symbol=ETHUSDT

# Order book
GET /depth?symbol=ETHUSDT&limit=100

# Recent trades
GET /trades?symbol=ETHUSDT&limit=500

# Historical trades
GET /historicalTrades?symbol=ETHUSDT&limit=500

# Kline/candlestick data
GET /klines?symbol=ETHUSDT&interval=1h&limit=1000

# Average price
GET /avgPrice?symbol=ETHUSDT

# Exchange information
GET /exchangeInfo

# 24hr ticker price change
GET /ticker/price?symbol=ETHUSDT
```

## 🔗 On-Chain Data APIs

### 4. Etherscan API
**Base URL**: `https://api.etherscan.io/api`
**Rate Limit**: 5 requests/second

#### Key Endpoints:
```http
# ETH price
GET ?module=stats&action=ethprice&apikey={API_KEY}

# ETH supply
GET ?module=stats&action=ethsupply&apikey={API_KEY}

# Latest block number
GET ?module=proxy&action=eth_blockNumber&apikey={API_KEY}

# Gas oracle
GET ?module=gastracker&action=gasoracle&apikey={API_KEY}

# Block information
GET ?module=proxy&action=eth_getBlockByNumber&tag={BLOCK_NUMBER}&boolean=true&apikey={API_KEY}

# Transaction information
GET ?module=proxy&action=eth_getTransactionByHash&txhash={TX_HASH}&apikey={API_KEY}

# Transaction count for address
GET ?module=proxy&action=eth_getTransactionCount&address={ADDRESS}&tag=latest&apikey={API_KEY}

# Token information
GET ?module=token&action=tokeninfo&contractaddress={CONTRACT_ADDRESS}&apikey={API_KEY}

# Token balance
GET ?module=account&action=tokenbalance&contractaddress={CONTRACT_ADDRESS}&address={ADDRESS}&tag=latest&apikey={API_KEY}
```

### 5. Infura API
**Base URL**: `https://mainnet.infura.io/v3/{PROJECT_ID}`
**Rate Limit**: 100,000 requests/day

#### Key Endpoints:
```http
# Get latest block
POST /v3/{PROJECT_ID}
Content-Type: application/json
{
  "jsonrpc": "2.0",
  "method": "eth_getBlockByNumber",
  "params": ["latest", true],
  "id": 1
}

# Get transaction by hash
POST /v3/{PROJECT_ID}
Content-Type: application/json
{
  "jsonrpc": "2.0",
  "method": "eth_getTransactionByHash",
  "params": ["{TX_HASH}"],
  "id": 1
}

# Get balance
POST /v3/{PROJECT_ID}
Content-Type: application/json
{
  "jsonrpc": "2.0",
  "method": "eth_getBalance",
  "params": ["{ADDRESS}", "latest"],
  "id": 1
}

# Get gas price
POST /v3/{PROJECT_ID}
Content-Type: application/json
{
  "jsonrpc": "2.0",
  "method": "eth_gasPrice",
  "params": [],
  "id": 1
}
```

### 6. Alchemy API
**Base URL**: `https://eth-mainnet.alchemyapi.io/v2/{API_KEY}`
**Rate Limit**: 300M requests/month

#### Key Endpoints:
```http
# Enhanced APIs for smart contract analysis
POST /v2/{API_KEY}
Content-Type: application/json
{
  "jsonrpc": "2.0",
  "method": "alchemy_getAssetTransfers",
  "params": [{
    "fromBlock": "0x0",
    "toBlock": "latest",
    "category": ["external", "internal", "erc20", "erc721", "erc1155"]
  }],
  "id": 1
}

# Get token balances
POST /v2/{API_KEY}
Content-Type: application/json
{
  "jsonrpc": "2.0",
  "method": "alchemy_getTokenBalances",
  "params": ["{ADDRESS}", ["0x{TOKEN_CONTRACT_ADDRESS}"]],
  "id": 1
}
```

## 📱 Sentiment Analysis APIs

### 7. Twitter API v2
**Base URL**: `https://api.twitter.com/2`
**Rate Limit**: 300 requests/15 minutes

#### Key Endpoints:
```http
# Recent tweets about Ethereum
GET /tweets/search/recent?query=ethereum OR ETH -is:retweet lang:en&max_results=100&tweet.fields=created_at,public_metrics,context_annotations

# User tweets
GET /users/by/username/vitalikbuterin/tweets?max_results=100&tweet.fields=created_at,public_metrics

# Tweet metrics
GET /tweets?ids={TWEET_IDS}&tweet.fields=public_metrics

# User metrics
GET /users/by/username/vitalikbuterin?user.fields=public_metrics

# Trending topics
GET /trends/by/woeid/1

# Tweet sentiment analysis (requires additional processing)
GET /tweets/search/recent?query=ethereum&max_results=100&tweet.fields=created_at,public_metrics
```

### 8. Reddit API
**Base URL**: `https://oauth.reddit.com`
**Rate Limit**: 60 requests/minute

#### Key Endpoints:
```http
# Ethereum subreddit posts
GET /r/ethereum/hot?limit=100&raw_json=1

# Ethereum subreddit comments
GET /r/ethereum/comments?limit=100&raw_json=1

# Search Reddit for Ethereum mentions
GET /search?q=ethereum&sort=relevance&limit=100&raw_json=1

# Crypto subreddit posts
GET /r/cryptocurrency/hot?limit=100&raw_json=1

# DeFi subreddit posts
GET /r/defi/hot?limit=100&raw_json=1

# Post details with comments
GET /r/ethereum/comments/{POST_ID}?raw_json=1
```

### 9. NewsAPI
**Base URL**: `https://newsapi.org/v2`
**Rate Limit**: 1,000 requests/day

#### Key Endpoints:
```http
# Everything about Ethereum
GET /everything?q=ethereum&language=en&sortBy=publishedAt&pageSize=100&apiKey={API_KEY}

# Everything about cryptocurrency
GET /everything?q=cryptocurrency&language=en&sortBy=publishedAt&pageSize=100&apiKey={API_KEY}

# Everything about DeFi
GET /everything?q=defi OR "decentralized finance"&language=en&sortBy=publishedAt&pageSize=100&apiKey={API_KEY}

# Top headlines
GET /top-headlines?category=business&language=en&pageSize=100&apiKey={API_KEY}

# Sources
GET /sources?category=business&language=en&apiKey={API_KEY}
```

### 10. CryptoPanic API
**Base URL**: `https://cryptopanic.com/api/v1`
**Rate Limit**: 100 requests/minute

#### Key Endpoints:
```http
# Latest news
GET /posts/?auth_token={API_KEY}&public=true&currencies=ETH&kind=news

# Trending news
GET /posts/?auth_token={API_KEY}&public=true&currencies=ETH&kind=trending

# Filtered news
GET /posts/?auth_token={API_KEY}&public=true&currencies=ETH&filter=hot

# News sentiment
GET /posts/?auth_token={API_KEY}&public=true&currencies=ETH&kind=news&metadata=true
```

## 🏛️ Macro-Economic APIs

### 11. Federal Reserve Economic Data (FRED)
**Base URL**: `https://api.stlouisfed.org/fred`
**Rate Limit**: 120 requests/minute

#### Key Endpoints:
```http
# Federal Funds Rate
GET /series/observations?series_id=FEDFUNDS&api_key={API_KEY}&file_type=json&limit=1&sort_order=desc

# Consumer Price Index (Inflation)
GET /series/observations?series_id=CPIAUCSL&api_key={API_KEY}&file_type=json&limit=1&sort_order=desc

# Unemployment Rate
GET /series/observations?series_id=UNRATE&api_key={API_KEY}&file_type=json&limit=1&sort_order=desc

# GDP Growth
GET /series/observations?series_id=GDPC1&api_key={API_KEY}&file_type=json&limit=1&sort_order=desc

# 10-Year Treasury Yield
GET /series/observations?series_id=DGS10&api_key={API_KEY}&file_type=json&limit=1&sort_order=desc

# 2-Year Treasury Yield
GET /series/observations?series_id=DGS2&api_key={API_KEY}&file_type=json&limit=1&sort_order=desc

# Money Supply (M2)
GET /series/observations?series_id=M2SL&api_key={API_KEY}&file_type=json&limit=1&sort_order=desc

# Personal Consumption Expenditures
GET /series/observations?series_id=PCEPI&api_key={API_KEY}&file_type=json&limit=1&sort_order=desc
```

### 12. Alpha Vantage API
**Base URL**: `https://www.alphavantage.co/query`
**Rate Limit**: 5 requests/minute (free), 75 requests/minute (premium)

#### Key Endpoints:
```http
# US Dollar Index (DXY)
GET ?function=FX_DAILY&from_symbol=USD&to_symbol=USDX&apikey={API_KEY}

# VIX (Volatility Index)
GET ?function=TIME_SERIES_DAILY&symbol=VIX&apikey={API_KEY}

# Gold Price
GET ?function=CURRENCY_EXCHANGE_RATE&from_currency=XAU&to_currency=USD&apikey={API_KEY}

# Oil Price (WTI)
GET ?function=TIME_SERIES_DAILY&symbol=WTI&apikey={API_KEY}

# Silver Price
GET ?function=CURRENCY_EXCHANGE_RATE&from_currency=XAG&to_currency=USD&apikey={API_KEY}

# S&P 500
GET ?function=TIME_SERIES_DAILY&symbol=SPY&apikey={API_KEY}

# NASDAQ
GET ?function=TIME_SERIES_DAILY&symbol=QQQ&apikey={API_KEY}

# Bitcoin Price
GET ?function=CURRENCY_EXCHANGE_RATE&from_currency=BTC&to_currency=USD&apikey={API_KEY}
```

### 13. World Bank API
**Base URL**: `https://api.worldbank.org/v2`
**Rate Limit**: 1,000 requests/day

#### Key Endpoints:
```http
# GDP Growth Rate (World)
GET /country/all/indicator/NY.GDP.MKTP.KD.ZG?format=json&date=2020:2024&per_page=100

# Inflation Rate (World)
GET /country/all/indicator/FP.CPI.TOTL.ZG?format=json&date=2020:2024&per_page=100

# Unemployment Rate (World)
GET /country/all/indicator/SL.UEM.TOTL.ZS?format=json&date=2020:2024&per_page=100

# Interest Rate (World)
GET /country/all/indicator/FR.INR.RINR?format=json&date=2020:2024&per_page=100
```

## 🏦 DeFi Analytics APIs

### 14. DeFiLlama API
**Base URL**: `https://api.llama.fi`
**Rate Limit**: 100 requests/minute

#### Key Endpoints:
```http
# Ethereum TVL
GET /tvl/ethereum

# All chains TVL
GET /tvl

# Protocol TVL
GET /protocols/{PROTOCOL_NAME}

# DeFi yields
GET /yields

# DeFi staking
GET /protocols/staking

# Historical TVL
GET /tvl/ethereum?timestamp={TIMESTAMP}

# Protocol information
GET /protocols

# Chains information
GET /chains

# Stablecoins
GET /stablecoins

# Treasury
GET /treasury

# Bridges
GET /bridges
```

### 15. Dune Analytics API
**Base URL**: `https://api.dune.com/api/v1`
**Rate Limit**: 1,000 requests/hour

#### Key Endpoints:
```http
# Execute query
POST /query/{QUERY_ID}/execute

# Get query results
GET /query/{QUERY_ID}/results

# Get query status
GET /query/{QUERY_ID}/status

# Get query metadata
GET /query/{QUERY_ID}

# List queries
GET /queries

# Popular queries
GET /queries/popular

# Search queries
GET /queries/search?q=ethereum

# Query favorites
GET /queries/favorites
```

### 16. DeFi Pulse API
**Base URL**: `https://data-api.defipulse.com/api/v1`
**Rate Limit**: 1,000 requests/day

#### Key Endpoints:
```http
# Get protocol data
GET /defipulse/api/GetHistory?project={PROTOCOL_NAME}&period=1y&api_key={API_KEY}

# Get all protocols
GET /defipulse/api/GetProjects?api_key={API_KEY}

# Get protocol details
GET /defipulse/api/GetProject?project={PROTOCOL_NAME}&api_key={API_KEY}

# Get TVL history
GET /defipulse/api/GetHistory?project={PROTOCOL_NAME}&period=1y&api_key={API_KEY}
```

## 📈 Technical Analysis APIs

### 17. TradingView API
**Base URL**: `https://scanner.tradingview.com`
**Rate Limit**: 1,000 requests/day

#### Key Endpoints:
```http
# Scanner data
POST /crypto/scan
Content-Type: application/json
{
  "filter": [
    {
      "left": "market_cap_basic",
      "right": 1000000000,
      "operation": "greater"
    }
  ],
  "options": {
    "lang": "en"
  },
  "symbols": {
    "query": {
      "types": ["crypto"]
    },
    "tickers": ["BINANCE:ETHUSDT"]
  },
  "columns": ["name", "close", "change", "change_abs", "volume", "market_cap_basic", "price_earnings_ttm", "number_of_employees", "sector", "industry"],
  "sort": {
    "sortBy": "market_cap_basic",
    "sortOrder": "desc"
  },
  "range": [0, 100]
}

# Historical data
GET /history?symbol=BINANCE:ETHUSDT&resolution=1D&from={FROM_TIMESTAMP}&to={TO_TIMESTAMP}

# Real-time data
GET /quote?symbols=BINANCE:ETHUSDT
```

### 18. Polygon.io API
**Base URL**: `https://api.polygon.io`
**Rate Limit**: 5 requests/minute (free), 1,000 requests/minute (premium)

#### Key Endpoints:
```http
# Previous close
GET /v2/aggs/ticker/X:ETHUSD/prev?adjusted=true&apikey={API_KEY}

# Aggregates (bars)
GET /v2/aggs/ticker/X:ETHUSD/range/1/day/2023-01-01/2023-12-31?adjusted=true&sort=asc&limit=50000&apikey={API_KEY}

# Grouped daily
GET /v2/aggs/grouped/locale/global/market/crypto/2023-01-01?adjusted=true&apikey={API_KEY}

# Daily open/close
GET /v1/open-close/X:ETHUSD/2023-01-01?adjusted=true&apikey={API_KEY}

# Trades
GET /v3/trades/X:ETHUSD?timestamp.gte=2023-01-01&timestamp.lte=2023-01-02&order=asc&limit=50000&apikey={API_KEY}

# Quotes
GET /v3/quotes/X:ETHUSD?timestamp.gte=2023-01-01&timestamp.lte=2023-01-02&order=asc&limit=50000&apikey={API_KEY}
```

## 🔮 Prediction Market APIs

### 19. Augur API
**Base URL**: `https://api.augur.net/api/v2`
**Rate Limit**: 100 requests/minute

#### Key Endpoints:
```http
# Markets
GET /markets

# Market details
GET /markets/{MARKET_ID}

# Market outcomes
GET /markets/{MARKET_ID}/outcomes

# Market trades
GET /markets/{MARKET_ID}/trades

# Market orders
GET /markets/{MARKET_ID}/orders

# User positions
GET /users/{USER_ID}/positions

# Categories
GET /categories
```

### 20. Polymarket API
**Base URL**: `https://gamma-api.polymarket.com`
**Rate Limit**: 100 requests/minute

#### Key Endpoints:
```http
# Markets
GET /markets

# Market details
GET /markets/{MARKET_ID}

# Market trades
GET /markets/{MARKET_ID}/trades

# Market orders
GET /markets/{MARKET_ID}/orders

# User positions
GET /users/{USER_ID}/positions

# Tokens
GET /tokens

# Categories
GET /categories
```

## 🌐 Additional Data Sources

### 21. Google Trends API
**Base URL**: `https://trends.google.com/trends/api`
**Rate Limit**: 100 requests/day

#### Key Endpoints:
```http
# Interest over time
GET /explore?hl=en-US&tz=-480&req={REQUEST_JSON}&tz=-480

# Related queries
GET /explore?hl=en-US&tz=-480&req={REQUEST_JSON}&tz=-480

# Interest by region
GET /explore?hl=en-US&tz=-480&req={REQUEST_JSON}&tz=-480
```

### 22. GitHub API
**Base URL**: `https://api.github.com`
**Rate Limit**: 5,000 requests/hour

#### Key Endpoints:
```http
# Repository information
GET /repos/ethereum/go-ethereum

# Repository commits
GET /repos/ethereum/go-ethereum/commits?per_page=100

# Repository releases
GET /repos/ethereum/go-ethereum/releases

# Repository stargazers
GET /repos/ethereum/go-ethereum/stargazers?per_page=100

# Repository forks
GET /repos/ethereum/go-ethereum/forks?per_page=100

# Repository issues
GET /repos/ethereum/go-ethereum/issues?state=all&per_page=100

# Repository pull requests
GET /repos/ethereum/go-ethereum/pulls?state=all&per_page=100
```

### 23. Glassnode API
**Base URL**: `https://api.glassnode.com`
**Rate Limit**: 10 requests/minute (free), 1,000 requests/minute (pro)

#### Key Endpoints:
```http
# Active addresses
GET /v1/metrics/addresses/active_count?a=ETH&i=24h&api_key={API_KEY}

# Transaction count
GET /v1/metrics/transactions/count?a=ETH&i=24h&api_key={API_KEY}

# Exchange flows
GET /v1/metrics/distribution/balance_exchanges?a=ETH&i=24h&api_key={API_KEY}

# Network value
GET /v1/metrics/network_value_to_transactions_ratio?a=ETH&i=24h&api_key={API_KEY}

# Market value
GET /v1/metrics/market/marketcap_usd?a=ETH&i=24h&api_key={API_KEY}

# Realized value
GET /v1/metrics/market/marketcap_realized_usd?a=ETH&i=24h&api_key={API_KEY}

# MVRV ratio
GET /v1/metrics/market/mvrv_usd?a=ETH&i=24h&api_key={API_KEY}
```

### 24. IntoTheBlock API
**Base URL**: `https://api.intotheblock.com`
**Rate Limit**: 100 requests/day (free), 10,000 requests/day (pro)

#### Key Endpoints:
```http
# Token overview
GET /token/overview?symbol=ETH&api_key={API_KEY}

# Token signals
GET /token/signals?symbol=ETH&api_key={API_KEY}

# Token holders
GET /token/holders?symbol=ETH&api_key={API_KEY}

# Token transactions
GET /token/transactions?symbol=ETH&api_key={API_KEY}

# Token exchanges
GET /token/exchanges?symbol=ETH&api_key={API_KEY}

# Token correlation
GET /token/correlation?symbol=ETH&api_key={API_KEY}
```

## 🔧 Integration Strategy

### Data Collection Schedule
- **Market Data**: Every 30 seconds
- **On-Chain Metrics**: Every 60 seconds
- **Sentiment Data**: Every 5 minutes
- **Technical Indicators**: Every 60 seconds
- **Macro-Economic**: Every hour
- **DeFi Metrics**: Every 5 minutes

### Fallback Strategy
1. **Primary API** fails → **Secondary API**
2. **Secondary API** fails → **Cached Data**
3. **All APIs** fail → **Default Values**

### Rate Limit Management
- Implement exponential backoff
- Use request queuing
- Distribute requests across multiple API keys
- Cache responses to reduce API calls

### Data Quality Assurance
- Validate data ranges
- Check for outliers
- Implement data consistency checks
- Monitor API response times

## 📊 Data Processing Pipeline

### 1. Data Collection
- Parallel API calls
- Error handling and retries
- Rate limit management
- Data validation

### 2. Data Processing
- Normalization
- Feature engineering
- Outlier detection
- Data aggregation

### 3. Model Training
- Historical data analysis
- Feature selection
- Model validation
- Performance metrics

### 4. Prediction Generation
- Real-time predictions
- Confidence scoring
- Risk assessment
- Factor analysis

### 5. Output Delivery
- REST API responses
- Real-time streaming
- Batch processing
- Monitoring and alerting

This comprehensive list provides the Ethereum Prediction Service with access to maximum relevant data sources for accurate price predictions and comprehensive market analysis.
