package com.defimon.multichain.config;

import com.defimon.multichain.plugin.PluginConfiguration;
import jakarta.validation.constraints.NotBlank;

/**
 * Bitcoin-specific plugin configuration.
 * 
 * This class extends the base PluginConfiguration with Bitcoin-specific
 * settings including RPC authentication, network parameters, and
 * UTXO-specific configurations.
 */
public class BitcoinConfiguration extends PluginConfiguration {
    
    @NotBlank(message = "RPC username is required")
    private String rpcUsername;
    
    @NotBlank(message = "RPC password is required")
    private String rpcPassword;
    
    private String network = "mainnet"; // mainnet, testnet, regtest
    
    private Boolean enableZMQ = false;
    
    private String zmqAddress = "tcp://127.0.0.1:28332";
    
    private Boolean enableUTXOIndexing = true;
    
    private Integer maxUTXOsPerAddress = 1000;
    
    private Boolean enableFeeEstimation = true;
    
    private Integer feeEstimationBlocks = 6;
    
    private Boolean enableMempoolMonitoring = true;
    
    private Integer mempoolMaxSize = 10000;
    
    private Boolean enableAddressIndexing = true;
    
    private Integer addressIndexMaxSize = 100000;
    
    private Boolean enableBlockFiltering = false;
    
    private String blockFilterType = "basic"; // basic, extended
    
    private Boolean enableCompactBlocks = true;
    
    private Integer compactBlocksMaxSize = 1000;
    
    private Boolean enablePeerDiscovery = false;
    
    private Integer maxPeers = 8;
    
    private Boolean enableMetrics = true;
    
    private Integer batchSize = 100;
    
    private Long batchTimeout = 5000L;
    
    // Constructors
    public BitcoinConfiguration() {
        super();
    }
    
    public BitcoinConfiguration(String chainId, String chainName, String rpcUrl) {
        super(chainId, chainName, rpcUrl);
    }
    
    // Getters and Setters
    public String getRpcUsername() {
        return rpcUsername;
    }
    
    public void setRpcUsername(String rpcUsername) {
        this.rpcUsername = rpcUsername;
    }
    
    public String getRpcPassword() {
        return rpcPassword;
    }
    
    public void setRpcPassword(String rpcPassword) {
        this.rpcPassword = rpcPassword;
    }
    
    public String getNetwork() {
        return network;
    }
    
    public void setNetwork(String network) {
        this.network = network;
    }
    
    public Boolean getEnableZMQ() {
        return enableZMQ;
    }
    
    public void setEnableZMQ(Boolean enableZMQ) {
        this.enableZMQ = enableZMQ;
    }
    
    public String getZmqAddress() {
        return zmqAddress;
    }
    
    public void setZmqAddress(String zmqAddress) {
        this.zmqAddress = zmqAddress;
    }
    
    public Boolean getEnableUTXOIndexing() {
        return enableUTXOIndexing;
    }
    
    public void setEnableUTXOIndexing(Boolean enableUTXOIndexing) {
        this.enableUTXOIndexing = enableUTXOIndexing;
    }
    
    public Integer getMaxUTXOsPerAddress() {
        return maxUTXOsPerAddress;
    }
    
    public void setMaxUTXOsPerAddress(Integer maxUTXOsPerAddress) {
        this.maxUTXOsPerAddress = maxUTXOsPerAddress;
    }
    
    public Boolean getEnableFeeEstimation() {
        return enableFeeEstimation;
    }
    
    public void setEnableFeeEstimation(Boolean enableFeeEstimation) {
        this.enableFeeEstimation = enableFeeEstimation;
    }
    
    public Integer getFeeEstimationBlocks() {
        return feeEstimationBlocks;
    }
    
    public void setFeeEstimationBlocks(Integer feeEstimationBlocks) {
        this.feeEstimationBlocks = feeEstimationBlocks;
    }
    
    public Boolean getEnableMempoolMonitoring() {
        return enableMempoolMonitoring;
    }
    
    public void setEnableMempoolMonitoring(Boolean enableMempoolMonitoring) {
        this.enableMempoolMonitoring = enableMempoolMonitoring;
    }
    
    public Integer getMempoolMaxSize() {
        return mempoolMaxSize;
    }
    
    public void setMempoolMaxSize(Integer mempoolMaxSize) {
        this.mempoolMaxSize = mempoolMaxSize;
    }
    
    public Boolean getEnableAddressIndexing() {
        return enableAddressIndexing;
    }
    
    public void setEnableAddressIndexing(Boolean enableAddressIndexing) {
        this.enableAddressIndexing = enableAddressIndexing;
    }
    
    public Integer getAddressIndexMaxSize() {
        return addressIndexMaxSize;
    }
    
    public void setAddressIndexMaxSize(Integer addressIndexMaxSize) {
        this.addressIndexMaxSize = addressIndexMaxSize;
    }
    
    public Boolean getEnableBlockFiltering() {
        return enableBlockFiltering;
    }
    
    public void setEnableBlockFiltering(Boolean enableBlockFiltering) {
        this.enableBlockFiltering = enableBlockFiltering;
    }
    
    public String getBlockFilterType() {
        return blockFilterType;
    }
    
    public void setBlockFilterType(String blockFilterType) {
        this.blockFilterType = blockFilterType;
    }
    
