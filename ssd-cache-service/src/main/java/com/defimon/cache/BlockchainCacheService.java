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
 * Blockchain Cache Service
 * 
 * Provides specialized caching for blockchain data including blocks, transactions,
 * and state data. Optimized for high-frequency access patterns and large data volumes.
 */
@Service
public class BlockchainCacheService {
    
    private static final Logger logger = LoggerFactory.getLogger(BlockchainCacheService.class);
    
    @Autowired
    private SSDCacheManager cacheManager;
    
    @Autowired
    private CacheMetricsService metricsService;
    
    // Cache configuration
    private static final String BLOCK_CACHE_PREFIX = "block:";
    private static final String TRANSACTION_CACHE_PREFIX = "tx:";
    private static final String STATE_CACHE_PREFIX = "state:";
    private static final String RECEIPT_CACHE_PREFIX = "receipt:";
    
    // Cache TTLs
    private static final long BLOCK_CACHE_TTL = 3600; // 1 hour
    private static final long TRANSACTION_CACHE_TTL = 1800; // 30 minutes
    private static final long STATE_CACHE_TTL = 7200; // 2 hours
    private static final long RECEIPT_CACHE_TTL = 3600; // 1 hour
    
    /**
     * Cache a blockchain block
     */
    @CachePut(value = "blockchain-blocks", key = "#blockHash")
    public CompletableFuture<Void> cacheBlock(String blockHash, Object blockData) {
        return cacheManager.put(
            BLOCK_CACHE_PREFIX + blockHash, 
            blockData, 
            SSDCacheManager.CacheTier.WARM
        ).thenRun(() -> {
            logger.debug("Cached block: {}", blockHash);
            metricsService.recordCachePut(BLOCK_CACHE_PREFIX + blockHash, SSDCacheManager.CacheTier.WARM, estimateSize(blockData));
        });
    }
    
    /**
     * Get a cached blockchain block
     */
    @Cacheable(value = "blockchain-blocks", key = "#blockHash")
    public CompletableFuture<Object> getBlock(String blockHash) {
        return cacheManager.get(BLOCK_CACHE_PREFIX + blockHash)
            .thenApply(result -> {
                if (result != null) {
                    logger.debug("Cache hit for block: {}", blockHash);
                    metricsService.recordCacheHit(BLOCK_CACHE_PREFIX + blockHash, SSDCacheManager.CacheTier.WARM);
                } else {
                    logger.debug("Cache miss for block: {}", blockHash);
                    metricsService.recordCacheMiss(BLOCK_CACHE_PREFIX + blockHash);
                }
                return result;
            });
    }
    
    /**
     * Cache a transaction
     */
    @CachePut(value = "blockchain-transactions", key = "#txHash")
    public CompletableFuture<Void> cacheTransaction(String txHash, Object transactionData) {
        return cacheManager.put(
            TRANSACTION_CACHE_PREFIX + txHash, 
            transactionData, 
            SSDCacheManager.CacheTier.HOT
        ).thenRun(() -> {
            logger.debug("Cached transaction: {}", txHash);
            metricsService.recordCachePut(TRANSACTION_CACHE_PREFIX + txHash, SSDCacheManager.CacheTier.HOT, estimateSize(transactionData));
        });
    }
    
    /**
     * Get a cached transaction
     */
    @Cacheable(value = "blockchain-transactions", key = "#txHash")
    public CompletableFuture<Object> getTransaction(String txHash) {
        return cacheManager.get(TRANSACTION_CACHE_PREFIX + txHash)
            .thenApply(result -> {
                if (result != null) {
                    logger.debug("Cache hit for transaction: {}", txHash);
                    metricsService.recordCacheHit(TRANSACTION_CACHE_PREFIX + txHash, SSDCacheManager.CacheTier.HOT);
                } else {
                    logger.debug("Cache miss for transaction: {}", txHash);
                    metricsService.recordCacheMiss(TRANSACTION_CACHE_PREFIX + txHash);
                }
                return result;
            });
    }
    
