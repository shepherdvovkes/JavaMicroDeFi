package com.defimon.multichain.service;

import com.defimon.multichain.model.Block;
import com.defimon.multichain.model.Transaction;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Service interface for blockchain synchronization operations.
 * 
 * This interface defines the contract for syncing blockchain data,
 * including blocks, transactions, and events. Implementations can
 * use different strategies (real-time, batch, etc.) based on chain requirements.
 */
public interface BlockchainSyncService {
    
    /**
     * Gets the latest block number from the blockchain.
     * 
     * @return the latest block number
     */
    Mono<Long> getLatestBlockNumber();
    
    /**
     * Gets a specific block by its number.
     * 
     * @param blockNumber the block number
     * @return the block data
     */
    Mono<Block> getBlockByNumber(Long blockNumber);
    
    /**
     * Gets a specific block by its hash.
     * 
     * @param blockHash the block hash
     * @return the block data
     */
    Mono<Block> getBlockByHash(String blockHash);
    
    /**
     * Gets the latest block.
     * 
     * @return the latest block
     */
    Mono<Block> getLatestBlock();
    
    /**
     * Gets a specific transaction by its hash.
     * 
     * @param transactionHash the transaction hash
     * @return the transaction data
     */
    Mono<Transaction> getTransaction(String transactionHash);
    
    /**
     * Gets all transactions in a specific block.
     * 
     * @param blockNumber the block number
     * @return list of transactions in the block
     */
    Mono<List<Transaction>> getBlockTransactions(Long blockNumber);
    
    /**
     * Gets all transactions in a specific block by hash.
     * 
     * @param blockHash the block hash
     * @return list of transactions in the block
     */
    Mono<List<Transaction>> getBlockTransactions(String blockHash);
    
    /**
     * Starts real-time synchronization of new blocks.
     * 
     * @return a flux of new blocks as they are mined
     */
    Flux<Block> syncNewBlocks();
    
    /**
     * Synchronizes a range of blocks.
     * 
     * @param fromBlock the starting block number
     * @param toBlock the ending block number (inclusive)
     * @return a flux of blocks in the range
     */
    Flux<Block> syncBlockRange(Long fromBlock, Long toBlock);
    
    /**
     * Gets the current synchronization status.
     * 
     * @return synchronization status information
     */
    Mono<SyncStatus> getSyncStatus();
    
    /**
     * Starts the synchronization process.
     */
    void startSync();
    
    /**
     * Stops the synchronization process.
     */
    void stopSync();
    
    /**
     * Checks if synchronization is currently running.
     * 
     * @return true if sync is running, false otherwise
     */
    boolean isSyncRunning();
    
    /**
     * Record for synchronization status information.
     * 
     * @param isRunning whether sync is currently running
     * @param latestSyncedBlock the latest block that has been synced
     * @param currentBlock the current block on the chain
     * @param syncProgress percentage of sync completion (0-100)
     * @param lastSyncTime timestamp of last successful sync
     * @param errorMessage any error message if sync failed
     */
    record SyncStatus(
        boolean isRunning,
        Long latestSyncedBlock,
        Long currentBlock,
        Double syncProgress,
        Long lastSyncTime,
        String errorMessage
    ) {}
}
