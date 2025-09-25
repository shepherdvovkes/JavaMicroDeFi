package com.defimon.linea.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Linea-specific configuration for the microservice.
 * 
 * This configuration class contains all Linea-specific settings including
 * RPC endpoints, collection intervals, database paths, and monitoring settings.
 */
@ConfigurationProperties(prefix = "linea")
public class LineaConfiguration {
    
    @NotBlank(message = "Linea RPC URL is required")
    private String rpcUrl;
    
    private String wssUrl;
    
    private String explorerUrl = "https://lineascan.build";
    
    private String chainId = "59144";
    
    private String networkName = "linea";
    
    private Long blockTime = 2000L; // 2 seconds
    
    // Database configuration
    private String databasePath = "/mnt/sata18tb/linea_data.db";
    
    private String archiveDatabasePath = "/mnt/sata18tb/linea_archive_data.db";
    
    private String databaseUrl = "jdbc:sqlite:/mnt/sata18tb/linea_data.db";
    
    private String archiveDatabaseUrl = "jdbc:sqlite:/mnt/sata18tb/linea_archive_data.db";
    
    // Collection intervals (in milliseconds)
    private Long blockCollectionInterval = 2000L;
    
    private Long transactionCollectionInterval = 1000L;
    
    private Long accountCollectionInterval = 5000L;
    
    private Long contractCollectionInterval = 10000L;
    
    private Long tokenCollectionInterval = 15000L;
    
    private Long defiCollectionInterval = 30000L;
    
    // Worker configuration
    private Integer maxConcurrentWorkers = 10;
    
    private Integer maxConcurrentRequests = 100;
    
    // Rate limiting
    private Integer rateLimitPerSecond = 100;
    
    private Integer wssRateLimitPerSecond = 50;
    
    // Timeouts
    private Long requestTimeoutSeconds = 30L;
    
    private Integer connectionTimeout = 30;
    
    private Integer responseTimeout = 30;
    
    // Retry configuration
    private Integer retryAttempts = 3;
    
    private Long retryDelaySeconds = 5L;
    
    // Archive collection settings
    private Integer archiveBatchSize = 1000;
    
    private Integer archiveConcurrentWorkers = 10;
    
    private Integer archiveMaxRetries = 3;
    
    private Long archiveRetryDelay = 5000L;
    
    private String archiveMode = "full"; // full, incremental, selective
    
    private Long archiveStartBlock = 0L;
    
    // Data validation
    private Boolean enableDataValidation = true;
    
    private Integer maxBlockGap = 10;
    
    private Integer minBlockSize = 1000;
    
    private Integer maxBlockSize = 30000000;
    
    private Boolean enableQualityChecks = true;
    
    private Integer minTransactionCount = 1;
    
    private Integer maxTransactionCount = 10000;
    
    // Monitoring and metrics
    private Boolean enableRealTimeMonitoring = true;
    
    private Boolean enableHistoricalData = true;
    
    private Boolean enableDefiMetrics = true;
    
    private Boolean enableBridgeMetrics = true;
    
    private Boolean enableTokenMetrics = true;
    
    private Boolean enableContractMetrics = true;
    
    // Backup settings
    private Boolean enableBackup = true;
    
    private Long backupIntervalHours = 6L;
    
    private Integer backupRetentionDays = 30;
    
    private String backupPath = "/mnt/sata18tb/linea_backups/";
    
    private Boolean compressArchiveData = true;
    
    private Integer archiveCompressionLevel = 6;
    
    // Known contracts
    private String bridgeContract = "0xA0b86a33E6441E0a4bFc0B4d5F3F3E5A4F3F3F3F";
    
    private String messageService = "0xd19bae9c65bde34f26c2ee8f2f3f3e5a4f3f3f3f";
    
    private List<String> defiProtocols = List.of(
        "0x794a61358D6845594F94dc1DB02A252b5b4814aD", // AAVE
        "0x1F98431c8aD98523631AE4a59f267346ea31F984", // Uniswap V3
        "0x1b02dA8Cb0d097eB8D57A175b88c7D8b47997506", // SushiSwap
        "0x7f90122BF0700F9E7e1F688fe926940E8839F353"  // Curve
    );
    
    private List<String> bridgeContracts = List.of(
        "0xA0b86a33E6441E0a4bFc0B4d5F3F3E5A4F3F3F3F",
        "0xd19bae9c65bde34f26c2ee8f2f3f3e5a4f3f3f3f"
    );
    
    // Token configuration
    private String nativeToken = "ETH";
    
    private String wrappedToken = "WETH";
    
    // Logging
    private String logLevel = "INFO";
    
    private String logFile = "/mnt/sata18tb/logs/linea_collector.log";
    
    private Integer logRetentionDays = 30;
    
    // API configuration
    private String apiHost = "0.0.0.0";
    
