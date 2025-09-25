package com.defimon.cache;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;

import java.time.Instant;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Data Aggregation Cache Service
 * 
 * Provides specialized caching for market data, price feeds, OHLCV data,
 * and aggregated analytics. Optimized for real-time data processing.
 */
@Service
public class DataAggregationCacheService {
    
    private static final Logger logger = LoggerFactory.getLogger(DataAggregationCacheService.class);
    
    @Autowired
    private SSDCacheManager cacheManager;
    
    @Autowired
    private CacheMetricsService metricsService;
    
    // Cache prefixes
    private static final String PRICE_CACHE_PREFIX = "price:";
    private static final String OHLCV_CACHE_PREFIX = "ohlcv:";
    private static final String MARKET_CACHE_PREFIX = "market:";
    private static final String AGGREGATED_CACHE_PREFIX = "aggregated:";
    private static final String VOLUME_CACHE_PREFIX = "volume:";
    private static final String CORRELATION_CACHE_PREFIX = "correlation:";
    
    // Cache TTLs (in seconds)
    private static final long PRICE_CACHE_TTL = 60; // 1 minute
    private static final long OHLCV_CACHE_TTL = 300; // 5 minutes
    private static final long MARKET_CACHE_TTL = 120; // 2 minutes
    private static final long AGGREGATED_CACHE_TTL = 1800; // 30 minutes
    private static final long VOLUME_CACHE_TTL = 600; // 10 minutes
    private static final long CORRELATION_CACHE_TTL = 3600; // 1 hour
    
    /**
     * Cache price data
     */
    @CachePut(value = "price-data", key = "#symbol + ':' + #timestamp")
    public CompletableFuture<Void> cachePriceData(String symbol, long timestamp, Object priceData) {
        String cacheKey = PRICE_CACHE_PREFIX + symbol + ":" + timestamp;
        return cacheManager.put(cacheKey, priceData, SSDCacheManager.CacheTier.HOT)
            .thenRun(() -> {
                logger.debug("Cached price data for {} at {}", symbol, timestamp);
                metricsService.recordCachePut(cacheKey, SSDCacheManager.CacheTier.HOT, estimateSize(priceData));
            });
    }
    
    /**
     * Get cached price data
     */
    @Cacheable(value = "price-data", key = "#symbol + ':' + #timestamp")
    public CompletableFuture<Object> getPriceData(String symbol, long timestamp) {
        String cacheKey = PRICE_CACHE_PREFIX + symbol + ":" + timestamp;
        return cacheManager.get(cacheKey)
            .thenApply(result -> {
                if (result != null) {
                    logger.debug("Cache hit for price data: {} at {}", symbol, timestamp);
                    metricsService.recordCacheHit(cacheKey, SSDCacheManager.CacheTier.HOT);
                } else {
                    logger.debug("Cache miss for price data: {} at {}", symbol, timestamp);
                    metricsService.recordCacheMiss(cacheKey);
                }
                return result;
            });
    }
    
    /**
     * Cache OHLCV data
     */
    @CachePut(value = "ohlcv-data", key = "#symbol + ':' + #timeframe + ':' + #timestamp")
    public CompletableFuture<Void> cacheOHLCVData(String symbol, String timeframe, long timestamp, Object ohlcvData) {
        String cacheKey = OHLCV_CACHE_PREFIX + symbol + ":" + timeframe + ":" + timestamp;
        return cacheManager.put(cacheKey, ohlcvData, SSDCacheManager.CacheTier.WARM)
            .thenRun(() -> {
                logger.debug("Cached OHLCV data for {} {} at {}", symbol, timeframe, timestamp);
                metricsService.recordCachePut(cacheKey, SSDCacheManager.CacheTier.WARM, estimateSize(ohlcvData));
            });
    }
    
