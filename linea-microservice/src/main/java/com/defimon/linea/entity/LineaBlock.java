package com.defimon.linea.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Document representing a Linea block.
 * 
 * This document stores comprehensive block data including gas information,
 * transaction details, and network metrics.
 */
@Document(collection = "linea_blocks")
public class LineaBlock {
    
    @Id
    @Field("block_number")
    private Long blockNumber;
    
    @Field("block_hash")
    private String blockHash;
    
    @Field("parent_hash")
    private String parentHash;
    
    @Field("timestamp")
    private LocalDateTime timestamp;
    
    @Field("gas_limit")
    private BigInteger gasLimit;
    
    @Field("gas_used")
    private BigInteger gasUsed;
    
    @Field("base_fee_per_gas")
    private BigInteger baseFeePerGas;
    
    @Field("difficulty")
    private BigInteger difficulty;
    
    @Field("size")
    private BigInteger size;
    
    @Field("transaction_count")
    private Integer transactionCount;
    
    @Field("extra_data")
    private String extraData;
    
    @Field("miner")
    private String miner;
    
    @Field("nonce")
    private String nonce;
    
    @Field("sha3_uncles")
    private String sha3Uncles;
    
    @Field("logs_bloom")
    private String logsBloom;
    
    @Field("state_root")
    private String stateRoot;
    
    @Field("receipts_root")
    private String receiptsRoot;
    
    @Field("transactions_root")
    private String transactionsRoot;
    
    @Field("total_difficulty")
    private BigInteger totalDifficulty;
    
    @Field("created_at")
    private LocalDateTime createdAt;
    
    @Field("updated_at")
    private LocalDateTime updatedAt;
    
    @Field("transactions")
    private List<LineaTransaction> transactions;
    
    // Constructors
    public LineaBlock() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public LineaBlock(Long blockNumber, String blockHash) {
        this();
        this.blockNumber = blockNumber;
        this.blockHash = blockHash;
    }
    
    // Getters and Setters
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
    
    public String getParentHash() {
        return parentHash;
    }
    
    public void setParentHash(String parentHash) {
        this.parentHash = parentHash;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public BigInteger getGasLimit() {
        return gasLimit;
    }
    
    public void setGasLimit(BigInteger gasLimit) {
        this.gasLimit = gasLimit;
    }
    
    public BigInteger getGasUsed() {
        return gasUsed;
    }
    
    public void setGasUsed(BigInteger gasUsed) {
        this.gasUsed = gasUsed;
    }
    
    public BigInteger getBaseFeePerGas() {
        return baseFeePerGas;
    }
    
    public void setBaseFeePerGas(BigInteger baseFeePerGas) {
        this.baseFeePerGas = baseFeePerGas;
    }
    
    public BigInteger getDifficulty() {
        return difficulty;
    }
    
    public void setDifficulty(BigInteger difficulty) {
        this.difficulty = difficulty;
    }
    
    public BigInteger getSize() {
        return size;
    }
    
    public void setSize(BigInteger size) {
        this.size = size;
    }
    
    public Integer getTransactionCount() {
        return transactionCount;
    }
    
    public void setTransactionCount(Integer transactionCount) {
        this.transactionCount = transactionCount;
    }
    
    public String getExtraData() {
        return extraData;
    }
    
    public void setExtraData(String extraData) {
        this.extraData = extraData;
    }
    
    public String getMiner() {
        return miner;
    }
    
    public void setMiner(String miner) {
        this.miner = miner;
    }
    
    public String getNonce() {
        return nonce;
    }
    
    public void setNonce(String nonce) {
        this.nonce = nonce;
    }
    
    public String getSha3Uncles() {
        return sha3Uncles;
    }
    
    public void setSha3Uncles(String sha3Uncles) {
        this.sha3Uncles = sha3Uncles;
    }
    
    public String getLogsBloom() {
        return logsBloom;
    }
    
    public void setLogsBloom(String logsBloom) {
        this.logsBloom = logsBloom;
    }
    
    public String getStateRoot() {
        return stateRoot;
    }
    
    public void setStateRoot(String stateRoot) {
        this.stateRoot = stateRoot;
    }
    
    public String getReceiptsRoot() {
        return receiptsRoot;
    }
    
    public void setReceiptsRoot(String receiptsRoot) {
        this.receiptsRoot = receiptsRoot;
    }
    
    public String getTransactionsRoot() {
        return transactionsRoot;
    }
    
    public void setTransactionsRoot(String transactionsRoot) {
        this.transactionsRoot = transactionsRoot;
    }
    
    public BigInteger getTotalDifficulty() {
        return totalDifficulty;
    }
    
    public void setTotalDifficulty(BigInteger totalDifficulty) {
        this.totalDifficulty = totalDifficulty;
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
    
    public List<LineaTransaction> getTransactions() {
        return transactions;
    }
    
    public void setTransactions(List<LineaTransaction> transactions) {
        this.transactions = transactions;
    }
    
    @Override
    public String toString() {
        return "LineaBlock{" +
                "blockNumber=" + blockNumber +
                ", blockHash='" + blockHash + '\'' +
                ", parentHash='" + parentHash + '\'' +
                ", timestamp=" + timestamp +
                ", gasLimit=" + gasLimit +
                ", gasUsed=" + gasUsed +
                ", baseFeePerGas=" + baseFeePerGas +
                ", difficulty=" + difficulty +
                ", size=" + size +
                ", transactionCount=" + transactionCount +
                ", miner='" + miner + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        LineaBlock that = (LineaBlock) o;
        
        if (blockNumber != null ? !blockNumber.equals(that.blockNumber) : that.blockNumber != null)
            return false;
        return blockHash != null ? blockHash.equals(that.blockHash) : that.blockHash == null;
    }
    
    @Override
    public int hashCode() {
        int result = blockNumber != null ? blockNumber.hashCode() : 0;
        result = 31 * result + (blockHash != null ? blockHash.hashCode() : 0);
        return result;
    }
}