package com.defimon.multichain.plugin.impl;

import com.defimon.multichain.model.ChainType;
import com.defimon.multichain.plugin.BlockchainPlugin;
import com.defimon.multichain.plugin.PluginConfiguration;
import com.defimon.multichain.plugin.context.PluginContext;
import com.defimon.multichain.service.BlockchainSyncService;
import com.defimon.multichain.service.DataProcessingService;
import com.defimon.multichain.service.TransactionService;
import com.defimon.multichain.service.WalletService;
import com.defimon.multichain.service.impl.EthereumSyncService;
import com.defimon.multichain.service.impl.EthereumTransactionService;
import com.defimon.multichain.service.impl.EthereumWalletService;
import com.defimon.multichain.service.impl.EthereumDataProcessingService;
import com.defimon.multichain.config.EthereumConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Ethereum blockchain plugin implementation.
 * 
 * This plugin provides Ethereum blockchain support with a hybrid architecture:
 * - Rust-based high-performance sync engine for data processing
 * - Java-based business logic for complex operations
 * - Virtual threads for I/O-bound integration tasks
 * 
 * Features:
 * - Real-time block synchronization
 * - Smart contract interaction
 * - Event extraction and processing
 * - Multi-signature wallet support
 * - Gas optimization
 */
@Component
public class EthereumPlugin implements BlockchainPlugin<EthereumConfiguration> {
    
    private static final Logger logger = LoggerFactory.getLogger(EthereumPlugin.class);
    
    private static final String CHAIN_ID = "1";
    private static final String CHAIN_NAME = "Ethereum";
    private static final String VERSION = "1.0.0";
    
    private EthereumConfiguration configuration;
    private PluginContext context;
    
    // Service instances
    private EthereumSyncService syncService;
    private EthereumTransactionService transactionService;
    private EthereumWalletService walletService;
    private EthereumDataProcessingService dataProcessingService;
    
    // Plugin state
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final AtomicBoolean started = new AtomicBoolean(false);
    private final AtomicBoolean healthy = new AtomicBoolean(false);
    
    @Override
    public String getChainId() {
        return CHAIN_ID;
    }
    
    @Override
    public String getChainName() {
        return CHAIN_NAME;
    }
    
    @Override
    public ChainType getChainType() {
        return ChainType.EVM;
    }
    
    @Override
    public String getVersion() {
        return VERSION;
    }
    
    @Override
    public void initialize(PluginContext context, EthereumConfiguration config) {
        logger.info("Initializing Ethereum plugin...");
        
        try {
            this.context = context;
            this.configuration = config;
            
            // Initialize services
            initializeServices();
            
            // Validate configuration
            validateConfiguration();
            
            // Set up health monitoring
            setupHealthMonitoring();
            
            initialized.set(true);
            healthy.set(true);
            
            logger.info("Ethereum plugin initialized successfully");
            
        } catch (Exception e) {
            logger.error("Failed to initialize Ethereum plugin", e);
            healthy.set(false);
            throw new RuntimeException("Ethereum plugin initialization failed", e);
        }
    }
    
    @Override
    public void start() {
        logger.info("Starting Ethereum plugin...");
        
        if (!initialized.get()) {
            throw new IllegalStateException("Plugin must be initialized before starting");
        }
        
        try {
            // Start services
            syncService.start();
            transactionService.start();
            walletService.start();
            dataProcessingService.start();
            
            started.set(true);
            healthy.set(true);
            
            logger.info("Ethereum plugin started successfully");
            
        } catch (Exception e) {
            logger.error("Failed to start Ethereum plugin", e);
            healthy.set(false);
            throw new RuntimeException("Ethereum plugin start failed", e);
        }
    }
    
