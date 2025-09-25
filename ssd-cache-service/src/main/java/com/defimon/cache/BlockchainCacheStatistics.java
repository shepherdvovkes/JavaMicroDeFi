package com.defimon.cache;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * Blockchain-specific cache statistics
 */
@Data
@Builder
public class BlockchainCacheStatistics {
    private final long totalOperations;
    private final double hitRatio;
    private final double averageLatency;
    private final int uniqueKeys;
    private final Map<SSDCacheManager.CacheTier, Long> tierDistribution;
    private final List<String> mostAccessedKeys;
    
    /**
     * Get performance summary for blockchain cache
     */
    public String getBlockchainPerformanceSummary() {
        return String.format(
            "Blockchain Cache Performance Summary:\n" +
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
     * Check if blockchain cache performance is optimal
     */
    public boolean isOptimalBlockchainPerformance() {
        return hitRatio >= 0.80 && averageLatency <= 10.0;
    }
    
    /**
     * Get blockchain-specific optimization recommendations
     */
    public String getBlockchainOptimizationRecommendations() {
        StringBuilder recommendations = new StringBuilder();
        
        if (hitRatio < 0.80) {
            recommendations.append("- Blockchain cache hit ratio is low. Consider increasing cache size for blocks and transactions.\n");
        }
        
        if (averageLatency > 10.0) {
            recommendations.append("- Average latency is high. Consider optimizing cache tier placement or increasing memory cache size.\n");
        }
        
        if (tierDistribution.getOrDefault(SSDCacheManager.CacheTier.HOT, 0L) < tierDistribution.getOrDefault(SSDCacheManager.CacheTier.WARM, 0L)) {
            recommendations.append("- Consider promoting more frequently accessed data to hot cache tier.\n");
        }
        
        return recommendations.length() > 0 ? recommendations.toString() : "Blockchain cache performance is optimal.";
    }
}

