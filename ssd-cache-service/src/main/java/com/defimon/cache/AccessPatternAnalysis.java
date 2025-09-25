package com.defimon.cache;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * Access pattern analysis for cache optimization
 */
@Data
@Builder
public class AccessPatternAnalysis {
    private final Map<SSDCacheManager.CacheTier, Long> tierDistribution;
    private final Map<String, Long> keyFrequency;
    private final List<String> mostAccessedKeys;
    private final List<String> leastAccessedKeys;
}

