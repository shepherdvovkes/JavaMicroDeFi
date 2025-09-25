package com.defimon.multichain.plugin.impl;

import com.defimon.multichain.model.ChainType;
import com.defimon.multichain.plugin.BlockchainPlugin;
import com.defimon.multichain.plugin.PluginConfiguration;
import com.defimon.multichain.plugin.context.PluginContext;
import com.defimon.multichain.service.BlockchainSyncService;
import com.defimon.multichain.service.DataProcessingService;
import com.defimon.multichain.service.TransactionService;
import com.defimon.multichain.service.WalletService;
import com.defimon.multichain.service.impl.BitcoinSyncService;
import com.defimon.multichain.service.impl.BitcoinTransactionService;
import com.defimon.multichain.service.impl.BitcoinWalletService;
import com.defimon.multichain.service.impl.BitcoinDataProcessingService;
import com.defimon.multichain.config.BitcoinConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Bitcoin blockchain plugin implementation.
 * 
 * This plugin provides Bitcoin blockchain support using Java 21 with:
 * - Virtual threads for I/O-bound RPC operations
 * - Batch processing for 10-minute block intervals
 * - UTXO model transaction handling
 * - Native Bitcoin Core RPC integration
 * 
 * Features:
 * - Block synchronization with batch processing
 * - UTXO transaction tracking
 * - Address generation and validation
 * - Multi-signature wallet support
 * - Fee estimation and optimization
 */
@Component
public class BitcoinPlugin implements BlockchainPlugin<BitcoinConfiguration> {
    
    private static final Logger logger = LoggerFactory.getLogger(BitcoinPlugin.class);
    
    private static final String CHAIN_ID = "bitcoin";
    private static final String CHAIN_NAME = "Bitcoin";
    private static final String VERSION = "1.0.0";
    
    private BitcoinConfiguration configuration;
    private PluginContext context;
    
    // Service instances
    private BitcoinSyncService syncService;
    private BitcoinTransactionService transactionService;
    private BitcoinWalletService walletService;
    private BitcoinDataProcessingService dataProcessingService;
    
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
        return ChainType.UTXO;
    }
    
    @Override
    public String getVersion() {
        return VERSION;
    }
    
    @Override
    public void initialize(PluginContext context, BitcoinConfiguration config) {
        logger.info("Initializing Bitcoin plugin...");
        
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
            
            logger.info("Bitcoin plugin initialized successfully");
            
        } catch (Exception e) {
            logger.error("Failed to initialize Bitcoin plugin", e);
            healthy.set(false);
            throw new RuntimeException("Bitcoin plugin initialization failed", e);
        }
    }
    
    @Override
    public void start() {
        logger.info("Starting Bitcoin plugin...");
        
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
            
            logger.info("Bitcoin plugin started successfully");
            
        } catch (Exception e) {
            logger.error("Failed to start Bitcoin plugin", e);
            healthy.set(false);
            throw new RuntimeException("Bitcoin plugin start failed", e);
        }
    }
    
    @Override
    public void stop() {
        logger.info("Stopping Bitcoin plugin...");
        
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
            
            logger.info("Bitcoin plugin stopped successfully");
            
        } catch (Exception e) {
            logger.error("Failed to stop Bitcoin plugin", e);
            throw new RuntimeException("Bitcoin plugin stop failed", e);
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
            logger.error("Health check failed for Bitcoin plugin", e);
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
    public BitcoinConfiguration getConfiguration() {
        return configuration;
    }
    
    /**
     * Initializes all service instances.
     */
    private void initializeServices() {
        logger.debug("Initializing Bitcoin services...");
        
        // Initialize sync service with virtual threads for RPC calls
        syncService = new BitcoinSyncService(configuration, context);
        
        // Initialize transaction service for UTXO handling
        transactionService = new BitcoinTransactionService(configuration, context);
        
        // Initialize wallet service for Bitcoin addresses
        walletService = new BitcoinWalletService(configuration, context);
        
        // Initialize data processing service for UTXO processing
        dataProcessingService = new BitcoinDataProcessingService(configuration, context);
        
        logger.debug("Bitcoin services initialized");
    }
    
    /**
     * Validates the plugin configuration.
     */
    private void validateConfiguration() {
        logger.debug("Validating Bitcoin plugin configuration...");
        
        if (configuration == null) {
            throw new IllegalArgumentException("Configuration cannot be null");
        }
        
        if (configuration.getRpcUrl() == null || configuration.getRpcUrl().isEmpty()) {
            throw new IllegalArgumentException("RPC URL is required");
        }
        
        if (!CHAIN_ID.equals(configuration.getChainId())) {
            throw new IllegalArgumentException("Invalid chain ID for Bitcoin plugin");
        }
        
        if (configuration.getRpcUsername() == null || configuration.getRpcUsername().isEmpty()) {
            throw new IllegalArgumentException("RPC username is required");
        }
        
        if (configuration.getRpcPassword() == null || configuration.getRpcPassword().isEmpty()) {
            throw new IllegalArgumentException("RPC password is required");
        }
        
        logger.debug("Bitcoin plugin configuration validated");
    }
    
    /**
     * Sets up health monitoring for the plugin.
     */
    private void setupHealthMonitoring() {
        logger.debug("Setting up health monitoring for Bitcoin plugin...");
        
        // Register health check metrics
        context.meterRegistry().gauge("bitcoin.plugin.healthy", this, plugin -> plugin.healthy.get() ? 1.0 : 0.0);
        context.meterRegistry().gauge("bitcoin.plugin.initialized", this, plugin -> plugin.initialized.get() ? 1.0 : 0.0);
        context.meterRegistry().gauge("bitcoin.plugin.started", this, plugin -> plugin.started.get() ? 1.0 : 0.0);
        
        logger.debug("Health monitoring setup complete for Bitcoin plugin");
    }
}