    /**
     * Cache state data
     */
    @CachePut(value = "blockchain-state", key = "#stateKey")
    public CompletableFuture<Void> cacheState(String stateKey, Object stateData) {
        return cacheManager.put(
            STATE_CACHE_PREFIX + stateKey, 
            stateData, 
            SSDCacheManager.CacheTier.WARM
        ).thenRun(() -> {
            logger.debug("Cached state: {}", stateKey);
            metricsService.recordCachePut(STATE_CACHE_PREFIX + stateKey, SSDCacheManager.CacheTier.WARM, estimateSize(stateData));
        });
    }
    
    /**
     * Get cached state data
     */
    @Cacheable(value = "blockchain-state", key = "#stateKey")
    public CompletableFuture<Object> getState(String stateKey) {
        return cacheManager.get(STATE_CACHE_PREFIX + stateKey)
            .thenApply(result -> {
                if (result != null) {
                    logger.debug("Cache hit for state: {}", stateKey);
                    metricsService.recordCacheHit(STATE_CACHE_PREFIX + stateKey, SSDCacheManager.CacheTier.WARM);
                } else {
                    logger.debug("Cache miss for state: {}", stateKey);
                    metricsService.recordCacheMiss(STATE_CACHE_PREFIX + stateKey);
                }
                return result;
            });
    }
    
    /**
     * Cache transaction receipt
     */
    @CachePut(value = "blockchain-receipts", key = "#txHash")
    public CompletableFuture<Void> cacheReceipt(String txHash, Object receiptData) {
        return cacheManager.put(
            RECEIPT_CACHE_PREFIX + txHash, 
            receiptData, 
            SSDCacheManager.CacheTier.WARM
        ).thenRun(() -> {
            logger.debug("Cached receipt: {}", txHash);
            metricsService.recordCachePut(RECEIPT_CACHE_PREFIX + txHash, SSDCacheManager.CacheTier.WARM, estimateSize(receiptData));
        });
    }
    
    /**
     * Get cached transaction receipt
     */
    @Cacheable(value = "blockchain-receipts", key = "#txHash")
    public CompletableFuture<Object> getReceipt(String txHash) {
        return cacheManager.get(RECEIPT_CACHE_PREFIX + txHash)
            .thenApply(result -> {
                if (result != null) {
                    logger.debug("Cache hit for receipt: {}", txHash);
                    metricsService.recordCacheHit(RECEIPT_CACHE_PREFIX + txHash, SSDCacheManager.CacheTier.WARM);
                } else {
                    logger.debug("Cache miss for receipt: {}", txHash);
                    metricsService.recordCacheMiss(RECEIPT_CACHE_PREFIX + txHash);
                }
                return result;
            });
    }
    
    /**
     * Batch cache blocks
     */
    public CompletableFuture<Void> cacheBlocks(Map<String, Object> blocks) {
        Map<String, Object> cacheData = blocks.entrySet().stream()
            .collect(Collectors.toMap(
                entry -> BLOCK_CACHE_PREFIX + entry.getKey(),
                Map.Entry::getValue
            ));
        
        return cacheManager.putBatch(cacheData, SSDCacheManager.CacheTier.WARM)
            .thenRun(() -> {
                logger.debug("Batch cached {} blocks", blocks.size());
                blocks.forEach((hash, data) -> {
                    metricsService.recordCachePut(BLOCK_CACHE_PREFIX + hash, SSDCacheManager.CacheTier.WARM, estimateSize(data));
                });
            });
    }
    
    /**
     * Batch get blocks
     */
    public CompletableFuture<Map<String, Object>> getBlocks(List<String> blockHashes) {
        List<String> cacheKeys = blockHashes.stream()
            .map(hash -> BLOCK_CACHE_PREFIX + hash)
            .collect(Collectors.toList());
        
        return cacheManager.getBatch(cacheKeys)
            .thenApply(results -> {
                Map<String, Object> blocks = new HashMap<>();
                for (String hash : blockHashes) {
                    String cacheKey = BLOCK_CACHE_PREFIX + hash;
                    if (results.containsKey(cacheKey)) {
                        blocks.put(hash, results.get(cacheKey));
                        metricsService.recordCacheHit(cacheKey, SSDCacheManager.CacheTier.WARM);
                    } else {
                        metricsService.recordCacheMiss(cacheKey);
                    }
                }
                return blocks;
            });
    }
    