    @Override
    public void stop() {
        logger.info("Stopping Ethereum plugin...");
        
        try {
            // Stop services gracefully
            if (syncService != null) {
                syncService.stop();
            }
            if (transactionService != null) {
                transactionService.stop();
            }
            if (walletService != null) {
                walletService.stop();
            }
            if (dataProcessingService != null) {
                dataProcessingService.stop();
            }
            
            started.set(false);
            
            logger.info("Ethereum plugin stopped successfully");
            
        } catch (Exception e) {
            logger.error("Failed to stop Ethereum plugin", e);
            throw new RuntimeException("Ethereum plugin stop failed", e);
        }
    }
    
    @Override
    public boolean isHealthy() {
        if (!initialized.get() || !started.get()) {
            return false;
        }
        
        try {
            // Check service health
            boolean syncHealthy = syncService != null && syncService.isHealthy();
            boolean transactionHealthy = transactionService != null && transactionService.isHealthy();
            boolean walletHealthy = walletService != null && walletService.isHealthy();
            boolean dataHealthy = dataProcessingService != null && dataProcessingService.isHealthy();
            
            boolean allHealthy = syncHealthy && transactionHealthy && walletHealthy && dataHealthy;
            healthy.set(allHealthy);
            
            return allHealthy;
            
        } catch (Exception e) {
            logger.error("Health check failed for Ethereum plugin", e);
            healthy.set(false);
            return false;
        }
    }
    
    @Override
    public BlockchainSyncService getSyncService() {
        if (!initialized.get()) {
            throw new IllegalStateException("Plugin not initialized");
        }
        return syncService;
    }
    
    @Override
    public TransactionService getTransactionService() {
        if (!initialized.get()) {
            throw new IllegalStateException("Plugin not initialized");
        }
        return transactionService;
    }
    
    @Override
    public WalletService getWalletService() {
        if (!initialized.get()) {
            throw new IllegalStateException("Plugin not initialized");
        }
        return walletService;
    }
    
    @Override
    public DataProcessingService getDataProcessingService() {
        if (!initialized.get()) {
            throw new IllegalStateException("Plugin not initialized");
        }
        return dataProcessingService;
    }
    
    @Override
    public EthereumConfiguration getConfiguration() {
        return configuration;
    }
    
    /**
     * Initializes all service instances.
     */
    private void initializeServices() {
        logger.debug("Initializing Ethereum services...");
        
        // Initialize sync service with Rust backend integration
        syncService = new EthereumSyncService(configuration, context);
        
        // Initialize transaction service with Java business logic
        transactionService = new EthereumTransactionService(configuration, context);
        
        // Initialize wallet service
        walletService = new EthereumWalletService(configuration, context);
        
        // Initialize data processing service
        dataProcessingService = new EthereumDataProcessingService(configuration, context);
        
        logger.debug("Ethereum services initialized");
    }
    
    /**
     * Validates the plugin configuration.
     */
    private void validateConfiguration() {
        logger.debug("Validating Ethereum plugin configuration...");
        
        if (configuration == null) {
            throw new IllegalArgumentException("Configuration cannot be null");
        }
        
        if (configuration.getRpcUrl() == null || configuration.getRpcUrl().isEmpty()) {
            throw new IllegalArgumentException("RPC URL is required");
        }
        
        if (!CHAIN_ID.equals(configuration.getChainId())) {
            throw new IllegalArgumentException("Invalid chain ID for Ethereum plugin");
        }
        
        logger.debug("Ethereum plugin configuration validated");
    }
    
    /**
     * Sets up health monitoring for the plugin.
     */
    private void setupHealthMonitoring() {
        logger.debug("Setting up health monitoring for Ethereum plugin...");
        
        // Register health check metrics
        context.meterRegistry().gauge("ethereum.plugin.healthy", this, plugin -> plugin.healthy.get() ? 1.0 : 0.0);
        context.meterRegistry().gauge("ethereum.plugin.initialized", this, plugin -> plugin.initialized.get() ? 1.0 : 0.0);
        context.meterRegistry().gauge("ethereum.plugin.started", this, plugin -> plugin.started.get() ? 1.0 : 0.0);
        
        logger.debug("Health monitoring setup complete for Ethereum plugin");
    }
}