    private Integer apiPort = 8008;
    
    private String nodeEnv = "production";
    
    private String workingDir = "/mnt/sata18tb";
    
    // Constructors
    public LineaConfiguration() {}
    
    // Getters and Setters
    public String getRpcUrl() {
        return rpcUrl;
    }
    
    public void setRpcUrl(String rpcUrl) {
        this.rpcUrl = rpcUrl;
    }
    
    public String getWssUrl() {
        return wssUrl;
    }
    
    public void setWssUrl(String wssUrl) {
        this.wssUrl = wssUrl;
    }
    
    public String getExplorerUrl() {
        return explorerUrl;
    }
    
    public void setExplorerUrl(String explorerUrl) {
        this.explorerUrl = explorerUrl;
    }
    
    public String getChainId() {
        return chainId;
    }
    
    public void setChainId(String chainId) {
        this.chainId = chainId;
    }
    
    public String getNetworkName() {
        return networkName;
    }
    
    public void setNetworkName(String networkName) {
        this.networkName = networkName;
    }
    
    public Long getBlockTime() {
        return blockTime;
    }
    
    public void setBlockTime(Long blockTime) {
        this.blockTime = blockTime;
    }
    
    public String getDatabasePath() {
        return databasePath;
    }
    
    public void setDatabasePath(String databasePath) {
        this.databasePath = databasePath;
    }
    
    public String getArchiveDatabasePath() {
        return archiveDatabasePath;
    }
    
    public void setArchiveDatabasePath(String archiveDatabasePath) {
        this.archiveDatabasePath = archiveDatabasePath;
    }
    
    public String getDatabaseUrl() {
        return databaseUrl;
    }
    
    public void setDatabaseUrl(String databaseUrl) {
        this.databaseUrl = databaseUrl;
    }
    
    public String getArchiveDatabaseUrl() {
        return archiveDatabaseUrl;
    }
    
    public void setArchiveDatabaseUrl(String archiveDatabaseUrl) {
        this.archiveDatabaseUrl = archiveDatabaseUrl;
    }
    
    public Long getBlockCollectionInterval() {
        return blockCollectionInterval;
    }
    
    public void setBlockCollectionInterval(Long blockCollectionInterval) {
        this.blockCollectionInterval = blockCollectionInterval;
    }
    
    public Long getTransactionCollectionInterval() {
        return transactionCollectionInterval;
    }
    
    public void setTransactionCollectionInterval(Long transactionCollectionInterval) {
        this.transactionCollectionInterval = transactionCollectionInterval;
    }
    
    public Long getAccountCollectionInterval() {
        return accountCollectionInterval;
    }
    
    public void setAccountCollectionInterval(Long accountCollectionInterval) {
        this.accountCollectionInterval = accountCollectionInterval;
    }
    
    public Long getContractCollectionInterval() {
        return contractCollectionInterval;
    }
    
    public void setContractCollectionInterval(Long contractCollectionInterval) {
        this.contractCollectionInterval = contractCollectionInterval;
    }
    
    public Long getTokenCollectionInterval() {
        return tokenCollectionInterval;
    }
    
    public void setTokenCollectionInterval(Long tokenCollectionInterval) {
        this.tokenCollectionInterval = tokenCollectionInterval;
    }
    
    public Long getDefiCollectionInterval() {
        return defiCollectionInterval;
    }
    
    public void setDefiCollectionInterval(Long defiCollectionInterval) {
        this.defiCollectionInterval = defiCollectionInterval;
    }
    
    public Integer getMaxConcurrentWorkers() {
        return maxConcurrentWorkers;
    }
    
    public void setMaxConcurrentWorkers(Integer maxConcurrentWorkers) {
        this.maxConcurrentWorkers = maxConcurrentWorkers;
    }
    
    public Integer getMaxConcurrentRequests() {
        return maxConcurrentRequests;
    }
    
    public void setMaxConcurrentRequests(Integer maxConcurrentRequests) {
        this.maxConcurrentRequests = maxConcurrentRequests;
    }
    
    public Integer getRateLimitPerSecond() {
        return rateLimitPerSecond;
    }
    
    public void setRateLimitPerSecond(Integer rateLimitPerSecond) {
        this.rateLimitPerSecond = rateLimitPerSecond;
    }
    
    public Integer getWssRateLimitPerSecond() {
        return wssRateLimitPerSecond;
    }
    
    public void setWssRateLimitPerSecond(Integer wssRateLimitPerSecond) {
        this.wssRateLimitPerSecond = wssRateLimitPerSecond;
    }
    
    public Long getRequestTimeoutSeconds() {
        return requestTimeoutSeconds;
    }
    
    public void setRequestTimeoutSeconds(Long requestTimeoutSeconds) {
        this.requestTimeoutSeconds = requestTimeoutSeconds;
    }
    
