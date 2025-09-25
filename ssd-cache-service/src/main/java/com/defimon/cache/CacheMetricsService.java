package com.defimon.cache;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cache Metrics Service
 * 
 * Provides detailed metrics collection and analysis for cache performance monitoring.
 * Tracks access patterns, performance metrics, and provides insights for optimization.
 */
@Service
public class CacheMetricsService {
    
    private static final Logger logger = LoggerFactory.getLogger(CacheMetricsService.class);
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    // Metrics storage
    private final Map<String, AccessPattern> accessPatterns = new ConcurrentHashMap<>();
    private final AtomicLong totalOperations = new AtomicLong(0);
    private final AtomicLong totalLatency = new AtomicLong(0);
    
    /**
     * Record a cache hit
     */
    public void recordCacheHit(String key, SSDCacheManager.CacheTier tier) {
        totalOperations.incrementAndGet();
        
        AccessPattern pattern = accessPatterns.computeIfAbsent(key, k -> new AccessPattern(k));
        pattern.recordHit(tier);
        
        // Record performance metrics
        recordPerformanceMetrics(key, tier, true);
        
        logger.debug("Cache hit for key: {} in tier: {}", key, tier);
    }
    
    /**
     * Record a cache miss
     */
    public void recordCacheMiss(String key) {
        totalOperations.incrementAndGet();
        
        AccessPattern pattern = accessPatterns.computeIfAbsent(key, k -> new AccessPattern(k));
        pattern.recordMiss();
        
        // Record performance metrics
        recordPerformanceMetrics(key, null, false);
        
        logger.debug("Cache miss for key: {}", key);
    }
    
    /**
     * Record a cache put operation
     */
    public void recordCachePut(String key, SSDCacheManager.CacheTier tier, long size) {
        AccessPattern pattern = accessPatterns.computeIfAbsent(key, k -> new AccessPattern(k));
        pattern.recordPut(tier, size);
        
        logger.debug("Cache put for key: {} in tier: {} with size: {}", key, tier, size);
    }
    
    /**
     * Record a cache eviction
     */
    public void recordCacheEviction(String key) {
        AccessPattern pattern = accessPatterns.get(key);
        if (pattern != null) {
            pattern.recordEviction();
        }
        
        logger.debug("Cache eviction for key: {}", key);
    }
    
    /**
     * Get detailed metrics for a specific key
     */
    public KeyMetrics getKeyMetrics(String key) {
        AccessPattern pattern = accessPatterns.get(key);
        if (pattern == null) {
            return KeyMetrics.empty(key);
        }
        
        return KeyMetrics.builder()
            .key(key)
            .totalAccesses(pattern.getTotalAccesses())
            .hits(pattern.getHits())
            .misses(pattern.getMisses())
            .hitRatio(pattern.getHitRatio())
            .averageSize(pattern.getAverageSize())
            .lastAccess(pattern.getLastAccess())
            .accessFrequency(pattern.getAccessFrequency())
            .build();
    }
    
    /**
     * Get overall cache performance metrics
     */
    public OverallMetrics getOverallMetrics() {
        long totalHits = accessPatterns.values().stream()
            .mapToLong(AccessPattern::getHits)
            .sum();
        
        long totalMisses = accessPatterns.values().stream()
            .mapToLong(AccessPattern::getMisses)
            .sum();
        
        long totalAccesses = totalHits + totalMisses;
        double hitRatio = totalAccesses > 0 ? (double) totalHits / totalAccesses : 0.0;
        
        double averageLatency = totalOperations.get() > 0 ? 
            (double) totalLatency.get() / totalOperations.get() : 0.0;
        
        return OverallMetrics.builder()
            .totalOperations(totalOperations.get())
            .totalHits(totalHits)
            .totalMisses(totalMisses)
            .hitRatio(hitRatio)
            .averageLatency(averageLatency)
            .uniqueKeys(accessPatterns.size())
            .build();
    }
    
    /**
     * Get access pattern analysis
     */
    public AccessPatternAnalysis getAccessPatternAnalysis() {
        Map<SSDCacheManager.CacheTier, Long> tierDistribution = new java.util.HashMap<>();
        Map<String, Long> keyFrequency = new java.util.HashMap<>();
        
        for (AccessPattern pattern : accessPatterns.values()) {
            // Tier distribution
            SSDCacheManager.CacheTier preferredTier = pattern.getPreferredTier();
            tierDistribution.merge(preferredTier, 1L, Long::sum);
            
            // Key frequency
            keyFrequency.put(pattern.getKey(), pattern.getTotalAccesses());
        }
        
        return AccessPatternAnalysis.builder()
            .tierDistribution(tierDistribution)
            .keyFrequency(keyFrequency)
            .mostAccessedKeys(getMostAccessedKeys(10))
            .leastAccessedKeys(getLeastAccessedKeys(10))
            .build();
    }
    
