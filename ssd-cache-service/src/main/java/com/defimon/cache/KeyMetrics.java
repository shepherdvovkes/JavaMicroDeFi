package com.defimon.cache;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;

/**
 * Key-specific cache metrics
 */
@Data
@Builder
public class KeyMetrics {
    private final String key;
    private final long totalAccesses;
    private final long hits;
    private final long misses;
    private final double hitRatio;
    private final long averageSize;
    private final Instant lastAccess;
    private final double accessFrequency;
    
    public static KeyMetrics empty(String key) {
        return KeyMetrics.builder()
            .key(key)
            .totalAccesses(0)
            .hits(0)
            .misses(0)
            .hitRatio(0.0)
            .averageSize(0)
            .lastAccess(Instant.now())
            .accessFrequency(0.0)
            .build();
    }
}

