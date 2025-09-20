use serde::{Deserialize, Serialize};
use chrono::{DateTime, Utc};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct OptionPriceRequest {
    pub option_type: OptionType,
    pub spot_price: f64,
    pub strike_price: f64,
    pub time_to_expiry: f64, // in years
    pub risk_free_rate: f64,
    pub volatility: f64,
    pub dividend_yield: Option<f64>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub enum OptionType {
    Call,
    Put,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct OptionPriceResponse {
    pub price: f64,
    pub delta: f64,
    pub gamma: f64,
    pub theta: f64,
    pub vega: f64,
    pub rho: f64,
    pub calculation_time_ms: u64,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ArbitrageRequest {
    pub exchanges: Vec<ExchangeData>,
    pub token_pair: TokenPair,
    pub amount: f64,
    pub max_slippage: f64,
    pub gas_costs: Vec<f64>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ExchangeData {
    pub name: String,
    pub price: f64,
    pub liquidity: f64,
    pub fee: f64,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct TokenPair {
    pub base: String,
    pub quote: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ArbitrageResponse {
    pub profitable: bool,
    pub profit_amount: f64,
    pub profit_percentage: f64,
    pub optimal_path: Vec<ArbitrageStep>,
    pub estimated_gas_cost: f64,
    pub net_profit: f64,
    pub calculation_time_ms: u64,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ArbitrageStep {
    pub exchange: String,
    pub action: String, // "buy" or "sell"
    pub amount: f64,
    pub price: f64,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PortfolioOptimizationRequest {
    pub assets: Vec<AssetData>,
    pub expected_returns: Vec<f64>,
    pub covariance_matrix: Vec<Vec<f64>>,
    pub risk_tolerance: f64,
    pub constraints: PortfolioConstraints,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct AssetData {
    pub symbol: String,
    pub current_price: f64,
    pub market_cap: f64,
    pub daily_volume: f64,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PortfolioConstraints {
    pub min_weight: f64,
    pub max_weight: f64,
    pub max_assets: Option<usize>,
    pub sector_limits: Option<std::collections::HashMap<String, f64>>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PortfolioOptimizationResponse {
    pub optimal_weights: Vec<f64>,
    pub expected_return: f64,
    pub expected_risk: f64,
    pub sharpe_ratio: f64,
    pub diversification_ratio: f64,
    pub calculation_time_ms: u64,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct RiskMetricsRequest {
    pub portfolio_values: Vec<f64>,
    pub confidence_level: f64,
    pub time_horizon: u32, // in days
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct RiskMetricsResponse {
    pub value_at_risk: f64,
    pub conditional_var: f64,
    pub max_drawdown: f64,
    pub volatility: f64,
    pub skewness: f64,
    pub kurtosis: f64,
    pub beta: Option<f64>,
    pub calculation_time_ms: u64,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct YieldFarmingRequest {
    pub pool_address: String,
    pub token_a: String,
    pub token_b: String,
    pub amount_a: f64,
    pub amount_b: f64,
    pub apr: f64,
    pub pool_fee: f64,
    pub time_period: u32, // in days
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct YieldFarmingResponse {
    pub projected_yield: f64,
    pub fee_earnings: f64,
    pub reward_tokens: f64,
    pub total_return: f64,
    pub annualized_return: f64,
    pub break_even_days: u32,
    pub calculation_time_ms: u64,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ImpermanentLossRequest {
    pub initial_price_a: f64,
    pub initial_price_b: f64,
    pub current_price_a: f64,
    pub current_price_b: f64,
    pub initial_amount_a: f64,
    pub initial_amount_b: f64,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ImpermanentLossResponse {
    pub impermanent_loss_percentage: f64,
    pub impermanent_loss_amount: f64,
    pub current_pool_value: f64,
    pub hodl_value: f64,
    pub fee_compensation_needed: f64,
    pub calculation_time_ms: u64,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct MathComputationTask {
    pub task_id: String,
    pub task_type: ComputationTaskType,
    pub payload: serde_json::Value,
    pub priority: TaskPriority,
    pub created_at: DateTime<Utc>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub enum ComputationTaskType {
    OptionPricing,
    ArbitrageCalculation,
    PortfolioOptimization,
    RiskMetrics,
    YieldFarming,
    ImpermanentLoss,
    CustomCalculation,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub enum TaskPriority {
    Low,
    Normal,
    High,
    Critical,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ComputationResult {
    pub task_id: String,
    pub success: bool,
    pub result: Option<serde_json::Value>,
    pub error: Option<String>,
    pub computation_time_ms: u64,
    pub processed_at: DateTime<Utc>,
}
