package com.defimon.cache;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cache Eviction Service
 * 
 * Provides intelligent cache eviction strategies for optimal performance.
 * Implements multiple eviction algorithms and automatic cache optimization.
 */
@Service
public class CacheEvictionService {
    
    private static final Logger logger = LoggerFactory.getLogger(CacheEvictionService.class);
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private CacheMetricsService metricsService;
    
    @Value("${cache.eviction.strategy:LRU}")
    private String evictionStrategy;
    
    @Value("${cache.eviction.memory.threshold:0.8}")
    private double memoryEvictionThreshold;
    
    @Value("${cache.eviction.ssd.threshold:0.9}")
    private double ssdEvictionThreshold;
    
    /**
     * Evict entries from memory cache
     */
    public void evictFromMemoryCache(Map<String, SSDCacheManager.CacheEntry> memoryCache, long targetSize) {
        long currentSize = calculateMemoryCacheSize(memoryCache);
        long sizeToEvict = currentSize - targetSize;
        
        if (sizeToEvict <= 0) {
            return;
        }
        
        logger.info("Evicting {} bytes from memory cache (current: {}, target: {})", 
            sizeToEvict, currentSize, targetSize);
        
        List<Map.Entry<String, SSDCacheManager.CacheEntry>> entries = new ArrayList<>(memoryCache.entrySet());
        
        switch (evictionStrategy.toUpperCase()) {
            case "LRU":
                evictLRU(entries, sizeToEvict, memoryCache);
                break;
            case "LFU":
                evictLFU(entries, sizeToEvict, memoryCache);
                break;
            case "TTL":
                evictTTL(entries, sizeToEvict, memoryCache);
                break;
            case "SIZE":
                evictBySize(entries, sizeToEvict, memoryCache);
                break;
            default:
                evictLRU(entries, sizeToEvict, memoryCache);
        }
        
        logger.info("Memory cache eviction completed. New size: {}", calculateMemoryCacheSize(memoryCache));
    }
    
    /**
     * Optimize SSD cache
     */
    public void optimizeSSDCache() {
        try {
            // Get SSD cache size
            long ssdCacheSize = getSSDCacheSize();
            long maxSSDCacheSize = getMaxSSDCacheSize();
            
            if (ssdCacheSize > maxSSDCacheSize * ssdEvictionThreshold) {
                logger.info("SSD cache optimization triggered. Current size: {}, Max: {}", 
                    ssdCacheSize, maxSSDCacheSize);
                
                // Evict least recently used entries from SSD cache
                evictFromSSDCache((long) (ssdCacheSize * 0.1)); // Evict 10%
            }
            
        } catch (Exception e) {
            logger.error("Error optimizing SSD cache", e);
        }
    }
    
    /**
     * Evict expired entries from all caches
     */
    public void evictExpiredEntries(Map<String, SSDCacheManager.CacheEntry> memoryCache) {
        Instant now = Instant.now();
        List<String> expiredKeys = new ArrayList<>();
        
        for (Map.Entry<String, SSDCacheManager.CacheEntry> entry : memoryCache.entrySet()) {
            if (isExpired(entry.getValue(), now)) {
                expiredKeys.add(entry.getKey());
            }
        }
        
        for (String key : expiredKeys) {
            memoryCache.remove(key);
        }
        
        if (!expiredKeys.isEmpty()) {
            logger.info("Evicted {} expired entries from memory cache", expiredKeys.size());
        }
    }
    
    /**
     * Get cache optimization recommendations
     */
    public CacheOptimizationRecommendations getOptimizationRecommendations() {
        CacheOptimizationRecommendations recommendations = new CacheOptimizationRecommendations();
        
        // Analyze memory cache
        long memoryUsage = getMemoryCacheUsage();
        long maxMemory = getMaxMemoryCacheSize();
        
        if (memoryUsage > maxMemory * memoryEvictionThreshold) {
            recommendations.addRecommendation(
                "Memory cache is " + String.format("%.1f", (double) memoryUsage / maxMemory * 100) + 
                "% full. Consider increasing memory cache size or improving eviction policies."
            );
        }
        
        // Analyze SSD cache
        long ssdUsage = getSSDCacheSize();
        long maxSSD = getMaxSSDCacheSize();
        
        if (ssdUsage > maxSSD * ssdEvictionThreshold) {
            recommendations.addRecommendation(
                "SSD cache is " + String.format("%.1f", (double) ssdUsage / maxSSD * 100) + 
                "% full. Consider increasing SSD cache size or optimizing data tiering."
            );
        }
        
        // Analyze hit ratios
        var overallMetrics = metricsService.getOverallMetrics();
        if (overallMetrics.getHitRatio() < 0.85) {
            recommendations.addRecommendation(
                "Overall cache hit ratio is " + String.format("%.1f", overallMetrics.getHitRatio() * 100) + 
                "%. Consider optimizing cache key strategies or increasing cache sizes."
            );
        }
        
        return recommendations;
    }
    
    // Private helper methods
    
    private void evictLRU(List<Map.Entry<String, SSDCacheManager.CacheEntry>> entries, 
                         long targetSize, Map<String, SSDCacheManager.CacheEntry> memoryCache) {
        // Sort by timestamp (oldest first)
        entries.sort((a, b) -> a.getValue().getTimestamp().compareTo(b.getValue().getTimestamp()));
        
        long evictedSize = 0;
        for (Map.Entry<String, SSDCacheManager.CacheEntry> entry : entries) {
            if (evictedSize >= targetSize) break;
            
            evictedSize += entry.getValue().getSize();
            memoryCache.remove(entry.getKey());
        }
    }
    
