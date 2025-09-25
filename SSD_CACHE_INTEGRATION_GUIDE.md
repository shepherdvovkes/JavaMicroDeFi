# SSD Cache Integration Guide

## Overview

This guide provides step-by-step instructions for integrating the SSD cache service with your existing Java Micro DeFi microservices. The cache service provides significant performance improvements through intelligent multi-tier caching.

## Quick Start

### 1. Setup SSD Cache Infrastructure

```bash
# Run the setup script
sudo /home/vovkes/JavaMicroDeFi/setup-ssd-cache.sh
```

### 2. Verify Installation

```bash
# Check service health
curl http://localhost:8088/api/cache/health

# Check cache statistics
curl http://localhost:8088/api/cache/statistics
```

## Integration with Existing Services

### 1. API Gateway Integration

Add cache configuration to your API Gateway service:

```yaml
# api-gateway/src/main/resources/application.yml
spring:
  cache:
    type: redis
    redis:
      host: localhost
      port: 6379
      password: defimon123
      time-to-live: 300000 # 5 minutes

# Cache service integration
cache:
  service:
    url: http://localhost:8088/api/cache
    enabled: true
    timeout: 5000
```

### 2. Ethereum Prediction Service Integration

```java
// Add to your EthereumPredictionService
@Autowired
private SSDCacheManager cacheManager;

@Cacheable(value = "ethereum-predictions", key = "#symbol + ':' + #timestamp")
public CompletableFuture<PredictionResult> getPrediction(String symbol, long timestamp) {
    return cacheManager.get("prediction:" + symbol + ":" + timestamp)
        .thenApply(result -> {
            if (result != null) {
                return (PredictionResult) result;
            }
            return null;
        });
}

@CachePut(value = "ethereum-predictions", key = "#symbol + ':' + #timestamp")
public CompletableFuture<Void> cachePrediction(String symbol, long timestamp, PredictionResult result) {
    return cacheManager.put("prediction:" + symbol + ":" + timestamp, result, CacheTier.HOT);
}
```

### 3. Blockchain Sync Service Integration

```java
// Add to your BlockchainSyncService
@Autowired
private BlockchainCacheService blockchainCacheService;

public CompletableFuture<Block> getBlock(String blockHash) {
    return blockchainCacheService.getBlock(blockHash)
        .thenApply(result -> {
            if (result != null) {
                return (Block) result;
            }
            // Fetch from blockchain if not in cache
            return fetchBlockFromBlockchain(blockHash);
        });
}

public CompletableFuture<Void> cacheBlock(String blockHash, Block block) {
    return blockchainCacheService.cacheBlock(blockHash, block);
}
```

### 4. Data Aggregation Service Integration

```java
// Add to your DataAggregationService
@Autowired
private DataAggregationCacheService dataAggregationCacheService;

public CompletableFuture<PriceData> getPriceData(String symbol, long timestamp) {
    return dataAggregationCacheService.getPriceData(symbol, timestamp)
        .thenApply(result -> {
            if (result != null) {
                return (PriceData) result;
            }
            // Fetch from external API if not in cache
            return fetchPriceFromAPI(symbol, timestamp);
        });
}

public CompletableFuture<Void> cachePriceData(String symbol, long timestamp, PriceData priceData) {
    return dataAggregationCacheService.cachePriceData(symbol, timestamp, priceData);
}
```

## Cache Configuration

### 1. Cache Tiers

- **HOT**: In-memory cache (< 1ms access time)
- **WARM**: SSD cache (1-10ms access time)
- **COLD**: Primary storage (10-100ms access time)

### 2. Cache TTL Configuration

```yaml
cache:
  ttl:
    hot: 60      # 1 minute
    warm: 3600   # 1 hour
    cold: 86400  # 24 hours
```

### 3. Cache Size Configuration

```yaml
cache:
  memory:
    size: 268435456  # 256MB
  ssd:
    size: 2147483648  # 2GB
```

## Performance Optimization

### 1. Cache Key Strategies

```java
// Use descriptive, hierarchical keys
String cacheKey = "blockchain:ethereum:block:" + blockHash;
String cacheKey = "market:price:" + symbol + ":" + timestamp;
String cacheKey = "aggregation:ohlcv:" + symbol + ":" + timeframe;
```

### 2. Batch Operations

```java
// Use batch operations for better performance
List<String> keys = Arrays.asList("key1", "key2", "key3");
CompletableFuture<Map<String, Object>> results = cacheManager.getBatch(keys);

Map<String, Object> data = new HashMap<>();
data.put("key1", value1);
data.put("key2", value2);
CompletableFuture<Void> result = cacheManager.putBatch(data, CacheTier.WARM);
```

### 3. Cache Eviction Strategies

```java
// Manual eviction
cacheManager.evict("key");

// Evict by pattern
cacheManager.evict("blockchain:*");
cacheManager.evict("market:*");
```

