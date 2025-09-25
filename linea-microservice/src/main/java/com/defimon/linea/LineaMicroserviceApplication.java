package com.defimon.linea;

import com.defimon.linea.config.LineaConfiguration;
import com.defimon.linea.service.LineaSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Linea Microservice Application
 * 
 * This is the main application class for the Linea blockchain data collection microservice.
 * It provides comprehensive data collection for Linea blockchain including:
 * - Real-time block synchronization with 10 concurrent workers
 * - Archive data collection for complete chain history
 * - DeFi protocol monitoring and metrics
 * - Bridge transaction tracking
 * - Token and contract analysis
 * - Network metrics and health monitoring
 * 
 * Features:
 * - Java 21 with virtual threads for high-performance I/O operations
 * - Spring Boot 3.2 with reactive programming
 * - SQLite database integration for data storage
 * - REST API endpoints for data access
 * - Comprehensive monitoring and metrics
 * - Docker containerization support
 */
@SpringBootApplication
@EnableConfigurationProperties(LineaConfiguration.class)
@EnableAsync
@EnableScheduling
public class LineaMicroserviceApplication implements CommandLineRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(LineaMicroserviceApplication.class);
    
    @Autowired
    private LineaConfiguration configuration;
    
    @Autowired
    private LineaSyncService syncService;
    
    public static void main(String[] args) {
        logger.info("🚀 Starting Linea Microservice Application...");
        
        // Set system properties for Java 21
        System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "20");
        System.setProperty("jdk.virtualThreadScheduler.parallelism", "20");
        
        SpringApplication.run(LineaMicroserviceApplication.class, args);
    }
    
    @Override
    public void run(String... args) throws Exception {
        logger.info("🎯 Linea Microservice Application started successfully!");
        logger.info("📊 Configuration:");
        logger.info("   - RPC URL: {}", configuration.getEffectiveRpcUrl());
        logger.info("   - Database Path: {}", configuration.getDatabasePath());
        logger.info("   - Archive Database Path: {}", configuration.getArchiveDatabasePath());
        logger.info("   - Max Concurrent Workers: {}", configuration.getMaxConcurrentWorkers());
        logger.info("   - Block Collection Interval: {}ms", configuration.getBlockCollectionInterval());
        logger.info("   - Transaction Collection Interval: {}ms", configuration.getTransactionCollectionInterval());
        logger.info("   - Account Collection Interval: {}ms", configuration.getAccountCollectionInterval());
        logger.info("   - Contract Collection Interval: {}ms", configuration.getContractCollectionInterval());
        logger.info("   - Token Collection Interval: {}ms", configuration.getTokenCollectionInterval());
        logger.info("   - DeFi Collection Interval: {}ms", configuration.getDefiCollectionInterval());
        logger.info("   - Rate Limit: {} requests/second", configuration.getRateLimitPerSecond());
        logger.info("   - Request Timeout: {} seconds", configuration.getRequestTimeoutSeconds());
        logger.info("   - Retry Attempts: {}", configuration.getRetryAttempts());
        logger.info("   - Archive Mode: {}", configuration.getArchiveMode());
        logger.info("   - Archive Start Block: {}", configuration.getArchiveStartBlock());
        logger.info("   - Archive Batch Size: {}", configuration.getArchiveBatchSize());
        logger.info("   - Archive Concurrent Workers: {}", configuration.getArchiveConcurrentWorkers());
        logger.info("   - Enable Real-time Monitoring: {}", configuration.getEnableRealTimeMonitoring());
        logger.info("   - Enable Historical Data: {}", configuration.getEnableHistoricalData());
        logger.info("   - Enable DeFi Metrics: {}", configuration.getEnableDefiMetrics());
        logger.info("   - Enable Bridge Metrics: {}", configuration.getEnableBridgeMetrics());
        logger.info("   - Enable Token Metrics: {}", configuration.getEnableTokenMetrics());
        logger.info("   - Enable Contract Metrics: {}", configuration.getEnableContractMetrics());
        logger.info("   - Enable Backup: {}", configuration.getEnableBackup());
        logger.info("   - Backup Interval: {} hours", configuration.getBackupIntervalHours());
        logger.info("   - Backup Retention: {} days", configuration.getBackupRetentionDays());
        logger.info("   - Backup Path: {}", configuration.getBackupPath());
        logger.info("   - Compress Archive Data: {}", configuration.getCompressArchiveData());
        logger.info("   - Archive Compression Level: {}", configuration.getArchiveCompressionLevel());
        logger.info("   - Native Token: {}", configuration.getNativeToken());
        logger.info("   - Wrapped Token: {}", configuration.getWrappedToken());
        logger.info("   - Chain ID: {}", configuration.getChainId());
        logger.info("   - Network Name: {}", configuration.getNetworkName());
        logger.info("   - Block Time: {}ms", configuration.getBlockTime());
        logger.info("   - Explorer URL: {}", configuration.getExplorerUrl());
        logger.info("   - Bridge Contract: {}", configuration.getBridgeContract());
        logger.info("   - Message Service: {}", configuration.getMessageService());
        logger.info("   - DeFi Protocols: {}", configuration.getDefiProtocols());
        logger.info("   - Bridge Contracts: {}", configuration.getBridgeContracts());
        logger.info("   - Log Level: {}", configuration.getLogLevel());
        logger.info("   - Log File: {}", configuration.getLogFile());
        logger.info("   - Log Retention: {} days", configuration.getLogRetentionDays());
        logger.info("   - API Host: {}", configuration.getApiHost());
        logger.info("   - API Port: {}", configuration.getApiPort());
        logger.info("   - Node Environment: {}", configuration.getNodeEnv());
        logger.info("   - Working Directory: {}", configuration.getWorkingDir());
        
        // Start the synchronization service
        if (configuration.getEnableRealTimeMonitoring()) {
            logger.info("🔄 Starting Linea synchronization service...");
            syncService.startSync();
            logger.info("✅ Linea synchronization service started successfully");
        } else {
            logger.info("⏸️ Real-time monitoring is disabled");
        }
        
        // Print startup banner
        printStartupBanner();
        
        logger.info("🎉 Linea Microservice Application is ready!");
        logger.info("📡 API endpoints available at: http://{}:{}", configuration.getApiHost(), configuration.getApiPort());
        logger.info("📊 Health check: http://{}:{}/actuator/health", configuration.getApiHost(), configuration.getApiPort());
        logger.info("📈 Metrics: http://{}:{}/actuator/metrics", configuration.getApiHost(), configuration.getApiPort());
        logger.info("🔍 API Documentation: http://{}:{}/swagger-ui.html", configuration.getApiHost(), configuration.getApiPort());
    }
    
    /**
     * Prints the startup banner.
     */
    private void printStartupBanner() {
        logger.info("""
                
                ╔══════════════════════════════════════════════════════════════════════════════╗
                ║                                                                              ║
                ║  🚀 LINEA MICROSERVICE - DEFIMON BLOCKCHAIN DATA COLLECTION                ║
                ║                                                                              ║
                ║  📊 Features:                                                               ║
                ║     • Real-time block synchronization with 10 concurrent workers           ║
                ║     • Archive data collection for complete chain history                   ║
                ║     • DeFi protocol monitoring and metrics                                 ║
                ║     • Bridge transaction tracking                                          ║
                ║     • Token and contract analysis                                          ║
                ║     • Network metrics and health monitoring                                ║
                ║                                                                              ║
                ║  🛠️ Technology Stack:                                                      ║
                ║     • Java 21 with virtual threads for high-performance I/O                ║
                ║     • Spring Boot 3.2 with reactive programming                            ║
                ║     • SQLite database integration for data storage                         ║
                ║     • REST API endpoints for data access                                   ║
                ║     • Comprehensive monitoring and metrics                                 ║
                ║     • Docker containerization support                                     ║
                ║                                                                              ║
                ║  📈 Data Collection:                                                       ║
                ║     • Blocks: Real-time and archive collection                            ║
                ║     • Transactions: Complete transaction data with receipts                ║
                ║     • Accounts: Balance, nonce, and code information                       ║
                ║     • Contracts: Creation, verification, and analysis                      ║
                ║     • Tokens: ERC20, ERC721, ERC1155 support                              ║
                ║     • DeFi: Protocol monitoring and TVL tracking                           ║
                ║     • Bridge: Cross-chain transaction tracking                             ║
                ║     • Metrics: Network health and performance indicators                   ║
                ║                                                                              ║
                ║  🔗 Linea Network:                                                         ║
                ║     • Chain ID: 59144                                                      ║
                ║     • Network: Linea Mainnet                                               ║
                ║     • Block Time: 2 seconds                                                ║
                ║     • Explorer: https://lineascan.build                                   ║
                ║     • RPC: {}                                                             ║
                ║                                                                              ║
                ║  📊 Database:                                                              ║
                ║     • Real-time: {}                                                       ║
                ║     • Archive: {}                                                         ║
                ║                                                                              ║
                ║  🚀 Ready to collect Linea blockchain data!                               ║
                ║                                                                              ║
                ╚══════════════════════════════════════════════════════════════════════════════╝
                """, configuration.getEffectiveRpcUrl(), configuration.getDatabasePath(), configuration.getArchiveDatabasePath());
    }
}
