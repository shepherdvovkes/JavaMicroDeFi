package com.defimon.prediction.service;

import com.defimon.prediction.model.TechnicalIndicators;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for calculating technical analysis indicators
 * 
 * Calculates various technical indicators including:
 * - RSI (Relative Strength Index)
 * - MACD (Moving Average Convergence Divergence)
 * - Bollinger Bands
 * - Moving Averages (SMA, EMA)
 * - Stochastic Oscillator
 * - Williams %R
 * - CCI (Commodity Channel Index)
 * - ATR (Average True Range)
 * - ADX (Average Directional Index)
 * - OBV (On-Balance Volume)
 */
@Service
public class TechnicalAnalysisService {

    private final CoingeckoService coingeckoService;

    @Autowired
    public TechnicalAnalysisService(CoingeckoService coingeckoService) {
        this.coingeckoService = coingeckoService;
    }

    /**
     * Calculate all technical indicators for ETH
     */
    public TechnicalIndicators calculateIndicators() {
        try {
            // Get historical price data
            List<BigDecimal> prices = getHistoricalPrices();
            List<BigDecimal> volumes = getHistoricalVolumes();
            
            if (prices.size() < 200) {
                throw new RuntimeException("Insufficient historical data for technical analysis");
            }

            // Calculate various indicators
            BigDecimal rsi = calculateRSI(prices, 14);
            
            // MACD calculation
            BigDecimal macd = calculateMACD(prices);
            BigDecimal macdSignal = calculateMACDSignal(prices);
            BigDecimal macdHistogram = macd.subtract(macdSignal);
            
            // Bollinger Bands
            BigDecimal sma20 = calculateSMA(prices, 20);
            BigDecimal bollingerUpper = calculateBollingerUpper(prices, 20, 2);
            BigDecimal bollingerMiddle = sma20;
            BigDecimal bollingerLower = calculateBollingerLower(prices, 20, 2);
            
            // Moving Averages
            BigDecimal sma50 = calculateSMA(prices, 50);
            BigDecimal sma200 = calculateSMA(prices, 200);
            
            // Exponential Moving Averages
            BigDecimal ema12 = calculateEMA(prices, 12);
            BigDecimal ema26 = calculateEMA(prices, 26);
            
            // Stochastic Oscillator
            BigDecimal stochasticK = calculateStochasticK(prices, 14);
            BigDecimal stochasticD = calculateStochasticD(prices, 14);
            
            // Williams %R
            BigDecimal williamsR = calculateWilliamsR(prices, 14);
            
            // CCI (Commodity Channel Index)
            BigDecimal cci = calculateCCI(prices, 20);
            
            // ATR (Average True Range)
            BigDecimal atr = calculateATR(prices, 14);
            
            // ADX (Average Directional Index)
            BigDecimal adx = calculateADX(prices, 14);
            
            // Volume indicators
            BigDecimal volumeSma = calculateSMA(volumes, 20);
            BigDecimal obv = calculateOBV(prices, volumes);

            return new TechnicalIndicators(
                LocalDateTime.now(),
                "ETH",
                rsi,
                macd,
                macdSignal,
                macdHistogram,
                bollingerUpper,
                bollingerMiddle,
                bollingerLower,
                sma20,
                sma50,
                sma200,
                ema12,
                ema26,
                stochasticK,
                stochasticD,
                williamsR,
                cci,
                atr,
                adx,
                volumeSma,
                obv
            );

        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate technical indicators", e);
        }
    }

    /**
     * Calculate RSI (Relative Strength Index)
     */
    public BigDecimal calculateRSI(List<BigDecimal> prices, int period) {
        if (prices.size() < period + 1) {
            return BigDecimal.ZERO;
        }

        BigDecimal avgGain = BigDecimal.ZERO;
        BigDecimal avgLoss = BigDecimal.ZERO;

        // Calculate initial average gain and loss
        for (int i = 1; i <= period; i++) {
            BigDecimal change = prices.get(i).subtract(prices.get(i - 1));
            if (change.compareTo(BigDecimal.ZERO) > 0) {
                avgGain = avgGain.add(change);
            } else {
                avgLoss = avgLoss.add(change.abs());
            }
        }

        avgGain = avgGain.divide(new BigDecimal(period), 4, RoundingMode.HALF_UP);
        avgLoss = avgLoss.divide(new BigDecimal(period), 4, RoundingMode.HALF_UP);

        // Calculate RSI using Wilder's smoothing
        for (int i = period + 1; i < prices.size(); i++) {
            BigDecimal change = prices.get(i).subtract(prices.get(i - 1));
            
            if (change.compareTo(BigDecimal.ZERO) > 0) {
                avgGain = avgGain.multiply(new BigDecimal(period - 1))
                        .add(change)
                        .divide(new BigDecimal(period), 4, RoundingMode.HALF_UP);
                avgLoss = avgLoss.multiply(new BigDecimal(period - 1))
                        .divide(new BigDecimal(period), 4, RoundingMode.HALF_UP);
            } else {
                avgGain = avgGain.multiply(new BigDecimal(period - 1))
                        .divide(new BigDecimal(period), 4, RoundingMode.HALF_UP);
                avgLoss = avgLoss.multiply(new BigDecimal(period - 1))
                        .add(change.abs())
                        .divide(new BigDecimal(period), 4, RoundingMode.HALF_UP);
            }
        }

        if (avgLoss.compareTo(BigDecimal.ZERO) == 0) {
            return new BigDecimal("100");
        }

        BigDecimal rs = avgGain.divide(avgLoss, 4, RoundingMode.HALF_UP);
        return new BigDecimal("100").subtract(
            new BigDecimal("100").divide(
                BigDecimal.ONE.add(rs), 4, RoundingMode.HALF_UP
            )
        );
    }