    public Integer getConnectionTimeout() {
        return connectionTimeout;
    }
    
    public void setConnectionTimeout(Integer connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }
    
    public Integer getResponseTimeout() {
        return responseTimeout;
    }
    
    public void setResponseTimeout(Integer responseTimeout) {
        this.responseTimeout = responseTimeout;
    }
    
    public Integer getRetryAttempts() {
        return retryAttempts;
    }
    
    public void setRetryAttempts(Integer retryAttempts) {
        this.retryAttempts = retryAttempts;
    }
    
    public Long getRetryDelaySeconds() {
        return retryDelaySeconds;
    }
    
    public void setRetryDelaySeconds(Long retryDelaySeconds) {
        this.retryDelaySeconds = retryDelaySeconds;
    }
    
    public Integer getArchiveBatchSize() {
        return archiveBatchSize;
    }
    
    public void setArchiveBatchSize(Integer archiveBatchSize) {
        this.archiveBatchSize = archiveBatchSize;
    }
    
    public Integer getArchiveConcurrentWorkers() {
        return archiveConcurrentWorkers;
    }
    
    public void setArchiveConcurrentWorkers(Integer archiveConcurrentWorkers) {
        this.archiveConcurrentWorkers = archiveConcurrentWorkers;
    }
    
    public Integer getArchiveMaxRetries() {
        return archiveMaxRetries;
    }
    
    public void setArchiveMaxRetries(Integer archiveMaxRetries) {
        this.archiveMaxRetries = archiveMaxRetries;
    }
    
    public Long getArchiveRetryDelay() {
        return archiveRetryDelay;
    }
    
    public void setArchiveRetryDelay(Long archiveRetryDelay) {
        this.archiveRetryDelay = archiveRetryDelay;
    }
    
    public String getArchiveMode() {
        return archiveMode;
    }
    
    public void setArchiveMode(String archiveMode) {
        this.archiveMode = archiveMode;
    }
    
    public Long getArchiveStartBlock() {
        return archiveStartBlock;
    }
    
    public void setArchiveStartBlock(Long archiveStartBlock) {
        this.archiveStartBlock = archiveStartBlock;
    }
    
    public Boolean getEnableDataValidation() {
        return enableDataValidation;
    }
    
    public void setEnableDataValidation(Boolean enableDataValidation) {
        this.enableDataValidation = enableDataValidation;
    }
    
    public Integer getMaxBlockGap() {
        return maxBlockGap;
    }
    
    public void setMaxBlockGap(Integer maxBlockGap) {
        this.maxBlockGap = maxBlockGap;
    }
    
    public Integer getMinBlockSize() {
        return minBlockSize;
    }
    
    public void setMinBlockSize(Integer minBlockSize) {
        this.minBlockSize = minBlockSize;
    }
    
    public Integer getMaxBlockSize() {
        return maxBlockSize;
    }
    
    public void setMaxBlockSize(Integer maxBlockSize) {
        this.maxBlockSize = maxBlockSize;
    }
    
    public Boolean getEnableQualityChecks() {
        return enableQualityChecks;
    }
    
    public void setEnableQualityChecks(Boolean enableQualityChecks) {
        this.enableQualityChecks = enableQualityChecks;
    }
    
    public Integer getMinTransactionCount() {
        return minTransactionCount;
    }
    
    public void setMinTransactionCount(Integer minTransactionCount) {
        this.minTransactionCount = minTransactionCount;
    }
    
    public Integer getMaxTransactionCount() {
        return maxTransactionCount;
    }
    
    public void setMaxTransactionCount(Integer maxTransactionCount) {
        this.maxTransactionCount = maxTransactionCount;
    }
    
    public Boolean getEnableRealTimeMonitoring() {
        return enableRealTimeMonitoring;
    }
    
    public void setEnableRealTimeMonitoring(Boolean enableRealTimeMonitoring) {
        this.enableRealTimeMonitoring = enableRealTimeMonitoring;
    }
    
    public Boolean getEnableHistoricalData() {
        return enableHistoricalData;
    }
    
    public void setEnableHistoricalData(Boolean enableHistoricalData) {
        this.enableHistoricalData = enableHistoricalData;
    }
    
    public Boolean getEnableDefiMetrics() {
        return enableDefiMetrics;
    }
    
    public void setEnableDefiMetrics(Boolean enableDefiMetrics) {
        this.enableDefiMetrics = enableDefiMetrics;
    }
    
    public Boolean getEnableBridgeMetrics() {
        return enableBridgeMetrics;
    }
    
    public void setEnableBridgeMetrics(Boolean enableBridgeMetrics) {
        this.enableBridgeMetrics = enableBridgeMetrics;
    }
    
    public Boolean getEnableTokenMetrics() {
        return enableTokenMetrics;
    }
    