    /**
     * Get cached OHLCV data
     */
    @Cacheable(value = "ohlcv-data", key = "#symbol + ':' + #timeframe + ':' + #timestamp")
    public CompletableFuture<Object> getOHLCVData(String symbol, String timeframe, long timestamp) {
        String cacheKey = OHLCV_CACHE_PREFIX + symbol + ":" + timeframe + ":" + timestamp;
        return cacheManager.get(cacheKey)
            .thenApply(result -> {
                if (result != null) {
                    logger.debug("Cache hit for OHLCV data: {} {} at {}", symbol, timeframe, timestamp);
                    metricsService.recordCacheHit(cacheKey, SSDCacheManager.CacheTier.WARM);
                } else {
                    logger.debug("Cache miss for OHLCV data: {} {} at {}", symbol, timeframe, timestamp);
                    metricsService.recordCacheMiss(cacheKey);
                }
                return result;
            });
    }
    
    /**
     * Cache market data
     */
    @CachePut(value = "market-data", key = "#marketId + ':' + #timestamp")
    public CompletableFuture<Void> cacheMarketData(String marketId, long timestamp, Object marketData) {
        String cacheKey = MARKET_CACHE_PREFIX + marketId + ":" + timestamp;
        return cacheManager.put(cacheKey, marketData, SSDCacheManager.CacheTier.HOT)
            .thenRun(() -> {
                logger.debug("Cached market data for {} at {}", marketId, timestamp);
                metricsService.recordCachePut(cacheKey, SSDCacheManager.CacheTier.HOT, estimateSize(marketData));
            });
    }
    
    /**
     * Get cached market data
     */
    @Cacheable(value = "market-data", key = "#marketId + ':' + #timestamp")
    public CompletableFuture<Object> getMarketData(String marketId, long timestamp) {
        String cacheKey = MARKET_CACHE_PREFIX + marketId + ":" + timestamp;
        return cacheManager.get(cacheKey)
            .thenApply(result -> {
                if (result != null) {
                    logger.debug("Cache hit for market data: {} at {}", marketId, timestamp);
                    metricsService.recordCacheHit(cacheKey, SSDCacheManager.CacheTier.HOT);
                } else {
                    logger.debug("Cache miss for market data: {} at {}", marketId, timestamp);
                    metricsService.recordCacheMiss(cacheKey);
                }
                return result;
            });
    }
    
    /**
     * Cache aggregated data
     */
    @CachePut(value = "aggregated-data", key = "#aggregationType + ':' + #timeframe + ':' + #timestamp")
    public CompletableFuture<Void> cacheAggregatedData(String aggregationType, String timeframe, long timestamp, Object aggregatedData) {
        String cacheKey = AGGREGATED_CACHE_PREFIX + aggregationType + ":" + timeframe + ":" + timestamp;
        return cacheManager.put(cacheKey, aggregatedData, SSDCacheManager.CacheTier.WARM)
            .thenRun(() -> {
                logger.debug("Cached aggregated data for {} {} at {}", aggregationType, timeframe, timestamp);
                metricsService.recordCachePut(cacheKey, SSDCacheManager.CacheTier.WARM, estimateSize(aggregatedData));
            });
    }
    
    /**
     * Get cached aggregated data
     */
    @Cacheable(value = "aggregated-data", key = "#aggregationType + ':' + #timeframe + ':' + #timestamp")
    public CompletableFuture<Object> getAggregatedData(String aggregationType, String timeframe, long timestamp) {
        String cacheKey = AGGREGATED_CACHE_PREFIX + aggregationType + ":" + timeframe + ":" + timestamp;
        return cacheManager.get(cacheKey)
            .thenApply(result -> {
                if (result != null) {
                    logger.debug("Cache hit for aggregated data: {} {} at {}", aggregationType, timeframe, timestamp);
                    metricsService.recordCacheHit(cacheKey, SSDCacheManager.CacheTier.WARM);
                } else {
                    logger.debug("Cache miss for aggregated data: {} {} at {}", aggregationType, timeframe, timestamp);
                    metricsService.recordCacheMiss(cacheKey);
                }
                return result;
            });
    }
    