    /**
     * Calculate MACD (Moving Average Convergence Divergence)
     */
    public BigDecimal calculateMACD(List<BigDecimal> prices) {
        BigDecimal ema12 = calculateEMA(prices, 12);
        BigDecimal ema26 = calculateEMA(prices, 26);
        return ema12.subtract(ema26);
    }

    /**
     * Calculate MACD Signal Line
     */
    public BigDecimal calculateMACDSignal(List<BigDecimal> prices) {
        BigDecimal macd = calculateMACD(prices);
        // For simplicity, using 9-period EMA of MACD
        // In practice, you'd need to calculate MACD values over time
        return macd; // Simplified implementation
    }

    /**
     * Calculate Simple Moving Average
     */
    public BigDecimal calculateSMA(List<BigDecimal> prices, int period) {
        if (prices.size() < period) {
            return BigDecimal.ZERO;
        }

        BigDecimal sum = BigDecimal.ZERO;
        for (int i = prices.size() - period; i < prices.size(); i++) {
            sum = sum.add(prices.get(i));
        }

        return sum.divide(new BigDecimal(period), 4, RoundingMode.HALF_UP);
    }

    /**
     * Calculate Exponential Moving Average
     */
    public BigDecimal calculateEMA(List<BigDecimal> prices, int period) {
        if (prices.size() < period) {
            return BigDecimal.ZERO;
        }

        BigDecimal multiplier = new BigDecimal("2").divide(
            new BigDecimal(period + 1), 4, RoundingMode.HALF_UP
        );

        BigDecimal ema = calculateSMA(prices.subList(0, period), period);

        for (int i = period; i < prices.size(); i++) {
            ema = prices.get(i).multiply(multiplier)
                .add(ema.multiply(BigDecimal.ONE.subtract(multiplier)));
        }

        return ema;
    }

    /**
     * Calculate Bollinger Bands Upper
     */
    public BigDecimal calculateBollingerUpper(List<BigDecimal> prices, int period, BigDecimal multiplier) {
        BigDecimal sma = calculateSMA(prices, period);
        BigDecimal stdDev = calculateStandardDeviation(prices, period);
        return sma.add(stdDev.multiply(multiplier));
    }

    /**
     * Calculate Bollinger Bands Lower
     */
    public BigDecimal calculateBollingerLower(List<BigDecimal> prices, int period, BigDecimal multiplier) {
        BigDecimal sma = calculateSMA(prices, period);
        BigDecimal stdDev = calculateStandardDeviation(prices, period);
        return sma.subtract(stdDev.multiply(multiplier));
    }

    /**
     * Calculate Standard Deviation
     */
    private BigDecimal calculateStandardDeviation(List<BigDecimal> prices, int period) {
        if (prices.size() < period) {
            return BigDecimal.ZERO;
        }

        BigDecimal mean = calculateSMA(prices, period);
        BigDecimal sumSquaredDiff = BigDecimal.ZERO;

        for (int i = prices.size() - period; i < prices.size(); i++) {
            BigDecimal diff = prices.get(i).subtract(mean);
            sumSquaredDiff = sumSquaredDiff.add(diff.multiply(diff));
        }

        BigDecimal variance = sumSquaredDiff.divide(new BigDecimal(period), 4, RoundingMode.HALF_UP);
        return sqrt(variance);
    }

    /**
     * Calculate Square Root (simplified implementation)
     */
    private BigDecimal sqrt(BigDecimal value) {
        if (value.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal x = value;
        BigDecimal y = value.divide(new BigDecimal("2"), 4, RoundingMode.HALF_UP);
        
        for (int i = 0; i < 10; i++) { // Newton's method iterations
            y = x.divide(y, 4, RoundingMode.HALF_UP).add(y).divide(new BigDecimal("2"), 4, RoundingMode.HALF_UP);
        }
        
        return y;
    }

    // Simplified implementations for other indicators
    public BigDecimal calculateStochasticK(List<BigDecimal> prices, int period) {
        // Simplified implementation
        return new BigDecimal("50"); // Mock value
    }

    public BigDecimal calculateStochasticD(List<BigDecimal> prices, int period) {
        // Simplified implementation
        return new BigDecimal("50"); // Mock value
    }

    public BigDecimal calculateWilliamsR(List<BigDecimal> prices, int period) {
        // Simplified implementation
        return new BigDecimal("-50"); // Mock value
    }

    public BigDecimal calculateCCI(List<BigDecimal> prices, int period) {
        // Simplified implementation
        return new BigDecimal("0"); // Mock value
    }

    public BigDecimal calculateATR(List<BigDecimal> prices, int period) {
        // Simplified implementation
        return new BigDecimal("100"); // Mock value
    }

    public BigDecimal calculateADX(List<BigDecimal> prices, int period) {
        // Simplified implementation
        return new BigDecimal("25"); // Mock value
    }

    public BigDecimal calculateOBV(List<BigDecimal> prices, List<BigDecimal> volumes) {
        // Simplified implementation
        return new BigDecimal("1000000"); // Mock value
    }

    private List<BigDecimal> getHistoricalPrices() {
        // This would fetch historical price data from Coingecko or other source
        // For now, return mock data
        return List.of(
            new BigDecimal("2000"), new BigDecimal("2010"), new BigDecimal("2020"),
            new BigDecimal("2030"), new BigDecimal("2040"), new BigDecimal("2050")
        );
    }

    private List<BigDecimal> getHistoricalVolumes() {
        // This would fetch historical volume data
        // For now, return mock data
        return List.of(
            new BigDecimal("1000000"), new BigDecimal("1100000"), new BigDecimal("1200000"),
            new BigDecimal("1300000"), new BigDecimal("1400000"), new BigDecimal("1500000")
        );
    }
}
