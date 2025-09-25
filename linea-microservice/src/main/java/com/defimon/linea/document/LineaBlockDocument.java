package com.defimon.linea.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;

/**
 * MongoDB document for Linea blocks.
 * 
 * Optimized for high-performance blockchain data collection with:
 * - Compound indexes for optimal query performance
 * - Flexible schema for blockchain data evolution
 * - Efficient storage and retrieval
 */
@Document(collection = "linea_blocks")
@CompoundIndexes({
    @CompoundIndex(name = "block_number_timestamp", def = "{'blockNumber': 1, 'timestamp': -1}"),
    @CompoundIndex(name = "timestamp_gas_used", def = "{'timestamp': -1, 'gasUsed': -1}"),
    @CompoundIndex(name = "block_number_transaction_count", def = "{'blockNumber': 1, 'transactionCount': -1}")
})
public class LineaBlockDocument {
    
    @Id
    private String id;
    
    @Indexed(unique = true)
    @Field("block_number")
    private Long blockNumber;
    
    @Indexed(unique = true)
    @Field("block_hash")
    private String blockHash;
    
    @Field("parent_hash")
    private String parentHash;
    
    @Indexed
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
    
    @Field("mix_hash")
    private String mixHash;
    
    @Field("nonce")
    private String nonce;
    
    @Field("receipts_root")
    private String receiptsRoot;
    
    @Field("sha3_uncles")
    private String sha3Uncles;
    
    @Field("state_root")
    private String stateRoot;
    
    @Field("transactions_root")
    private String transactionsRoot;
    
    @Field("withdrawals_root")
    private String withdrawalsRoot;
    
    @Field("withdrawals")
    private List<Object> withdrawals;
    
    @Field("blob_gas_used")
    private BigInteger blobGasUsed;
    
    @Field("excess_blob_gas")
    private BigInteger excessBlobGas;
    
    @Field("parent_beacon_block_root")
    private String parentBeaconBlockRoot;
    
    @Field("created_at")
    private LocalDateTime createdAt;
    
    @Field("updated_at")
    private LocalDateTime updatedAt;
    
    // Constructors
    public LineaBlockDocument() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public LineaBlockDocument(Long blockNumber, String blockHash) {
        this();
        this.blockNumber = blockNumber;
        this.blockHash = blockHash;
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
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
    
    public String getMixHash() {
        return mixHash;
    }
    
    public void setMixHash(String mixHash) {
        this.mixHash = mixHash;
    }
    
    public String getNonce() {
        return nonce;
    }
    
    public void setNonce(String nonce) {
        this.nonce = nonce;
    }
    
    public String getReceiptsRoot() {
        return receiptsRoot;
    }
    
    public void setReceiptsRoot(String receiptsRoot) {
        this.receiptsRoot = receiptsRoot;
    }
    
    public String getSha3Uncles() {
        return sha3Uncles;
    }
    
    public void setSha3Uncles(String sha3Uncles) {
        this.sha3Uncles = sha3Uncles;
    }
    
    public String getStateRoot() {
        return stateRoot;
    }
    
    public void setStateRoot(String stateRoot) {
        this.stateRoot = stateRoot;
    }
    
    public String getTransactionsRoot() {
        return transactionsRoot;
    }
    
    public void setTransactionsRoot(String transactionsRoot) {
        this.transactionsRoot = transactionsRoot;
    }
    
    public String getWithdrawalsRoot() {
        return withdrawalsRoot;
    }
    
    public void setWithdrawalsRoot(String withdrawalsRoot) {
        this.withdrawalsRoot = withdrawalsRoot;
    }
    
    public List<Object> getWithdrawals() {
        return withdrawals;
    }
    
    public void setWithdrawals(List<Object> withdrawals) {
        this.withdrawals = withdrawals;
    }
    
    public BigInteger getBlobGasUsed() {
        return blobGasUsed;
    }
    
    public void setBlobGasUsed(BigInteger blobGasUsed) {
        this.blobGasUsed = blobGasUsed;
    }
    
    public BigInteger getExcessBlobGas() {
        return excessBlobGas;
    }
    
    public void setExcessBlobGas(BigInteger excessBlobGas) {
        this.excessBlobGas = excessBlobGas;
    }
    
    public String getParentBeaconBlockRoot() {
        return parentBeaconBlockRoot;
    }
    
    public void setParentBeaconBlockRoot(String parentBeaconBlockRoot) {
        this.parentBeaconBlockRoot = parentBeaconBlockRoot;
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
    
    @Override
    public String toString() {
        return "LineaBlockDocument{" +
                "id='" + id + '\'' +
                ", blockNumber=" + blockNumber +
                ", blockHash='" + blockHash + '\'' +
                ", parentHash='" + parentHash + '\'' +
                ", timestamp=" + timestamp +
                ", gasLimit=" + gasLimit +
                ", gasUsed=" + gasUsed +
                ", baseFeePerGas=" + baseFeePerGas +
                ", difficulty=" + difficulty +
                ", size=" + size +
                ", transactionCount=" + transactionCount +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
