package com.defimon.multichain.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Represents the result of a transaction submission.
 * 
 * This record encapsulates the response from sending a transaction,
 * including the transaction hash, status, and any error information.
 * 
 * @param transactionHash the hash of the submitted transaction
 * @param status the transaction status
 * @param blockNumber the block number where the transaction was included (if confirmed)
 * @param blockHash the block hash (if confirmed)
    * @param gasUsed the actual gas used by the transaction (if confirmed)
 * @param gasPrice the gas price used
 * @param fee the total transaction fee
 * @param timestamp the timestamp when the transaction was submitted
 * @param chainId the chain ID
 * @param errorMessage error message if the transaction failed
 * @param errorCode error code if the transaction failed
 * @param extraData additional result data
 */
public record TransactionResult(
    @JsonProperty("transactionHash") String transactionHash,
    @JsonProperty("status") TransactionStatus status,
    @JsonProperty("blockNumber") Long blockNumber,
    @JsonProperty("blockHash") String blockHash,
    @JsonProperty("gasUsed") BigDecimal gasUsed,
    @JsonProperty("gasPrice") BigDecimal gasPrice,
    @JsonProperty("fee") BigDecimal fee,
    @JsonProperty("timestamp") LocalDateTime timestamp,
    @JsonProperty("chainId") String chainId,
    @JsonProperty("errorMessage") String errorMessage,
    @JsonProperty("errorCode") String errorCode,
    @JsonProperty("extraData") Map<String, Object> extraData
) {
    
    /**
     * Enumeration of transaction result statuses.
     */
    public enum TransactionStatus {
        SUBMITTED("Submitted"),
        CONFIRMED("Confirmed"),
        FAILED("Failed"),
        REJECTED("Rejected"),
        PENDING("Pending");
        
        private final String displayName;
        
        TransactionStatus(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    /**
     * Creates a successful transaction result.
     * 
     * @param transactionHash the transaction hash
     * @param chainId the chain ID
     * @return a successful TransactionResult
     */
    public static TransactionResult success(String transactionHash, String chainId) {
        return new TransactionResult(
            transactionHash, TransactionStatus.SUBMITTED, null, null,
            null, null, null, LocalDateTime.now(), chainId,
            null, null, Map.of()
        );
    }
    
    /**
     * Creates a failed transaction result.
     * 
     * @param errorMessage the error message
     * @param errorCode the error code
     * @param chainId the chain ID
     * @return a failed TransactionResult
     */
    public static TransactionResult failure(String errorMessage, String errorCode, String chainId) {
        return new TransactionResult(
            null, TransactionStatus.FAILED, null, null,
            null, null, null, LocalDateTime.now(), chainId,
            errorMessage, errorCode, Map.of()
        );
    }
    
    /**
     * Creates a confirmed transaction result.
     * 
     * @param transactionHash the transaction hash
     * @param blockNumber the block number
     * @param blockHash the block hash
     * @param gasUsed the gas used
     * @param gasPrice the gas price
     * @param chainId the chain ID
     * @return a confirmed TransactionResult
     */
    public static TransactionResult confirmed(String transactionHash, Long blockNumber, String blockHash,
                                           BigDecimal gasUsed, BigDecimal gasPrice, String chainId) {
        BigDecimal fee = gasUsed != null && gasPrice != null ? gasUsed.multiply(gasPrice) : null;
        return new TransactionResult(
            transactionHash, TransactionStatus.CONFIRMED, blockNumber, blockHash,
            gasUsed, gasPrice, fee, LocalDateTime.now(), chainId,
            null, null, Map.of()
        );
    }
    
    /**
     * Checks if the transaction was successful.
     * 
     * @return true if the transaction was submitted successfully
     */
    public boolean isSuccess() {
        return status == TransactionStatus.SUBMITTED || status == TransactionStatus.CONFIRMED;
    }
    
    /**
     * Checks if the transaction failed.
     * 
     * @return true if the transaction failed
     */
    public boolean isFailure() {
        return status == TransactionStatus.FAILED || status == TransactionStatus.REJECTED;
    }
    
    /**
     * Checks if the transaction is confirmed.
     * 
     * @return true if the transaction is confirmed
     */
    public boolean isConfirmed() {
        return status == TransactionStatus.CONFIRMED;
    }
    
    /**
     * Checks if the transaction is pending.
     * 
     * @return true if the transaction is pending
     */
    public boolean isPending() {
        return status == TransactionStatus.PENDING;
    }
    
    /**
     * Gets the transaction hash with 0x prefix if not already present.
     * 
     * @return the transaction hash with 0x prefix
     */
    public String getHashWithPrefix() {
        if (transactionHash == null) {
            return null;
        }
        if (transactionHash.startsWith("0x")) {
            return transactionHash;
        }
        return "0x" + transactionHash;
    }
}