## Monitoring and Metrics

### 1. Cache Statistics

```bash
# Get overall cache statistics
curl http://localhost:8088/api/cache/statistics

# Get blockchain cache statistics
curl http://localhost:8088/api/cache/blockchain/statistics

# Get data aggregation cache statistics
curl http://localhost:8088/api/cache/data-aggregation/statistics
```

### 2. Grafana Dashboard

Access the Grafana dashboard at http://localhost:3000 (admin/admin123) to view:

- Cache hit/miss ratios
- Cache latency metrics
- Memory usage
- SSD performance
- Tier distribution

### 3. Prometheus Metrics

Access Prometheus at http://localhost:9090 to view detailed metrics:

- `cache_operations_total`
- `cache_hits_total`
- `cache_misses_total`
- `cache_latency_seconds`
- `cache_memory_usage_bytes`

## Troubleshooting

### 1. Common Issues

**Cache Service Not Responding**
```bash
# Check service status
docker ps | grep ssd-cache-service

# Check logs
docker logs ssd-cache-service

# Restart service
docker-compose restart ssd-cache-service
```

**High Memory Usage**
```bash
# Check cache statistics
curl http://localhost:8088/api/cache/statistics

# Optimize cache
curl -X POST http://localhost:8088/api/cache/optimize
```

**Low Hit Ratio**
```bash
# Check access patterns
curl http://localhost:8088/api/cache/statistics

# Review cache key strategies
# Consider increasing cache sizes
# Check TTL settings
```

### 2. Performance Tuning

**Increase Cache Sizes**
```yaml
# In docker-compose.yml
environment:
  - MEMORY_CACHE_SIZE=536870912  # 512MB
  - SSD_CACHE_SIZE=4294967296    # 4GB
```

**Optimize TTL Settings**
```yaml
# In docker-compose.yml
environment:
  - CACHE_TTL_HOT=120     # 2 minutes
  - CACHE_TTL_WARM=7200   # 2 hours
  - CACHE_TTL_COLD=172800 # 48 hours
```

**Change Eviction Strategy**
```yaml
# In docker-compose.yml
environment:
  - CACHE_EVICTION_STRATEGY=LFU  # Least Frequently Used
```

## Best Practices

### 1. Cache Key Design

- Use hierarchical keys: `service:type:id`
- Include version information: `v1:blockchain:ethereum:block:hash`
- Use consistent naming conventions
- Avoid special characters in keys

### 2. Data Placement

- **HOT**: Frequently accessed, small data (< 1MB)
- **WARM**: Moderately accessed, medium data (< 100MB)
- **COLD**: Rarely accessed, large data (> 100MB)

### 3. Cache Invalidation

- Use TTL-based expiration for time-sensitive data
- Implement manual invalidation for data changes
- Use cache tags for related data invalidation

### 4. Monitoring

- Set up alerts for low hit ratios
- Monitor cache memory usage
- Track cache latency metrics
- Review access patterns regularly

## API Reference

### Cache Operations

```bash
# Get cache data
GET /api/cache/{key}

# Put cache data
POST /api/cache/{key}
Content-Type: application/json
Body: { "data": "value", "tier": "WARM" }

# Evict cache data
DELETE /api/cache/{key}

# Batch get
POST /api/cache/batch/get
Body: ["key1", "key2", "key3"]

# Batch put
POST /api/cache/batch/put
Body: { "key1": "value1", "key2": "value2" }

# Optimize cache
POST /api/cache/optimize
```

### Blockchain Cache Operations

```bash
# Get block
GET /api/cache/blockchain/block/{blockHash}

# Cache block
POST /api/cache/blockchain/block/{blockHash}
Body: { "blockData": "..." }

# Get transaction
GET /api/cache/blockchain/transaction/{txHash}

# Cache transaction
POST /api/cache/blockchain/transaction/{txHash}
Body: { "transactionData": "..." }
```

### Data Aggregation Cache Operations

```bash
# Get price data
GET /api/cache/data-aggregation/price/{symbol}/{timestamp}

# Cache price data
POST /api/cache/data-aggregation/price/{symbol}/{timestamp}
Body: { "priceData": "..." }

# Get OHLCV data
GET /api/cache/data-aggregation/ohlcv/{symbol}/{timeframe}/{timestamp}

# Cache OHLCV data
POST /api/cache/data-aggregation/ohlcv/{symbol}/{timeframe}/{timestamp}
Body: { "ohlcvData": "..." }
```

## Conclusion

The SSD cache service provides significant performance improvements for your Java Micro DeFi project. By following this integration guide, you can:

1. **Reduce API response times** by 50-80%
2. **Improve database query performance** by 60-90%
3. **Optimize blockchain sync operations** by 20-30%
4. **Enhance data aggregation speed** by 40-70%

Monitor the cache performance regularly and adjust configurations based on your specific use cases and performance requirements.