    public void setEnableTokenMetrics(Boolean enableTokenMetrics) {
        this.enableTokenMetrics = enableTokenMetrics;
    }
    
    public Boolean getEnableContractMetrics() {
        return enableContractMetrics;
    }
    
    public void setEnableContractMetrics(Boolean enableContractMetrics) {
        this.enableContractMetrics = enableContractMetrics;
    }
    
    public Boolean getEnableBackup() {
        return enableBackup;
    }
    
    public void setEnableBackup(Boolean enableBackup) {
        this.enableBackup = enableBackup;
    }
    
    public Long getBackupIntervalHours() {
        return backupIntervalHours;
    }
    
    public void setBackupIntervalHours(Long backupIntervalHours) {
        this.backupIntervalHours = backupIntervalHours;
    }
    
    public Integer getBackupRetentionDays() {
        return backupRetentionDays;
    }
    
    public void setBackupRetentionDays(Integer backupRetentionDays) {
        this.backupRetentionDays = backupRetentionDays;
    }
    
    public String getBackupPath() {
        return backupPath;
    }
    
    public void setBackupPath(String backupPath) {
        this.backupPath = backupPath;
    }
    
    public Boolean getCompressArchiveData() {
        return compressArchiveData;
    }
    
    public void setCompressArchiveData(Boolean compressArchiveData) {
        this.compressArchiveData = compressArchiveData;
    }
    
    public Integer getArchiveCompressionLevel() {
        return archiveCompressionLevel;
    }
    
    public void setArchiveCompressionLevel(Integer archiveCompressionLevel) {
        this.archiveCompressionLevel = archiveCompressionLevel;
    }
    
    public String getBridgeContract() {
        return bridgeContract;
    }
    
    public void setBridgeContract(String bridgeContract) {
        this.bridgeContract = bridgeContract;
    }
    
    public String getMessageService() {
        return messageService;
    }
    
    public void setMessageService(String messageService) {
        this.messageService = messageService;
    }
    
    public List<String> getDefiProtocols() {
        return defiProtocols;
    }
    
    public void setDefiProtocols(List<String> defiProtocols) {
        this.defiProtocols = defiProtocols;
    }
    
    public List<String> getBridgeContracts() {
        return bridgeContracts;
    }
    
    public void setBridgeContracts(List<String> bridgeContracts) {
        this.bridgeContracts = bridgeContracts;
    }
    
    public String getNativeToken() {
        return nativeToken;
    }
    
    public void setNativeToken(String nativeToken) {
        this.nativeToken = nativeToken;
    }
    
    public String getWrappedToken() {
        return wrappedToken;
    }
    
    public void setWrappedToken(String wrappedToken) {
        this.wrappedToken = wrappedToken;
    }
    
    public String getLogLevel() {
        return logLevel;
    }
    
    public void setLogLevel(String logLevel) {
        this.logLevel = logLevel;
    }
    
    public String getLogFile() {
        return logFile;
    }
    
    public void setLogFile(String logFile) {
        this.logFile = logFile;
    }
    
    public Integer getLogRetentionDays() {
        return logRetentionDays;
    }
    
    public void setLogRetentionDays(Integer logRetentionDays) {
        this.logRetentionDays = logRetentionDays;
    }
    
    public String getApiHost() {
        return apiHost;
    }
    
    public void setApiHost(String apiHost) {
        this.apiHost = apiHost;
    }
    
    public Integer getApiPort() {
        return apiPort;
    }
    
    public void setApiPort(Integer apiPort) {
        this.apiPort = apiPort;
    }
    
    public String getNodeEnv() {
        return nodeEnv;
    }
    
    public void setNodeEnv(String nodeEnv) {
        this.nodeEnv = nodeEnv;
    }
    
    public String getWorkingDir() {
        return workingDir;
    }
    
    public void setWorkingDir(String workingDir) {
        this.workingDir = workingDir;
    }
    
    /**
     * Gets the effective RPC URL for Linea.
     */
    public String getEffectiveRpcUrl() {
        return rpcUrl;
    }
    
    /**
     * Gets the effective WebSocket URL for Linea.
     */
    public String getEffectiveWssUrl() {
        if (wssUrl != null && !wssUrl.isEmpty()) {
            return wssUrl;
        }
        return rpcUrl.replace("https://", "wss://").replace("http://", "ws://");
    }
    
    /**
     * Checks if WebSocket support is available.
     */
    public boolean hasWebSocketSupport() {
        return wssUrl != null && !wssUrl.isEmpty();
    }
    
    /**
     * Gets the known DeFi protocols on Linea.
     */
    public List<String> getKnownDefiProtocols() {
        return defiProtocols;
    }
    
    /**
     * Gets the known bridge contracts on Linea.
     */
    public List<String> getKnownBridgeContracts() {
        return bridgeContracts;
    }
}
