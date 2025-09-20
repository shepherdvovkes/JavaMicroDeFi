use anyhow::Result;
use nalgebra::{DMatrix, DVector};
use statrs::distribution::{Normal, ContinuousCDF};
use rayon::prelude::*;
use std::f64::consts::{E, PI};

use crate::models::*;

pub struct FinancialCalculations;

impl FinancialCalculations {
    /// Black-Scholes option pricing
    pub fn black_scholes_price(
        spot: f64,
        strike: f64,
        time_to_expiry: f64,
        risk_free_rate: f64,
        volatility: f64,
        option_type: &OptionType,
        dividend_yield: f64,
    ) -> Result<f64> {
        let d1 = (spot.ln() - strike.ln() + (risk_free_rate - dividend_yield + 0.5 * volatility.powi(2)) * time_to_expiry) 
            / (volatility * time_to_expiry.sqrt());
        let d2 = d1 - volatility * time_to_expiry.sqrt();

        let normal = Normal::new(0.0, 1.0)?;
        let nd1 = normal.cdf(d1);
        let nd2 = normal.cdf(d2);
        let n_minus_d1 = normal.cdf(-d1);
        let n_minus_d2 = normal.cdf(-d2);

        let price = match option_type {
            OptionType::Call => {
                spot * (-dividend_yield * time_to_expiry).exp() * nd1 
                - strike * (-risk_free_rate * time_to_expiry).exp() * nd2
            },
            OptionType::Put => {
                strike * (-risk_free_rate * time_to_expiry).exp() * n_minus_d2 
                - spot * (-dividend_yield * time_to_expiry).exp() * n_minus_d1
            }
        };

        Ok(price)
    }

    /// Calculate Greeks for options
    pub fn calculate_greeks(
        spot: f64,
        strike: f64,
        time_to_expiry: f64,
        risk_free_rate: f64,
        volatility: f64,
        option_type: &OptionType,
        dividend_yield: f64,
    ) -> Result<(f64, f64, f64, f64, f64)> { // (delta, gamma, theta, vega, rho)
        let d1 = (spot.ln() - strike.ln() + (risk_free_rate - dividend_yield + 0.5 * volatility.powi(2)) * time_to_expiry) 
            / (volatility * time_to_expiry.sqrt());
        let d2 = d1 - volatility * time_to_expiry.sqrt();

        let normal = Normal::new(0.0, 1.0)?;
        let nd1 = normal.cdf(d1);
        let nd2 = normal.cdf(d2);
        let n_minus_d1 = normal.cdf(-d1);
        let n_minus_d2 = normal.cdf(-d2);
        
        // Standard normal probability density function
        let phi_d1 = (-0.5 * d1.powi(2)).exp() / (2.0 * PI).sqrt();

        let delta = match option_type {
            OptionType::Call => (-dividend_yield * time_to_expiry).exp() * nd1,
            OptionType::Put => (-dividend_yield * time_to_expiry).exp() * (nd1 - 1.0),
        };

        let gamma = (-dividend_yield * time_to_expiry).exp() * phi_d1 / (spot * volatility * time_to_expiry.sqrt());

        let theta = match option_type {
            OptionType::Call => {
                -spot * phi_d1 * volatility * (-dividend_yield * time_to_expiry).exp() / (2.0 * time_to_expiry.sqrt())
                - risk_free_rate * strike * (-risk_free_rate * time_to_expiry).exp() * nd2
                + dividend_yield * spot * (-dividend_yield * time_to_expiry).exp() * nd1
            },
            OptionType::Put => {
                -spot * phi_d1 * volatility * (-dividend_yield * time_to_expiry).exp() / (2.0 * time_to_expiry.sqrt())
                + risk_free_rate * strike * (-risk_free_rate * time_to_expiry).exp() * n_minus_d2
                - dividend_yield * spot * (-dividend_yield * time_to_expiry).exp() * n_minus_d1
            }
        };

        let vega = spot * (-dividend_yield * time_to_expiry).exp() * phi_d1 * time_to_expiry.sqrt();

        let rho = match option_type {
            OptionType::Call => strike * time_to_expiry * (-risk_free_rate * time_to_expiry).exp() * nd2,
            OptionType::Put => -strike * time_to_expiry * (-risk_free_rate * time_to_expiry).exp() * n_minus_d2,
        };

        Ok((delta, gamma, theta / 365.0, vega / 100.0, rho / 100.0)) // Convert to daily theta, percentage vega and rho
    }

