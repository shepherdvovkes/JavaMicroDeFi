package com.defimon.multichain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Represents a request to send a transaction.
 * 
 * This record encapsulates all the information needed to send a transaction
 * on any supported blockchain network, with validation and type safety.
 * 
 * @param to the recipient address
 * @param value the transaction value in native currency
 * @param gas the gas limit for the transaction
 * @param gasPrice the gas price
 * @param maxFeePerGas the maximum fee per gas (EIP-1559)
 * @param maxPriorityFeePerGas the maximum priority fee per gas (EIP-1559)
 * @param nonce the transaction nonce
 * @param data the transaction input data
 * @param chainId the chain ID
 * @param privateKey the private key for signing (should be encrypted in production)
 * @param extraData additional transaction data
 */
public record TransactionRequest(
    @JsonProperty("to") @NotBlank(message = "Recipient address is required") String to,
    @JsonProperty("value") @NotNull(message = "Transaction value is required") BigDecimal value,
    @JsonProperty("gas") BigDecimal gas,
    @JsonProperty("gasPrice") BigDecimal gasPrice,
    @JsonProperty("maxFeePerGas") BigDecimal maxFeePerGas,
    @JsonProperty("maxPriorityFeePerGas") BigDecimal maxPriorityFeePerGas,
    @JsonProperty("nonce") Long nonce,
    @JsonProperty("data") String data,
    @JsonProperty("chainId") String chainId,
    @JsonProperty("privateKey") @NotBlank(message = "Private key is required") String privateKey,
    @JsonProperty("extraData") Map<String, Object> extraData
) {
    
    /**
     * Creates a new TransactionRequest with default values for optional fields.
     * 
     * @param to the recipient address
     * @param value the transaction value
     * @param privateKey the private key
     * @return a new TransactionRequest instance
     */
    public static TransactionRequest create(String to, BigDecimal value, String privateKey) {
        return new TransactionRequest(
            to, value, null, null, null, null, null, null, null, privateKey, Map.of()
        );
    }
    
    /**
     * Creates a new TransactionRequest for a simple transfer.
     * 
     * @param to the recipient address
     * @param value the transaction value
     * @param gasPrice the gas price
     * @param privateKey the private key
     * @return a new TransactionRequest instance
     */
    public static TransactionRequest createTransfer(String to, BigDecimal value, BigDecimal gasPrice, String privateKey) {
        return new TransactionRequest(
            to, value, null, gasPrice, null, null, null, "0x", null, privateKey, Map.of()
        );
    }
    
    /**
     * Creates a new TransactionRequest for a contract interaction.
     * 
     * @param to the contract address
     * @param value the transaction value
     * @param data the contract method call data
     * @param gasPrice the gas price
     * @param privateKey the private key
     * @return a new TransactionRequest instance
     */
    public static TransactionRequest createContractCall(String to, BigDecimal value, String data, BigDecimal gasPrice, String privateKey) {
        return new TransactionRequest(
            to, value, null, gasPrice, null, null, null, data, null, privateKey, Map.of()
        );
    }
    
    /**
     * Checks if this transaction uses EIP-1559 fee structure.
     * 
     * @return true if maxFeePerGas and maxPriorityFeePerGas are set
     */
    public boolean usesEIP1559() {
        return maxFeePerGas != null && maxPriorityFeePerGas != null;
    }
    
    /**
     * Checks if this transaction is a simple value transfer.
     * 
     * @return true if the transaction has no data or empty data
     */
    public boolean isSimpleTransfer() {
        return data == null || data.isEmpty() || "0x".equals(data);
    }
    
    /**
     * Gets the effective gas price for this transaction.
     * 
     * @return the gas price to use (either gasPrice or maxFeePerGas)
     */
    public BigDecimal getEffectiveGasPrice() {
        return usesEIP1559() ? maxFeePerGas : gasPrice;
    }
    
    /**
     * Validates that the transaction request has the required fields for the chain type.
     * 
     * @param chainType the type of blockchain
     * @return true if the request is valid for the chain type
     */
    public boolean isValidForChainType(ChainType chainType) {
        switch (chainType) {
            case EVM -> {
                // EVM chains require gas and chain ID
                return gas != null && chainId != null;
            }
            case UTXO -> {
                // UTXO chains don't use gas or chain ID
                return gas == null && chainId == null;
            }
            case ACCOUNT_MODEL -> {
                // Account-based chains require chain ID but may not use gas
                return chainId != null;
            }
            default -> {
                return true;
            }
        }
    }
}
