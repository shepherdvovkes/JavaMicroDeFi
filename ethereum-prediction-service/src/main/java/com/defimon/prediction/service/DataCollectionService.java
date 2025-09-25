package com.defimon.prediction.service;

import com.defimon.prediction.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Service responsible for collecting data from various sources
 * that affect Ethereum price
 */
@Service
public class DataCollectionService {

    private final WebClient webClient;
    private final CoingeckoService coingeckoService;
    private final CoinMarketCapService coinMarketCapService;
    private final EtherscanService etherscanService;
    private final SentimentAnalysisService sentimentAnalysisService;
    private final TechnicalAnalysisService technicalAnalysisService;
    private final MacroEconomicService macroEconomicService;
    private final DeFiAnalyticsService deFiAnalyticsService;

    @Autowired
    public DataCollectionService(
            WebClient.Builder webClientBuilder,
            CoingeckoService coingeckoService,
            CoinMarketCapService coinMarketCapService,
            EtherscanService etherscanService,
            SentimentAnalysisService sentimentAnalysisService,
            TechnicalAnalysisService technicalAnalysisService,
            MacroEconomicService macroEconomicService,
            DeFiAnalyticsService deFiAnalyticsService) {
        this.webClient = webClientBuilder.build();
        this.coingeckoService = coingeckoService;
        this.coinMarketCapService = coinMarketCapService;
        this.etherscanService = etherscanService;
        this.sentimentAnalysisService = sentimentAnalysisService;
        this.technicalAnalysisService = technicalAnalysisService;
        this.macroEconomicService = macroEconomicService;
        this.deFiAnalyticsService = deFiAnalyticsService;
    }

    /**
     * Collect comprehensive market data for ETH price prediction
     */
    public CompletableFuture<ComprehensiveMarketData> collectComprehensiveData() {
        return CompletableFuture.allOf(
            collectMarketDataAsync(),
            collectOnChainMetricsAsync(),
            collectSentimentDataAsync(),
            collectTechnicalIndicatorsAsync(),
            collectMacroEconomicDataAsync(),
            collectDeFiMetricsAsync()
        ).thenApply(v -> new ComprehensiveMarketData(
            LocalDateTime.now(),
            collectMarketDataSync(),
            collectOnChainMetricsSync(),
            collectSentimentDataSync(),
            collectTechnicalIndicatorsSync(),
            collectMacroEconomicDataSync(),
            collectDeFiMetricsSync()
        ));
    }

    private CompletableFuture<MarketData> collectMarketDataAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return coingeckoService.getEthereumData();
            } catch (Exception e) {
                // Fallback to CoinMarketCap
                return coinMarketCapService.getEthereumData();
            }
        });
    }

    private CompletableFuture<OnChainMetrics> collectOnChainMetricsAsync() {
        return CompletableFuture.supplyAsync(() -> etherscanService.getOnChainMetrics());
    }

    private CompletableFuture<SentimentData> collectSentimentDataAsync() {
        return CompletableFuture.supplyAsync(() -> sentimentAnalysisService.analyzeSentiment());
    }

    private CompletableFuture<TechnicalIndicators> collectTechnicalIndicatorsAsync() {
        return CompletableFuture.supplyAsync(() -> technicalAnalysisService.calculateIndicators());
    }

    private CompletableFuture<MacroEconomicData> collectMacroEconomicDataAsync() {
        return CompletableFuture.supplyAsync(() -> macroEconomicService.getMacroEconomicData());
    }

    private CompletableFuture<DeFiMetrics> collectDeFiMetricsAsync() {
        return CompletableFuture.supplyAsync(() -> deFiAnalyticsService.getDeFiMetrics());
    }

    // Synchronous methods for fallback
    private MarketData collectMarketDataSync() {
        try {
            return coingeckoService.getEthereumData();
        } catch (Exception e) {
            return coinMarketCapService.getEthereumData();
        }
    }

    private OnChainMetrics collectOnChainMetricsSync() {
        return etherscanService.getOnChainMetrics();
    }

    private SentimentData collectSentimentDataSync() {
        return sentimentAnalysisService.analyzeSentiment();
    }

    private TechnicalIndicators collectTechnicalIndicatorsSync() {
        return technicalAnalysisService.calculateIndicators();
    }

    private MacroEconomicData collectMacroEconomicDataSync() {
        return macroEconomicService.getMacroEconomicData();
    }

    private DeFiMetrics collectDeFiMetricsSync() {
        return deFiAnalyticsService.getDeFiMetrics();
    }

    /**
     * Stream real-time data updates
     */
    public Flux<MarketData> streamMarketData() {
        return Flux.interval(java.time.Duration.ofSeconds(30))
                .flatMap(tick -> Mono.fromCallable(() -> collectMarketDataSync()))
                .onErrorContinue((error, obj) -> {
                    // Log error and continue
                    System.err.println("Error collecting market data: " + error.getMessage());
                });
    }

    /**
     * Stream on-chain metrics updates
     */
    public Flux<OnChainMetrics> streamOnChainMetrics() {
        return Flux.interval(java.time.Duration.ofMinutes(1))
                .flatMap(tick -> Mono.fromCallable(() -> collectOnChainMetricsSync()))
                .onErrorContinue((error, obj) -> {
                    System.err.println("Error collecting on-chain metrics: " + error.getMessage());
                });
    }

    /**
     * Comprehensive market data container
     */
    public record ComprehensiveMarketData(
        LocalDateTime timestamp,
        MarketData marketData,
        OnChainMetrics onChainMetrics,
        SentimentData sentimentData,
        TechnicalIndicators technicalIndicators,
        MacroEconomicData macroEconomicData,
        DeFiMetrics deFiMetrics
    ) {}

    /**
     * DeFi metrics record
     */
    public record DeFiMetrics(
        LocalDateTime timestamp,
        java.math.BigDecimal totalValueLocked,
        java.math.BigDecimal defiDominance,
        java.math.BigDecimal lendingVolume,
        java.math.BigDecimal dexVolume,
        java.math.BigDecimal stakingRewards,
        java.math.BigDecimal yieldFarmingApy,
        Map<String, Object> protocolMetrics
    ) {}
}