    public Boolean getEnableCompactBlocks() {
        return enableCompactBlocks;
    }
    
    public void setEnableCompactBlocks(Boolean enableCompactBlocks) {
        this.enableCompactBlocks = enableCompactBlocks;
    }
    
    public Integer getCompactBlocksMaxSize() {
        return compactBlocksMaxSize;
    }
    
    public void setCompactBlocksMaxSize(Integer compactBlocksMaxSize) {
        this.compactBlocksMaxSize = compactBlocksMaxSize;
    }
    
    public Boolean getEnablePeerDiscovery() {
        return enablePeerDiscovery;
    }
    
    public void setEnablePeerDiscovery(Boolean enablePeerDiscovery) {
        this.enablePeerDiscovery = enablePeerDiscovery;
    }
    
    public Integer getMaxPeers() {
        return maxPeers;
    }
    
    public void setMaxPeers(Integer maxPeers) {
        this.maxPeers = maxPeers;
    }
    
    public Boolean getEnableMetrics() {
        return enableMetrics;
    }
    
    public void setEnableMetrics(Boolean enableMetrics) {
        this.enableMetrics = enableMetrics;
    }
    
    public Integer getBatchSize() {
        return batchSize;
    }
    
    public void setBatchSize(Integer batchSize) {
        this.batchSize = batchSize;
    }
    
    public Long getBatchTimeout() {
        return batchTimeout;
    }
    
    public void setBatchTimeout(Long batchTimeout) {
        this.batchTimeout = batchTimeout;
    }
    
    /**
     * Gets the effective RPC URL with authentication.
     * 
     * @return the RPC URL with credentials
     */
    public String getEffectiveRpcUrl() {
        String baseUrl = getRpcUrl();
        if (baseUrl.contains("://")) {
            return baseUrl.replaceFirst("://", "://" + rpcUsername + ":" + rpcPassword + "@");
        }
        return baseUrl;
    }
    
    /**
     * Checks if this is a testnet configuration.
     * 
     * @return true if this is testnet
     */
    public boolean isTestnet() {
        return "testnet".equals(network);
    }
    
    /**
     * Checks if this is a mainnet configuration.
     * 
     * @return true if this is mainnet
     */
    public boolean isMainnet() {
        return "mainnet".equals(network);
    }
    
    /**
     * Checks if this is a regtest configuration.
     * 
     * @return true if this is regtest
     */
    public boolean isRegtest() {
        return "regtest".equals(network);
    }
    
    /**
     * Gets the network magic bytes for Bitcoin.
     * 
     * @return the network magic bytes
     */
    public byte[] getNetworkMagic() {
        return switch (network) {
            case "mainnet" -> new byte[]{(byte) 0xF9, (byte) 0xBE, (byte) 0xB4, (byte) 0xD9};
            case "testnet" -> new byte[]{(byte) 0x0B, (byte) 0x11, (byte) 0x09, (byte) 0x07};
            case "regtest" -> new byte[]{(byte) 0xFA, (byte) 0xBF, (byte) 0xB5, (byte) 0xDA};
            default -> new byte[]{(byte) 0xF9, (byte) 0xBE, (byte) 0xB4, (byte) 0xD9};
        };
    }
    
    /**
     * Gets the default port for the network.
     * 
     * @return the default port
     */
    public int getDefaultPort() {
        return switch (network) {
            case "mainnet" -> 8333;
            case "testnet" -> 18333;
            case "regtest" -> 18444;
            default -> 8333;
        };
    }
    
    /**
     * Gets the RPC port for the network.
     * 
     * @return the RPC port
     */
    public int getRpcPort() {
        return switch (network) {
            case "mainnet" -> 8332;
            case "testnet" -> 18332;
            case "regtest" -> 18443;
            default -> 8332;
        };
    }
    
    @Override
    public String toString() {
        return "BitcoinConfiguration{" +
                "rpcUsername='" + rpcUsername + '\'' +
                ", rpcPassword='" + "[HIDDEN]" + '\'' +
                ", network='" + network + '\'' +
                ", enableZMQ=" + enableZMQ +
                ", zmqAddress='" + zmqAddress + '\'' +
                ", enableUTXOIndexing=" + enableUTXOIndexing +
                ", maxUTXOsPerAddress=" + maxUTXOsPerAddress +
                ", enableFeeEstimation=" + enableFeeEstimation +
                ", feeEstimationBlocks=" + feeEstimationBlocks +
                ", enableMempoolMonitoring=" + enableMempoolMonitoring +
                ", mempoolMaxSize=" + mempoolMaxSize +
                ", enableAddressIndexing=" + enableAddressIndexing +
                ", addressIndexMaxSize=" + addressIndexMaxSize +
                ", enableBlockFiltering=" + enableBlockFiltering +
                ", blockFilterType='" + blockFilterType + '\'' +
                ", enableCompactBlocks=" + enableCompactBlocks +
                ", compactBlocksMaxSize=" + compactBlocksMaxSize +
                ", enablePeerDiscovery=" + enablePeerDiscovery +
                ", maxPeers=" + maxPeers +
                ", enableMetrics=" + enableMetrics +
                ", batchSize=" + batchSize +
                ", batchTimeout=" + batchTimeout +
                "} " + super.toString();
    }
}
