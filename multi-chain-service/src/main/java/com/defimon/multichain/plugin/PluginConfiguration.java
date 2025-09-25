package com.defimon.multichain.plugin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Base configuration class for all blockchain plugins.
 * 
 * This abstract class provides common configuration properties that all
 * blockchain plugins need, while allowing for chain-specific extensions.
 */
public abstract class PluginConfiguration {
    
    @NotBlank(message = "Chain ID is required")
    private String chainId;
    
    @NotBlank(message = "Chain name is required")
    private String chainName;
    
    @NotNull(message = "Enabled flag is required")
    private Boolean enabled = false;
    
    @NotBlank(message = "RPC URL is required")
    private String rpcUrl;
    
    private Long blockTime = 12000L; // Default 12 seconds
    
    private String syncStrategy = "realtime";
    
    private String technology = "java21";
    
    private Integer maxRetries = 3;
    
    private Long retryDelay = 1000L;
    
    private Integer connectionTimeout = 30000;
    
    private Integer readTimeout = 60000;
    
    // Constructors
    public PluginConfiguration() {}
    
    public PluginConfiguration(String chainId, String chainName, String rpcUrl) {
        this.chainId = chainId;
        this.chainName = chainName;
        this.rpcUrl = rpcUrl;
    }
    
    // Getters and Setters
    public String getChainId() {
        return chainId;
    }
    
    public void setChainId(String chainId) {
        this.chainId = chainId;
    }
    
    public String getChainName() {
        return chainName;
    }
    
    public void setChainName(String chainName) {
        this.chainName = chainName;
    }
    
    public Boolean getEnabled() {
        return enabled;
    }
    
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
    
    public String getRpcUrl() {
        return rpcUrl;
    }
    
    public void setRpcUrl(String rpcUrl) {
        this.rpcUrl = rpcUrl;
    }
    
    public Long getBlockTime() {
        return blockTime;
    }
    
    public void setBlockTime(Long blockTime) {
        this.blockTime = blockTime;
    }
    
    public String getSyncStrategy() {
        return syncStrategy;
    }
    
    public void setSyncStrategy(String syncStrategy) {
        this.syncStrategy = syncStrategy;
    }
    
    public String getTechnology() {
        return technology;
    }
    
    public void setTechnology(String technology) {
        this.technology = technology;
    }
    
    public Integer getMaxRetries() {
        return maxRetries;
    }
    
    public void setMaxRetries(Integer maxRetries) {
        this.maxRetries = maxRetries;
    }
    
    public Long getRetryDelay() {
        return retryDelay;
    }
    
    public void setRetryDelay(Long retryDelay) {
        this.retryDelay = retryDelay;
    }
    
    public Integer getConnectionTimeout() {
        return connectionTimeout;
    }
    
    public void setConnectionTimeout(Integer connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }
    
    public Integer getReadTimeout() {
        return readTimeout;
    }
    
    public void setReadTimeout(Integer readTimeout) {
        this.readTimeout = readTimeout;
    }
    
    @Override
    public String toString() {
        return "PluginConfiguration{" +
                "chainId='" + chainId + '\'' +
                ", chainName='" + chainName + '\'' +
                ", enabled=" + enabled +
                ", rpcUrl='" + rpcUrl + '\'' +
                ", blockTime=" + blockTime +
                ", syncStrategy='" + syncStrategy + '\'' +
                ", technology='" + technology + '\'' +
                ", maxRetries=" + maxRetries +
                ", retryDelay=" + retryDelay +
                ", connectionTimeout=" + connectionTimeout +
                ", readTimeout=" + readTimeout +
                '}';
    }
}
