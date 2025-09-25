package com.defimon.multichain.service;

import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service interface for wallet operations.
 * 
 * This interface defines the contract for wallet-related operations
 * including address generation, key management, and wallet utilities
 * across different blockchain networks.
 */
public interface WalletService {
    
    /**
     * Generates a new wallet address.
     * 
     * @return the generated wallet address
     */
    Mono<String> generateAddress();
    
    /**
     * Generates a new wallet address from a private key.
     * 
     * @param privateKey the private key
     * @return the wallet address
     */
    Mono<String> getAddressFromPrivateKey(String privateKey);
    
    /**
     * Validates if an address is valid for this blockchain.
     * 
     * @param address the address to validate
     * @return true if the address is valid
     */
    Mono<Boolean> isValidAddress(String address);
    
    /**
     * Gets the balance of an address.
     * 
     * @param address the wallet address
     * @return the balance in the native currency
     */
    Mono<BigDecimal> getBalance(String address);
    
    /**
     * Gets the nonce for an address (for account-based chains).
     * 
     * @param address the wallet address
     * @return the nonce value
     */
    Mono<Long> getNonce(String address);
    
    /**
     * Gets multiple balances for a list of addresses.
     * 
     * @param addresses the list of wallet addresses
     * @return list of balances corresponding to the addresses
     */
    Mono<List<BigDecimal>> getBalances(List<String> addresses);
    
    /**
     * Gets wallet information including balance and transaction count.
     * 
     * @param address the wallet address
     * @return wallet information
     */
    Mono<WalletInfo> getWalletInfo(String address);
    
    /**
     * Signs a message with the provided private key.
     * 
     * @param message the message to sign
     * @param privateKey the private key
     * @return the signature
     */
    Mono<String> signMessage(String message, String privateKey);
    
    /**
     * Verifies a message signature.
     * 
     * @param message the original message
     * @param signature the signature
     * @param address the address that should have signed the message
     * @return true if the signature is valid
     */
    Mono<Boolean> verifySignature(String message, String signature, String address);
    
    /**
     * Gets the transaction history for an address.
     * 
     * @param address the wallet address
     * @param limit maximum number of transactions to return
     * @param offset pagination offset
     * @return list of transactions
     */
    Mono<List<TransactionHistory>> getTransactionHistory(String address, Integer limit, Integer offset);
    
    /**
     * Record for wallet information.
     * 
     * @param address the wallet address
     * @param balance the current balance
     * @param nonce the current nonce (for account-based chains)
     * @param transactionCount total number of transactions
     * @param firstTransactionTime timestamp of first transaction
     * @param lastTransactionTime timestamp of last transaction
     */
    record WalletInfo(
        String address,
        BigDecimal balance,
        Long nonce,
        Long transactionCount,
        Long firstTransactionTime,
        Long lastTransactionTime
    ) {}
    
    /**
     * Record for transaction history entry.
     * 
     * @param transactionHash the transaction hash
     * @param blockNumber the block number
     * @param timestamp the transaction timestamp
     * @param from the sender address
     * @param to the recipient address
     * @param value the transaction value
     * @param gasUsed the gas used
     * @param gasPrice the gas price
     * @param status the transaction status
     */
    record TransactionHistory(
        String transactionHash,
        Long blockNumber,
        Long timestamp,
        String from,
        String to,
        BigDecimal value,
        BigDecimal gasUsed,
        BigDecimal gasPrice,
        String status
    ) {}
}
