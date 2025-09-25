package com.defimon.cache;

import lombok.Builder;
import lombok.Data;

/**
 * Overall cache performance metrics
 */
@Data
@Builder
public class OverallMetrics {
    private final long totalOperations;
    private final long totalHits;
    private final long totalMisses;
    private final double hitRatio;
    private final double averageLatency;
    private final int uniqueKeys;
}

