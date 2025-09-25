package com.defimon.cache;

import lombok.Builder;
import lombok.Data;

/**
 * Cache Statistics
 * 
 * Provides comprehensive statistics for cache performance monitoring
 * and optimization decisions.
 */
@Data
@Builder
public class CacheStatistics {
    
    // Overall statistics
    private final long totalRequests;
    private final long totalHits;
    private final long totalMisses;
    private final double hitRatio;
    
    // Memory cache statistics
    private final long memoryHits;
    private final long memoryMisses;
    private final double memoryHitRatio;
    
    // SSD cache statistics
    private final long ssdHits;
    private final long ssdMisses;
    private final double ssdHitRatio;
    
    // Cache size information
    private final int memoryCacheSize;
    private final long memoryCacheMemoryUsage;
    
    /**
     * Get performance summary
     */
    public String getPerformanceSummary() {
        return String.format(
            "Cache Performance Summary:\n" +
            "  Overall Hit Ratio: %.2f%% (%d/%d)\n" +
            "  Memory Hit Ratio: %.2f%% (%d/%d)\n" +
            "  SSD Hit Ratio: %.2f%% (%d/%d)\n" +
            "  Memory Cache: %d entries, %d bytes\n",
            hitRatio * 100, totalHits, totalRequests,
            memoryHitRatio * 100, memoryHits, memoryHits + memoryMisses,
            ssdHitRatio * 100, ssdHits, ssdHits + ssdMisses,
            memoryCacheSize, memoryCacheMemoryUsage
        );
    }
    
    /**
     * Check if cache performance is optimal
     */
    public boolean isOptimalPerformance() {
        return hitRatio >= 0.85 && memoryHitRatio >= 0.70 && ssdHitRatio >= 0.60;
    }
    
    /**
     * Get recommendations for optimization
     */
    public String getOptimizationRecommendations() {
        StringBuilder recommendations = new StringBuilder();
        
        if (hitRatio < 0.85) {
            recommendations.append("- Overall hit ratio is low. Consider increasing cache size or improving cache key strategies.\n");
        }
        
        if (memoryHitRatio < 0.70) {
            recommendations.append("- Memory cache hit ratio is low. Consider increasing memory cache size or improving data placement.\n");
        }
        
        if (ssdHitRatio < 0.60) {
            recommendations.append("- SSD cache hit ratio is low. Consider optimizing SSD cache configuration or data tiering.\n");
        }
        
        if (memoryCacheMemoryUsage > 0.9 * 134217728) { // 90% of 128MB default
            recommendations.append("- Memory cache is nearly full. Consider increasing memory cache size or improving eviction policies.\n");
        }
        
        return recommendations.length() > 0 ? recommendations.toString() : "Cache performance is optimal.";
    }
}

