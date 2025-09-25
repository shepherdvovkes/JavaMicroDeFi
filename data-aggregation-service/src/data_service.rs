use anyhow::Result;
use chrono::Utc;
use dashmap::DashMap;
use log::{error, info};
use std::sync::Arc;
use std::time::Instant;
use tokio::time::{interval, Duration};

use crate::aggregation::DataAggregator;
use crate::kafka_consumer::KafkaConsumerService;
use crate::mongodb_client::MongoDBService;
use crate::models::*;

#[derive(Clone)]
pub struct DataAggregationService {
    kafka_consumer: KafkaConsumerService,
    mongodb_service: MongoDBService,
    real_time_cache: Arc<DashMap<String, RealTimeDataPoint>>,
}

impl DataAggregationService {
    pub async fn new(kafka_brokers: &str, mongodb_uri: &str) -> Result<Self> {
        let kafka_consumer = KafkaConsumerService::new(kafka_brokers, "data-aggregation-group")?;
        let mongodb_service = MongoDBService::new(mongodb_uri).await?;
        let real_time_cache = Arc::new(DashMap::new());

        Ok(Self {
            kafka_consumer,
            mongodb_service,
            real_time_cache,
        })
    }

    pub async fn start_consumers(&self) -> Result<()> {
        self.kafka_consumer.subscribe_to_price_updates().await?;
        
        let service = self.clone();
        self.kafka_consumer.start_consuming(move |event| {
            tokio::runtime::Handle::current().block_on(async {
                service.process_streaming_event(event).await
            })
        }).await
    }

    pub async fn start_real_time_aggregation(&self) -> Result<()> {
        let mut interval = interval(Duration::from_secs(60)); // Aggregate every minute
        let service = self.clone();

        loop {
            interval.tick().await;
            if let Err(e) = service.perform_real_time_aggregation().await {
                error!("Real-time aggregation error: {}", e);
            }
        }
    }

    async fn process_streaming_event(&self, event: StreamingDataEvent) -> Result<()> {
        match event.event_type {
            StreamEventType::PriceUpdate => {
                self.handle_price_update(&event).await?;
            }
            StreamEventType::VolumeUpdate => {
                self.handle_volume_update(&event).await?;
            }
            StreamEventType::TradeExecution => {
                self.handle_trade_execution(&event).await?;
            }
            StreamEventType::LiquidityChange => {
                self.handle_liquidity_change(&event).await?;
            }
            _ => {
                // Handle other event types as needed
            }
        }
        Ok(())
    }

    async fn handle_price_update(&self, event: &StreamingDataEvent) -> Result<()> {
        // Extract price data from event
        if let Ok(price) = event.data.get("price").unwrap_or(&serde_json::Value::Null).as_f64() {
            let volume = event.data.get("volume").unwrap_or(&serde_json::Value::Null).as_f64().unwrap_or(0.0);
            
            let price_data = PriceDataPoint {
                timestamp: event.timestamp.timestamp(),
                symbol: event.symbol.clone(),
                price,
                volume,
                source: "blockchain".to_string(),
            };

            // Store in database
            self.mongodb_service.store_price_data(&price_data).await?;

            // Update real-time cache
            let real_time_data = RealTimeDataPoint {
                symbol: event.symbol.clone(),
                price,
                volume_24h: volume, // This should be calculated properly
                price_change_24h: 0.0, // This should be calculated properly
                last_updated: event.timestamp,
            };

            self.real_time_cache.insert(event.symbol.clone(), real_time_data);
        }

        Ok(())
    }

    async fn handle_volume_update(&self, _event: &StreamingDataEvent) -> Result<()> {
        // Handle volume updates
        Ok(())
    }

    async fn handle_trade_execution(&self, event: &StreamingDataEvent) -> Result<()> {
        // Extract trade data and update relevant metrics
        info!("Processing trade execution for symbol: {}", event.symbol);
        Ok(())
    }

    async fn handle_liquidity_change(&self, _event: &StreamingDataEvent) -> Result<()> {
        // Handle liquidity changes
        Ok(())
    }