    /**
     * Batch cache transactions
     */
    public CompletableFuture<Void> cacheTransactions(Map<String, Object> transactions) {
        Map<String, Object> cacheData = transactions.entrySet().stream()
            .collect(Collectors.toMap(
                entry -> TRANSACTION_CACHE_PREFIX + entry.getKey(),
                Map.Entry::getValue
            ));
        
        return cacheManager.putBatch(cacheData, SSDCacheManager.CacheTier.HOT)
            .thenRun(() -> {
                logger.debug("Batch cached {} transactions", transactions.size());
                transactions.forEach((hash, data) -> {
                    metricsService.recordCachePut(TRANSACTION_CACHE_PREFIX + hash, SSDCacheManager.CacheTier.HOT, estimateSize(data));
                });
            });
    }
    
    /**
     * Batch get transactions
     */
    public CompletableFuture<Map<String, Object>> getTransactions(List<String> txHashes) {
        List<String> cacheKeys = txHashes.stream()
            .map(hash -> TRANSACTION_CACHE_PREFIX + hash)
            .collect(Collectors.toList());
        
        return cacheManager.getBatch(cacheKeys)
            .thenApply(results -> {
                Map<String, Object> transactions = new HashMap<>();
                for (String hash : txHashes) {
                    String cacheKey = TRANSACTION_CACHE_PREFIX + hash;
                    if (results.containsKey(cacheKey)) {
                        transactions.put(hash, results.get(cacheKey));
                        metricsService.recordCacheHit(cacheKey, SSDCacheManager.CacheTier.HOT);
                    } else {
                        metricsService.recordCacheMiss(cacheKey);
                    }
                }
                return transactions;
            });
    }
    
    /**
     * Evict block from cache
     */
    @CacheEvict(value = "blockchain-blocks", key = "#blockHash")
    public CompletableFuture<Void> evictBlock(String blockHash) {
        return cacheManager.evict(BLOCK_CACHE_PREFIX + blockHash)
            .thenRun(() -> {
                logger.debug("Evicted block: {}", blockHash);
                metricsService.recordCacheEviction(BLOCK_CACHE_PREFIX + blockHash);
            });
    }
    
    /**
     * Evict transaction from cache
     */
    @CacheEvict(value = "blockchain-transactions", key = "#txHash")
    public CompletableFuture<Void> evictTransaction(String txHash) {
        return cacheManager.evict(TRANSACTION_CACHE_PREFIX + txHash)
            .thenRun(() -> {
                logger.debug("Evicted transaction: {}", txHash);
                metricsService.recordCacheEviction(TRANSACTION_CACHE_PREFIX + txHash);
            });
    }
    
    /**
     * Evict state from cache
     */
    @CacheEvict(value = "blockchain-state", key = "#stateKey")
    public CompletableFuture<Void> evictState(String stateKey) {
        return cacheManager.evict(STATE_CACHE_PREFIX + stateKey)
            .thenRun(() -> {
                logger.debug("Evicted state: {}", stateKey);
                metricsService.recordCacheEviction(STATE_CACHE_PREFIX + stateKey);
            });
    }
    
    /**
     * Get blockchain cache statistics
     */
    public BlockchainCacheStatistics getBlockchainCacheStatistics() {
        var overallMetrics = metricsService.getOverallMetrics();
        var accessPatterns = metricsService.getAccessPatternAnalysis();
        
        return BlockchainCacheStatistics.builder()
            .totalOperations(overallMetrics.getTotalOperations())
            .hitRatio(overallMetrics.getHitRatio())
            .averageLatency(overallMetrics.getAverageLatency())
            .uniqueKeys(overallMetrics.getUniqueKeys())
            .tierDistribution(accessPatterns.getTierDistribution())
            .mostAccessedKeys(accessPatterns.getMostAccessedKeys())
            .build();
    }
    
    /**
     * Optimize blockchain cache
     */
    public CompletableFuture<Void> optimizeBlockchainCache() {
        return cacheManager.optimize()
            .thenRun(() -> {
                logger.info("Blockchain cache optimization completed");
            });
    }
    
    /**
     * Clear all blockchain cache
     */
    public CompletableFuture<Void> clearBlockchainCache() {
        return CompletableFuture.allOf(
            cacheManager.evict("block:*"),
            cacheManager.evict("tx:*"),
            cacheManager.evict("state:*"),
            cacheManager.evict("receipt:*")
        ).thenRun(() -> {
            logger.info("Blockchain cache cleared");
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

