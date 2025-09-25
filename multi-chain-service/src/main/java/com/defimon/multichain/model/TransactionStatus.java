package com.defimon.multichain.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Represents a blockchain transaction.
 * 
 * This record encapsulates all the information about a blockchain transaction,
 * including sender/receiver information, value, gas details, and metadata
 * that's common across different blockchain networks.
 * 
 * @param hash the transaction hash
 * @param blockNumber the block number where this transaction was included
 * @param blockHash the block hash
 * @param transactionIndex the index of the transaction within the block
 * @param from the sender address
 * @param to the recipient address (null for contract creation)
 * @param value the transaction value in native currency
 * @param gas the gas limit for the transaction
 * @param gasPrice the gas price
 * @param gasUsed the actual gas used (for confirmed transactions)
 * @param nonce the transaction nonce
 * @param input the transaction input data
 * @param output the transaction output data (for UTXO chains)
 * @param status the transaction status
 * @param timestamp the transaction timestamp
 * @param chainId the chain ID
 * @param extraData additional transaction data
 */
public record Transaction(
    @JsonProperty("hash") String hash,
    @JsonProperty("blockNumber") Long blockNumber,
    @JsonProperty("blockHash") String blockHash,
    @JsonProperty("transactionIndex") Integer transactionIndex,
    @JsonProperty("from") String from,
    @JsonProperty("to") String to,
    @JsonProperty("value") BigDecimal value,
    @JsonProperty("gas") BigDecimal gas,
    @JsonProperty("gasPrice") BigDecimal gasPrice,
    @JsonProperty("gasUsed") BigDecimal gasUsed,
    @JsonProperty("nonce") Long nonce,
    @JsonProperty("input") String input,
    @JsonProperty("output") String output,
    @JsonProperty("status") TransactionStatus status,
    @JsonProperty("timestamp") LocalDateTime timestamp,
    @JsonProperty("chainId") String chainId,
    @JsonProperty("extraData") Map<String, Object> extraData
) {
    
    /**
     * Enumeration of transaction statuses.
     */
    public enum TransactionStatus {
        PENDING("Pending"),
        CONFIRMED("Confirmed"),
        FAILED("Failed"),
        DROPPED("Dropped");
        
        private final String displayName;
        
        TransactionStatus(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    /**
     * Creates a new Transaction with default values for optional fields.
     * 
     * @param hash the transaction hash
     * @param from the sender address
     * @param to the recipient address
     * @param value the transaction value
     * @param chainId the chain ID
     * @return a new Transaction instance
     */
    public static Transaction create(String hash, String from, String to, BigDecimal value, String chainId) {
        return new Transaction(
            hash, null, null, null, from, to, value,
            null, null, null, null, null, null,
            TransactionStatus.PENDING, null, chainId, Map.of()
        );
    }
    
    /**
     * Checks if this transaction is a contract creation transaction.
     * 
     * @return true if the transaction creates a contract
     */
    public boolean isContractCreation() {
        return to == null || to.isEmpty();
    }
    
    /**
     * Checks if this transaction is a simple value transfer.
     * 
     * @return true if the transaction is a simple transfer
     */
    public boolean isSimpleTransfer() {
        return input == null || input.isEmpty() || "0x".equals(input);
    }
    
    /**
     * Gets the transaction fee (gas used * gas price).
     * 
     * @return the transaction fee
     */
    public BigDecimal getFee() {
        if (gasUsed == null || gasPrice == null) {
            return null;
        }
        return gasUsed.multiply(gasPrice);
    }
    
    /**
     * Gets the transaction hash with 0x prefix if not already present.
     * 
     * @return the transaction hash with 0x prefix
     */
    public String getHashWithPrefix() {
        if (hash.startsWith("0x")) {
            return hash;
        }
        return "0x" + hash;
    }
    
    /**
     * Checks if this transaction is confirmed.
     * 
     * @return true if the transaction status is CONFIRMED
     */
    public boolean isConfirmed() {
        return status == TransactionStatus.CONFIRMED;
    }
    
    /**
     * Checks if this transaction is pending.
     * 
     * @return true if the transaction status is PENDING
     */
    public boolean isPending() {
        return status == TransactionStatus.PENDING;
    }
    
    /**
     * Checks if this transaction failed.
     * 
     * @return true if the transaction status is FAILED
     */
    public boolean isFailed() {
        return status == TransactionStatus.FAILED;
    }
}
