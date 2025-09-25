package com.defimon.prediction.service;

import com.defimon.prediction.model.OnChainMetrics;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for collecting on-chain metrics from Etherscan API
 * 
 * Etherscan provides comprehensive Ethereum blockchain data:
 * - Transaction data and gas metrics
 * - Network statistics
 * - Smart contract interactions
 * - Block data and network health metrics
 */
@Service
public class EtherscanService {

    private final WebClient webClient;
    private final String apiKey;

    public EtherscanService(WebClient.Builder webClientBuilder,
                          @Value("${etherscan.api.key:}") String apiKey) {
        this.webClient = webClientBuilder
                .baseUrl("https://api.etherscan.io/api")
                .build();
        this.apiKey = apiKey;
    }

    /**
     * Get comprehensive on-chain metrics for Ethereum
     */
    public OnChainMetrics getOnChainMetrics() {
        try {
            // Collect various metrics in parallel
            Map<String, Object> ethPrice = getEthPrice();
            Map<String, Object> ethSupply = getEthSupply();
            Map<String, Object> latestBlock = getLatestBlock();
            Map<String, Object> gasOracle = getGasOracle();
            Map<String, Object> networkStats = getNetworkStats();

            return buildOnChainMetrics(ethPrice, ethSupply, latestBlock, gasOracle, networkStats);

        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch Etherscan data", e);
        }
    }

    /**
     * Get current ETH price from Etherscan
     */
    public Map<String, Object> getEthPrice() {
        try {
            return webClient.get()
                    .uri("?module=stats&action=ethprice&apikey={apiKey}", apiKey)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch ETH price", e);
        }
    }

    /**
     * Get ETH supply information
     */
    public Map<String, Object> getEthSupply() {
        try {
            return webClient.get()
                    .uri("?module=stats&action=ethsupply&apikey={apiKey}", apiKey)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch ETH supply", e);
        }
    }

    /**
     * Get latest block information
     */
    public Map<String, Object> getLatestBlock() {
        try {
            return webClient.get()
                    .uri("?module=proxy&action=eth_blockNumber&apikey={apiKey}", apiKey)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch latest block", e);
        }
    }

    /**
     * Get gas oracle data
     */
    public Map<String, Object> getGasOracle() {
        try {
            return webClient.get()
                    .uri("?module=gastracker&action=gasoracle&apikey={apiKey}", apiKey)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch gas oracle", e);
        }
    }

    /**
     * Get network statistics
     */
    public Map<String, Object> getNetworkStats() {
        try {
            return webClient.get()
                    .uri("?module=stats&action=ethsupply2&apikey={apiKey}", apiKey)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch network stats", e);
        }
    }

    /**
     * Get transaction count for an address
     */
    public Map<String, Object> getTransactionCount(String address) {
        try {
            return webClient.get()
                    .uri("?module=proxy&action=eth_getTransactionCount&address={address}&tag=latest&apikey={apiKey}", 
                         address, apiKey)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch transaction count", e);
        }
    }

    /**
     * Get block information by number
     */
    public Map<String, Object> getBlockByNumber(String blockNumber) {
        try {
            return webClient.get()
                    .uri("?module=proxy&action=eth_getBlockByNumber&tag={blockNumber}&boolean=true&apikey={apiKey}", 
                         blockNumber, apiKey)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch block data", e);
        }
    }

    /**
     * Get transaction information
     */
    public Map<String, Object> getTransaction(String txHash) {
        try {
            return webClient.get()
                    .uri("?module=proxy&action=eth_getTransactionByHash&txhash={txHash}&apikey={apiKey}", 
                         txHash, apiKey)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch transaction data", e);
        }
    }

    /**
     * Get token information
     */
    public Map<String, Object> getTokenInfo(String contractAddress) {
        try {
            return webClient.get()
                    .uri("?module=token&action=tokeninfo&contractaddress={contractAddress}&apikey={apiKey}", 
                         contractAddress, apiKey)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch token info", e);
        }
    }

    private OnChainMetrics buildOnChainMetrics(
            Map<String, Object> ethPrice,
            Map<String, Object> ethSupply,
            Map<String, Object> latestBlock,
            Map<String, Object> gasOracle,
            Map<String, Object> networkStats) {

        // Extract data from responses
        @SuppressWarnings("unchecked")
        Map<String, Object> priceResult = (Map<String, Object>) ethPrice.get("result");
        String ethUsdPrice = (String) priceResult.get("ethusd");

        @SuppressWarnings("unchecked")
        Map<String, Object> gasResult = (Map<String, Object>) gasOracle.get("result");
        String safeGasPrice = (String) gasResult.get("SafeGasPrice");
        String fastGasPrice = (String) gasResult.get("FastGasPrice");
        String proposeGasPrice = (String) gasResult.get("ProposeGasPrice");

        String latestBlockHex = (String) latestBlock.get("result");
        Long latestBlockNumber = Long.parseLong(latestBlockHex.substring(2), 16);

        // Additional metrics
        Map<String, Object> additionalMetrics = new HashMap<>();
        additionalMetrics.put("eth_usd_price", ethUsdPrice);
        additionalMetrics.put("safe_gas_price", safeGasPrice);
        additionalMetrics.put("fast_gas_price", fastGasPrice);
        additionalMetrics.put("propose_gas_price", proposeGasPrice);
        additionalMetrics.put("latest_block_number", latestBlockNumber);

        return new OnChainMetrics(
            LocalDateTime.now(),
            null, // active_addresses - would need additional API call
            null, // transaction_count - would need additional API call
            new BigDecimal(safeGasPrice != null ? safeGasPrice : "0"),
            new BigDecimal(fastGasPrice != null ? fastGasPrice : "0"),
            null, // gas_used - would need block data
            null, // gas_limit - would need block data
            null, // block_time_avg - would need historical analysis
            null, // network_hash_rate - not directly available
            null, // difficulty - would need additional API call
            null, // total_value_locked_defi - would need DeFi protocol APIs
            null, // staked_eth - would need beacon chain data
            null, // burned_eth - would need EIP-1559 analysis
            null, // minted_eth - would need block reward analysis
            additionalMetrics
        );
    }
}