    private void evictLFU(List<Map.Entry<String, SSDCacheManager.CacheEntry>> entries, 
                         long targetSize, Map<String, SSDCacheManager.CacheEntry> memoryCache) {
        // Sort by access count (least frequent first)
        entries.sort((a, b) -> Long.compare(a.getValue().getAccessCount(), b.getValue().getAccessCount()));
        
        long evictedSize = 0;
        for (Map.Entry<String, SSDCacheManager.CacheEntry> entry : entries) {
            if (evictedSize >= targetSize) break;
            
            evictedSize += entry.getValue().getSize();
            memoryCache.remove(entry.getKey());
        }
    }
    
    private void evictTTL(List<Map.Entry<String, SSDCacheManager.CacheEntry>> entries, 
                          long targetSize, Map<String, SSDCacheManager.CacheEntry> memoryCache) {
        Instant now = Instant.now();
        
        // Sort by TTL (expiring soon first)
        entries.sort((a, b) -> {
            Duration ttlA = getTTLForTier(a.getValue().getTier());
            Duration ttlB = getTTLForTier(b.getValue().getTier());
            Instant expiryA = a.getValue().getTimestamp().plus(ttlA);
            Instant expiryB = b.getValue().getTimestamp().plus(ttlB);
            return expiryA.compareTo(expiryB);
        });
        
        long evictedSize = 0;
        for (Map.Entry<String, SSDCacheManager.CacheEntry> entry : entries) {
            if (evictedSize >= targetSize) break;
            
            evictedSize += entry.getValue().getSize();
            memoryCache.remove(entry.getKey());
        }
    }
    
    private void evictBySize(List<Map.Entry<String, SSDCacheManager.CacheEntry>> entries, 
                            long targetSize, Map<String, SSDCacheManager.CacheEntry> memoryCache) {
        // Sort by size (largest first)
        entries.sort((a, b) -> Long.compare(b.getValue().getSize(), a.getValue().getSize()));
        
        long evictedSize = 0;
        for (Map.Entry<String, SSDCacheManager.CacheEntry> entry : entries) {
            if (evictedSize >= targetSize) break;
            
            evictedSize += entry.getValue().getSize();
            memoryCache.remove(entry.getKey());
        }
    }
    
    private void evictFromSSDCache(long sizeToEvict) {
        try {
            // Get all SSD cache keys with their sizes
            Set<String> keys = redisTemplate.keys("ssd:*");
            if (keys == null || keys.isEmpty()) {
                return;
            }
            
            // Simple eviction - remove oldest keys
            List<String> sortedKeys = new ArrayList<>(keys);
            sortedKeys.sort(String::compareTo); // Simple ordering
            
            long evictedSize = 0;
            for (String key : sortedKeys) {
                if (evictedSize >= sizeToEvict) break;
                
                // Estimate size (in real implementation, track actual sizes)
                long estimatedSize = 1024; // Default estimate
                evictedSize += estimatedSize;
                
                redisTemplate.delete(key);
            }
            
            logger.info("Evicted {} bytes from SSD cache", evictedSize);
            
        } catch (Exception e) {
            logger.error("Error evicting from SSD cache", e);
        }
    }
    
    private long calculateMemoryCacheSize(Map<String, SSDCacheManager.CacheEntry> memoryCache) {
        return memoryCache.values().stream()
            .mapToLong(SSDCacheManager.CacheEntry::getSize)
            .sum();
    }
    
    private long getMemoryCacheUsage() {
        // In real implementation, get actual memory usage
        return 0;
    }
    
    private long getMaxMemoryCacheSize() {
        // In real implementation, get configured max size
        return 134217728; // 128MB default
    }
    
    private long getSSDCacheSize() {
        try {
            Set<String> keys = redisTemplate.keys("ssd:*");
            return keys != null ? keys.size() * 1024 : 0; // Estimate
        } catch (Exception e) {
            logger.error("Error getting SSD cache size", e);
            return 0;
        }
    }
    
    private long getMaxSSDCacheSize() {
        // In real implementation, get configured max size
        return 1073741824; // 1GB default
    }
    
    private boolean isExpired(SSDCacheManager.CacheEntry entry, Instant now) {
        Duration ttl = getTTLForTier(entry.getTier());
        return Duration.between(entry.getTimestamp(), now).compareTo(ttl) > 0;
    }
    
    private Duration getTTLForTier(SSDCacheManager.CacheTier tier) {
        switch (tier) {
            case HOT: return Duration.ofSeconds(60);
            case WARM: return Duration.ofSeconds(3600);
            case COLD: return Duration.ofSeconds(86400);
            default: return Duration.ofSeconds(3600);
        }
    }
    
    /**
     * Cache optimization recommendations
     */
    public static class CacheOptimizationRecommendations {
        private final List<String> recommendations = new ArrayList<>();
        
        public void addRecommendation(String recommendation) {
            recommendations.add(recommendation);
        }
        
        public List<String> getRecommendations() {
            return new ArrayList<>(recommendations);
        }
        
        public boolean hasRecommendations() {
            return !recommendations.isEmpty();
        }
        
        public String getSummary() {
            if (recommendations.isEmpty()) {
                return "Cache performance is optimal.";
            }
            
            StringBuilder summary = new StringBuilder("Cache optimization recommendations:\n");
            for (int i = 0; i < recommendations.size(); i++) {
                summary.append(String.format("%d. %s\n", i + 1, recommendations.get(i)));
            }
            return summary.toString();
        }
    }
}

