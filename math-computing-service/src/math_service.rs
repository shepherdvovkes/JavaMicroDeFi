use anyhow::Result;
use chrono::Utc;
use log::{error, info};
use std::time::Instant;

use crate::calculations::FinancialCalculations;
use crate::kafka_consumer::KafkaConsumerService;
use crate::models::*;

#[derive(Clone)]
pub struct MathComputingService {
    kafka_consumer: KafkaConsumerService,
}

impl MathComputingService {
    pub async fn new(kafka_brokers: &str) -> Result<Self> {
        let kafka_consumer = KafkaConsumerService::new(kafka_brokers, "math-computing-group")?;

        Ok(Self {
            kafka_consumer,
        })
    }

    pub async fn start_consumer(&self) -> Result<()> {
        self.kafka_consumer.subscribe_to_computation_requests().await?;
        
        let service = self.clone();
        self.kafka_consumer.start_consuming(move |task| {
            tokio::runtime::Handle::current().block_on(async {
                service.process_computation_task(task).await
            })
        }).await
    }

    pub async fn calculate_option_price(&self, request: OptionPriceRequest) -> Result<OptionPriceResponse> {
        let start_time = Instant::now();
        
        let dividend_yield = request.dividend_yield.unwrap_or(0.0);
        
        let price = FinancialCalculations::black_scholes_price(
            request.spot_price,
            request.strike_price,
            request.time_to_expiry,
            request.risk_free_rate,
            request.volatility,
            &request.option_type,
            dividend_yield,
        )?;

        let (delta, gamma, theta, vega, rho) = FinancialCalculations::calculate_greeks(
            request.spot_price,
            request.strike_price,
            request.time_to_expiry,
            request.risk_free_rate,
            request.volatility,
            &request.option_type,
            dividend_yield,
        )?;

        let calculation_time_ms = start_time.elapsed().as_millis() as u64;

        Ok(OptionPriceResponse {
            price,
            delta,
            gamma,
            theta,
            vega,
            rho,
            calculation_time_ms,
        })
    }

    pub async fn calculate_arbitrage_opportunity(&self, request: ArbitrageRequest) -> Result<ArbitrageResponse> {
        let start_time = Instant::now();
        
        let mut result = FinancialCalculations::calculate_arbitrage(
            &request.exchanges,
            request.amount,
            request.max_slippage,
        )?;

        // Subtract gas costs from net profit
        let total_gas_cost: f64 = request.gas_costs.iter().sum();
        result.estimated_gas_cost = total_gas_cost;
        result.net_profit = result.profit_amount - total_gas_cost;
        result.calculation_time_ms = start_time.elapsed().as_millis() as u64;

        Ok(result)
    }

    pub async fn optimize_portfolio(&self, request: PortfolioOptimizationRequest) -> Result<PortfolioOptimizationResponse> {
        let start_time = Instant::now();
        
        let optimal_weights = FinancialCalculations::optimize_portfolio(
            &request.expected_returns,
            &request.covariance_matrix,
            request.risk_tolerance,
        )?;

        // Calculate expected return and risk
        let expected_return: f64 = optimal_weights.iter()
            .zip(request.expected_returns.iter())
            .map(|(w, r)| w * r)
            .sum();

        // Calculate portfolio variance
        let mut portfolio_variance = 0.0;
        for i in 0..optimal_weights.len() {
            for j in 0..optimal_weights.len() {
                portfolio_variance += optimal_weights[i] * optimal_weights[j] * request.covariance_matrix[i][j];
            }
        }
        let expected_risk = portfolio_variance.sqrt();

        // Calculate Sharpe ratio (assuming risk-free rate of 0 for simplicity)
        let sharpe_ratio = if expected_risk > 0.0 { expected_return / expected_risk } else { 0.0 };

        // Calculate diversification ratio (simplified)
        let avg_volatility: f64 = request.covariance_matrix.iter()
            .map(|row| row[0].sqrt()) // Diagonal elements are variances
            .sum::<f64>() / request.covariance_matrix.len() as f64;
        let diversification_ratio = if expected_risk > 0.0 { avg_volatility / expected_risk } else { 1.0 };

        let calculation_time_ms = start_time.elapsed().as_millis() as u64;

        Ok(PortfolioOptimizationResponse {
            optimal_weights,
            expected_return,
            expected_risk,
            sharpe_ratio,
            diversification_ratio,
            calculation_time_ms,
        })
    }

