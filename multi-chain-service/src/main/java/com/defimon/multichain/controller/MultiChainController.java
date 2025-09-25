package com.defimon.multichain.controller;

import com.defimon.multichain.manager.PluginManager;
import com.defimon.multichain.model.Block;
import com.defimon.multichain.model.Transaction;
import com.defimon.multichain.model.TransactionRequest;
import com.defimon.multichain.model.TransactionResult;
import com.defimon.multichain.plugin.BlockchainPlugin;
import com.defimon.multichain.service.BlockchainSyncService;
import com.defimon.multichain.service.TransactionService;
import com.defimon.multichain.service.WalletService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Unified REST API controller for multi-chain operations.
 * 
 * This controller provides a single API endpoint for all blockchain operations
 * across different chains, abstracting away chain-specific differences and
 * providing a consistent interface for clients.
 * 
 * Features:
 * - Unified API for all supported chains
 * - Virtual threads for high concurrency
 * - Comprehensive metrics and monitoring
 * - Error handling and validation
 * - Async operations with reactive streams
 */
@RestController
@RequestMapping("/api/v1/chains")
@CrossOrigin(origins = "*")
public class MultiChainController {
    
    private static final Logger logger = LoggerFactory.getLogger(MultiChainController.class);
    
    private final PluginManager pluginManager;
    private final MeterRegistry meterRegistry;
    
    // Metrics
    private final Counter apiRequestCounter;
    private final Counter apiErrorCounter;
    private final Timer apiResponseTimer;
    
    @Autowired
    public MultiChainController(PluginManager pluginManager, MeterRegistry meterRegistry) {
        this.pluginManager = pluginManager;
        this.meterRegistry = meterRegistry;
        
        // Initialize metrics
        this.apiRequestCounter = Counter.builder("multichain.api.requests")
                .description("Number of API requests")
                .register(meterRegistry);
        
        this.apiErrorCounter = Counter.builder("multichain.api.errors")
                .description("Number of API errors")
                .register(meterRegistry);
        
        this.apiResponseTimer = Timer.builder("multichain.api.response.time")
                .description("API response time")
                .register(meterRegistry);
    }
    
    /**
     * Gets information about all available chains.
     * 
     * @return list of chain information
     */
    @GetMapping("/info")
    public ResponseEntity<List<PluginManager.PluginInfo>> getChainInfo() {
        return Timer.Sample.start(meterRegistry)
                .stop(apiResponseTimer)
                .recordCallable(() -> {
                    apiRequestCounter.increment("endpoint", "info");
                    
                    List<PluginManager.PluginInfo> chainInfo = pluginManager.getPluginInfo();
                    
                    logger.debug("Retrieved chain info for {} chains", chainInfo.size());
                    return ResponseEntity.ok(chainInfo);
                });
    }
    
    /**
     * Gets the latest block for a specific chain.
     * 
     * @param chainId the chain ID
     * @return the latest block
     */
    @GetMapping("/{chainId}/blocks/latest")
    public CompletableFuture<ResponseEntity<Block>> getLatestBlock(@PathVariable String chainId) {
        return CompletableFuture.supplyAsync(() -> {
            return Timer.Sample.start(meterRegistry)
                    .stop(apiResponseTimer)
                    .recordCallable(() -> {
                        apiRequestCounter.increment("endpoint", "latest_block", "chain", chainId);
                        
                        try {
                            BlockchainPlugin<?> plugin = pluginManager.getPlugin(chainId);
                            if (plugin == null) {
                                apiErrorCounter.increment("endpoint", "latest_block", "chain", chainId, "error", "plugin_not_found");
                                return ResponseEntity.notFound().build();
                            }
                            
                            if (!plugin.getConfiguration().getEnabled()) {
                                apiErrorCounter.increment("endpoint", "latest_block", "chain", chainId, "error", "plugin_disabled");
                                return ResponseEntity.status(503).body(null);
                            }
                            
                            Block latestBlock = plugin.getSyncService().getLatestBlock().block();
                            
                            logger.debug("Retrieved latest block {} for chain {}", latestBlock.number(), chainId);
                            return ResponseEntity.ok(latestBlock);
                            
                        } catch (Exception e) {
                            logger.error("Failed to get latest block for chain: {}", chainId, e);
                            apiErrorCounter.increment("endpoint", "latest_block", "chain", chainId, "error", "exception");
                            return ResponseEntity.status(500).body(null);
                        }
                    });
        });
    }
    
