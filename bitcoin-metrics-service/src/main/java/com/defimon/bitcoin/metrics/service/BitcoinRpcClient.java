package com.defimon.bitcoin.metrics.service;

import com.defimon.bitcoin.metrics.config.BitcoinMetricsConfig;
import com.defimon.bitcoin.metrics.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.concurrent.TimeoutException;

/**
 * Bitcoin RPC Client Service
 */
@Service
public class BitcoinRpcClient {
    
    private static final Logger logger = LoggerFactory.getLogger(BitcoinRpcClient.class);
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final BitcoinMetricsConfig config;
    
    @Autowired
    public BitcoinRpcClient(BitcoinMetricsConfig config) {
        this.config = config;
        this.objectMapper = new ObjectMapper();
        
        // Create basic auth header
        String credentials = config.getRpc().getUsername() + ":" + config.getRpc().getPassword();
        String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        
        this.webClient = WebClient.builder()
                .baseUrl(config.getRpc().getUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + encodedCredentials)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)) // 10MB
                .build();
    }
    
    /**
     * Make RPC call to Bitcoin node
     */
    public <T> Mono<T> call(String method, Class<T> responseType, Object... params) {
        BitcoinRpcRequest request = new BitcoinRpcRequest(
                "metrics-" + System.currentTimeMillis(),
                method,
                Arrays.asList(params)
        );
        
        return webClient.post()
                .bodyValue(request)
                .retrieve()
                .bodyToMono(BitcoinRpcResponse.class)
                .timeout(java.time.Duration.ofMillis(config.getRpc().getTimeout()))
                .map(response -> {
                    if (response.getError() != null) {
                        throw new RuntimeException("Bitcoin RPC Error: " + response.getError().getMessage());
                    }
                    return response.getResult();
                })
                .map(result -> {
                    try {
                        // Convert the result to JSON string and then to the target type
                        String json = objectMapper.writeValueAsString(result);
                        return objectMapper.readValue(json, responseType);
                    } catch (Exception e) {
                        logger.error("Failed to deserialize RPC response for method: {}", method, e);
                        throw new RuntimeException("Failed to deserialize RPC response", e);
                    }
                })
                .doOnSuccess(result -> logger.debug("RPC call successful: {}", method))
                .doOnError(error -> logger.error("RPC call failed: {} - {}", method, error.getMessage()));
    }
    
    /**
     * Get blockchain information
     */
    public Mono<BitcoinBlockchainInfo> getBlockchainInfo() {
        return call("getblockchaininfo", BitcoinBlockchainInfo.class);
    }
    
    /**
     * Get network information
     */
    public Mono<BitcoinNetworkInfo> getNetworkInfo() {
        return call("getnetworkinfo", BitcoinNetworkInfo.class);
    }
    
    /**
     * Get mempool information
     */
    public Mono<BitcoinMempoolInfo> getMempoolInfo() {
        return call("getmempoolinfo", BitcoinMempoolInfo.class);
    }
    
    /**
     * Get current block count
     */
    public Mono<Long> getBlockCount() {
        return call("getblockcount", Long.class);
    }
    
    /**
     * Get block hash by height
     */
    public Mono<String> getBlockHash(long height) {
        return call("getblockhash", String.class, height);
    }
    
    /**
     * Get raw mempool
     */
    public Mono<String[]> getRawMempool() {
        return call("getrawmempool", String[].class, false);
    }
    
    /**
     * Get connection count
     */
    public Mono<Long> getConnectionCount() {
        return getNetworkInfo().map(BitcoinNetworkInfo::getConnections);
    }
    
    /**
     * Test RPC connectivity
     */
    public Mono<Boolean> testConnection() {
        return getBlockCount()
                .map(count -> true)
                .onErrorReturn(false)
                .doOnSuccess(connected -> logger.info("Bitcoin RPC connection test: {}", connected ? "SUCCESS" : "FAILED"));
    }
}