    /**
     * Store metrics in Redis for persistence
     */
    public void persistMetrics() {
        try {
            ValueOperations<String, Object> ops = redisTemplate.opsForValue();
            
            // Store overall metrics
            OverallMetrics metrics = getOverallMetrics();
            ops.set("cache:metrics:overall", metrics);
            
            // Store access patterns for top keys
            Map<String, KeyMetrics> topKeys = getTopKeysMetrics(100);
            ops.set("cache:metrics:topkeys", topKeys);
            
            logger.info("Cache metrics persisted to Redis");
            
        } catch (Exception e) {
            logger.error("Error persisting cache metrics", e);
        }
    }
    
    /**
     * Load metrics from Redis
     */
    public void loadMetrics() {
        try {
            ValueOperations<String, Object> ops = redisTemplate.opsForValue();
            
            // Load overall metrics
            Object overallMetrics = ops.get("cache:metrics:overall");
            if (overallMetrics instanceof OverallMetrics) {
                logger.info("Loaded overall metrics from Redis");
            }
            
            // Load access patterns
            Object topKeys = ops.get("cache:metrics:topkeys");
            if (topKeys instanceof Map) {
                logger.info("Loaded access patterns from Redis");
            }
            
        } catch (Exception e) {
            logger.error("Error loading cache metrics", e);
        }
    }
    
    // Private helper methods
    
    private void recordPerformanceMetrics(String key, SSDCacheManager.CacheTier tier, boolean hit) {
        long startTime = System.nanoTime();
        
        // Simulate latency measurement (in real implementation, measure actual latency)
        long latency = hit ? 
            (tier == SSDCacheManager.CacheTier.HOT ? 1000 : 5000) : // 1ms for hot, 5ms for warm
            50000; // 50ms for miss
        
        totalLatency.addAndGet(latency);
    }
    
    private java.util.List<String> getMostAccessedKeys(int limit) {
        return accessPatterns.values().stream()
            .sorted((a, b) -> Long.compare(b.getTotalAccesses(), a.getTotalAccesses()))
            .limit(limit)
            .map(AccessPattern::getKey)
            .collect(java.util.stream.Collectors.toList());
    }
    
    private java.util.List<String> getLeastAccessedKeys(int limit) {
        return accessPatterns.values().stream()
            .sorted((a, b) -> Long.compare(a.getTotalAccesses(), b.getTotalAccesses()))
            .limit(limit)
            .map(AccessPattern::getKey)
            .collect(java.util.stream.Collectors.toList());
    }
    
    private Map<String, KeyMetrics> getTopKeysMetrics(int limit) {
        return accessPatterns.values().stream()
            .sorted((a, b) -> Long.compare(b.getTotalAccesses(), a.getTotalAccesses()))
            .limit(limit)
            .collect(java.util.stream.Collectors.toMap(
                AccessPattern::getKey,
                pattern -> getKeyMetrics(pattern.getKey())
            ));
    }
    
    /**
     * Access pattern tracking for individual keys
     */
    private static class AccessPattern {
        private final String key;
        private long hits = 0;
        private long misses = 0;
        private long puts = 0;
        private long evictions = 0;
        private long totalSize = 0;
        private Instant lastAccess = Instant.now();
        private SSDCacheManager.CacheTier preferredTier = SSDCacheManager.CacheTier.WARM;
        
        public AccessPattern(String key) {
            this.key = key;
        }
        
        public void recordHit(SSDCacheManager.CacheTier tier) {
            hits++;
            lastAccess = Instant.now();
            updatePreferredTier(tier);
        }
        
        public void recordMiss() {
            misses++;
            lastAccess = Instant.now();
        }
        
        public void recordPut(SSDCacheManager.CacheTier tier, long size) {
            puts++;
            totalSize += size;
            lastAccess = Instant.now();
            updatePreferredTier(tier);
        }
        
        public void recordEviction() {
            evictions++;
        }
        
        private void updatePreferredTier(SSDCacheManager.CacheTier tier) {
            // Simple heuristic - prefer higher tier for frequently accessed data
            if (tier == SSDCacheManager.CacheTier.HOT || 
                (tier == SSDCacheManager.CacheTier.WARM && preferredTier == SSDCacheManager.CacheTier.COLD)) {
                preferredTier = tier;
            }
        }
        
        // Getters
        public String getKey() { return key; }
        public long getHits() { return hits; }
        public long getMisses() { return misses; }
        public long getTotalAccesses() { return hits + misses; }
        public double getHitRatio() { 
            long total = getTotalAccesses();
            return total > 0 ? (double) hits / total : 0.0; 
        }
        public long getAverageSize() { 
            return puts > 0 ? totalSize / puts : 0; 
        }
        public Instant getLastAccess() { return lastAccess; }
        public double getAccessFrequency() {
            Duration sinceFirstAccess = Duration.between(Instant.now().minus(Duration.ofDays(1)), lastAccess);
            return sinceFirstAccess.toHours() > 0 ? (double) getTotalAccesses() / sinceFirstAccess.toHours() : 0.0;
        }
        public SSDCacheManager.CacheTier getPreferredTier() { return preferredTier; }
    }
}

