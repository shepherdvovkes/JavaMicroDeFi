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
 * MongoDB document for Linea transactions.
 * 
 * Optimized for high-performance blockchain data collection with:
 * - Compound indexes for optimal query performance
 * - Flexible schema for transaction data
 * - Efficient storage and retrieval
 */
@Document(collection = "linea_transactions")
@CompoundIndexes({
    @CompoundIndex(name = "block_number_transaction_index", def = "{'blockNumber': 1, 'transactionIndex': 1}"),
    @CompoundIndex(name = "from_address_timestamp", def = "{'fromAddress': 1, 'timestamp': -1}"),
    @CompoundIndex(name = "to_address_timestamp", def = "{'toAddress': 1, 'timestamp': -1}"),
    @CompoundIndex(name = "timestamp_value", def = "{'timestamp': -1, 'value': -1}"),
    @CompoundIndex(name = "type_gas_price", def = "{'type': 1, 'gasPrice': -1}")
})
public class LineaTransactionDocument {
    
    @Id
    private String id;
    
    @Indexed(unique = true)
    @Field("transaction_hash")
    private String transactionHash;
    
    @Indexed
    @Field("block_number")
    private Long blockNumber;
    
    @Field("block_hash")
    private String blockHash;
    
    @Field("transaction_index")
    private Integer transactionIndex;
    
    @Indexed
    @Field("from_address")
    private String fromAddress;
    
    @Indexed
    @Field("to_address")
    private String toAddress;
    
    @Field("value")
    private String value;
    
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
    private List<Object> accessList;
    
    @Field("chain_id")
    private Integer chainId;
    
    @Field("blob_versioned_hashes")
    private List<String> blobVersionedHashes;
    
    @Field("timestamp")
    private LocalDateTime timestamp;
    
    @Field("created_at")
    private LocalDateTime createdAt;
    
    @Field("updated_at")
    private LocalDateTime updatedAt;
    
    // Constructors
    public LineaTransactionDocument() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.chainId = 59144; // Linea chain ID
    }
    
    public LineaTransactionDocument(String transactionHash, Long blockNumber) {
        this();
        this.transactionHash = transactionHash;
        this.blockNumber = blockNumber;
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
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
    
    public String getValue() {
        return value;
    }
    
    public void setValue(String value) {
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
    
    public List<Object> getAccessList() {
        return accessList;
    }
    
    public void setAccessList(List<Object> accessList) {
        this.accessList = accessList;
    }
    
    public Integer getChainId() {
        return chainId;
    }
    
    public void setChainId(Integer chainId) {
        this.chainId = chainId;
    }
    
    public List<String> getBlobVersionedHashes() {
        return blobVersionedHashes;
    }
    
    public void setBlobVersionedHashes(List<String> blobVersionedHashes) {
        this.blobVersionedHashes = blobVersionedHashes;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
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
        return "LineaTransactionDocument{" +
                "id='" + id + '\'' +
                ", transactionHash='" + transactionHash + '\'' +
                ", blockNumber=" + blockNumber +
                ", blockHash='" + blockHash + '\'' +
                ", transactionIndex=" + transactionIndex +
                ", fromAddress='" + fromAddress + '\'' +
                ", toAddress='" + toAddress + '\'' +
                ", value='" + value + '\'' +
                ", gas=" + gas +
                ", gasPrice=" + gasPrice +
                ", maxFeePerGas=" + maxFeePerGas +
                ", maxPriorityFeePerGas=" + maxPriorityFeePerGas +
                ", nonce=" + nonce +
                ", type=" + type +
                ", chainId=" + chainId +
                ", timestamp=" + timestamp +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
