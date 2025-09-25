package com.defimon.bitcoin.metrics.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Bitcoin Metrics Configuration
 */
@Configuration
@ConfigurationProperties(prefix = "bitcoin.metrics")
public class BitcoinMetricsConfig {
    
    private BitcoinRpc rpc = new BitcoinRpc();
    private Metrics metrics = new Metrics();
    
    public static class BitcoinRpc {
        private String host = "localhost";
        private int port = 8332;
        private String username = "bitcoin";
        private String password = "ultrafast_archive_node_2024";
        private int timeout = 30000;
        private int connectionTimeout = 10000;
        
        // Getters and Setters
        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
        
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        
        public int getTimeout() { return timeout; }
        public void setTimeout(int timeout) { this.timeout = timeout; }
        
        public int getConnectionTimeout() { return connectionTimeout; }
        public void setConnectionTimeout(int connectionTimeout) { this.connectionTimeout = connectionTimeout; }
        
        public String getUrl() {
            return "http://" + host + ":" + port;
        }
    }
    
    public static class Metrics {
        private int collectionInterval = 30000; // 30 seconds
        private boolean enableBlockchainMetrics = true;
        private boolean enableNetworkMetrics = true;
        private boolean enableMempoolMetrics = true;
        private boolean enableSystemMetrics = true;
        
        // Getters and Setters
        public int getCollectionInterval() { return collectionInterval; }
        public void setCollectionInterval(int collectionInterval) { this.collectionInterval = collectionInterval; }
        
        public boolean isEnableBlockchainMetrics() { return enableBlockchainMetrics; }
        public void setEnableBlockchainMetrics(boolean enableBlockchainMetrics) { this.enableBlockchainMetrics = enableBlockchainMetrics; }
        
        public boolean isEnableNetworkMetrics() { return enableNetworkMetrics; }
        public void setEnableNetworkMetrics(boolean enableNetworkMetrics) { this.enableNetworkMetrics = enableNetworkMetrics; }
        
        public boolean isEnableMempoolMetrics() { return enableMempoolMetrics; }
        public void setEnableMempoolMetrics(boolean enableMempoolMetrics) { this.enableMempoolMetrics = enableMempoolMetrics; }
        
        public boolean isEnableSystemMetrics() { return enableSystemMetrics; }
        public void setEnableSystemMetrics(boolean enableSystemMetrics) { this.enableSystemMetrics = enableSystemMetrics; }
    }
    
    // Getters and Setters
    public BitcoinRpc getRpc() { return rpc; }
    public void setRpc(BitcoinRpc rpc) { this.rpc = rpc; }
    
    public Metrics getMetrics() { return metrics; }
    public void setMetrics(Metrics metrics) { this.metrics = metrics; }
}