    /**
     * Gets a specific block by number for a chain.
     * 
     * @param chainId the chain ID
     * @param blockNumber the block number
     * @return the block
     */
    @GetMapping("/{chainId}/blocks/{blockNumber}")
    public CompletableFuture<ResponseEntity<Block>> getBlockByNumber(
            @PathVariable String chainId,
            @PathVariable Long blockNumber) {
        return CompletableFuture.supplyAsync(() -> {
            return Timer.Sample.start(meterRegistry)
                    .stop(apiResponseTimer)
                    .recordCallable(() -> {
                        apiRequestCounter.increment("endpoint", "block_by_number", "chain", chainId);
                        
                        try {
                            BlockchainPlugin<?> plugin = pluginManager.getPlugin(chainId);
                            if (plugin == null) {
                                apiErrorCounter.increment("endpoint", "block_by_number", "chain", chainId, "error", "plugin_not_found");
                                return ResponseEntity.notFound().build();
                            }
                            
                            if (!plugin.getConfiguration().getEnabled()) {
                                apiErrorCounter.increment("endpoint", "block_by_number", "chain", chainId, "error", "plugin_disabled");
                                return ResponseEntity.status(503).body(null);
                            }
                            
                            Block block = plugin.getSyncService().getBlockByNumber(blockNumber).block();
                            
                            logger.debug("Retrieved block {} for chain {}", blockNumber, chainId);
                            return ResponseEntity.ok(block);
                            
                        } catch (Exception e) {
                            logger.error("Failed to get block {} for chain: {}", blockNumber, chainId, e);
                            apiErrorCounter.increment("endpoint", "block_by_number", "chain", chainId, "error", "exception");
                            return ResponseEntity.status(500).body(null);
                        }
                    });
        });
    }
    
    /**
     * Gets a specific block by hash for a chain.
     * 
     * @param chainId the chain ID
     * @param blockHash the block hash
     * @return the block
     */
    @GetMapping("/{chainId}/blocks/hash/{blockHash}")
    public CompletableFuture<ResponseEntity<Block>> getBlockByHash(
            @PathVariable String chainId,
            @PathVariable String blockHash) {
        return CompletableFuture.supplyAsync(() -> {
            return Timer.Sample.start(meterRegistry)
                    .stop(apiResponseTimer)
                    .recordCallable(() -> {
                        apiRequestCounter.increment("endpoint", "block_by_hash", "chain", chainId);
                        
                        try {
                            BlockchainPlugin<?> plugin = pluginManager.getPlugin(chainId);
                            if (plugin == null) {
                                apiErrorCounter.increment("endpoint", "block_by_hash", "chain", chainId, "error", "plugin_not_found");
                                return ResponseEntity.notFound().build();
                            }
                            
                            if (!plugin.getConfiguration().getEnabled()) {
                                apiErrorCounter.increment("endpoint", "block_by_hash", "chain", chainId, "error", "plugin_disabled");
                                return ResponseEntity.status(503).body(null);
                            }
                            
                            Block block = plugin.getSyncService().getBlockByHash(blockHash).block();
                            
                            logger.debug("Retrieved block {} for chain {}", blockHash, chainId);
                            return ResponseEntity.ok(block);
                            
                        } catch (Exception e) {
                            logger.error("Failed to get block {} for chain: {}", blockHash, chainId, e);
                            apiErrorCounter.increment("endpoint", "block_by_hash", "chain", chainId, "error", "exception");
                            return ResponseEntity.status(500).body(null);
                        }
                    });
        });
    }
    
    /**
     * Gets a transaction by hash for a chain.
     * 
     * @param chainId the chain ID
     * @param txHash the transaction hash
     * @return the transaction
     */
    @GetMapping("/{chainId}/transactions/{txHash}")
    public CompletableFuture<ResponseEntity<Transaction>> getTransaction(
            @PathVariable String chainId,
            @PathVariable String txHash) {
        return CompletableFuture.supplyAsync(() -> {
            return Timer.Sample.start(meterRegistry)
                    .stop(apiResponseTimer)
                    .recordCallable(() -> {
                        apiRequestCounter.increment("endpoint", "transaction", "chain", chainId);
                        
                        try {
                            BlockchainPlugin<?> plugin = pluginManager.getPlugin(chainId);
                            if (plugin == null) {
                                apiErrorCounter.increment("endpoint", "transaction", "chain", chainId, "error", "plugin_not_found");
                                return ResponseEntity.notFound().build();
                            }
                            
                            if (!plugin.getConfiguration().getEnabled()) {
                                apiErrorCounter.increment("endpoint", "transaction", "chain", chainId, "error", "plugin_disabled");
                                return ResponseEntity.status(503).body(null);
                            }
                            
                            Transaction transaction = plugin.getTransactionService().getTransaction(txHash).block();
                            
                            logger.debug("Retrieved transaction {} for chain {}", txHash, chainId);
                            return ResponseEntity.ok(transaction);
                            
                        } catch (Exception e) {
                            logger.error("Failed to get transaction {} for chain: {}", txHash, chainId, e);
                            apiErrorCounter.increment("endpoint", "transaction", "chain", chainId, "error", "exception");
                            return ResponseEntity.status(500).body(null);
                        }
                    });
        });
    }
    
