package com.defimon.linea.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import java.math.BigInteger;
import java.time.LocalDateTime;

/**
 * Document representing a Linea account.
 * 
 * This document stores comprehensive account data including balance,
 * transaction count, and contract information.
 */
@Document(collection = "linea_accounts")
public class LineaAccount {
    
    @Id
    @Field("address")
    private String address;
    
    @Field("balance")
    private BigInteger balance;
    
    @Field("nonce")
    private BigInteger nonce;
    
    @Field("code")
    private String code;
    
    @Field("storage_root")
    private String storageRoot;
    
    @Field("is_contract")
    private Boolean isContract;
    
    @Field("contract_name")
    private String contractName;
    
    @Field("contract_symbol")
    private String contractSymbol;
    
    @Field("contract_decimals")
    private Integer contractDecimals;
    
    @Field("total_supply")
    private BigInteger totalSupply;
    
    @Field("transaction_count")
    private Long transactionCount;
    
    @Field("first_seen_block")
    private Long firstSeenBlock;
    
    @Field("last_seen_block")
    private Long lastSeenBlock;
    
    @Field("created_at")
    private LocalDateTime createdAt;
    
    @Field("updated_at")
    private LocalDateTime updatedAt;
    
    // Constructors
    public LineaAccount() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public LineaAccount(String address) {
        this();
        this.address = address;
    }
    
    // Getters and Setters
    public String getAddress() {
        return address;
    }
    
    public void setAddress(String address) {
        this.address = address;
    }
    
    public BigInteger getBalance() {
        return balance;
    }
    
    public void setBalance(BigInteger balance) {
        this.balance = balance;
    }
    
    public BigInteger getNonce() {
        return nonce;
    }
    
    public void setNonce(BigInteger nonce) {
        this.nonce = nonce;
    }
    
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    public String getStorageRoot() {
        return storageRoot;
    }
    
    public void setStorageRoot(String storageRoot) {
        this.storageRoot = storageRoot;
    }
    
    public Boolean getIsContract() {
        return isContract;
    }
    
    public void setIsContract(Boolean isContract) {
        this.isContract = isContract;
    }
    
    public String getContractName() {
        return contractName;
    }
    
    public void setContractName(String contractName) {
        this.contractName = contractName;
    }
    
    public String getContractSymbol() {
        return contractSymbol;
    }
    
    public void setContractSymbol(String contractSymbol) {
        this.contractSymbol = contractSymbol;
    }
    
    public Integer getContractDecimals() {
        return contractDecimals;
    }
    
    public void setContractDecimals(Integer contractDecimals) {
        this.contractDecimals = contractDecimals;
    }
    
    public BigInteger getTotalSupply() {
        return totalSupply;
    }
    
    public void setTotalSupply(BigInteger totalSupply) {
        this.totalSupply = totalSupply;
    }
    
    public Long getTransactionCount() {
        return transactionCount;
    }
    
    public void setTransactionCount(Long transactionCount) {
        this.transactionCount = transactionCount;
    }
    
    public Long getFirstSeenBlock() {
        return firstSeenBlock;
    }
    
    public void setFirstSeenBlock(Long firstSeenBlock) {
        this.firstSeenBlock = firstSeenBlock;
    }
    
    public Long getLastSeenBlock() {
        return lastSeenBlock;
    }
    
    public void setLastSeenBlock(Long lastSeenBlock) {
        this.lastSeenBlock = lastSeenBlock;
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
        return "LineaAccount{" +
                "address='" + address + '\'' +
                ", balance=" + balance +
                ", nonce=" + nonce +
                ", isContract=" + isContract +
                ", contractName='" + contractName + '\'' +
                ", contractSymbol='" + contractSymbol + '\'' +
                ", transactionCount=" + transactionCount +
                ", firstSeenBlock=" + firstSeenBlock +
                ", lastSeenBlock=" + lastSeenBlock +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        LineaAccount that = (LineaAccount) o;
        return address != null ? address.equals(that.address) : that.address == null;
    }
    
    @Override
    public int hashCode() {
        return address != null ? address.hashCode() : 0;
    }
}