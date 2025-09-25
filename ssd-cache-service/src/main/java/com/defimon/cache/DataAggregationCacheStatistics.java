package com.defimon.cache;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * Data aggregation-specific cache statistics
 */
@Data
@Builder
public class DataAggregationCacheStatistics {
    private final long totalOperations;
    private final double hitRatio;
    private final double averageLatency;
    private final int uniqueKeys;
    private final Map<SSDCacheManager.CacheTier, Long> tierDistribution;
    private final List<String> mostAccessedKeys;
    
    /**
     * Get performance summary for data aggregation cache
     */
    public String getDataAggregationPerformanceSummary() {
        return String.format(
            "Data Aggregation Cache Performance Summary:\n" +
            "  Total Operations: %d\n" +
            "  Hit Ratio: %.2f%%\n" +
            "  Average Latency: %.2f ms\n" +
            "  Unique Keys: %d\n" +
            "  Tier Distribution: %s\n" +
            "  Most Accessed Keys: %s\n",
            totalOperations,
            hitRatio * 100,
            averageLatency,
            uniqueKeys,
            tierDistribution,
            mostAccessedKeys
        );
    }
    
    /**
     * Check if data aggregation cache performance is optimal
     */
    public boolean isOptimalDataAggregationPerformance() {
        return hitRatio >= 0.85 && averageLatency <= 5.0;
    }
    
    /**
     * Get data aggregation-specific optimization recommendations
     */
    public String getDataAggregationOptimizationRecommendations() {
        StringBuilder recommendations = new StringBuilder();
        
        if (hitRatio < 0.85) {
            recommendations.append("- Data aggregation cache hit ratio is low. Consider increasing cache size for price and market data.\n");
        }
        
        if (averageLatency > 5.0) {
            recommendations.append("- Average latency is high. Consider optimizing cache tier placement for real-time data.\n");
        }
        
        if (tierDistribution.getOrDefault(SSDCacheManager.CacheTier.HOT, 0L) < tierDistribution.getOrDefault(SSDCacheManager.CacheTier.WARM, 0L)) {
            recommendations.append("- Consider promoting more frequently accessed real-time data to hot cache tier.\n");
        }
        
        return recommendations.length() > 0 ? recommendations.toString() : "Data aggregation cache performance is optimal.";
    }
}

