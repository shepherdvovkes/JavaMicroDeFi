package com.defimon.cache;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.SetOperations;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SSD Cache Manager Service
 * 
 * Provides intelligent multi-tier caching with SSD optimization for the Java Micro DeFi project.
 * Implements L1 (in-memory), L2 (SSD cache), and L3 (primary storage) caching strategy.
 * 
 * Features:
 * - Intelligent cache placement based on access patterns
 * - Automatic data tiering and eviction policies
 * - Performance monitoring and optimization
 * - Integration with Redis and MongoDB
 * - Blockchain data caching optimization
 */
@Service
public class SSDCacheManager {
    
    private static final Logger logger = LoggerFactory.getLogger(SSDCacheManager.class);
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private CacheMetricsService metricsService;
    
    @Autowired
    private CacheEvictionService evictionService;
    
    @Value("${cache.ssd.enabled:true}")
    private boolean ssdCacheEnabled;
    
    @Value("${cache.ssd.size:1073741824}") // 1GB default
    private long ssdCacheSize;
    
    @Value("${cache.memory.size:134217728}") // 128MB default
    private long memoryCacheSize;
    
    @Value("${cache.ttl.hot:60}") // 1 minute
    private long hotDataTTL;
    
    @Value("${cache.ttl.warm:3600}") // 1 hour
    private long warmDataTTL;
    
    @Value("${cache.ttl.cold:86400}") // 24 hours
    private long coldDataTTL;
    
    // In-memory cache for hot data
    private final Map<String, CacheEntry> memoryCache = new ConcurrentHashMap<>();
    
    // Cache statistics
    private final AtomicLong cacheHits = new AtomicLong(0);
    private final AtomicLong cacheMisses = new AtomicLong(0);
    private final AtomicLong ssdCacheHits = new AtomicLong(0);
    private final AtomicLong ssdCacheMisses = new AtomicLong(0);
    
    /**
     * Cache entry with metadata for intelligent tiering
     */
    private static class CacheEntry {
        private final Object data;
        private final Instant timestamp;
        private final long accessCount;
        private final long size;
        private final CacheTier tier;
        
        public CacheEntry(Object data, long size, CacheTier tier) {
            this.data = data;
            this.timestamp = Instant.now();
            this.accessCount = 1;
            this.size = size;
            this.tier = tier;
        }
        
        public Object getData() { return data; }
        public Instant getTimestamp() { return timestamp; }
        public long getAccessCount() { return accessCount; }
        public long getSize() { return size; }
        public CacheTier getTier() { return tier; }
        
        public CacheEntry withIncrementedAccess() {
            return new CacheEntry(data, size, tier) {
                @Override
                public long getAccessCount() { return accessCount + 1; }
            };
        }
    }
    
    /**
     * Cache tiers for intelligent data placement
     */
    public enum CacheTier {
        HOT,    // L1: In-memory, < 1ms access
        WARM,   // L2: SSD cache, 1-10ms access
        COLD    // L3: Primary storage, 10-100ms access
    }
    
    /**
     * Get data with intelligent cache lookup
     */
    @Cacheable(value = "ssd-cache", key = "#key")
    public CompletableFuture<Object> get(String key) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // L1: Check in-memory cache first
                CacheEntry entry = memoryCache.get(key);
                if (entry != null && !isExpired(entry)) {
                    cacheHits.incrementAndGet();
                    metricsService.recordCacheHit(key, CacheTier.HOT);
                    return entry.getData();
                }
                
                // L2: Check SSD cache (Redis)
                if (ssdCacheEnabled) {
                    ValueOperations<String, Object> ops = redisTemplate.opsForValue();
                    Object cachedData = ops.get("ssd:" + key);
                    if (cachedData != null) {
                        ssdCacheHits.incrementAndGet();
                        metricsService.recordCacheHit(key, CacheTier.WARM);
                        
                        // Promote to L1 if frequently accessed
                        promoteToMemoryCache(key, cachedData);
                        return cachedData;
                    }
                }
                
