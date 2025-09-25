package com.defimon.prediction.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Price prediction result with confidence metrics and factor analysis
 */
public record PricePrediction(
    @JsonProperty("timestamp") 
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    LocalDateTime timestamp,
    
    @JsonProperty("current_price") 
    BigDecimal currentPrice,
    
    @JsonProperty("predicted_price") 
    BigDecimal predictedPrice,
    
    @JsonProperty("price_change_percentage") 
    BigDecimal priceChangePercentage,
    
    @JsonProperty("confidence_score") 
    BigDecimal confidenceScore,
    
    @JsonProperty("time_horizon") 
    TimeHorizon timeHorizon,
    
    @JsonProperty("prediction_model") 
    String predictionModel,
    
    @JsonProperty("factor_analysis") 
    FactorAnalysis factorAnalysis,
    
    @JsonProperty("risk_metrics") 
    RiskMetrics riskMetrics,
    
    @JsonProperty("market_conditions") 
    MarketConditions marketConditions
) {
    
    public enum TimeHorizon {
        SHORT_TERM("1h", "1 hour"),
        MEDIUM_TERM("24h", "24 hours"), 
        LONG_TERM("7d", "7 days"),
        EXTENDED("30d", "30 days");
        
        private final String code;
        private final String description;
        
        TimeHorizon(String code, String description) {
            this.code = code;
            this.description = description;
        }
        
        public String getCode() { return code; }
        public String getDescription() { return description; }
    }
    
    public record FactorAnalysis(
        @JsonProperty("primary_factors") List<FactorImpact> primaryFactors,
        @JsonProperty("secondary_factors") List<FactorImpact> secondaryFactors,
        @JsonProperty("sentiment_score") BigDecimal sentimentScore,
        @JsonProperty("technical_indicators") Map<String, BigDecimal> technicalIndicators,
        @JsonProperty("on_chain_metrics") Map<String, BigDecimal> onChainMetrics
    ) {}
    
    public record FactorImpact(
        @JsonProperty("factor_name") String factorName,
        @JsonProperty("impact_score") BigDecimal impactScore,
        @JsonProperty("weight") BigDecimal weight,
        @JsonProperty("description") String description
    ) {}
    
    public record RiskMetrics(
        @JsonProperty("volatility") BigDecimal volatility,
        @JsonProperty("value_at_risk") BigDecimal valueAtRisk,
        @JsonProperty("expected_shortfall") BigDecimal expectedShortfall,
        @JsonProperty("max_drawdown") BigDecimal maxDrawdown,
        @JsonProperty("sharpe_ratio") BigDecimal sharpeRatio
    ) {}
    
    public record MarketConditions(
        @JsonProperty("market_regime") String marketRegime,
        @JsonProperty("liquidity_score") BigDecimal liquidityScore,
        @JsonProperty("correlation_btc") BigDecimal correlationBtc,
        @JsonProperty("fear_greed_index") BigDecimal fearGreedIndex
    ) {}
}
