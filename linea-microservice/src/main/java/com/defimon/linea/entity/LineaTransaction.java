package com.defimon.linea.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import java.math.BigInteger;
import java.time.LocalDateTime;

/**
 * Document representing a Linea transaction.
 * 
 * This document stores comprehensive transaction data including gas information,
 * value transfers, and contract interactions.
 */
@Document(collection = "linea_transactions")
public class LineaTransaction {
    
    @Id
    @Field("transaction_hash")
    private String transactionHash;
    
    @Field("block_number")
    private Long blockNumber;
    
    @Field("block_hash")
    private String blockHash;
    
    @Field("transaction_index")
    private Integer transactionIndex;
    
    @Field("from_address")
    private String fromAddress;
    
    @Field("to_address")
    private String toAddress;
    
    @Field("value")
    private BigInteger value;
    
    @Field("gas")
    private BigInteger gas;
    
    @Field("gas_price")
    private BigInteger gasPrice;
    
    @Field("max_fee_per_gas")
    private BigInteger maxFeePerGas;
    
    @Field("max_priority_fee_per_gas")
    private BigInteger maxPriorityFeePerGas;
    
    @Field("nonce")
    private BigInteger nonce;
    
    @Field("input_data")
    private String inputData;
    
    @Field("v")
    private String v;
    
    @Field("r")
    private String r;
    
    @Field("s")
    private String s;
    
    @Field("type")
    private Integer type;
    
    @Field("access_list")
    private String accessList;
    
    @Field("chain_id")
    private BigInteger chainId;
    
    @Field("status")
    private Integer status;
    
    @Field("gas_used")
    private BigInteger gasUsed;
    
    @Field("effective_gas_price")
    private BigInteger effectiveGasPrice;
    
    @Field("created_at")
    private LocalDateTime createdAt;
    
    @Field("updated_at")
    private LocalDateTime updatedAt;
    
    @Field("receipt")
    private LineaTransactionReceipt receipt;
    
    // Constructors
    public LineaTransaction() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public LineaTransaction(String transactionHash, Long blockNumber) {
        this();
        this.transactionHash = transactionHash;
        this.blockNumber = blockNumber;
    }
    
    // Getters and Setters
    public String getTransactionHash() {
        return transactionHash;
    }
    
    public void setTransactionHash(String transactionHash) {
        this.transactionHash = transactionHash;
    }
    
    public Long getBlockNumber() {
        return blockNumber;
    }
    
    public void setBlockNumber(Long blockNumber) {
        this.blockNumber = blockNumber;
    }
    
    public String getBlockHash() {
        return blockHash;
    }
    
    public void setBlockHash(String blockHash) {
        this.blockHash = blockHash;
    }
    
    public Integer getTransactionIndex() {
        return transactionIndex;
    }
    
    public void setTransactionIndex(Integer transactionIndex) {
        this.transactionIndex = transactionIndex;
    }
    
    public String getFromAddress() {
        return fromAddress;
    }
    
    public void setFromAddress(String fromAddress) {
        this.fromAddress = fromAddress;
    }
    
    public String getToAddress() {
        return toAddress;
    }
    
    public void setToAddress(String toAddress) {
        this.toAddress = toAddress;
    }
    
    public BigInteger getValue() {
        return value;
    }
    
    public void setValue(BigInteger value) {
        this.value = value;
    }
    
    public BigInteger getGas() {
        return gas;
    }
    
    public void setGas(BigInteger gas) {
        this.gas = gas;
    }
    
    public BigInteger getGasPrice() {
        return gasPrice;
    }
    
    public void setGasPrice(BigInteger gasPrice) {
        this.gasPrice = gasPrice;
    }
    
    public BigInteger getMaxFeePerGas() {
        return maxFeePerGas;
    }
    
    public void setMaxFeePerGas(BigInteger maxFeePerGas) {
        this.maxFeePerGas = maxFeePerGas;
    }
    
    public BigInteger getMaxPriorityFeePerGas() {
        return maxPriorityFeePerGas;
    }
    
    public void setMaxPriorityFeePerGas(BigInteger maxPriorityFeePerGas) {
        this.maxPriorityFeePerGas = maxPriorityFeePerGas;
    }
    
    public BigInteger getNonce() {
        return nonce;
    }
    
    public void setNonce(BigInteger nonce) {
        this.nonce = nonce;
    }
    
    public String getInputData() {
        return inputData;
    }
    
    public void setInputData(String inputData) {
        this.inputData = inputData;
    }
    
    public String getV() {
        return v;
    }
    
    public void setV(String v) {
        this.v = v;
    }
    
    public String getR() {
        return r;
    }
    
    public void setR(String r) {
        this.r = r;
    }
    
    public String getS() {
        return s;
    }
    
    public void setS(String s) {
        this.s = s;
    }
    
    public Integer getType() {
        return type;
    }
    
    public void setType(Integer type) {
        this.type = type;
    }
    
    public String getAccessList() {
        return accessList;
    }
    
    public void setAccessList(String accessList) {
        this.accessList = accessList;
    }
    
    public BigInteger getChainId() {
        return chainId;
    }
    
    public void setChainId(BigInteger chainId) {
        this.chainId = chainId;
    }
    
    public Integer getStatus() {
        return status;
    }
    
    public void setStatus(Integer status) {
        this.status = status;
    }
    
    public BigInteger getGasUsed() {
        return gasUsed;
    }
    
    public void setGasUsed(BigInteger gasUsed) {
        this.gasUsed = gasUsed;
    }
    
    public BigInteger getEffectiveGasPrice() {
        return effectiveGasPrice;
    }
    
    public void setEffectiveGasPrice(BigInteger effectiveGasPrice) {
        this.effectiveGasPrice = effectiveGasPrice;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public LineaTransactionReceipt getReceipt() {
        return receipt;
    }
    
    public void setReceipt(LineaTransactionReceipt receipt) {
        this.receipt = receipt;
    }
    
    @Override
    public String toString() {
        return "LineaTransaction{" +
                "transactionHash='" + transactionHash + '\'' +
                ", blockNumber=" + blockNumber +
                ", fromAddress='" + fromAddress + '\'' +
                ", toAddress='" + toAddress + '\'' +
                ", value=" + value +
                ", gas=" + gas +
                ", gasPrice=" + gasPrice +
                ", nonce=" + nonce +
                ", status=" + status +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        LineaTransaction that = (LineaTransaction) o;
        return transactionHash != null ? transactionHash.equals(that.transactionHash) : that.transactionHash == null;
    }
    
    @Override
    public int hashCode() {
        return transactionHash != null ? transactionHash.hashCode() : 0;
    }
}