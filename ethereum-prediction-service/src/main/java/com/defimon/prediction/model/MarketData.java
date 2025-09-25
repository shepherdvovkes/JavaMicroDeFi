package com.defimon.prediction.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Market data collected from various sources
 */
public record MarketData(
    @JsonProperty("timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    LocalDateTime timestamp,
    
    @JsonProperty("source")
    String source,
    
    @JsonProperty("symbol")
    String symbol,
    
    @JsonProperty("price")
    BigDecimal price,
    
    @JsonProperty("volume_24h")
    BigDecimal volume24h,
    
    @JsonProperty("market_cap")
    BigDecimal marketCap,
    
    @JsonProperty("price_change_24h")
    BigDecimal priceChange24h,
    
    @JsonProperty("price_change_percentage_24h")
    BigDecimal priceChangePercentage24h,
    
    @JsonProperty("high_24h")
    BigDecimal high24h,
    
    @JsonProperty("low_24h")
    BigDecimal low24h,
    
    @JsonProperty("additional_metrics")
    Map<String, Object> additionalMetrics
) {}

/**
 * On-chain metrics for Ethereum network
 */
public record OnChainMetrics(
    @JsonProperty("timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    LocalDateTime timestamp,
    
    @JsonProperty("active_addresses")
    Long activeAddresses,
    
    @JsonProperty("transaction_count")
    Long transactionCount,
    
    @JsonProperty("gas_price_avg")
    BigDecimal gasPriceAvg,
    
    @JsonProperty("gas_price_max")
    BigDecimal gasPriceMax,
    
    @JsonProperty("gas_used")
    Long gasUsed,
    
    @JsonProperty("gas_limit")
    Long gasLimit,
    
    @JsonProperty("block_time_avg")
    BigDecimal blockTimeAvg,
    
    @JsonProperty("network_hash_rate")
    BigDecimal networkHashRate,
    
    @JsonProperty("difficulty")
    BigDecimal difficulty,
    
    @JsonProperty("total_value_locked_defi")
    BigDecimal totalValueLockedDefi,
    
    @JsonProperty("staked_eth")
    BigDecimal stakedEth,
    
    @JsonProperty("burned_eth")
    BigDecimal burnedEth,
    
    @JsonProperty("minted_eth")
    BigDecimal mintedEth,
    
    @JsonProperty("additional_metrics")
    Map<String, Object> additionalMetrics
) {}

/**
 * Sentiment analysis data
 */
public record SentimentData(
    @JsonProperty("timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    LocalDateTime timestamp,
    
    @JsonProperty("source")
    String source,
    
    @JsonProperty("overall_sentiment")
    BigDecimal overallSentiment,
    
    @JsonProperty("sentiment_breakdown")
    Map<String, BigDecimal> sentimentBreakdown,
    
    @JsonProperty("mention_count")
    Long mentionCount,
    
    @JsonProperty("engagement_metrics")
    Map<String, Long> engagementMetrics,
    
    @JsonProperty("influencer_sentiment")
    Map<String, BigDecimal> influencerSentiment,
    
    @JsonProperty("news_sentiment")
    BigDecimal newsSentiment,
    
    @JsonProperty("social_media_sentiment")
    BigDecimal socialMediaSentiment
) {}

/**
 * Technical analysis indicators
 */
public record TechnicalIndicators(
    @JsonProperty("timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    LocalDateTime timestamp,
    
    @JsonProperty("symbol")
    String symbol,
    
    @JsonProperty("rsi")
    BigDecimal rsi,
    
    @JsonProperty("macd")
    BigDecimal macd,
    
    @JsonProperty("macd_signal")
    BigDecimal macdSignal,
    
    @JsonProperty("macd_histogram")
    BigDecimal macdHistogram,
    
    @JsonProperty("bollinger_upper")
    BigDecimal bollingerUpper,
    
    @JsonProperty("bollinger_middle")
    BigDecimal bollingerMiddle,
    
    @JsonProperty("bollinger_lower")
    BigDecimal bollingerLower,
    
    @JsonProperty("sma_20")
    BigDecimal sma20,
    
    @JsonProperty("sma_50")
    BigDecimal sma50,
    
    @JsonProperty("sma_200")
    BigDecimal sma200,
    
    @JsonProperty("ema_12")
    BigDecimal ema12,
    
    @JsonProperty("ema_26")
    BigDecimal ema26,
    
    @JsonProperty("stochastic_k")
    BigDecimal stochasticK,
    
    @JsonProperty("stochastic_d")
    BigDecimal stochasticD,
    
    @JsonProperty("williams_r")
    BigDecimal williamsR,
    
    @JsonProperty("cci")
    BigDecimal cci,
    
    @JsonProperty("atr")
    BigDecimal atr,
    
    @JsonProperty("adx")
    BigDecimal adx,
    
    @JsonProperty("volume_sma")
    BigDecimal volumeSma,
    
    @JsonProperty("obv")
    BigDecimal obv
) {}

/**
 * Macroeconomic indicators
 */
public record MacroEconomicData(
    @JsonProperty("timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    LocalDateTime timestamp,
    
    @JsonProperty("dxy")
    BigDecimal dxy,
    
    @JsonProperty("fed_funds_rate")
    BigDecimal fedFundsRate,
    
    @JsonProperty("inflation_rate")
    BigDecimal inflationRate,
    
    @JsonProperty("unemployment_rate")
    BigDecimal unemploymentRate,
    
    @JsonProperty("gdp_growth")
    BigDecimal gdpGrowth,
    
    @JsonProperty("vix")
    BigDecimal vix,
    
    @JsonProperty("gold_price")
    BigDecimal goldPrice,
    
    @JsonProperty("oil_price")
    BigDecimal oilPrice,
    
    @JsonProperty("treasury_10y")
    BigDecimal treasury10y,
    
    @JsonProperty("treasury_2y")
    BigDecimal treasury2y,
    
    @JsonProperty("yield_curve")
    BigDecimal yieldCurve
) {}