                // Cache miss - will be handled by calling service
                cacheMisses.incrementAndGet();
                ssdCacheMisses.incrementAndGet();
                metricsService.recordCacheMiss(key);
                return null;
                
            } catch (Exception e) {
                logger.error("Error retrieving from cache for key: {}", key, e);
                return null;
            }
        });
    }
    
    /**
     * Put data into appropriate cache tier
     */
    @CachePut(value = "ssd-cache", key = "#key")
    public CompletableFuture<Void> put(String key, Object data, CacheTier preferredTier) {
        return CompletableFuture.runAsync(() -> {
            try {
                long dataSize = estimateSize(data);
                
                // Determine optimal cache tier based on data characteristics
                CacheTier actualTier = determineOptimalTier(key, data, dataSize, preferredTier);
                
                switch (actualTier) {
                    case HOT:
                        putInMemoryCache(key, data, dataSize);
                        break;
                    case WARM:
                        putInSSDCache(key, data);
                        break;
                    case COLD:
                        // Data will be stored in primary storage by calling service
                        logger.debug("Data for key {} stored in primary storage", key);
                        break;
                }
                
                metricsService.recordCachePut(key, actualTier, dataSize);
                
            } catch (Exception e) {
                logger.error("Error storing in cache for key: {}", key, e);
            }
        });
    }
    
    /**
     * Evict data from cache
     */
    @CacheEvict(value = "ssd-cache", key = "#key")
    public CompletableFuture<Void> evict(String key) {
        return CompletableFuture.runAsync(() -> {
            try {
                // Remove from memory cache
                memoryCache.remove(key);
                
                // Remove from SSD cache
                if (ssdCacheEnabled) {
                    redisTemplate.delete("ssd:" + key);
                }
                
                metricsService.recordCacheEviction(key);
                logger.debug("Evicted cache entry for key: {}", key);
                
            } catch (Exception e) {
                logger.error("Error evicting cache for key: {}", key, e);
            }
        });
    }
    
    /**
     * Batch operations for improved performance
     */
    public CompletableFuture<Map<String, Object>> getBatch(List<String> keys) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, Object> results = new HashMap<>();
            
            // Batch get from Redis for SSD cache
            if (ssdCacheEnabled && !keys.isEmpty()) {
                List<String> ssdKeys = keys.stream()
                    .map(key -> "ssd:" + key)
                    .collect(Collectors.toList());
                
                List<Object> values = redisTemplate.opsForValue().multiGet(ssdKeys);
                for (int i = 0; i < keys.size(); i++) {
                    if (values.get(i) != null) {
                        results.put(keys.get(i), values.get(i));
                        ssdCacheHits.incrementAndGet();
                    }
                }
            }
            
            // Fill remaining from memory cache
            for (String key : keys) {
                if (!results.containsKey(key)) {
                    CacheEntry entry = memoryCache.get(key);
                    if (entry != null && !isExpired(entry)) {
                        results.put(key, entry.getData());
                        cacheHits.incrementAndGet();
                    }
                }
            }
            
            return results;
        });
    }
    
    /**
     * Batch put operations
     */
    public CompletableFuture<Void> putBatch(Map<String, Object> data, CacheTier tier) {
        return CompletableFuture.runAsync(() -> {
            if (ssdCacheEnabled && tier == CacheTier.WARM) {
                // Batch put to Redis
                Map<String, Object> ssdData = data.entrySet().stream()
                    .collect(Collectors.toMap(
                        entry -> "ssd:" + entry.getKey(),
                        Map.Entry::getValue
                    ));
                
                redisTemplate.opsForValue().multiSet(ssdData);
            } else {
                // Individual puts for memory cache
                data.forEach((key, value) -> {
                    long size = estimateSize(value);
                    putInMemoryCache(key, value, size);
                });
            }
        });
    }
    
    /**
     * Get cache statistics
     */
    public CacheStatistics getStatistics() {
        long totalHits = cacheHits.get() + ssdCacheHits.get();
        long totalMisses = cacheMisses.get() + ssdCacheMisses.get();
        long totalRequests = totalHits + totalMisses;
        
        double hitRatio = totalRequests > 0 ? (double) totalHits / totalRequests : 0.0;
        double memoryHitRatio = (cacheHits.get() + cacheMisses.get()) > 0 ? 
            (double) cacheHits.get() / (cacheHits.get() + cacheMisses.get()) : 0.0;
        double ssdHitRatio = (ssdCacheHits.get() + ssdCacheMisses.get()) > 0 ? 
            (double) ssdCacheHits.get() / (ssdCacheHits.get() + ssdCacheMisses.get()) : 0.0;
        
        return CacheStatistics.builder()
            .totalRequests(totalRequests)
            .totalHits(totalHits)
            .totalMisses(totalMisses)
            .hitRatio(hitRatio)
            .memoryHits(cacheHits.get())
            .memoryMisses(cacheMisses.get())
            .memoryHitRatio(memoryHitRatio)
            .ssdHits(ssdCacheHits.get())
            .ssdMisses(ssdCacheMisses.get())
            .ssdHitRatio(ssdHitRatio)
            .memoryCacheSize(memoryCache.size())
            .memoryCacheMemoryUsage(calculateMemoryUsage())
            .build();
    }
    
    /**
     * Optimize cache based on access patterns
     */
    public CompletableFuture<Void> optimize() {
        return CompletableFuture.runAsync(() -> {
            try {
                // Evict expired entries
                evictExpiredEntries();
                
                // Optimize memory cache based on access patterns
                optimizeMemoryCache();
                
                // Trigger SSD cache optimization
                if (ssdCacheEnabled) {
                    evictionService.optimizeSSDCache();
                }
                
                logger.info("Cache optimization completed");
                
            } catch (Exception e) {
                logger.error("Error during cache optimization", e);
            }
        });
    }
    
    // Private helper methods
    
    private void putInMemoryCache(String key, Object data, long size) {
        if (size <= memoryCacheSize) {
            // Check if we need to evict to make space
            if (calculateMemoryUsage() + size > memoryCacheSize) {
                evictionService.evictFromMemoryCache(memoryCache, size);
            }
            
            CacheEntry entry = new CacheEntry(data, size, CacheTier.HOT);
            memoryCache.put(key, entry);
        }
    }
    
    private void putInSSDCache(String key, Object data) {
        if (ssdCacheEnabled) {
            ValueOperations<String, Object> ops = redisTemplate.opsForValue();
            ops.set("ssd:" + key, data, Duration.ofSeconds(warmDataTTL));
        }
    }
    
    private void promoteToMemoryCache(String key, Object data) {
        long size = estimateSize(data);
        if (size <= memoryCacheSize) {
            putInMemoryCache(key, data, size);
        }
    }
    
    private CacheTier determineOptimalTier(String key, Object data, long size, CacheTier preferredTier) {
        // Simple heuristic - can be enhanced with ML algorithms
        if (size <= 1024 * 1024 && preferredTier == CacheTier.HOT) { // 1MB
            return CacheTier.HOT;
        } else if (size <= 100 * 1024 * 1024 && preferredTier != CacheTier.COLD) { // 100MB
            return CacheTier.WARM;
        } else {
            return CacheTier.COLD;
        }
    }
    
    private boolean isExpired(CacheEntry entry) {
        Duration ttl = getTTLForTier(entry.getTier());
        return Duration.between(entry.getTimestamp(), Instant.now()).compareTo(ttl) > 0;
    }
    
    private Duration getTTLForTier(CacheTier tier) {
        switch (tier) {
            case HOT: return Duration.ofSeconds(hotDataTTL);
            case WARM: return Duration.ofSeconds(warmDataTTL);
            case COLD: return Duration.ofSeconds(coldDataTTL);
            default: return Duration.ofSeconds(warmDataTTL);
        }
    }
    
    private long estimateSize(Object data) {
        // Simple size estimation - can be enhanced
        if (data == null) return 0;
        if (data instanceof String) return ((String) data).length() * 2;
        if (data instanceof byte[]) return ((byte[]) data).length;
        return 1024; // Default estimate
    }
    
    private long calculateMemoryUsage() {
        return memoryCache.values().stream()
            .mapToLong(CacheEntry::getSize)
            .sum();
    }
    
    private void evictExpiredEntries() {
        memoryCache.entrySet().removeIf(entry -> isExpired(entry.getValue()));
    }
    
    private void optimizeMemoryCache() {
        // Simple LRU optimization - can be enhanced with more sophisticated algorithms
        if (calculateMemoryUsage() > memoryCacheSize * 0.8) {
            evictionService.evictFromMemoryCache(memoryCache, memoryCacheSize / 4);
        }
    }
}

