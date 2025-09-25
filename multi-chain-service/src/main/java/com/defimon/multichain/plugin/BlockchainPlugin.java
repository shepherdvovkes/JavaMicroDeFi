package com.defimon.multichain.plugin;

import com.defimon.multichain.model.ChainType;
import com.defimon.multichain.plugin.context.PluginContext;
import com.defimon.multichain.service.BlockchainSyncService;
import com.defimon.multichain.service.DataProcessingService;
import com.defimon.multichain.service.TransactionService;
import com.defimon.multichain.service.WalletService;

/**
 * Core interface for all blockchain plugins.
 * 
 * This interface defines the contract that all blockchain plugins must implement,
 * enabling a unified approach to multi-chain operations while allowing for
 * chain-specific optimizations and implementations.
 * 
 * @param <T> The configuration type for this plugin
 */
public interface BlockchainPlugin<T extends PluginConfiguration> {
    
    /**
     * Returns the unique chain identifier for this blockchain.
     * 
     * @return the chain ID (e.g., "1" for Ethereum, "bitcoin" for Bitcoin)
     */
    String getChainId();
    
    /**
     * Returns the human-readable name of this blockchain.
     * 
     * @return the chain name (e.g., "Ethereum", "Bitcoin", "Polygon")
     */
    String getChainName();
    
    /**
     * Returns the type of blockchain this plugin supports.
     * 
     * @return the chain type (EVM, UTXO, ACCOUNT_MODEL)
     */
    ChainType getChainType();
    
    /**
     * Returns the version of this plugin.
     * 
     * @return the plugin version
     */
    String getVersion();
    
    /**
     * Initializes the plugin with the provided context and configuration.
     * This method is called once when the plugin is loaded.
     * 
     * @param context the plugin context with shared resources
     * @param config the plugin-specific configuration
     */
    void initialize(PluginContext context, T config);
    
    /**
     * Starts the plugin and begins processing.
     * This method is called after initialization.
     */
    void start();
    
    /**
     * Stops the plugin and cleans up resources.
     * This method is called when the plugin is being unloaded.
     */
    void stop();
    
    /**
     * Checks if the plugin is healthy and functioning correctly.
     * 
     * @return true if the plugin is healthy, false otherwise
     */
    boolean isHealthy();
    
    /**
     * Returns the blockchain synchronization service for this chain.
     * 
     * @return the sync service instance
     */
    BlockchainSyncService getSyncService();
    
    /**
     * Returns the transaction service for this chain.
     * 
     * @return the transaction service instance
     */
    TransactionService getTransactionService();
    
    /**
     * Returns the wallet service for this chain.
     * 
     * @return the wallet service instance
     */
    WalletService getWalletService();
    
    /**
     * Returns the data processing service for this chain.
     * 
     * @return the data processing service instance
     */
    DataProcessingService getDataProcessingService();
    
    /**
     * Returns the plugin configuration.
     * 
     * @return the configuration instance
     */
    T getConfiguration();
}