    async fn perform_real_time_aggregation(&self) -> Result<()> {
        // Perform OHLCV aggregation for different timeframes
        let symbols = vec!["ETH".to_string(), "BTC".to_string()]; // This should be dynamic
        let timeframes = vec!["1m", "5m", "15m", "1h"];

        for symbol in &symbols {
            for timeframe in &timeframes {
                if let Err(e) = self.aggregate_ohlcv_for_symbol(symbol, timeframe).await {
                    error!("Failed to aggregate OHLCV for {} {}: {}", symbol, timeframe, e);
                }
            }
        }

        Ok(())
    }

    async fn aggregate_ohlcv_for_symbol(&self, symbol: &str, timeframe: &str) -> Result<()> {
        // Get recent price data
        let price_data = self.mongodb_service.get_price_history(symbol, timeframe, 1000).await?;
        
        if price_data.is_empty() {
            return Ok(());
        }

        // Aggregate to OHLCV
        let ohlcv_data = DataAggregator::aggregate_ohlcv(&price_data, timeframe)?;
        
        if !ohlcv_data.is_empty() {
            self.mongodb_service.store_ohlcv_data(&ohlcv_data).await?;
        }

        Ok(())
    }

    pub async fn get_price_history(&self, symbol: &str, timeframe: &str, limit: usize) -> Result<PriceHistoryResponse> {
        let data = self.mongodb_service.get_price_history(symbol, timeframe, limit).await?;
        
        Ok(PriceHistoryResponse {
            symbol: symbol.to_string(),
            timeframe: timeframe.to_string(),
            total_points: data.len(),
            data,
        })
    }

    pub async fn get_volume_analysis(&self, symbol: &str, period_hours: u64) -> Result<VolumeAnalysisResponse> {
        let volume_data = self.mongodb_service.get_volume_data(symbol, period_hours).await?;
        
        let total_volume: f64 = volume_data.iter().map(|d| d.volume).sum();
        let average_volume = if volume_data.is_empty() { 0.0 } else { total_volume / volume_data.len() as f64 };
        
        // Calculate hourly breakdown
        let mut hourly_breakdown = vec![HourlyVolumeData { hour: 0, volume: 0.0, percentage_of_total: 0.0 }; 24];
        
        for data in &volume_data {
            let hour = (data.timestamp % 86400) / 3600;
            if hour < 24 {
                hourly_breakdown[hour as usize].volume += data.volume;
            }
        }
        
        // Calculate percentages
        for hour_data in &mut hourly_breakdown {
            hour_data.percentage_of_total = if total_volume > 0.0 {
                (hour_data.volume / total_volume) * 100.0
            } else {
                0.0
            };
        }

        // Determine trend (simplified)
        let volume_trend = if volume_data.len() >= 2 {
            let recent_volume = volume_data.iter().rev().take(volume_data.len() / 2).map(|d| d.volume).sum::<f64>();
            let older_volume = volume_data.iter().take(volume_data.len() / 2).map(|d| d.volume).sum::<f64>();
            
            if recent_volume > older_volume * 1.1 {
                VolumeTrend::Increasing
            } else if recent_volume < older_volume * 0.9 {
                VolumeTrend::Decreasing
            } else {
                VolumeTrend::Stable
            }
        } else {
            VolumeTrend::Stable
        };

        Ok(VolumeAnalysisResponse {
            symbol: symbol.to_string(),
            period_hours,
            total_volume,
            average_volume,
            volume_trend,
            hourly_breakdown,
        })
    }

    pub async fn get_market_summary(&self) -> Result<MarketSummaryResponse> {
        // Try to get cached summary first
        if let Some(summary) = self.mongodb_service.get_market_summary().await? {
            // Check if summary is recent (within last 5 minutes)
            let age = Utc::now().timestamp() - summary.timestamp.timestamp();
            if age < 300 {
                return Ok(summary);
            }
        }

        // Generate new summary
        let summary = self.generate_market_summary().await?;
        self.mongodb_service.update_market_summary(&summary).await?;
        
        Ok(summary)
    }