    /**
     * Cache volume data
     */
    @CachePut(value = "volume-data", key = "#symbol + ':' + #timeframe + ':' + #timestamp")
    public CompletableFuture<Void> cacheVolumeData(String symbol, String timeframe, long timestamp, Object volumeData) {
        String cacheKey = VOLUME_CACHE_PREFIX + symbol + ":" + timeframe + ":" + timestamp;
        return cacheManager.put(cacheKey, volumeData, SSDCacheManager.CacheTier.WARM)
            .thenRun(() -> {
                logger.debug("Cached volume data for {} {} at {}", symbol, timeframe, timestamp);
                metricsService.recordCachePut(cacheKey, SSDCacheManager.CacheTier.WARM, estimateSize(volumeData));
            });
    }
    
    /**
     * Get cached volume data
     */
    @Cacheable(value = "volume-data", key = "#symbol + ':' + #timeframe + ':' + #timestamp")
    public CompletableFuture<Object> getVolumeData(String symbol, String timeframe, long timestamp) {
        String cacheKey = VOLUME_CACHE_PREFIX + symbol + ":" + timeframe + ":" + timestamp;
        return cacheManager.get(cacheKey)
            .thenApply(result -> {
                if (result != null) {
                    logger.debug("Cache hit for volume data: {} {} at {}", symbol, timeframe, timestamp);
                    metricsService.recordCacheHit(cacheKey, SSDCacheManager.CacheTier.WARM);
                } else {
                    logger.debug("Cache miss for volume data: {} {} at {}", symbol, timeframe, timestamp);
                    metricsService.recordCacheMiss(cacheKey);
                }
                return result;
            });
    }
    
    /**
     * Cache correlation data
     */
    @CachePut(value = "correlation-data", key = "#symbol1 + ':' + #symbol2 + ':' + #timeframe + ':' + #timestamp")
    public CompletableFuture<Void> cacheCorrelationData(String symbol1, String symbol2, String timeframe, long timestamp, Object correlationData) {
        String cacheKey = CORRELATION_CACHE_PREFIX + symbol1 + ":" + symbol2 + ":" + timeframe + ":" + timestamp;
        return cacheManager.put(cacheKey, correlationData, SSDCacheManager.CacheTier.COLD)
            .thenRun(() -> {
                logger.debug("Cached correlation data for {} vs {} {} at {}", symbol1, symbol2, timeframe, timestamp);
                metricsService.recordCachePut(cacheKey, SSDCacheManager.CacheTier.COLD, estimateSize(correlationData));
            });
    }
    
    /**
     * Get cached correlation data
     */
    @Cacheable(value = "correlation-data", key = "#symbol1 + ':' + #symbol2 + ':' + #timeframe + ':' + #timestamp")
    public CompletableFuture<Object> getCorrelationData(String symbol1, String symbol2, String timeframe, long timestamp) {
        String cacheKey = CORRELATION_CACHE_PREFIX + symbol1 + ":" + symbol2 + ":" + timeframe + ":" + timestamp;
        return cacheManager.get(cacheKey)
            .thenApply(result -> {
                if (result != null) {
                    logger.debug("Cache hit for correlation data: {} vs {} {} at {}", symbol1, symbol2, timeframe, timestamp);
                    metricsService.recordCacheHit(cacheKey, SSDCacheManager.CacheTier.COLD);
                } else {
                    logger.debug("Cache miss for correlation data: {} vs {} {} at {}", symbol1, symbol2, timeframe, timestamp);
                    metricsService.recordCacheMiss(cacheKey);
                }
                return result;
            });
    }
    
    /**
     * Batch cache price data
     */
    public CompletableFuture<Void> cachePriceDataBatch(Map<String, Object> priceDataMap) {
        Map<String, Object> cacheData = priceDataMap.entrySet().stream()
            .collect(Collectors.toMap(
                entry -> PRICE_CACHE_PREFIX + entry.getKey(),
                Map.Entry::getValue
            ));
        
        return cacheManager.putBatch(cacheData, SSDCacheManager.CacheTier.HOT)
            .thenRun(() -> {
                logger.debug("Batch cached {} price data entries", priceDataMap.size());
                priceDataMap.forEach((key, data) -> {
                    metricsService.recordCachePut(PRICE_CACHE_PREFIX + key, SSDCacheManager.CacheTier.HOT, estimateSize(data));
                });
            });
    }
    
