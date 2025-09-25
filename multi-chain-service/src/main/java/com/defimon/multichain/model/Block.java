package com.defimon.multichain.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Represents a blockchain block.
 * 
 * This record encapsulates all the information about a blockchain block,
 * including header information, transactions, and metadata that's common
 * across different blockchain networks.
 * 
 * @param number the block number
 * @param hash the block hash
 * @param parentHash the hash of the parent block
 * @param timestamp the block timestamp
 * @param nonce the block nonce
 * @param difficulty the block difficulty (for PoW chains)
 * @param gasLimit the gas limit (for EVM chains)
 * @param gasUsed the gas used (for EVM chains)
 * @param miner the address of the miner/validator
 * @param size the size of the block in bytes
 * @param transactionCount the number of transactions in the block
 * @param transactions the list of transaction hashes
 * @param extraData additional block data
 * @param chainId the chain ID
 */
public record Block(
    @JsonProperty("number") Long number,
    @JsonProperty("hash") String hash,
    @JsonProperty("parentHash") String parentHash,
    @JsonProperty("timestamp") LocalDateTime timestamp,
    @JsonProperty("nonce") String nonce,
    @JsonProperty("difficulty") BigDecimal difficulty,
    @JsonProperty("gasLimit") BigDecimal gasLimit,
    @JsonProperty("gasUsed") BigDecimal gasUsed,
    @JsonProperty("miner") String miner,
    @JsonProperty("size") Long size,
    @JsonProperty("transactionCount") Integer transactionCount,
    @JsonProperty("transactions") List<String> transactions,
    @JsonProperty("extraData") Map<String, Object> extraData,
    @JsonProperty("chainId") String chainId
) {
    
    /**
     * Creates a new Block with default values for optional fields.
     * 
     * @param number the block number
     * @param hash the block hash
     * @param parentHash the parent block hash
     * @param timestamp the block timestamp
     * @param chainId the chain ID
     * @return a new Block instance
     */
    public static Block create(Long number, String hash, String parentHash, LocalDateTime timestamp, String chainId) {
        return new Block(
            number, hash, parentHash, timestamp,
            null, null, null, null, null,
            null, null, List.of(), Map.of(), chainId
        );
    }
    
    /**
     * Checks if this block is empty (no transactions).
     * 
     * @return true if the block has no transactions
     */
    public boolean isEmpty() {
        return transactions == null || transactions.isEmpty();
    }
    
    /**
     * Gets the block number as a string.
     * 
     * @return the block number as a string
     */
    public String getBlockNumberHex() {
        return "0x" + Long.toHexString(number);
    }
    
    /**
     * Gets the gas utilization percentage (for EVM chains).
     * 
     * @return the gas utilization percentage (0-100)
     */
    public Double getGasUtilization() {
        if (gasLimit == null || gasUsed == null || gasLimit.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return gasUsed.divide(gasLimit, 4, BigDecimal.ROUND_HALF_UP)
                     .multiply(BigDecimal.valueOf(100))
                     .doubleValue();
    }
}
