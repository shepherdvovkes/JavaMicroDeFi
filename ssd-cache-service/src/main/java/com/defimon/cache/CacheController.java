package com.defimon.cache;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cache REST Controller
 * 
 * Provides REST API endpoints for cache operations, statistics, and management.
 * Supports both individual and batch operations for optimal performance.
 */
@RestController
@RequestMapping("/api/cache")
public class CacheController {
    
    private static final Logger logger = LoggerFactory.getLogger(CacheController.class);
    
    @Autowired
    private SSDCacheManager cacheManager;
    
    @Autowired
    private BlockchainCacheService blockchainCacheService;
    
    @Autowired
    private DataAggregationCacheService dataAggregationCacheService;
    
    @Autowired
    private CacheMetricsService metricsService;
    
    /**
     * Get cache statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<CacheStatistics> getCacheStatistics() {
        try {
            CacheStatistics statistics = cacheManager.getStatistics();
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            logger.error("Error getting cache statistics", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get blockchain cache statistics
     */
    @GetMapping("/blockchain/statistics")
    public ResponseEntity<BlockchainCacheStatistics> getBlockchainCacheStatistics() {
        try {
            BlockchainCacheStatistics statistics = blockchainCacheService.getBlockchainCacheStatistics();
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            logger.error("Error getting blockchain cache statistics", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get data aggregation cache statistics
     */
    @GetMapping("/data-aggregation/statistics")
    public ResponseEntity<DataAggregationCacheStatistics> getDataAggregationCacheStatistics() {
        try {
            DataAggregationCacheStatistics statistics = dataAggregationCacheService.getDataAggregationCacheStatistics();
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            logger.error("Error getting data aggregation cache statistics", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get cache data by key
     */
    @GetMapping("/{key}")
    public CompletableFuture<ResponseEntity<Object>> getCacheData(@PathVariable String key) {
        return cacheManager.get(key)
            .thenApply(result -> {
                if (result != null) {
                    return ResponseEntity.ok(result);
                } else {
                    return ResponseEntity.notFound().build();
                }
            })
            .exceptionally(throwable -> {
                logger.error("Error getting cache data for key: {}", key, throwable);
                return ResponseEntity.internalServerError().build();
            });
    }
    
    /**
     * Put cache data
     */
    @PostMapping("/{key}")
    public CompletableFuture<ResponseEntity<Void>> putCacheData(
            @PathVariable String key,
            @RequestBody Object data,
            @RequestParam(defaultValue = "WARM") String tier) {
        
        SSDCacheManager.CacheTier cacheTier = SSDCacheManager.CacheTier.valueOf(tier.toUpperCase());
        
        return cacheManager.put(key, data, cacheTier)
            .thenApply(result -> ResponseEntity.ok().<Void>build())
            .exceptionally(throwable -> {
                logger.error("Error putting cache data for key: {}", key, throwable);
                return ResponseEntity.internalServerError().build();
            });
    }
    
    /**
     * Evict cache data
     */
    @DeleteMapping("/{key}")
    public CompletableFuture<ResponseEntity<Void>> evictCacheData(@PathVariable String key) {
        return cacheManager.evict(key)
            .thenApply(result -> ResponseEntity.ok().<Void>build())
            .exceptionally(throwable -> {
                logger.error("Error evicting cache data for key: {}", key, throwable);
                return ResponseEntity.internalServerError().build();
            });
    }
    
    /**
     * Batch get cache data
     */
    @PostMapping("/batch/get")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> getBatchCacheData(@RequestBody java.util.List<String> keys) {
        return cacheManager.getBatch(keys)
            .thenApply(ResponseEntity::ok)
            .exceptionally(throwable -> {
                logger.error("Error getting batch cache data", throwable);
                return ResponseEntity.internalServerError().build();
            });
    }
    
    /**
     * Batch put cache data
     */
    @PostMapping("/batch/put")
    public CompletableFuture<ResponseEntity<Void>> putBatchCacheData(
            @RequestBody Map<String, Object> data,
            @RequestParam(defaultValue = "WARM") String tier) {
        
        SSDCacheManager.CacheTier cacheTier = SSDCacheManager.CacheTier.valueOf(tier.toUpperCase());
        
        return cacheManager.putBatch(data, cacheTier)
            .thenApply(result -> ResponseEntity.ok().<Void>build())
            .exceptionally(throwable -> {
                logger.error("Error putting batch cache data", throwable);
                return ResponseEntity.internalServerError().build();
            });
    }
    
    /**
     * Optimize cache
     */
    @PostMapping("/optimize")
    public CompletableFuture<ResponseEntity<String>> optimizeCache() {
        return cacheManager.optimize()
            .thenApply(result -> ResponseEntity.ok("Cache optimization completed"))
            .exceptionally(throwable -> {
                logger.error("Error optimizing cache", throwable);
                return ResponseEntity.internalServerError().build();
            });
    }
    
    /**
     * Blockchain cache operations
     */
    
    @GetMapping("/blockchain/block/{blockHash}")
    public CompletableFuture<ResponseEntity<Object>> getBlock(@PathVariable String blockHash) {
        return blockchainCacheService.getBlock(blockHash)
            .thenApply(result -> {
                if (result != null) {
                    return ResponseEntity.ok(result);
                } else {
                    return ResponseEntity.notFound().build();
                }
            })
            .exceptionally(throwable -> {
                logger.error("Error getting block: {}", blockHash, throwable);
                return ResponseEntity.internalServerError().build();
            });
    }
    
    @PostMapping("/blockchain/block/{blockHash}")
    public CompletableFuture<ResponseEntity<Void>> cacheBlock(
            @PathVariable String blockHash,
            @RequestBody Object blockData) {
        
        return blockchainCacheService.cacheBlock(blockHash, blockData)
            .thenApply(result -> ResponseEntity.ok().<Void>build())
            .exceptionally(throwable -> {
                logger.error("Error caching block: {}", blockHash, throwable);
                return ResponseEntity.internalServerError().build();
            });
    }
    
    @GetMapping("/blockchain/transaction/{txHash}")
    public CompletableFuture<ResponseEntity<Object>> getTransaction(@PathVariable String txHash) {
        return blockchainCacheService.getTransaction(txHash)
            .thenApply(result -> {
                if (result != null) {
                    return ResponseEntity.ok(result);
                } else {
                    return ResponseEntity.notFound().build();
                }
            })
            .exceptionally(throwable -> {
                logger.error("Error getting transaction: {}", txHash, throwable);
                return ResponseEntity.internalServerError().build();
            });
    }
    
    @PostMapping("/blockchain/transaction/{txHash}")
    public CompletableFuture<ResponseEntity<Void>> cacheTransaction(
            @PathVariable String txHash,
            @RequestBody Object transactionData) {
        
        return blockchainCacheService.cacheTransaction(txHash, transactionData)
            .thenApply(result -> ResponseEntity.ok().<Void>build())
            .exceptionally(throwable -> {
                logger.error("Error caching transaction: {}", txHash, throwable);
                return ResponseEntity.internalServerError().build();
            });
    }
    
    /**
     * Data aggregation cache operations
     */
    
    @GetMapping("/data-aggregation/price/{symbol}/{timestamp}")
    public CompletableFuture<ResponseEntity<Object>> getPriceData(
            @PathVariable String symbol,
            @PathVariable long timestamp) {
        
        return dataAggregationCacheService.getPriceData(symbol, timestamp)
            .thenApply(result -> {
                if (result != null) {
                    return ResponseEntity.ok(result);
                } else {
                    return ResponseEntity.notFound().build();
                }
            })
            .exceptionally(throwable -> {
                logger.error("Error getting price data: {} at {}", symbol, timestamp, throwable);
                return ResponseEntity.internalServerError().build();
            });
    }
    
    @PostMapping("/data-aggregation/price/{symbol}/{timestamp}")
    public CompletableFuture<ResponseEntity<Void>> cachePriceData(
            @PathVariable String symbol,
            @PathVariable long timestamp,
            @RequestBody Object priceData) {
        
        return dataAggregationCacheService.cachePriceData(symbol, timestamp, priceData)
            .thenApply(result -> ResponseEntity.ok().<Void>build())
            .exceptionally(throwable -> {
                logger.error("Error caching price data: {} at {}", symbol, timestamp, throwable);
                return ResponseEntity.internalServerError().build();
            });
    }
    
    @GetMapping("/data-aggregation/ohlcv/{symbol}/{timeframe}/{timestamp}")
    public CompletableFuture<ResponseEntity<Object>> getOHLCVData(
            @PathVariable String symbol,
            @PathVariable String timeframe,
            @PathVariable long timestamp) {
        
        return dataAggregationCacheService.getOHLCVData(symbol, timeframe, timestamp)
            .thenApply(result -> {
                if (result != null) {
                    return ResponseEntity.ok(result);
                } else {
                    return ResponseEntity.notFound().build();
                }
            })
            .exceptionally(throwable -> {
                logger.error("Error getting OHLCV data: {} {} at {}", symbol, timeframe, timestamp, throwable);
                return ResponseEntity.internalServerError().build();
            });
    }
    
    @PostMapping("/data-aggregation/ohlcv/{symbol}/{timeframe}/{timestamp}")
    public CompletableFuture<ResponseEntity<Void>> cacheOHLCVData(
            @PathVariable String symbol,
            @PathVariable String timeframe,
            @PathVariable long timestamp,
            @RequestBody Object ohlcvData) {
        
        return dataAggregationCacheService.cacheOHLCVData(symbol, timeframe, timestamp, ohlcvData)
            .thenApply(result -> ResponseEntity.ok().<Void>build())
            .exceptionally(throwable -> {
                logger.error("Error caching OHLCV data: {} {} at {}", symbol, timeframe, timestamp, throwable);
                return ResponseEntity.internalServerError().build();
            });
    }
    
    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        try {
            CacheStatistics statistics = cacheManager.getStatistics();
            return ResponseEntity.ok(Map.of(
                "status", "UP",
                "hitRatio", statistics.getHitRatio(),
                "totalOperations", statistics.getTotalRequests(),
                "memoryCacheSize", statistics.getMemoryCacheSize(),
                "timestamp", System.currentTimeMillis()
            ));
        } catch (Exception e) {
            logger.error("Error in health check", e);
            return ResponseEntity.ok(Map.of(
                "status", "DOWN",
                "error", e.getMessage(),
                "timestamp", System.currentTimeMillis()
            ));
        }
    }
}