    async fn generate_market_summary(&self) -> Result<MarketSummaryResponse> {
        // This is a simplified implementation
        Ok(MarketSummaryResponse {
            total_market_cap: 1_000_000_000.0, // Placeholder
            total_volume_24h: 10_000_000.0,    // Placeholder
            active_pairs: 100,                  // Placeholder
            top_gainers: Vec::new(),
            top_losers: Vec::new(),
            most_active: Vec::new(),
            timestamp: Utc::now(),
        })
    }

    pub async fn get_liquidity_metrics(&self, symbol: &str) -> Result<LiquidityMetricsResponse> {
        // Simplified liquidity metrics calculation
        Ok(LiquidityMetricsResponse {
            symbol: symbol.to_string(),
            total_liquidity: 1_000_000.0,  // Placeholder
            bid_ask_spread: 0.001,         // Placeholder
            depth_1_percent: 100_000.0,    // Placeholder
            depth_5_percent: 500_000.0,    // Placeholder
            liquidity_score: 0.85,         // Placeholder
            timestamp: Utc::now(),
        })
    }

    pub async fn get_ohlcv_data(&self, symbol: &str, timeframe: &str, limit: usize) -> Result<OHLCVResponse> {
        let data = self.mongodb_service.get_ohlcv_data(symbol, timeframe, limit).await?;
        
        Ok(OHLCVResponse {
            symbol: symbol.to_string(),
            timeframe: timeframe.to_string(),
            total_candles: data.len(),
            data,
        })
    }

    pub async fn get_real_time_feed(&self, symbols: &[String]) -> Result<RealTimeFeedResponse> {
        let mut data = Vec::new();
        
        for symbol in symbols {
            if let Some(real_time_data) = self.real_time_cache.get(symbol) {
                data.push(real_time_data.clone());
            }
        }

        Ok(RealTimeFeedResponse {
            symbols: symbols.to_vec(),
            data,
            timestamp: Utc::now(),
        })
    }

    pub async fn calculate_correlation(&self, request: CorrelationRequest) -> Result<CorrelationResponse> {
        let start_time = Instant::now();
        
        // Get price data for all symbols
        let mut price_data = Vec::new();
        for symbol in &request.symbols {
            let data = self.mongodb_service.get_price_history(symbol, &request.timeframe, 1000).await?;
            let prices: Vec<f64> = data.iter().map(|d| d.price).collect();
            price_data.push(prices);
        }

        // Calculate correlation matrix
        let mut correlation_matrix = vec![vec![0.0; request.symbols.len()]; request.symbols.len()];
        
        for i in 0..request.symbols.len() {
            for j in 0..request.symbols.len() {
                if i == j {
                    correlation_matrix[i][j] = 1.0;
                } else {
                    correlation_matrix[i][j] = DataAggregator::calculate_correlation(&price_data[i], &price_data[j])?;
                }
            }
        }

        info!("Correlation calculation took: {:?}", start_time.elapsed());

        Ok(CorrelationResponse {
            correlation_matrix,
            symbols: request.symbols,
            period_days: request.period_days,
            calculation_timestamp: Utc::now(),
        })
    }

    pub async fn calculate_volatility(&self, request: VolatilityRequest) -> Result<VolatilityResponse> {
        let price_data = self.mongodb_service.get_price_history(&request.symbol, "1d", request.period_days as usize).await?;
        let prices: Vec<f64> = price_data.iter().map(|d| d.price).collect();
        
        if prices.is_empty() {
            return Err(anyhow::anyhow!("No price data available for symbol: {}", request.symbol));
        }

        // Calculate returns
        let returns: Vec<f64> = prices.windows(2)
            .map(|w| (w[1] - w[0]) / w[0])
            .collect();

        // Calculate volatility (standard deviation of returns)
        let mean = returns.iter().sum::<f64>() / returns.len() as f64;
        let variance = returns.iter()
            .map(|r| (r - mean).powi(2))
            .sum::<f64>() / returns.len() as f64;
        let volatility = variance.sqrt();
        
        // Annualize volatility (assuming 365 days per year)
        let annualized_volatility = volatility * (365.0_f64).sqrt();

        Ok(VolatilityResponse {
            symbol: request.symbol,
            volatility,
            annualized_volatility,
            method: request.calculation_method,
            period_days: request.period_days,
            calculation_timestamp: Utc::now(),
        })
    }
}
