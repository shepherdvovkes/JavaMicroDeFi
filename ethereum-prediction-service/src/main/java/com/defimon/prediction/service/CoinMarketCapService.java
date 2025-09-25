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
 * Service for collecting market data from CoinMarketCap API
 * 
 * CoinMarketCap provides:
 * - Real-time cryptocurrency prices and market data
 * - Historical price data
 * - Market dominance metrics
 * - Professional API with higher rate limits
 */
@Service
public class CoinMarketCapService {

    private final WebClient webClient;
    private final String apiKey;

    public CoinMarketCapService(WebClient.Builder webClientBuilder,
                              @Value("${coinmarketcap.api.key:}") String apiKey) {
        this.webClient = webClientBuilder
                .baseUrl("https://pro-api.coinmarketcap.com/v1")
                .defaultHeader("X-CMC_PRO_API_KEY", apiKey)
                .build();
        this.apiKey = apiKey;
    }

    /**
     * Get Ethereum market data from CoinMarketCap
     */
    public MarketData getEthereumData() {
        try {
            Map<String, Object> response = webClient.get()
                    .uri("/cryptocurrency/quotes/latest?symbol=ETH&convert=USD")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            return parseMarketData(response);

        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch CoinMarketCap data", e);
        }
    }

    /**
     * Get historical price data
     */
    public Map<String, Object> getHistoricalData(int count, String interval) {
        try {
            return webClient.get()
                    .uri("/cryptocurrency/quotes/historical?symbol=ETH&count={count}&interval={interval}&convert=USD", 
                         count, interval)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch historical data", e);
        }
    }

    /**
     * Get global cryptocurrency metrics
     */
    public Map<String, Object> getGlobalMetrics() {
        try {
            return webClient.get()
                    .uri("/global-metrics/quotes/latest")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch global metrics", e);
        }
    }

    /**
     * Get trending cryptocurrencies
     */
    public Map<String, Object> getTrendingCryptocurrencies() {
        try {
            return webClient.get()
                    .uri("/cryptocurrency/trending/most-visited")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch trending data", e);
        }
    }

    /**
     * Get market pairs for ETH
     */
    public Map<String, Object> getMarketPairs() {
        try {
            return webClient.get()
                    .uri("/cryptocurrency/market-pairs/latest?symbol=ETH")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch market pairs", e);
        }
    }

    private MarketData parseMarketData(Map<String, Object> response) {
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) response.get("data");
        @SuppressWarnings("unchecked")
        Map<String, Object> ethData = (Map<String, Object>) data.get("ETH");
        
        if (ethData == null) {
            throw new RuntimeException("ETH data not found in response");
        }

        // Extract quote data
        @SuppressWarnings("unchecked")
        Map<String, Object> quote = (Map<String, Object>) ethData.get("quote");
        @SuppressWarnings("unchecked")
        Map<String, Object> usdQuote = (Map<String, Object>) quote.get("USD");

        BigDecimal price = new BigDecimal(usdQuote.get("price").toString());
        BigDecimal volume24h = new BigDecimal(usdQuote.get("volume_24h").toString());
        BigDecimal marketCap = new BigDecimal(usdQuote.get("market_cap").toString());
        BigDecimal change24h = new BigDecimal(usdQuote.get("price_change_24h").toString());
        BigDecimal changePercentage24h = new BigDecimal(usdQuote.get("percent_change_24h").toString());
        BigDecimal high24h = new BigDecimal(usdQuote.get("high_24h").toString());
        BigDecimal low24h = new BigDecimal(usdQuote.get("low_24h").toString());

        // Additional metrics
        Map<String, Object> additionalMetrics = new HashMap<>();
        additionalMetrics.put("market_cap_rank", ethData.get("cmc_rank"));
        additionalMetrics.put("circulating_supply", ethData.get("circulating_supply"));
        additionalMetrics.put("total_supply", ethData.get("total_supply"));
        additionalMetrics.put("max_supply", ethData.get("max_supply"));
        additionalMetrics.put("last_updated", ethData.get("last_updated"));
        additionalMetrics.put("date_added", ethData.get("date_added"));
        additionalMetrics.put("tags", ethData.get("tags"));
        additionalMetrics.put("percent_change_7d", usdQuote.get("percent_change_7d"));
        additionalMetrics.put("percent_change_30d", usdQuote.get("percent_change_30d"));
        additionalMetrics.put("percent_change_90d", usdQuote.get("percent_change_90d"));

        return new MarketData(
            LocalDateTime.now(),
            "coinmarketcap",
            "ETH",
            price,
            volume24h,
            marketCap,
            change24h,
            changePercentage24h,
            high24h,
            low24h,
            additionalMetrics
        );
    }
}
