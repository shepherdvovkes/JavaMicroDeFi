package com.defimon.multichain;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Multi-Chain Service Application
 * 
 * A plugin-based architecture for supporting multiple blockchain networks
 * with Java 21, Spring Boot 3.2, and virtual threads.
 * 
 * Features:
 * - Plugin architecture for dynamic chain support
 * - Virtual threads for high concurrency
 * - Unified API for all blockchain operations
 * - Comprehensive monitoring and metrics
 * - Hot-swappable chain plugins
 */
@SpringBootApplication
@EnableConfigurationProperties
@EnableAsync
@EnableScheduling
public class MultiChainServiceApplication {

    public static void main(String[] args) {
        // Configure virtual threads for the entire application
        System.setProperty("spring.threads.virtual.enabled", "true");
        
        SpringApplication.run(MultiChainServiceApplication.class, args);
    }
}
