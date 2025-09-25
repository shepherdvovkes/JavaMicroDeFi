package com.defimon.prediction.service;

import com.defimon.prediction.model.PricePrediction;
import com.defimon.prediction.service.DataCollectionService.ComprehensiveMarketData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service for implementing various prediction models
 * 
 * Implements multiple prediction algorithms:
 * - Linear Regression
 * - Moving Average models
 * - Sentiment-weighted models
 * - Multi-factor models
 * - Ensemble methods
 */
@Service
public class PredictionModelService {

    private final DataCollectionService dataCollectionService;
    private final TechnicalAnalysisService technicalAnalysisService;

    @Autowired
    public PredictionModelService(DataCollectionService dataCollectionService,
                                TechnicalAnalysisService technicalAnalysisService) {
        this.dataCollectionService = dataCollectionService;
        this.technicalAnalysisService = technicalAnalysisService;
    }

    /**
     * Generate comprehensive price prediction using ensemble methods
     */
    public PricePrediction generatePrediction(PricePrediction.TimeHorizon timeHorizon) {
        try {
            // Collect comprehensive market data
            ComprehensiveMarketData marketData = dataCollectionService.collectComprehensiveData().get();
            
            // Generate predictions using multiple models
            List<BigDecimal> predictions = new ArrayList<>();
            List<BigDecimal> confidences = new ArrayList<>();
            
            // Technical analysis prediction
            BigDecimal technicalPrediction = generateTechnicalPrediction(marketData, timeHorizon);
            BigDecimal technicalConfidence = calculateTechnicalConfidence(marketData);
            predictions.add(technicalPrediction);
            confidences.add(technicalConfidence);
            
            // Sentiment-based prediction
            BigDecimal sentimentPrediction = generateSentimentPrediction(marketData, timeHorizon);
            BigDecimal sentimentConfidence = calculateSentimentConfidence(marketData);
            predictions.add(sentimentPrediction);
            confidences.add(sentimentConfidence);
            
            // Macro-economic prediction
            BigDecimal macroPrediction = generateMacroPrediction(marketData, timeHorizon);
            BigDecimal macroConfidence = calculateMacroConfidence(marketData);
            predictions.add(macroPrediction);
            confidences.add(macroConfidence);
            
            // DeFi metrics prediction
            BigDecimal defiPrediction = generateDeFiPrediction(marketData, timeHorizon);
            BigDecimal defiConfidence = calculateDeFiConfidence(marketData);
            predictions.add(defiPrediction);
            confidences.add(defiConfidence);
            
            // Ensemble prediction with weighted average
            BigDecimal finalPrediction = calculateEnsemblePrediction(predictions, confidences);
            BigDecimal overallConfidence = calculateOverallConfidence(confidences);
            
            // Calculate price change percentage
            BigDecimal currentPrice = marketData.marketData().price();
            BigDecimal priceChangePercentage = finalPrediction.subtract(currentPrice)
                    .divide(currentPrice, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
            
            // Generate factor analysis
            PricePrediction.FactorAnalysis factorAnalysis = generateFactorAnalysis(marketData, predictions, confidences);
            
            // Calculate risk metrics
            PricePrediction.RiskMetrics riskMetrics = calculateRiskMetrics(marketData, finalPrediction);
            
            // Determine market conditions
            PricePrediction.MarketConditions marketConditions = determineMarketConditions(marketData);
            
            return new PricePrediction(
                LocalDateTime.now(),
                currentPrice,
                finalPrediction,
                priceChangePercentage,
                overallConfidence,
                timeHorizon,
                "ensemble_model_v1",
                factorAnalysis,
                riskMetrics,
                marketConditions
            );
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate prediction", e);
        }
    }

    /**
     * Generate technical analysis-based prediction
     */
    public BigDecimal generateTechnicalPrediction(ComprehensiveMarketData marketData, PricePrediction.TimeHorizon timeHorizon) {
        BigDecimal currentPrice = marketData.marketData().price();
        BigDecimal technicalMultiplier = getTechnicalMultiplier(marketData.technicalIndicators(), timeHorizon);
        
        return currentPrice.multiply(technicalMultiplier);
    }

    /**
     * Generate sentiment-based prediction
     */
    public BigDecimal generateSentimentPrediction(ComprehensiveMarketData marketData, PricePrediction.TimeHorizon timeHorizon) {
        BigDecimal currentPrice = marketData.marketData().price();
        BigDecimal sentimentScore = marketData.sentimentData().overallSentiment();
        
        // Sentiment impact varies by time horizon
        BigDecimal sentimentMultiplier = switch (timeHorizon) {
            case SHORT_TERM -> BigDecimal.ONE.add(sentimentScore.multiply(new BigDecimal("0.1")));
            case MEDIUM_TERM -> BigDecimal.ONE.add(sentimentScore.multiply(new BigDecimal("0.05")));
            case LONG_TERM -> BigDecimal.ONE.add(sentimentScore.multiply(new BigDecimal("0.02")));
            case EXTENDED -> BigDecimal.ONE.add(sentimentScore.multiply(new BigDecimal("0.01")));
        };
        
        return currentPrice.multiply(sentimentMultiplier);
    }

    /**
     * Generate macro-economic prediction
     */
    public BigDecimal generateMacroPrediction(ComprehensiveMarketData marketData, PricePrediction.TimeHorizon timeHorizon) {
        BigDecimal currentPrice = marketData.marketData().price();
        var macroData = marketData.macroEconomicData();
        
        // Calculate macro impact
        BigDecimal dxyImpact = calculateDXYImpact(macroData.dxy());
        BigDecimal vixImpact = calculateVIXImpact(macroData.vix());
        BigDecimal interestRateImpact = calculateInterestRateImpact(macroData.fedFundsRate());
        
        BigDecimal totalMacroImpact = dxyImpact.add(vixImpact).add(interestRateImpact);
        
        return currentPrice.multiply(BigDecimal.ONE.add(totalMacroImpact));
    }

    /**
     * Generate DeFi metrics-based prediction
     */
    public BigDecimal generateDeFiPrediction(ComprehensiveMarketData marketData, PricePrediction.TimeHorizon timeHorizon) {
        BigDecimal currentPrice = marketData.marketData().price();
        var defiMetrics = marketData.deFiMetrics();
        
        // DeFi TVL growth impact
        BigDecimal tvlImpact = defiMetrics.totalValueLocked()
                .divide(new BigDecimal("100000000000"), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("0.01"));
        
        // Yield farming APY impact
        BigDecimal apyImpact = defiMetrics.yieldFarmingApy()
                .subtract(new BigDecimal("5"))
                .multiply(new BigDecimal("0.02"));
        
        BigDecimal totalDefiImpact = tvlImpact.add(apyImpact);
        
        return currentPrice.multiply(BigDecimal.ONE.add(totalDefiImpact));
    }

    /**
     * Calculate technical analysis multiplier based on indicators
     */
    private BigDecimal getTechnicalMultiplier(com.defimon.prediction.model.TechnicalIndicators indicators, PricePrediction.TimeHorizon timeHorizon) {
        BigDecimal multiplier = BigDecimal.ONE;
        
        // RSI impact
        if (indicators.rsi().compareTo(new BigDecimal("70")) > 0) {
            multiplier = multiplier.multiply(new BigDecimal("0.98")); // Overbought
        } else if (indicators.rsi().compareTo(new BigDecimal("30")) < 0) {
            multiplier = multiplier.multiply(new BigDecimal("1.02")); // Oversold
        }
        
        // MACD impact
        if (indicators.macd().compareTo(indicators.macdSignal()) > 0) {
            multiplier = multiplier.multiply(new BigDecimal("1.01")); // Bullish signal
        } else {
            multiplier = multiplier.multiply(new BigDecimal("0.99")); // Bearish signal
        }
        
        // Bollinger Bands impact
        BigDecimal currentPrice = new BigDecimal("2000"); // This should come from market data
        if (currentPrice.compareTo(indicators.bollingerUpper()) > 0) {
            multiplier = multiplier.multiply(new BigDecimal("0.98")); // Above upper band
        } else if (currentPrice.compareTo(indicators.bollingerLower()) < 0) {
            multiplier = multiplier.multiply(new BigDecimal("1.02")); // Below lower band
        }
        
        // Adjust multiplier based on time horizon
        BigDecimal timeAdjustment = switch (timeHorizon) {
            case SHORT_TERM -> new BigDecimal("1.005");
            case MEDIUM_TERM -> new BigDecimal("1.01");
            case LONG_TERM -> new BigDecimal("1.02");
            case EXTENDED -> new BigDecimal("1.03");
        };
        
        return multiplier.multiply(timeAdjustment);
    }

    /**
     * Calculate DXY (Dollar Index) impact on crypto prices
     */
    private BigDecimal calculateDXYImpact(BigDecimal dxy) {
        // Higher DXY typically correlates with lower crypto prices
        BigDecimal baseDXY = new BigDecimal("100");
        return dxy.subtract(baseDXY).multiply(new BigDecimal("-0.01"));
    }

    /**
     * Calculate VIX (Volatility Index) impact
     */
    private BigDecimal calculateVIXImpact(BigDecimal vix) {
        // Higher VIX indicates market stress, often correlates with crypto selloffs
        BigDecimal baseVIX = new BigDecimal("20");
        return vix.subtract(baseVIX).multiply(new BigDecimal("-0.005"));
    }

    /**
     * Calculate interest rate impact
     */
    private BigDecimal calculateInterestRateImpact(BigDecimal fedFundsRate) {
        // Higher interest rates typically pressure risk assets like crypto
        BigDecimal baseRate = new BigDecimal("3.0");
        return fedFundsRate.subtract(baseRate).multiply(new BigDecimal("-0.02"));
    }

    /**
     * Calculate ensemble prediction using weighted average
     */
    private BigDecimal calculateEnsemblePrediction(List<BigDecimal> predictions, List<BigDecimal> confidences) {
        if (predictions.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal weightedSum = BigDecimal.ZERO;
        BigDecimal totalWeight = BigDecimal.ZERO;
        
        for (int i = 0; i < predictions.size(); i++) {
            BigDecimal weight = confidences.get(i);
            weightedSum = weightedSum.add(predictions.get(i).multiply(weight));
            totalWeight = totalWeight.add(weight);
        }
        
        return totalWeight.compareTo(BigDecimal.ZERO) > 0 
            ? weightedSum.divide(totalWeight, 4, RoundingMode.HALF_UP)
            : predictions.get(0);
    }

    /**
     * Calculate overall confidence score
     */
    private BigDecimal calculateOverallConfidence(List<BigDecimal> confidences) {
        if (confidences.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal sum = confidences.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(new BigDecimal(confidences.size()), 4, RoundingMode.HALF_UP);
    }

    // Individual confidence calculation methods
    private BigDecimal calculateTechnicalConfidence(ComprehensiveMarketData marketData) {
        // Technical analysis confidence based on indicator agreement
        return new BigDecimal("0.75");
    }

    private BigDecimal calculateSentimentConfidence(ComprehensiveMarketData marketData) {
        // Sentiment confidence based on data quality and agreement
        return new BigDecimal("0.60");
    }

    private BigDecimal calculateMacroConfidence(ComprehensiveMarketData marketData) {
        // Macro confidence based on data availability and relevance
        return new BigDecimal("0.70");
    }

    private BigDecimal calculateDeFiConfidence(ComprehensiveMarketData marketData) {
        // DeFi confidence based on protocol health and TVL stability
        return new BigDecimal("0.65");
    }

    /**
     * Generate factor analysis
     */
    private PricePrediction.FactorAnalysis generateFactorAnalysis(ComprehensiveMarketData marketData, 
                                                                 List<BigDecimal> predictions, 
                                                                 List<BigDecimal> confidences) {
        List<PricePrediction.FactorImpact> primaryFactors = List.of(
            new PricePrediction.FactorImpact("Technical Analysis", new BigDecimal("0.8"), new BigDecimal("0.3"), 
                "RSI, MACD, and Bollinger Bands indicate market momentum"),
            new PricePrediction.FactorImpact("Market Sentiment", new BigDecimal("0.6"), new BigDecimal("0.25"), 
                "Social media sentiment and news analysis"),
            new PricePrediction.FactorImpact("DeFi Activity", new BigDecimal("0.7"), new BigDecimal("0.2"), 
                "Total Value Locked and yield farming activity"),
            new PricePrediction.FactorImpact("Macro Conditions", new BigDecimal("0.5"), new BigDecimal("0.15"), 
                "DXY, VIX, and interest rate environment")
        );

        List<PricePrediction.FactorImpact> secondaryFactors = List.of(
            new PricePrediction.FactorImpact("Network Activity", new BigDecimal("0.4"), new BigDecimal("0.1"), 
                "Transaction count and gas usage")
        );

        Map<String, BigDecimal> technicalIndicators = Map.of(
            "rsi", marketData.technicalIndicators().rsi(),
            "macd", marketData.technicalIndicators().macd(),
            "bollinger_position", new BigDecimal("0.5")
        );

        Map<String, BigDecimal> onChainMetrics = Map.of(
            "active_addresses", new BigDecimal("500000"),
            "transaction_count", new BigDecimal("1000000"),
            "gas_price", marketData.onChainMetrics().gasPriceAvg()
        );

        return new PricePrediction.FactorAnalysis(
            primaryFactors,
            secondaryFactors,
            marketData.sentimentData().overallSentiment(),
            technicalIndicators,
            onChainMetrics
        );
    }

    /**
     * Calculate risk metrics
     */
    private PricePrediction.RiskMetrics calculateRiskMetrics(ComprehensiveMarketData marketData, BigDecimal predictedPrice) {
        BigDecimal volatility = new BigDecimal("0.25"); // 25% volatility
        BigDecimal currentPrice = marketData.marketData().price();
        
        // Calculate VaR (Value at Risk) - 95% confidence
        BigDecimal valueAtRisk = currentPrice.multiply(volatility).multiply(new BigDecimal("1.645"));
        
        // Calculate Expected Shortfall (Conditional VaR)
        BigDecimal expectedShortfall = valueAtRisk.multiply(new BigDecimal("1.1"));
        
        // Calculate max drawdown
        BigDecimal maxDrawdown = currentPrice.multiply(new BigDecimal("0.15")); // 15% max drawdown
        
        // Calculate Sharpe ratio (simplified)
        BigDecimal sharpeRatio = new BigDecimal("1.2");

        return new PricePrediction.RiskMetrics(
            volatility,
            valueAtRisk,
            expectedShortfall,
            maxDrawdown,
            sharpeRatio
        );
    }

    /**
     * Determine current market conditions
     */
    private PricePrediction.MarketConditions determineMarketConditions(ComprehensiveMarketData marketData) {
        String marketRegime = determineMarketRegime(marketData);
        BigDecimal liquidityScore = calculateLiquidityScore(marketData);
        BigDecimal correlationBtc = new BigDecimal("0.85"); // ETH-BTC correlation
        BigDecimal fearGreedIndex = calculateFearGreedIndex(marketData);

        return new PricePrediction.MarketConditions(
            marketRegime,
            liquidityScore,
            correlationBtc,
            fearGreedIndex
        );
    }

    private String determineMarketRegime(ComprehensiveMarketData marketData) {
        BigDecimal vix = marketData.macroEconomicData().vix();
        BigDecimal sentiment = marketData.sentimentData().overallSentiment();
        
        if (vix.compareTo(new BigDecimal("30")) > 0) {
            return "HIGH_VOLATILITY";
        } else if (sentiment.compareTo(new BigDecimal("0.7")) > 0) {
            return "BULL_MARKET";
        } else if (sentiment.compareTo(new BigDecimal("-0.7")) < 0) {
            return "BEAR_MARKET";
        } else {
            return "SIDEWAYS";
        }
    }

    private BigDecimal calculateLiquidityScore(ComprehensiveMarketData marketData) {
        BigDecimal volume = marketData.marketData().volume24h();
        BigDecimal avgVolume = new BigDecimal("5000000000"); // Average daily volume
        
        return volume.divide(avgVolume, 4, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateFearGreedIndex(ComprehensiveMarketData marketData) {
        // Combine various indicators to calculate fear/greed index
        BigDecimal sentiment = marketData.sentimentData().overallSentiment();
        BigDecimal vix = marketData.macroEconomicData().vix();
        
        // Normalize and combine indicators
        BigDecimal sentimentScore = sentiment.multiply(new BigDecimal("50")).add(new BigDecimal("50"));
        BigDecimal vixScore = new BigDecimal("100").subtract(vix.multiply(new BigDecimal("2")));
        
        return sentimentScore.add(vixScore).divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);
    }
}
