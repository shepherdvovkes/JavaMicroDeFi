package com.defimon.prediction.service;

import com.defimon.prediction.model.MacroEconomicData;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Service for collecting macroeconomic data that affects cryptocurrency markets
 * 
 * Collects data from:
 * - Federal Reserve Economic Data (FRED)
 * - World Bank APIs
 * - Economic indicators (DXY, VIX, etc.)
 * - Commodity prices
 * - Interest rates
 */
@Service
public class MacroEconomicService {

    private final WebClient webClient;
    private final String fredApiKey;
    private final String alphaVantageApiKey;

    public MacroEconomicService(WebClient.Builder webClientBuilder,
                              @Value("${fred.api.key:}") String fredApiKey,
                              @Value("${alphavantage.api.key:}") String alphaVantageApiKey) {
        this.webClient = webClientBuilder.build();
        this.fredApiKey = fredApiKey;
        this.alphaVantageApiKey = alphaVantageApiKey;
    }

    /**
     * Get comprehensive macroeconomic data
     */
    public MacroEconomicData getMacroEconomicData() {
        try {
            // Collect data from various sources
            BigDecimal dxy = getDollarIndex();
            BigDecimal fedFundsRate = getFedFundsRate();
            BigDecimal inflationRate = getInflationRate();
            BigDecimal unemploymentRate = getUnemploymentRate();
            BigDecimal gdpGrowth = getGDPGrowth();
            BigDecimal vix = getVIX();
            BigDecimal goldPrice = getGoldPrice();
            BigDecimal oilPrice = getOilPrice();
            BigDecimal treasury10y = getTreasury10Y();
            BigDecimal treasury2y = getTreasury2Y();
            BigDecimal yieldCurve = treasury10y.subtract(treasury2y);

            return new MacroEconomicData(
                LocalDateTime.now(),
                dxy,
                fedFundsRate,
                inflationRate,
                unemploymentRate,
                gdpGrowth,
                vix,
                goldPrice,
                oilPrice,
                treasury10y,
                treasury2y,
                yieldCurve
            );

        } catch (Exception e) {
            // Return neutral/default values if data collection fails
            return createDefaultMacroEconomicData();
        }
    }

    /**
     * Get US Dollar Index (DXY) from Alpha Vantage
     */
    public BigDecimal getDollarIndex() {
        try {
            Map<String, Object> response = webClient.get()
                    .uri("https://www.alphavantage.co/query?function=FX_DAILY&from_symbol=USD&to_symbol=USDX&apikey={apiKey}", 
                         alphaVantageApiKey)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            // Parse response to get latest DXY value
            return parseAlphaVantagePrice(response);

        } catch (Exception e) {
            return new BigDecimal("103.50"); // Default DXY value
        }
    }

    /**
     * Get Federal Funds Rate from FRED
     */
    public BigDecimal getFedFundsRate() {
        try {
            Map<String, Object> response = webClient.get()
                    .uri("https://api.stlouisfed.org/fred/series/observations?series_id=FEDFUNDS&api_key={apiKey}&file_type=json&limit=1&sort_order=desc", 
                         fredApiKey)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            return parseFREDValue(response);

        } catch (Exception e) {
            return new BigDecimal("5.25"); // Default Fed Funds Rate
        }
    }

    /**
     * Get inflation rate from FRED
     */
    public BigDecimal getInflationRate() {
        try {
            Map<String, Object> response = webClient.get()
                    .uri("https://api.stlouisfed.org/fred/series/observations?series_id=CPIAUCSL&api_key={apiKey}&file_type=json&limit=1&sort_order=desc", 
                         fredApiKey)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            return parseFREDValue(response);

        } catch (Exception e) {
            return new BigDecimal("3.2"); // Default inflation rate
        }
    }

    /**
     * Get unemployment rate from FRED
     */
    public BigDecimal getUnemploymentRate() {
        try {
            Map<String, Object> response = webClient.get()
                    .uri("https://api.stlouisfed.org/fred/series/observations?series_id=UNRATE&api_key={apiKey}&file_type=json&limit=1&sort_order=desc", 
                         fredApiKey)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            return parseFREDValue(response);

        } catch (Exception e) {
            return new BigDecimal("3.8"); // Default unemployment rate
        }
    }

    /**
     * Get GDP growth rate from FRED
     */
    public BigDecimal getGDPGrowth() {
        try {
            Map<String, Object> response = webClient.get()
                    .uri("https://api.stlouisfed.org/fred/series/observations?series_id=GDPC1&api_key={apiKey}&file_type=json&limit=1&sort_order=desc", 
                         fredApiKey)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            return parseFREDValue(response);

        } catch (Exception e) {
            return new BigDecimal("2.1"); // Default GDP growth
        }
    }

