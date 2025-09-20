use serde::{Deserialize, Serialize};
use chrono::{DateTime, Utc};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PriceDataPoint {
    pub timestamp: i64,
    pub symbol: String,
    pub price: f64,
    pub volume: f64,
    pub source: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct OHLCVData {
    pub timestamp: i64,
    pub symbol: String,
    pub open: f64,
    pub high: f64,
    pub low: f64,
    pub close: f64,
    pub volume: f64,
    pub timeframe: String, // "1m", "5m", "1h", "1d", etc.
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct AggregatedData {
    pub symbol: String,
    pub data_type: AggregationType,
    pub timeframe: String,
    pub value: f64,
    pub metadata: serde_json::Value,
    pub timestamp: DateTime<Utc>,
    pub created_at: DateTime<Utc>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub enum AggregationType {
    Price,
    Volume,
    Liquidity,
    Volatility,
    MarketCap,
    TradingActivity,
    Custom(String),
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PriceHistoryResponse {
    pub symbol: String,
    pub timeframe: String,
    pub data: Vec<PriceDataPoint>,
    pub total_points: usize,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct VolumeAnalysisResponse {
    pub symbol: String,
    pub period_hours: u64,
    pub total_volume: f64,
    pub average_volume: f64,
    pub volume_trend: VolumeTrend,
    pub hourly_breakdown: Vec<HourlyVolumeData>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub enum VolumeTrend {
    Increasing,
    Decreasing,
    Stable,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct HourlyVolumeData {
    pub hour: u8,
    pub volume: f64,
    pub percentage_of_total: f64,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct MarketSummaryResponse {
    pub total_market_cap: f64,
    pub total_volume_24h: f64,
    pub active_pairs: u32,
    pub top_gainers: Vec<TokenPerformance>,
    pub top_losers: Vec<TokenPerformance>,
    pub most_active: Vec<TokenActivity>,
    pub timestamp: DateTime<Utc>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct TokenPerformance {
    pub symbol: String,
    pub current_price: f64,
    pub price_change_24h: f64,
    pub price_change_percentage_24h: f64,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct TokenActivity {
    pub symbol: String,
    pub volume_24h: f64,
    pub trades_count: u64,
    pub unique_traders: u64,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct LiquidityMetricsResponse {
    pub symbol: String,
    pub total_liquidity: f64,
    pub bid_ask_spread: f64,
    pub depth_1_percent: f64,
    pub depth_5_percent: f64,
    pub liquidity_score: f64,
    pub timestamp: DateTime<Utc>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct OHLCVResponse {
    pub symbol: String,
    pub timeframe: String,
    pub data: Vec<OHLCVData>,
    pub total_candles: usize,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct RealTimeFeedResponse {
    pub symbols: Vec<String>,
    pub data: Vec<RealTimeDataPoint>,
    pub timestamp: DateTime<Utc>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct RealTimeDataPoint {
    pub symbol: String,
    pub price: f64,
    pub volume_24h: f64,
    pub price_change_24h: f64,
    pub last_updated: DateTime<Utc>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct CorrelationRequest {
    pub symbols: Vec<String>,
    pub timeframe: String,
    pub period_days: u32,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct CorrelationResponse {
    pub correlation_matrix: Vec<Vec<f64>>,
    pub symbols: Vec<String>,
    pub period_days: u32,
    pub calculation_timestamp: DateTime<Utc>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct VolatilityRequest {
    pub symbol: String,
    pub period_days: u32,
    pub calculation_method: VolatilityMethod,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub enum VolatilityMethod {
    StandardDeviation,
    EWMA, // Exponentially Weighted Moving Average
    GARCH, // Generalized Autoregressive Conditional Heteroskedasticity
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct VolatilityResponse {
    pub symbol: String,
    pub volatility: f64,
    pub annualized_volatility: f64,
    pub method: VolatilityMethod,
    pub period_days: u32,
    pub calculation_timestamp: DateTime<Utc>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct StreamingDataEvent {
    pub event_type: StreamEventType,
    pub symbol: String,
    pub data: serde_json::Value,
    pub timestamp: DateTime<Utc>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub enum StreamEventType {
    PriceUpdate,
    VolumeUpdate,
    TradeExecution,
    LiquidityChange,
    NewBlock,
    ContractEvent,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct AggregationTask {
    pub task_id: String,
    pub task_type: AggregationTaskType,
    pub symbol: String,
    pub timeframe: String,
    pub start_time: DateTime<Utc>,
    pub end_time: DateTime<Utc>,
    pub parameters: serde_json::Value,
    pub created_at: DateTime<Utc>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub enum AggregationTaskType {
    OHLCV,
    VolumeProfile,
    LiquidityMetrics,
    CorrelationAnalysis,
    VolatilityCalculation,
    CustomAnalysis,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct AggregationResult {
    pub task_id: String,
    pub success: bool,
    pub result: Option<serde_json::Value>,
    pub error: Option<String>,
    pub processing_time_ms: u64,
    pub processed_at: DateTime<Utc>,
}