    /// Calculate arbitrage opportunity
    pub fn calculate_arbitrage(exchanges: &[ExchangeData], amount: f64, max_slippage: f64) -> Result<ArbitrageResponse> {
        let mut best_profit = 0.0;
        let mut optimal_path = Vec::new();
        let mut profitable = false;

        // Find the exchange with lowest price (to buy) and highest price (to sell)
        let min_exchange = exchanges.iter().min_by(|a, b| a.price.partial_cmp(&b.price).unwrap()).unwrap();
        let max_exchange = exchanges.iter().max_by(|a, b| a.price.partial_cmp(&b.price).unwrap()).unwrap();

        if min_exchange.name != max_exchange.name {
            // Calculate slippage impact
            let buy_slippage = Self::calculate_slippage(amount, min_exchange.liquidity);
            let sell_slippage = Self::calculate_slippage(amount, max_exchange.liquidity);

            if buy_slippage <= max_slippage && sell_slippage <= max_slippage {
                let buy_price = min_exchange.price * (1.0 + buy_slippage + min_exchange.fee);
                let sell_price = max_exchange.price * (1.0 - sell_slippage - max_exchange.fee);

                let profit = (sell_price - buy_price) * amount;
                let profit_percentage = (profit / (buy_price * amount)) * 100.0;

                if profit > 0.0 {
                    profitable = true;
                    best_profit = profit;

                    optimal_path = vec![
                        ArbitrageStep {
                            exchange: min_exchange.name.clone(),
                            action: "buy".to_string(),
                            amount,
                            price: buy_price,
                        },
                        ArbitrageStep {
                            exchange: max_exchange.name.clone(),
                            action: "sell".to_string(),
                            amount,
                            price: sell_price,
                        },
                    ];
                }
            }
        }

        Ok(ArbitrageResponse {
            profitable,
            profit_amount: best_profit,
            profit_percentage: if profitable { (best_profit / (amount * min_exchange.price)) * 100.0 } else { 0.0 },
            optimal_path,
            estimated_gas_cost: 0.01, // Placeholder
            net_profit: best_profit - 0.01, // Subtract gas cost
            calculation_time_ms: 0, // Will be set by caller
        })
    }

    fn calculate_slippage(amount: f64, liquidity: f64) -> f64 {
        // Simple slippage model: slippage increases quadratically with trade size relative to liquidity
        let ratio = amount / liquidity;
        ratio.powi(2) * 0.1 // Max 10% slippage when trade size equals liquidity
    }

    /// Portfolio optimization using Mean-Variance Optimization
    pub fn optimize_portfolio(
        expected_returns: &[f64],
        covariance_matrix: &[Vec<f64>],
        risk_tolerance: f64,
    ) -> Result<Vec<f64>> {
        let n = expected_returns.len();
        
        // Convert to nalgebra matrices
        let returns = DVector::from_vec(expected_returns.to_vec());
        let mut cov_data = Vec::new();
        for row in covariance_matrix {
            cov_data.extend_from_slice(row);
        }
        let covariance = DMatrix::from_vec(n, n, cov_data);

        // Simple mean-variance optimization
        // w = (λΣ^(-1)μ) / (1^T Σ^(-1) μ) where λ is risk tolerance
        let cov_inv = covariance.try_inverse().ok_or_else(|| anyhow::anyhow!("Covariance matrix is not invertible"))?;
        let ones = DVector::from_element(n, 1.0);
        
        let numerator = &cov_inv * &returns * risk_tolerance;
        let denominator = ones.transpose() * &cov_inv * &returns;
        
        let weights = numerator / denominator[(0, 0)];
        
        // Normalize weights to sum to 1
        let weight_sum: f64 = weights.iter().sum();
        let normalized_weights: Vec<f64> = weights.iter().map(|w| w / weight_sum).collect();

        Ok(normalized_weights)
    }

    /// Calculate Value at Risk (VaR)
    pub fn calculate_var(returns: &[f64], confidence_level: f64) -> Result<f64> {
        let mut sorted_returns = returns.to_vec();
        sorted_returns.sort_by(|a, b| a.partial_cmp(b).unwrap());
        
        let index = ((1.0 - confidence_level) * sorted_returns.len() as f64).floor() as usize;
        Ok(-sorted_returns[index.min(sorted_returns.len() - 1)])
    }

