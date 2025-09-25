package com.defimon.multichain.service;

import com.defimon.multichain.model.Transaction;
import com.defimon.multichain.model.TransactionRequest;
import com.defimon.multichain.model.TransactionResult;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service interface for blockchain transaction operations.
 * 
 * This interface defines the contract for transaction-related operations
 * including sending transactions, querying transaction status, and
 * handling different transaction types across various blockchain networks.
 */
public interface TransactionService {
    
    /**
     * Sends a transaction to the blockchain.
     * 
     * @param request the transaction request
     * @return the transaction result
     */
    Mono<TransactionResult> sendTransaction(TransactionRequest request);
    
    /**
     * Gets a transaction by its hash.
     * 
     * @param transactionHash the transaction hash
     * @return the transaction data
     */
    Mono<Transaction> getTransaction(String transactionHash);
    
    /**
     * Gets the transaction receipt (confirmation details).
     * 
     * @param transactionHash the transaction hash
     * @return the transaction receipt
     */
    Mono<TransactionReceipt> getTransactionReceipt(String transactionHash);
    
    /**
     * Gets the balance of an address.
     * 
     * @param address the wallet address
     * @return the balance in the native currency
     */
    Mono<BigDecimal> getBalance(String address);
    
    /**
     * Gets the token balance for a specific contract address.
     * 
     * @param address the wallet address
     * @param contractAddress the token contract address
     * @return the token balance
     */
    Mono<BigDecimal> getTokenBalance(String address, String contractAddress);
    
    /**
     * Estimates the gas/fee required for a transaction.
     * 
     * @param request the transaction request
     * @return the estimated gas/fee
     */
    Mono<BigDecimal> estimateGas(TransactionRequest request);
    
    /**
     * Gets the current gas price/fee rate.
     * 
     * @return the current gas price
     */
    Mono<BigDecimal> getGasPrice();
    
    /**
     * Gets pending transactions from the mempool.
     * 
     * @param limit maximum number of transactions to return
     * @return list of pending transactions
     */
    Mono<List<Transaction>> getPendingTransactions(Integer limit);
    
    /**
     * Gets transactions for a specific address.
     * 
     * @param address the wallet address
     * @param limit maximum number of transactions to return
     * @param offset pagination offset
     * @return list of transactions for the address
     */
    Mono<List<Transaction>> getAddressTransactions(String address, Integer limit, Integer offset);
    
    /**
     * Waits for a transaction to be confirmed.
     * 
     * @param transactionHash the transaction hash
     * @param maxWaitTime maximum time to wait in milliseconds
     * @return the confirmed transaction
     */
    Mono<Transaction> waitForConfirmation(String transactionHash, Long maxWaitTime);
    
    /**
     * Cancels a pending transaction by sending a replacement with higher gas.
     * 
     * @param originalHash the original transaction hash
     * @param newGasPrice the new gas price
     * @return the replacement transaction result
     */
    Mono<TransactionResult> cancelTransaction(String originalHash, BigDecimal newGasPrice);
    
    /**
     * Record for transaction receipt information.
     * 
     * @param transactionHash the transaction hash
     * @param blockNumber the block number where the transaction was included
     * @param blockHash the block hash
     * @param status the transaction status (success/failure)
     * @param gasUsed the actual gas used by the transaction
     * @param gasPrice the gas price used
     * @param effectiveGasPrice the effective gas price (after EIP-1559)
     * @param logs list of event logs emitted by the transaction
     */
    record TransactionReceipt(
        String transactionHash,
        Long blockNumber,
        String blockHash,
        TransactionStatus status,
        BigDecimal gasUsed,
        BigDecimal gasPrice,
        BigDecimal effectiveGasPrice,
        List<TransactionLog> logs
    ) {}
    
    /**
     * Enumeration of transaction statuses.
     */
    enum TransactionStatus {
        PENDING,
        CONFIRMED,
        FAILED,
        DROPPED
    }
    
    /**
     * Record for transaction log information.
     * 
     * @param address the contract address that emitted the log
     * @param topics list of indexed topics
     * @param data the log data
     * @param logIndex the log index within the transaction
     * @param transactionIndex the transaction index within the block
     */
    record TransactionLog(
        String address,
        List<String> topics,
        String data,
        Integer logIndex,
        Integer transactionIndex
    ) {}
}