    /**
     * Sends a transaction for a specific chain.
     * 
     * @param chainId the chain ID
     * @param request the transaction request
     * @return the transaction result
     */
    @PostMapping("/{chainId}/transactions")
    public CompletableFuture<ResponseEntity<TransactionResult>> sendTransaction(
            @PathVariable String chainId,
            @RequestBody TransactionRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            return Timer.Sample.start(meterRegistry)
                    .stop(apiResponseTimer)
                    .recordCallable(() -> {
                        apiRequestCounter.increment("endpoint", "send_transaction", "chain", chainId);
                        
                        try {
                            BlockchainPlugin<?> plugin = pluginManager.getPlugin(chainId);
                            if (plugin == null) {
                                apiErrorCounter.increment("endpoint", "send_transaction", "chain", chainId, "error", "plugin_not_found");
                                return ResponseEntity.notFound().build();
                            }
                            
                            if (!plugin.getConfiguration().getEnabled()) {
                                apiErrorCounter.increment("endpoint", "send_transaction", "chain", chainId, "error", "plugin_disabled");
                                return ResponseEntity.status(503).body(null);
                            }
                            
                            // Set chain ID in request
                            request = new TransactionRequest(
                                request.to(), request.value(), request.gas(), request.gasPrice(),
                                request.maxFeePerGas(), request.maxPriorityFeePerGas(), request.nonce(),
                                request.data(), chainId, request.privateKey(), request.extraData()
                            );
                            
                            TransactionResult result = plugin.getTransactionService().sendTransaction(request).block();
                            
                            logger.info("Sent transaction {} for chain {}", result.transactionHash(), chainId);
                            return ResponseEntity.ok(result);
                            
                        } catch (Exception e) {
                            logger.error("Failed to send transaction for chain: {}", chainId, e);
                            apiErrorCounter.increment("endpoint", "send_transaction", "chain", chainId, "error", "exception");
                            return ResponseEntity.status(500).body(null);
                        }
                    });
        });
    }
    
    /**
     * Gets the balance of an address for a specific chain.
     * 
     * @param chainId the chain ID
     * @param address the wallet address
     * @return the balance
     */
    @GetMapping("/{chainId}/addresses/{address}/balance")
    public CompletableFuture<ResponseEntity<BigDecimal>> getBalance(
            @PathVariable String chainId,
            @PathVariable String address) {
        return CompletableFuture.supplyAsync(() -> {
            return Timer.Sample.start(meterRegistry)
                    .stop(apiResponseTimer)
                    .recordCallable(() -> {
                        apiRequestCounter.increment("endpoint", "balance", "chain", chainId);
                        
                        try {
                            BlockchainPlugin<?> plugin = pluginManager.getPlugin(chainId);
                            if (plugin == null) {
                                apiErrorCounter.increment("endpoint", "balance", "chain", chainId, "error", "plugin_not_found");
                                return ResponseEntity.notFound().build();
                            }
                            
                            if (!plugin.getConfiguration().getEnabled()) {
                                apiErrorCounter.increment("endpoint", "balance", "chain", chainId, "error", "plugin_disabled");
                                return ResponseEntity.status(503).body(null);
                            }
                            
                            BigDecimal balance = plugin.getWalletService().getBalance(address).block();
                            
                            logger.debug("Retrieved balance {} for address {} on chain {}", balance, address, chainId);
                            return ResponseEntity.ok(balance);
                            
                        } catch (Exception e) {
                            logger.error("Failed to get balance for address {} on chain: {}", address, chainId, e);
                            apiErrorCounter.increment("endpoint", "balance", "chain", chainId, "error", "exception");
                            return ResponseEntity.status(500).body(null);
                        }
                    });
        });
    }
    
    /**
     * Gets the synchronization status for a specific chain.
     * 
     * @param chainId the chain ID
     * @return the sync status
     */
    @GetMapping("/{chainId}/sync/status")
    public CompletableFuture<ResponseEntity<BlockchainSyncService.SyncStatus>> getSyncStatus(
            @PathVariable String chainId) {
        return CompletableFuture.supplyAsync(() -> {
            return Timer.Sample.start(meterRegistry)
                    .stop(apiResponseTimer)
                    .recordCallable(() -> {
                        apiRequestCounter.increment("endpoint", "sync_status", "chain", chainId);
                        
                        try {
                            BlockchainPlugin<?> plugin = pluginManager.getPlugin(chainId);
                            if (plugin == null) {
                                apiErrorCounter.increment("endpoint", "sync_status", "chain", chainId, "error", "plugin_not_found");
                                return ResponseEntity.notFound().build();
                            }
                            
                            if (!plugin.getConfiguration().getEnabled()) {
                                apiErrorCounter.increment("endpoint", "sync_status", "chain", chainId, "error", "plugin_disabled");
                                return ResponseEntity.status(503).body(null);
                            }
                            
                            BlockchainSyncService.SyncStatus status = plugin.getSyncService().getSyncStatus().block();
                            
                            logger.debug("Retrieved sync status for chain {}", chainId);
                            return ResponseEntity.ok(status);
                            
                        } catch (Exception e) {
                            logger.error("Failed to get sync status for chain: {}", chainId, e);
                            apiErrorCounter.increment("endpoint", "sync_status", "chain", chainId, "error", "exception");
                            return ResponseEntity.status(500).body(null);
                        }
                    });
        });
    }
    
    /**
     * Enables a plugin for a specific chain.
     * 
     * @param chainId the chain ID
     * @return success response
     */
    @PostMapping("/{chainId}/enable")
    public ResponseEntity<String> enablePlugin(@PathVariable String chainId) {
        return Timer.Sample.start(meterRegistry)
                .stop(apiResponseTimer)
                .recordCallable(() -> {
                    apiRequestCounter.increment("endpoint", "enable_plugin", "chain", chainId);
                    
                    try {
                        pluginManager.enablePlugin(chainId);
                        
                        logger.info("Enabled plugin for chain: {}", chainId);
                        return ResponseEntity.ok("Plugin enabled successfully");
                        
                    } catch (Exception e) {
                        logger.error("Failed to enable plugin for chain: {}", chainId, e);
                        apiErrorCounter.increment("endpoint", "enable_plugin", "chain", chainId, "error", "exception");
                        return ResponseEntity.status(500).body("Failed to enable plugin: " + e.getMessage());
                    }
                });
    }
    
    /**
     * Disables a plugin for a specific chain.
     * 
     * @param chainId the chain ID
     * @return success response
     */
    @PostMapping("/{chainId}/disable")
    public ResponseEntity<String> disablePlugin(@PathVariable String chainId) {
        return Timer.Sample.start(meterRegistry)
                .stop(apiResponseTimer)
                .recordCallable(() -> {
                    apiRequestCounter.increment("endpoint", "disable_plugin", "chain", chainId);
                    
                    try {
                        pluginManager.disablePlugin(chainId);
                        
                        logger.info("Disabled plugin for chain: {}", chainId);
                        return ResponseEntity.ok("Plugin disabled successfully");
                        
                    } catch (Exception e) {
                        logger.error("Failed to disable plugin for chain: {}", chainId, e);
                        apiErrorCounter.increment("endpoint", "disable_plugin", "chain", chainId, "error", "exception");
                        return ResponseEntity.status(500).body("Failed to disable plugin: " + e.getMessage());
                    }
                });
    }
    
    /**
     * Gets the health status of all plugins.
     * 
     * @return health status map
     */
    @GetMapping("/health")
    public ResponseEntity<Object> getHealthStatus() {
        return Timer.Sample.start(meterRegistry)
                .stop(apiResponseTimer)
                .recordCallable(() -> {
                    apiRequestCounter.increment("endpoint", "health");
                    
                    try {
                        var healthStatus = pluginManager.getPluginHealthStatus();
                        var pluginInfo = pluginManager.getPluginInfo();
                        
                        var response = new java.util.HashMap<String, Object>();
                        response.put("overall", pluginManager.getEnabledPlugins().stream().allMatch(plugin -> plugin.isHealthy()));
                        response.put("plugins", healthStatus);
                        response.put("info", pluginInfo);
                        
                        logger.debug("Retrieved health status for {} plugins", healthStatus.size());
                        return ResponseEntity.ok(response);
                        
                    } catch (Exception e) {
                        logger.error("Failed to get health status", e);
                        apiErrorCounter.increment("endpoint", "health", "error", "exception");
                        return ResponseEntity.status(500).body("Failed to get health status: " + e.getMessage());
                    }
                });
    }
}