    /// Calculate Conditional Value at Risk (CVaR)
    pub fn calculate_cvar(returns: &[f64], confidence_level: f64) -> Result<f64> {
        let var = Self::calculate_var(returns, confidence_level)?;
        let tail_returns: Vec<f64> = returns.iter()
            .filter(|&&r| r <= -var)
            .cloned()
            .collect();
        
        if tail_returns.is_empty() {
            return Ok(var);
        }
        
        let cvar = -tail_returns.iter().sum::<f64>() / tail_returns.len() as f64;
        Ok(cvar)
    }

    /// Calculate maximum drawdown
    pub fn calculate_max_drawdown(values: &[f64]) -> f64 {
        let mut max_drawdown = 0.0;
        let mut peak = values[0];
        
        for &value in values.iter().skip(1) {
            if value > peak {
                peak = value;
            } else {
                let drawdown = (peak - value) / peak;
                if drawdown > max_drawdown {
                    max_drawdown = drawdown;
                }
            }
        }
        
        max_drawdown
    }

    /// Calculate volatility (standard deviation of returns)
    pub fn calculate_volatility(returns: &[f64]) -> f64 {
        let mean = returns.iter().sum::<f64>() / returns.len() as f64;
        let variance = returns.iter()
            .map(|r| (r - mean).powi(2))
            .sum::<f64>() / (returns.len() - 1) as f64;
        variance.sqrt()
    }

    /// Calculate yield farming returns
    pub fn calculate_yield_farming_returns(
        amount_a: f64,
        amount_b: f64,
        apr: f64,
        pool_fee: f64,
        days: u32,
    ) -> Result<YieldFarmingResponse> {
        let total_value = amount_a + amount_b;
        let time_fraction = days as f64 / 365.0;
        
        // Calculate reward tokens based on APR
        let reward_tokens = total_value * apr * time_fraction;
        
        // Calculate fee earnings (simplified)
        let fee_earnings = total_value * pool_fee * time_fraction * 0.1; // Assume 10% of fees go to LPs
        
        let total_return = reward_tokens + fee_earnings;
        let projected_yield = total_return / total_value;
        let annualized_return = projected_yield / time_fraction;
        
        // Calculate break-even days (simplified)
        let break_even_days = if annualized_return > 0.0 {
            (1.0 / annualized_return * 365.0) as u32
        } else {
            u32::MAX
        };

        Ok(YieldFarmingResponse {
            projected_yield,
            fee_earnings,
            reward_tokens,
            total_return,
            annualized_return,
            break_even_days,
            calculation_time_ms: 0, // Will be set by caller
        })
    }

    /// Calculate impermanent loss
    pub fn calculate_impermanent_loss(
        initial_price_a: f64,
        initial_price_b: f64,
        current_price_a: f64,
        current_price_b: f64,
        initial_amount_a: f64,
        initial_amount_b: f64,
    ) -> Result<ImpermanentLossResponse> {
        // Calculate price ratio changes
        let price_ratio_initial = initial_price_a / initial_price_b;
        let price_ratio_current = current_price_a / current_price_b;
        let price_change_ratio = price_ratio_current / price_ratio_initial;
        
        // Calculate current amounts in the pool (constant product formula)
        let k = initial_amount_a * initial_amount_b; // Constant product
        let current_amount_a = (k / price_change_ratio).sqrt();
        let current_amount_b = k / current_amount_a;
        
        // Calculate current pool value
        let current_pool_value = current_amount_a * current_price_a + current_amount_b * current_price_b;
        
        // Calculate HODL value
        let hodl_value = initial_amount_a * current_price_a + initial_amount_b * current_price_b;
        
        // Calculate impermanent loss
        let impermanent_loss_amount = hodl_value - current_pool_value;
        let impermanent_loss_percentage = (impermanent_loss_amount / hodl_value) * 100.0;
        
        // Calculate fee compensation needed to break even
        let fee_compensation_needed = impermanent_loss_amount;

        Ok(ImpermanentLossResponse {
            impermanent_loss_percentage,
            impermanent_loss_amount,
            current_pool_value,
            hodl_value,
            fee_compensation_needed,
            calculation_time_ms: 0, // Will be set by caller
        })
    }
}