    /**
     * Get VIX (Volatility Index) from Alpha Vantage
     */
    public BigDecimal getVIX() {
        try {
            Map<String, Object> response = webClient.get()
                    .uri("https://www.alphavantage.co/query?function=TIME_SERIES_DAILY&symbol=VIX&apikey={apiKey}", 
                         alphaVantageApiKey)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            return parseAlphaVantagePrice(response);

        } catch (Exception e) {
            return new BigDecimal("18.5"); // Default VIX value
        }
    }

    /**
     * Get Gold price from Alpha Vantage
     */
    public BigDecimal getGoldPrice() {
        try {
            Map<String, Object> response = webClient.get()
                    .uri("https://www.alphavantage.co/query?function=CURRENCY_EXCHANGE_RATE&from_currency=XAU&to_currency=USD&apikey={apiKey}", 
                         alphaVantageApiKey)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            return parseAlphaVantagePrice(response);

        } catch (Exception e) {
            return new BigDecimal("2050.00"); // Default gold price
        }
    }

    /**
     * Get Oil price from Alpha Vantage
     */
    public BigDecimal getOilPrice() {
        try {
            Map<String, Object> response = webClient.get()
                    .uri("https://www.alphavantage.co/query?function=TIME_SERIES_DAILY&symbol=WTI&apikey={apiKey}", 
                         alphaVantageApiKey)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            return parseAlphaVantagePrice(response);

        } catch (Exception e) {
            return new BigDecimal("75.50"); // Default oil price
        }
    }

    /**
     * Get 10-Year Treasury yield from FRED
     */
    public BigDecimal getTreasury10Y() {
        try {
            Map<String, Object> response = webClient.get()
                    .uri("https://api.stlouisfed.org/fred/series/observations?series_id=DGS10&api_key={apiKey}&file_type=json&limit=1&sort_order=desc", 
                         fredApiKey)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            return parseFREDValue(response);

        } catch (Exception e) {
            return new BigDecimal("4.25"); // Default 10Y Treasury yield
        }
    }

    /**
     * Get 2-Year Treasury yield from FRED
     */
    public BigDecimal getTreasury2Y() {
        try {
            Map<String, Object> response = webClient.get()
                    .uri("https://api.stlouisfed.org/fred/series/observations?series_id=DGS2&api_key={apiKey}&file_type=json&limit=1&sort_order=desc", 
                         fredApiKey)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            return parseFREDValue(response);

        } catch (Exception e) {
            return new BigDecimal("4.75"); // Default 2Y Treasury yield
        }
    }

    /**
     * Parse FRED API response to extract value
     */
    @SuppressWarnings("unchecked")
    private BigDecimal parseFREDValue(Map<String, Object> response) {
        try {
            Map<String, Object> observations = (Map<String, Object>) response.get("observations");
            if (observations != null && observations.containsKey("0")) {
                Map<String, Object> firstObservation = (Map<String, Object>) observations.get("0");
                String value = (String) firstObservation.get("value");
                if (value != null && !value.equals(".")) {
                    return new BigDecimal(value);
                }
            }
        } catch (Exception e) {
            // Fall through to default
        }
        return BigDecimal.ZERO;
    }

    /**
     * Parse Alpha Vantage API response to extract price
     */
    @SuppressWarnings("unchecked")
    private BigDecimal parseAlphaVantagePrice(Map<String, Object> response) {
        try {
            // Handle different Alpha Vantage response formats
            if (response.containsKey("Realtime Currency Exchange Rate")) {
                Map<String, Object> exchangeRate = (Map<String, Object>) response.get("Realtime Currency Exchange Rate");
                String rate = (String) exchangeRate.get("5. Exchange Rate");
                return new BigDecimal(rate);
            } else if (response.containsKey("Time Series (Daily)")) {
                Map<String, Object> timeSeries = (Map<String, Object>) response.get("Time Series (Daily)");
                // Get the most recent date
                String latestDate = timeSeries.keySet().stream().findFirst().orElse(null);
                if (latestDate != null) {
                    Map<String, Object> dailyData = (Map<String, Object>) timeSeries.get(latestDate);
                    String close = (String) dailyData.get("4. close");
                    return new BigDecimal(close);
                }
            }
        } catch (Exception e) {
            // Fall through to default
        }
        return BigDecimal.ZERO;
    }

    /**
     * Create default macroeconomic data when API calls fail
     */
    private MacroEconomicData createDefaultMacroEconomicData() {
        return new MacroEconomicData(
            LocalDateTime.now(),
            new BigDecimal("103.50"), // DXY
            new BigDecimal("5.25"),   // Fed Funds Rate
            new BigDecimal("3.2"),    // Inflation Rate
            new BigDecimal("3.8"),    // Unemployment Rate
            new BigDecimal("2.1"),    // GDP Growth
            new BigDecimal("18.5"),   // VIX
            new BigDecimal("2050.00"), // Gold Price
            new BigDecimal("75.50"),  // Oil Price
            new BigDecimal("4.25"),   // 10Y Treasury
            new BigDecimal("4.75"),   // 2Y Treasury
            new BigDecimal("-0.50")   // Yield Curve (10Y - 2Y)
        );
    }
}