    /**
     * Batch get price data
     */
    public CompletableFuture<Map<String, Object>> getPriceDataBatch(List<String> priceKeys) {
        List<String> cacheKeys = priceKeys.stream()
            .map(key -> PRICE_CACHE_PREFIX + key)
            .collect(Collectors.toList());
        
        return cacheManager.getBatch(cacheKeys)
            .thenApply(results -> {
                Map<String, Object> priceData = new HashMap<>();
                for (String key : priceKeys) {
                    String cacheKey = PRICE_CACHE_PREFIX + key;
                    if (results.containsKey(cacheKey)) {
                        priceData.put(key, results.get(cacheKey));
                        metricsService.recordCacheHit(cacheKey, SSDCacheManager.CacheTier.HOT);
                    } else {
                        metricsService.recordCacheMiss(cacheKey);
                    }
                }
                return priceData;
            });
    }
    
    /**
     * Evict expired price data
     */
    @CacheEvict(value = "price-data", allEntries = true)
    public CompletableFuture<Void> evictExpiredPriceData() {
        return CompletableFuture.runAsync(() -> {
            logger.info("Evicting expired price data");
            // In real implementation, evict based on timestamp
        });
    }
    
    /**
     * Evict expired OHLCV data
     */
    @CacheEvict(value = "ohlcv-data", allEntries = true)
    public CompletableFuture<Void> evictExpiredOHLCVData() {
        return CompletableFuture.runAsync(() -> {
            logger.info("Evicting expired OHLCV data");
            // In real implementation, evict based on timestamp
        });
    }
    
    /**
     * Evict expired market data
     */
    @CacheEvict(value = "market-data", allEntries = true)
    public CompletableFuture<Void> evictExpiredMarketData() {
        return CompletableFuture.runAsync(() -> {
            logger.info("Evicting expired market data");
            // In real implementation, evict based on timestamp
        });
    }
    
    /**
     * Get data aggregation cache statistics
     */
    public DataAggregationCacheStatistics getDataAggregationCacheStatistics() {
        var overallMetrics = metricsService.getOverallMetrics();
        var accessPatterns = metricsService.getAccessPatternAnalysis();
        
        return DataAggregationCacheStatistics.builder()
            .totalOperations(overallMetrics.getTotalOperations())
            .hitRatio(overallMetrics.getHitRatio())
            .averageLatency(overallMetrics.getAverageLatency())
            .uniqueKeys(overallMetrics.getUniqueKeys())
            .tierDistribution(accessPatterns.getTierDistribution())
            .mostAccessedKeys(accessPatterns.getMostAccessedKeys())
            .build();
    }
    
    /**
     * Optimize data aggregation cache
     */
    public CompletableFuture<Void> optimizeDataAggregationCache() {
        return cacheManager.optimize()
            .thenRun(() -> {
                logger.info("Data aggregation cache optimization completed");
            });
    }
    
    /**
     * Clear all data aggregation cache
     */
    public CompletableFuture<Void> clearDataAggregationCache() {
        return CompletableFuture.allOf(
            cacheManager.evict("price:*"),
            cacheManager.evict("ohlcv:*"),
            cacheManager.evict("market:*"),
            cacheManager.evict("aggregated:*"),
            cacheManager.evict("volume:*"),
            cacheManager.evict("correlation:*")
        ).thenRun(() -> {
            logger.info("Data aggregation cache cleared");
        });
    }
    
    // Private helper methods
    
    private long estimateSize(Object data) {
        if (data == null) return 0;
        if (data instanceof String) return ((String) data).length() * 2;
        if (data instanceof byte[]) return ((byte[]) data).length;
        if (data instanceof Map) return ((Map<?, ?>) data).size() * 1024;
        if (data instanceof List) return ((List<?>) data).size() * 512;
        return 1024; // Default estimate
    }
}

