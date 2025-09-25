package com.defimon.linea.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Document representing a Linea transaction receipt.
 * 
 * This document stores comprehensive transaction receipt data including
 * gas usage, logs, and execution status.
 */
@Document(collection = "linea_transaction_receipts")
public class LineaTransactionReceipt {
    
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
    
    @Field("gas_used")
    private BigInteger gasUsed;
    
    @Field("effective_gas_price")
    private BigInteger effectiveGasPrice;
    
    @Field("cumulative_gas_used")
    private BigInteger cumulativeGasUsed;
    
    @Field("status")
    private Integer status;
    
    @Field("logs_bloom")
    private String logsBloom;
    
    @Field("logs")
    private List<TransactionLog> logs;
    
    @Field("contract_address")
    private String contractAddress;
    
    @Field("root")
    private String root;
    
    @Field("type")
    private Integer type;
    
    @Field("created_at")
    private LocalDateTime createdAt;
    
    @Field("updated_at")
    private LocalDateTime updatedAt;
    
    // Constructors
    public LineaTransactionReceipt() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public LineaTransactionReceipt(String transactionHash, Long blockNumber) {
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
    
    public BigInteger getCumulativeGasUsed() {
        return cumulativeGasUsed;
    }
    
    public void setCumulativeGasUsed(BigInteger cumulativeGasUsed) {
        this.cumulativeGasUsed = cumulativeGasUsed;
    }
    
    public Integer getStatus() {
        return status;
    }
    
    public void setStatus(Integer status) {
        this.status = status;
    }
    
    public String getLogsBloom() {
        return logsBloom;
    }
    
    public void setLogsBloom(String logsBloom) {
        this.logsBloom = logsBloom;
    }
    
    public List<TransactionLog> getLogs() {
        return logs;
    }
    
    public void setLogs(List<TransactionLog> logs) {
        this.logs = logs;
    }
    
    public String getContractAddress() {
        return contractAddress;
    }
    
    public void setContractAddress(String contractAddress) {
        this.contractAddress = contractAddress;
    }
    
    public String getRoot() {
        return root;
    }
    
    public void setRoot(String root) {
        this.root = root;
    }
    
    public Integer getType() {
        return type;
    }
    
    public void setType(Integer type) {
        this.type = type;
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
        return "LineaTransactionReceipt{" +
                "transactionHash='" + transactionHash + '\'' +
                ", blockNumber=" + blockNumber +
                ", fromAddress='" + fromAddress + '\'' +
                ", toAddress='" + toAddress + '\'' +
                ", gasUsed=" + gasUsed +
                ", status=" + status +
                ", contractAddress='" + contractAddress + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        LineaTransactionReceipt that = (LineaTransactionReceipt) o;
        return transactionHash != null ? transactionHash.equals(that.transactionHash) : that.transactionHash == null;
    }
    
    @Override
    public int hashCode() {
        return transactionHash != null ? transactionHash.hashCode() : 0;
    }
    
    // Inner class for transaction logs
    public static class TransactionLog {
        @Field("address")
        private String address;
        
        @Field("topics")
        private List<String> topics;
        
        @Field("data")
        private String data;
        
        @Field("log_index")
        private Integer logIndex;
        
        @Field("transaction_index")
        private Integer transactionIndex;
        
        @Field("transaction_hash")
        private String transactionHash;
        
        @Field("block_number")
        private Long blockNumber;
        
        @Field("block_hash")
        private String blockHash;
        
        // Constructors
        public TransactionLog() {}
        
        // Getters and Setters
        public String getAddress() {
            return address;
        }
        
        public void setAddress(String address) {
            this.address = address;
        }
        
        public List<String> getTopics() {
            return topics;
        }
        
        public void setTopics(List<String> topics) {
            this.topics = topics;
        }
        
        public String getData() {
            return data;
        }
        
        public void setData(String data) {
            this.data = data;
        }
        
        public Integer getLogIndex() {
            return logIndex;
        }
        
        public void setLogIndex(Integer logIndex) {
            this.logIndex = logIndex;
        }
        
        public Integer getTransactionIndex() {
            return transactionIndex;
        }
        
        public void setTransactionIndex(Integer transactionIndex) {
            this.transactionIndex = transactionIndex;
        }
        
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
    }
}