    pub async fn calculate_risk_metrics(&self, request: RiskMetricsRequest) -> Result<RiskMetricsResponse> {
        let start_time = Instant::now();
        
        // Calculate returns from portfolio values
        let returns: Vec<f64> = request.portfolio_values
            .windows(2)
            .map(|window| (window[1] - window[0]) / window[0])
            .collect();

        let value_at_risk = FinancialCalculations::calculate_var(&returns, request.confidence_level)?;
        let conditional_var = FinancialCalculations::calculate_cvar(&returns, request.confidence_level)?;
        let max_drawdown = FinancialCalculations::calculate_max_drawdown(&request.portfolio_values);
        let volatility = FinancialCalculations::calculate_volatility(&returns);

        // Calculate skewness and kurtosis
        let mean_return = returns.iter().sum::<f64>() / returns.len() as f64;
        let variance = returns.iter().map(|r| (r - mean_return).powi(2)).sum::<f64>() / returns.len() as f64;
        let std_dev = variance.sqrt();

        let skewness = if std_dev > 0.0 {
            returns.iter().map(|r| ((r - mean_return) / std_dev).powi(3)).sum::<f64>() / returns.len() as f64
        } else {
            0.0
        };

        let kurtosis = if std_dev > 0.0 {
            returns.iter().map(|r| ((r - mean_return) / std_dev).powi(4)).sum::<f64>() / returns.len() as f64 - 3.0
        } else {
            0.0
        };

        let calculation_time_ms = start_time.elapsed().as_millis() as u64;

        Ok(RiskMetricsResponse {
            value_at_risk,
            conditional_var,
            max_drawdown,
            volatility,
            skewness,
            kurtosis,
            beta: None, // Would need market data to calculate
            calculation_time_ms,
        })
    }

    pub async fn calculate_yield_farming_returns(&self, request: YieldFarmingRequest) -> Result<YieldFarmingResponse> {
        let start_time = Instant::now();
        
        let mut result = FinancialCalculations::calculate_yield_farming_returns(
            request.amount_a,
            request.amount_b,
            request.apr,
            request.pool_fee,
            request.time_period,
        )?;

        result.calculation_time_ms = start_time.elapsed().as_millis() as u64;
        Ok(result)
    }

    pub async fn calculate_impermanent_loss(&self, request: ImpermanentLossRequest) -> Result<ImpermanentLossResponse> {
        let start_time = Instant::now();
        
        let mut result = FinancialCalculations::calculate_impermanent_loss(
            request.initial_price_a,
            request.initial_price_b,
            request.current_price_a,
            request.current_price_b,
            request.initial_amount_a,
            request.initial_amount_b,
        )?;

        result.calculation_time_ms = start_time.elapsed().as_millis() as u64;
        Ok(result)
    }

    async fn process_computation_task(&self, task: MathComputationTask) -> Result<ComputationResult> {
        let start_time = Instant::now();
        info!("Processing computation task: {} of type: {:?}", task.task_id, task.task_type);

        let result = match task.task_type {
            ComputationTaskType::OptionPricing => {
                match serde_json::from_value::<OptionPriceRequest>(task.payload) {
                    Ok(request) => {
                        match self.calculate_option_price(request).await {
                            Ok(response) => Some(serde_json::to_value(response)?),
                            Err(e) => return Ok(ComputationResult {
                                task_id: task.task_id,
                                success: false,
                                result: None,
                                error: Some(e.to_string()),
                                computation_time_ms: start_time.elapsed().as_millis() as u64,
                                processed_at: Utc::now(),
                            }),
                        }
                    }
                    Err(e) => return Ok(ComputationResult {
                        task_id: task.task_id,
                        success: false,
                        result: None,
                        error: Some(format!("Failed to parse request: {}", e)),
                        computation_time_ms: start_time.elapsed().as_millis() as u64,
                        processed_at: Utc::now(),
                    }),
                }
            }
            ComputationTaskType::ArbitrageCalculation => {
                match serde_json::from_value::<ArbitrageRequest>(task.payload) {
                    Ok(request) => {
                        match self.calculate_arbitrage_opportunity(request).await {
                            Ok(response) => Some(serde_json::to_value(response)?),
                            Err(e) => return Ok(ComputationResult {
                                task_id: task.task_id,
                                success: false,
                                result: None,
                                error: Some(e.to_string()),
                                computation_time_ms: start_time.elapsed().as_millis() as u64,
                                processed_at: Utc::now(),
                            }),
                        }
                    }
                    Err(e) => return Ok(ComputationResult {
                        task_id: task.task_id,
                        success: false,
                        result: None,
                        error: Some(format!("Failed to parse request: {}", e)),
                        computation_time_ms: start_time.elapsed().as_millis() as u64,
                        processed_at: Utc::now(),
                    }),
                }
            }
            _ => {
                return Ok(ComputationResult {
                    task_id: task.task_id,
                    success: false,
                    result: None,
                    error: Some("Computation type not implemented".to_string()),
                    computation_time_ms: start_time.elapsed().as_millis() as u64,
                    processed_at: Utc::now(),
                });
            }
        };

        Ok(ComputationResult {
            task_id: task.task_id,
            success: true,
            result,
            error: None,
            computation_time_ms: start_time.elapsed().as_millis() as u64,
            processed_at: Utc::now(),
        })
    }
}
