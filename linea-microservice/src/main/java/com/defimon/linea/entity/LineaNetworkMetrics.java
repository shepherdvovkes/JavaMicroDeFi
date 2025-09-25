package com.defimon.linea.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import java.math.BigInteger;
import java.time.LocalDateTime;

/**
 * Document representing Linea network metrics.
 * 
 * This document stores comprehensive network metrics including
 * block statistics, transaction counts, and gas information.
 */
@Document(collection = "linea_network_metrics")
public class LineaNetworkMetrics {
    
    @Id
    @Field("timestamp")
    private LocalDateTime timestamp;
    
    @Field("block_number")
    private Long blockNumber;
    
    @Field("total_blocks")
    private Long totalBlocks;
    
    @Field("total_transactions")
    private Long totalTransactions;
    
    @Field("total_accounts")
    private Long totalAccounts;
    
    @Field("total_contracts")
    private Long totalContracts;
    
    @Field("average_block_time")
    private Double averageBlockTime;
    
    @Field("average_gas_used")
    private BigInteger averageGasUsed;
    
    @Field("average_gas_price")
    private BigInteger averageGasPrice;
    
    @Field("total_gas_used")
    private BigInteger totalGasUsed;
    
    @Field("network_hash_rate")
    private BigInteger networkHashRate;
    
    @Field("difficulty")
    private BigInteger difficulty;
    
    @Field("total_difficulty")
    private BigInteger totalDifficulty;
    
    @Field("active_addresses_24h")
    private Long activeAddresses24h;
    
    @Field("new_addresses_24h")
    private Long newAddresses24h;
    
    @Field("transaction_volume_24h")
    private BigInteger transactionVolume24h;
    
    @Field("gas_used_24h")
    private BigInteger gasUsed24h;
    
    @Field("blocks_24h")
    private Long blocks24h;
    
    @Field("transactions_24h")
    private Long transactions24h;
    
    @Field("created_at")
    private LocalDateTime createdAt;
    
    @Field("updated_at")
    private LocalDateTime updatedAt;
    
    // Constructors
    public LineaNetworkMetrics() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public LineaNetworkMetrics(LocalDateTime timestamp, Long blockNumber) {
        this();
        this.timestamp = timestamp;
        this.blockNumber = blockNumber;
    }
    
    // Getters and Setters
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public Long getBlockNumber() {
        return blockNumber;
    }
    
    public void setBlockNumber(Long blockNumber) {
        this.blockNumber = blockNumber;
    }
    
    public Long getTotalBlocks() {
        return totalBlocks;
    }
    
    public void setTotalBlocks(Long totalBlocks) {
        this.totalBlocks = totalBlocks;
    }
    
    public Long getTotalTransactions() {
        return totalTransactions;
    }
    
    public void setTotalTransactions(Long totalTransactions) {
        this.totalTransactions = totalTransactions;
    }
    
    public Long getTotalAccounts() {
        return totalAccounts;
    }
    
    public void setTotalAccounts(Long totalAccounts) {
        this.totalAccounts = totalAccounts;
    }
    
    public Long getTotalContracts() {
        return totalContracts;
    }
    
    public void setTotalContracts(Long totalContracts) {
        this.totalContracts = totalContracts;
    }
    
    public Double getAverageBlockTime() {
        return averageBlockTime;
    }
    
    public void setAverageBlockTime(Double averageBlockTime) {
        this.averageBlockTime = averageBlockTime;
    }
    
    public BigInteger getAverageGasUsed() {
        return averageGasUsed;
    }
    
    public void setAverageGasUsed(BigInteger averageGasUsed) {
        this.averageGasUsed = averageGasUsed;
    }
    
    public BigInteger getAverageGasPrice() {
        return averageGasPrice;
    }
    
    public void setAverageGasPrice(BigInteger averageGasPrice) {
        this.averageGasPrice = averageGasPrice;
    }
    
    public BigInteger getTotalGasUsed() {
        return totalGasUsed;
    }
    
    public void setTotalGasUsed(BigInteger totalGasUsed) {
        this.totalGasUsed = totalGasUsed;
    }
    
    public BigInteger getNetworkHashRate() {
        return networkHashRate;
    }
    
    public void setNetworkHashRate(BigInteger networkHashRate) {
        this.networkHashRate = networkHashRate;
    }
    
    public BigInteger getDifficulty() {
        return difficulty;
    }
    
    public void setDifficulty(BigInteger difficulty) {
        this.difficulty = difficulty;
    }
    
    public BigInteger getTotalDifficulty() {
        return totalDifficulty;
    }
    
    public void setTotalDifficulty(BigInteger totalDifficulty) {
        this.totalDifficulty = totalDifficulty;
    }
    
    public Long getActiveAddresses24h() {
        return activeAddresses24h;
    }
    
    public void setActiveAddresses24h(Long activeAddresses24h) {
        this.activeAddresses24h = activeAddresses24h;
    }
    
    public Long getNewAddresses24h() {
        return newAddresses24h;
    }
    
    public void setNewAddresses24h(Long newAddresses24h) {
        this.newAddresses24h = newAddresses24h;
    }
    
    public BigInteger getTransactionVolume24h() {
        return transactionVolume24h;
    }
    
    public void setTransactionVolume24h(BigInteger transactionVolume24h) {
        this.transactionVolume24h = transactionVolume24h;
    }
    
    public BigInteger getGasUsed24h() {
        return gasUsed24h;
    }
    
    public void setGasUsed24h(BigInteger gasUsed24h) {
        this.gasUsed24h = gasUsed24h;
    }
    
    public Long getBlocks24h() {
        return blocks24h;
    }
    
    public void setBlocks24h(Long blocks24h) {
        this.blocks24h = blocks24h;
    }
    
    public Long getTransactions24h() {
        return transactions24h;
    }
    
    public void setTransactions24h(Long transactions24h) {
        this.transactions24h = transactions24h;
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
        return "LineaNetworkMetrics{" +
                "timestamp=" + timestamp +
                ", blockNumber=" + blockNumber +
                ", totalBlocks=" + totalBlocks +
                ", totalTransactions=" + totalTransactions +
                ", totalAccounts=" + totalAccounts +
                ", totalContracts=" + totalContracts +
                ", averageBlockTime=" + averageBlockTime +
                ", averageGasUsed=" + averageGasUsed +
                ", averageGasPrice=" + averageGasPrice +
                ", totalGasUsed=" + totalGasUsed +
                ", networkHashRate=" + networkHashRate +
                ", difficulty=" + difficulty +
                ", totalDifficulty=" + totalDifficulty +
                ", activeAddresses24h=" + activeAddresses24h +
                ", newAddresses24h=" + newAddresses24h +
                ", transactionVolume24h=" + transactionVolume24h +
                ", gasUsed24h=" + gasUsed24h +
                ", blocks24h=" + blocks24h +
                ", transactions24h=" + transactions24h +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        LineaNetworkMetrics that = (LineaNetworkMetrics) o;
        return timestamp != null ? timestamp.equals(that.timestamp) : that.timestamp == null;
    }
    
    @Override
    public int hashCode() {
        return timestamp != null ? timestamp.hashCode() : 0;
    }
}