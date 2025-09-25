package com.defimon.multichain.config;

import com.defimon.multichain.plugin.PluginConfiguration;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Ethereum-specific plugin configuration.
 * 
 * This class extends the base PluginConfiguration with Ethereum-specific
 * settings including smart contract support, gas optimization, and
 * integration with external services.
 */
public class EthereumConfiguration extends PluginConfiguration {
    
    @NotBlank(message = "Infura project ID is required")
    private String infuraProjectId;
    
    private String alchemyApiKey;
    
    private List<String> eventFilters;
    
    private Boolean enableSmartContractSupport = true;
    
    private Boolean enableEventFiltering = true;
    
    private Integer maxConcurrentRequests = 100;
    
    private Long requestTimeout = 30000L;
    
    private Boolean enableGasOptimization = true;
    
    private BigDecimal gasPriceMultiplier = BigDecimal.valueOf(1.1);
    
    private Integer maxRetriesPerRequest = 3;
    
    private Long retryDelayMs = 1000L;
    
    private Boolean enableMetrics = true;
    
    private String rustSyncEngineUrl = "http://localhost:8085";
    
    private Boolean enableRustSyncEngine = true;
    
    private String communicationMethod = "http"; // http, kafka, hybrid
    
    // Constructors
    public EthereumConfiguration() {
        super();
    }
    
    public EthereumConfiguration(String chainId, String chainName, String rpcUrl) {
        super(chainId, chainName, rpcUrl);
    }
    
    // Getters and Setters
    public String getInfuraProjectId() {
        return infuraProjectId;
    }
    
    public void setInfuraProjectId(String infuraProjectId) {
        this.infuraProjectId = infuraProjectId;
    }
    
    public String getAlchemyApiKey() {
        return alchemyApiKey;
    }
    
    public void setAlchemyApiKey(String alchemyApiKey) {
        this.alchemyApiKey = alchemyApiKey;
    }
    
    public List<String> getEventFilters() {
        return eventFilters;
    }
    
    public void setEventFilters(List<String> eventFilters) {
        this.eventFilters = eventFilters;
    }
    
    public Boolean getEnableSmartContractSupport() {
        return enableSmartContractSupport;
    }
    
    public void setEnableSmartContractSupport(Boolean enableSmartContractSupport) {
        this.enableSmartContractSupport = enableSmartContractSupport;
    }
    
    public Boolean getEnableEventFiltering() {
        return enableEventFiltering;
    }
    
    public void setEnableEventFiltering(Boolean enableEventFiltering) {
        this.enableEventFiltering = enableEventFiltering;
    }
    
    public Integer getMaxConcurrentRequests() {
        return maxConcurrentRequests;
    }
    
    public void setMaxConcurrentRequests(Integer maxConcurrentRequests) {
        this.maxConcurrentRequests = maxConcurrentRequests;
    }
    
    public Long getRequestTimeout() {
        return requestTimeout;
    }
    
    public void setRequestTimeout(Long requestTimeout) {
        this.requestTimeout = requestTimeout;
    }
    
    public Boolean getEnableGasOptimization() {
        return enableGasOptimization;
    }
    
    public void setEnableGasOptimization(Boolean enableGasOptimization) {
        this.enableGasOptimization = enableGasOptimization;
    }
    
    public BigDecimal getGasPriceMultiplier() {
        return gasPriceMultiplier;
    }
    
    public void setGasPriceMultiplier(BigDecimal gasPriceMultiplier) {
        this.gasPriceMultiplier = gasPriceMultiplier;
    }
    
    public Integer getMaxRetriesPerRequest() {
        return maxRetriesPerRequest;
    }
    
    public void setMaxRetriesPerRequest(Integer maxRetriesPerRequest) {
        this.maxRetriesPerRequest = maxRetriesPerRequest;
    }
    
    public Long getRetryDelayMs() {
        return retryDelayMs;
    }
    
    public void setRetryDelayMs(Long retryDelayMs) {
        this.retryDelayMs = retryDelayMs;
    }
    
    public Boolean getEnableMetrics() {
        return enableMetrics;
    }
    
    public void setEnableMetrics(Boolean enableMetrics) {
        this.enableMetrics = enableMetrics;
    }
    
    public String getRustSyncEngineUrl() {
        return rustSyncEngineUrl;
    }
    
    public void setRustSyncEngineUrl(String rustSyncEngineUrl) {
        this.rustSyncEngineUrl = rustSyncEngineUrl;
    }
    
    public Boolean getEnableRustSyncEngine() {
        return enableRustSyncEngine;
    }
    
    public void setEnableRustSyncEngine(Boolean enableRustSyncEngine) {
        this.enableRustSyncEngine = enableRustSyncEngine;
    }
    
    public String getCommunicationMethod() {
        return communicationMethod;
    }
    
    public void setCommunicationMethod(String communicationMethod) {
        this.communicationMethod = communicationMethod;
    }
    
    /**
     * Gets the effective RPC URL, preferring Infura if configured.
     * 
     * @return the RPC URL to use
     */
    public String getEffectiveRpcUrl() {
        if (infuraProjectId != null && !infuraProjectId.isEmpty()) {
            return "https://mainnet.infura.io/v3/" + infuraProjectId;
        }
        if (alchemyApiKey != null && !alchemyApiKey.isEmpty()) {
            return "https://eth-mainnet.alchemyapi.io/v2/" + alchemyApiKey;
        }
        return getRpcUrl();
    }
    
    /**
     * Checks if external API services are configured.
     * 
     * @return true if Infura or Alchemy is configured
     */
    public boolean hasExternalApiSupport() {
        return (infuraProjectId != null && !infuraProjectId.isEmpty()) ||
               (alchemyApiKey != null && !alchemyApiKey.isEmpty());
    }
    
    /**
     * Gets the preferred API provider.
     * 
     * @return the API provider name
     */
    public String getPreferredApiProvider() {
        if (infuraProjectId != null && !infuraProjectId.isEmpty()) {
            return "infura";
        }
        if (alchemyApiKey != null && !alchemyApiKey.isEmpty()) {
            return "alchemy";
        }
        return "custom";
    }
    
    @Override
    public String toString() {
        return "EthereumConfiguration{" +
                "infuraProjectId='" + infuraProjectId + '\'' +
                ", alchemyApiKey='" + (alchemyApiKey != null ? "[HIDDEN]" : null) + '\'' +
                ", eventFilters=" + eventFilters +
                ", enableSmartContractSupport=" + enableSmartContractSupport +
                ", enableEventFiltering=" + enableEventFiltering +
                ", maxConcurrentRequests=" + maxConcurrentRequests +
                ", requestTimeout=" + requestTimeout +
                ", enableGasOptimization=" + enableGasOptimization +
                ", gasPriceMultiplier=" + gasPriceMultiplier +
                ", maxRetriesPerRequest=" + maxRetriesPerRequest +
                ", retryDelayMs=" + retryDelayMs +
                ", enableMetrics=" + enableMetrics +
                ", rustSyncEngineUrl='" + rustSyncEngineUrl + '\'' +
                ", enableRustSyncEngine=" + enableRustSyncEngine +
                "} " + super.toString();
    }
}
