package com.defimon.prediction.service;

import com.defimon.prediction.model.MarketData;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for collecting market data from CoinGecko API
 * 
 * CoinGecko provides comprehensive cryptocurrency market data including:
 * - Real-time and historical prices
 * - Market capitalization and volume
 * - Price change metrics
 * - Developer and community metrics
 */
@Service
public class CoingeckoService {

    private final WebClient webClient;
    private final String apiKey;

    public CoingeckoService(WebClient.Builder webClientBuilder,
                          @Value("${coingecko.api.key:}") String apiKey) {
        this.webClient = webClientBuilder
                .baseUrl("https://api.coingecko.com/api/v3")
                .build();
        this.apiKey = apiKey;
    }

    /**
     * Get comprehensive Ethereum market data from CoinGecko
     */
    public MarketData getEthereumData() {
        try {
            // Get basic market data
            Map<String, Object> marketData = webClient.get()
                    .uri("/coins/ethereum?localization=false&tickers=false&market_data=true&community_data=true&developer_data=true&sparkline=false")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            return parseMarketData(marketData);

        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch CoinGecko data", e);
        }
    }

    /**
     * Get historical price data for technical analysis
     */
    public Map<String, Object> getHistoricalData(int days) {
        try {
            return webClient.get()
                    .uri("/coins/ethereum/market_chart?vs_currency=usd&days={days}&interval=daily", days)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch historical data", e);
        }
    }

    /**
     * Get market dominance and global metrics
     */
    public Map<String, Object> getGlobalMetrics() {
        try {
            return webClient.get()
                    .uri("/global")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch global metrics", e);
        }
    }

    /**
     * Get trending cryptocurrencies (sentiment indicator)
     */
    public Map<String, Object> getTrendingCoins() {
        try {
            return webClient.get()
                    .uri("/search/trending")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch trending data", e);
        }
    }

    /**
     * Get developer activity metrics
     */
    public Map<String, Object> getDeveloperData() {
        try {
            return webClient.get()
                    .uri("/coins/ethereum/developer_data")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch developer data", e);
        }
    }

    /**
     * Get community metrics (social indicators)
     */
    public Map<String, Object> getCommunityData() {
        try {
            return webClient.get()
                    .uri("/coins/ethereum/community_data")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch community data", e);
        }
    }

    /**
     * Get exchange data and trading pairs
     */
    public Map<String, Object> getExchangeData() {
        try {
            return webClient.get()
                    .uri("/coins/ethereum/tickers")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch exchange data", e);
        }
    }

    private MarketData parseMarketData(Map<String, Object> data) {
        @SuppressWarnings("unchecked")
        Map<String, Object> marketData = (Map<String, Object>) data.get("market_data");
        
        if (marketData == null) {
            throw new RuntimeException("Market data not found in response");
        }

        // Extract price data
        @SuppressWarnings("unchecked")
        Map<String, Object> currentPrice = (Map<String, Object>) marketData.get("current_price");
        BigDecimal price = new BigDecimal(currentPrice.get("usd").toString());

        // Extract volume data
        @SuppressWarnings("unchecked")
        Map<String, Object> volume = (Map<String, Object>) marketData.get("total_volume");
        BigDecimal volume24h = new BigDecimal(volume.get("usd").toString());

        // Extract market cap
        @SuppressWarnings("unchecked")
        Map<String, Object> marketCap = (Map<String, Object>) marketData.get("market_cap");
        BigDecimal marketCapValue = new BigDecimal(marketCap.get("usd").toString());

        // Extract price changes
        @SuppressWarnings("unchecked")
        Map<String, Object> priceChange24h = (Map<String, Object>) marketData.get("price_change_24h");
        BigDecimal change24h = new BigDecimal(priceChange24h.get("usd").toString());

        @SuppressWarnings("unchecked")
        Map<String, Object> priceChangePercentage24h = (Map<String, Object>) marketData.get("price_change_percentage_24h");
        BigDecimal changePercentage24h = new BigDecimal(priceChangePercentage24h.get("usd").toString());

        // Extract high/low data
        @SuppressWarnings("unchecked")
        Map<String, Object> high24h = (Map<String, Object>) marketData.get("high_24h");
        BigDecimal high24hValue = new BigDecimal(high24h.get("usd").toString());

        @SuppressWarnings("unchecked")
        Map<String, Object> low24h = (Map<String, Object>) marketData.get("low_24h");
        BigDecimal low24hValue = new BigDecimal(low24h.get("usd").toString());

        // Additional metrics
        Map<String, Object> additionalMetrics = new HashMap<>();
        additionalMetrics.put("market_cap_rank", marketData.get("market_cap_rank"));
        additionalMetrics.put("price_change_percentage_7d", 
            ((Map<String, Object>) marketData.get("price_change_percentage_7d")).get("usd"));
        additionalMetrics.put("price_change_percentage_30d", 
            ((Map<String, Object>) marketData.get("price_change_percentage_30d")).get("usd"));
        additionalMetrics.put("circulating_supply", marketData.get("circulating_supply"));
        additionalMetrics.put("total_supply", marketData.get("total_supply"));

        return new MarketData(
            LocalDateTime.now(),
            "coingecko",
            "ETH",
            price,
            volume24h,
            marketCapValue,
            change24h,
            changePercentage24h,
            high24hValue,
            low24hValue,
            additionalMetrics
        );
    }
}
