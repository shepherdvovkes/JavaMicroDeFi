package com.defimon.cache;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * SSD Cache Service Application
 * 
 * Main application class for the SSD cache service that provides intelligent
 * multi-tier caching for the Java Micro DeFi project.
 * 
 * Features:
 * - Multi-tier caching (L1: Memory, L2: SSD, L3: Primary Storage)
 * - Intelligent cache placement and eviction
 * - Blockchain data caching optimization
 * - Data aggregation cache optimization
 * - Performance monitoring and metrics
 * - Integration with Redis and MongoDB
 */
@SpringBootApplication
@EnableCaching
@EnableAsync
@EnableScheduling
public class SSDCacheApplication {

    public static void main(String[] args) {
        SpringApplication.run(SSDCacheApplication.class, args);
    }
}

