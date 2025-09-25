use anyhow::Result;
use std::collections::HashMap;

use crate::models::*;

pub struct DataAggregator;

impl DataAggregator {
    pub fn aggregate_ohlcv(price_data: &[PriceDataPoint], timeframe: &str) -> Result<Vec<OHLCVData>> {
        let mut ohlcv_data = Vec::new();
        let interval_ms = Self::timeframe_to_ms(timeframe)?;
        
        // Group price data by time intervals
        let mut grouped_data: HashMap<i64, Vec<&PriceDataPoint>> = HashMap::new();
        
        for point in price_data {
            let interval_start = (point.timestamp / interval_ms) * interval_ms;
            grouped_data.entry(interval_start).or_insert_with(Vec::new).push(point);
        }
        
        for (timestamp, points) in grouped_data {
            if points.is_empty() {
                continue;
            }
            
            // Sort by timestamp to ensure proper OHLC calculation
            let mut sorted_points = points;
            sorted_points.sort_by_key(|p| p.timestamp);
            
            let open = sorted_points[0].price;
            let close = sorted_points[sorted_points.len() - 1].price;
            let high = sorted_points.iter().map(|p| p.price).fold(f64::NEG_INFINITY, f64::max);
            let low = sorted_points.iter().map(|p| p.price).fold(f64::INFINITY, f64::min);
            let volume = sorted_points.iter().map(|p| p.volume).sum();
            
            ohlcv_data.push(OHLCVData {
                timestamp,
                symbol: sorted_points[0].symbol.clone(),
                open,
                high,
                low,
                close,
                volume,
                timeframe: timeframe.to_string(),
            });
        }
        
        ohlcv_data.sort_by_key(|d| d.timestamp);
        Ok(ohlcv_data)
    }
    
    pub fn calculate_volume_profile(price_data: &[PriceDataPoint], price_levels: u32) -> Result<Vec<(f64, f64)>> {
        if price_data.is_empty() {
            return Ok(Vec::new());
        }
        
        let min_price = price_data.iter().map(|p| p.price).fold(f64::INFINITY, f64::min);
        let max_price = price_data.iter().map(|p| p.price).fold(f64::NEG_INFINITY, f64::max);
        let price_step = (max_price - min_price) / price_levels as f64;
        
        let mut volume_profile = vec![0.0; price_levels as usize];
        
        for point in price_data {
            let level = ((point.price - min_price) / price_step).floor() as usize;
            let level = level.min(price_levels as usize - 1);
            volume_profile[level] += point.volume;
        }
        
        let result: Vec<(f64, f64)> = volume_profile
            .into_iter()
            .enumerate()
            .map(|(i, volume)| (min_price + i as f64 * price_step, volume))
            .collect();
        
        Ok(result)
    }
    
    pub fn calculate_moving_average(prices: &[f64], window: usize) -> Vec<f64> {
        if prices.len() < window {
            return Vec::new();
        }
        
        let mut moving_averages = Vec::new();
        
        for i in window - 1..prices.len() {
            let sum: f64 = prices[i - window + 1..=i].iter().sum();
            let avg = sum / window as f64;
            moving_averages.push(avg);
        }
        
        moving_averages
    }
    
    pub fn calculate_volatility(prices: &[f64], window: usize) -> Vec<f64> {
        if prices.len() < window + 1 {
            return Vec::new();
        }
        
        // Calculate returns
        let returns: Vec<f64> = prices.windows(2)
            .map(|w| (w[1] - w[0]) / w[0])
            .collect();
        
        let mut volatilities = Vec::new();
        
        for i in window - 1..returns.len() {
            let window_returns = &returns[i - window + 1..=i];
            let mean = window_returns.iter().sum::<f64>() / window as f64;
            let variance = window_returns.iter()
                .map(|r| (r - mean).powi(2))
                .sum::<f64>() / window as f64;
            let volatility = variance.sqrt();
            volatilities.push(volatility);
        }
        
        volatilities
    }
    
    pub fn calculate_correlation(prices1: &[f64], prices2: &[f64]) -> Result<f64> {
        if prices1.len() != prices2.len() || prices1.is_empty() {
            return Err(anyhow::anyhow!("Price arrays must have the same non-zero length"));
        }
        
        let n = prices1.len() as f64;
        let mean1 = prices1.iter().sum::<f64>() / n;
        let mean2 = prices2.iter().sum::<f64>() / n;
        
        let numerator: f64 = prices1.iter()
            .zip(prices2.iter())
            .map(|(p1, p2)| (p1 - mean1) * (p2 - mean2))
            .sum();
        
        let sum_sq1: f64 = prices1.iter().map(|p| (p - mean1).powi(2)).sum();
        let sum_sq2: f64 = prices2.iter().map(|p| (p - mean2).powi(2)).sum();
        
        let denominator = (sum_sq1 * sum_sq2).sqrt();
        
        if denominator == 0.0 {
            return Ok(0.0);
        }
        
        Ok(numerator / denominator)
    }
    
    pub fn calculate_rsi(prices: &[f64], period: usize) -> Vec<f64> {
        if prices.len() < period + 1 {
            return Vec::new();
        }
        
        let changes: Vec<f64> = prices.windows(2)
            .map(|w| w[1] - w[0])
            .collect();
        
        let mut rsi_values = Vec::new();
        
        for i in period - 1..changes.len() {
            let window_changes = &changes[i - period + 1..=i];
            
            let gains: f64 = window_changes.iter().filter(|&&x| x > 0.0).sum();
            let losses: f64 = window_changes.iter().filter(|&&x| x < 0.0).map(|x| -x).sum();
            
            let avg_gain = gains / period as f64;
            let avg_loss = losses / period as f64;
            
            let rsi = if avg_loss == 0.0 {
                100.0
            } else {
                let rs = avg_gain / avg_loss;
                100.0 - (100.0 / (1.0 + rs))
            };
            
            rsi_values.push(rsi);
        }
        
        rsi_values
    }
    
    pub fn detect_support_resistance(prices: &[f64], window: usize, threshold: f64) -> (Vec<f64>, Vec<f64>) {
        let mut support_levels = Vec::new();
        let mut resistance_levels = Vec::new();
        
        if prices.len() < window * 2 + 1 {
            return (support_levels, resistance_levels);
        }
        
        for i in window..prices.len() - window {
            let current_price = prices[i];
            let left_window = &prices[i - window..i];
            let right_window = &prices[i + 1..i + window + 1];
            
            // Check for local minimum (support)
            let is_local_min = left_window.iter().all(|&p| current_price <= p + threshold) &&
                             right_window.iter().all(|&p| current_price <= p + threshold);
            
            // Check for local maximum (resistance)
            let is_local_max = left_window.iter().all(|&p| current_price >= p - threshold) &&
                             right_window.iter().all(|&p| current_price >= p - threshold);
            
            if is_local_min {
                support_levels.push(current_price);
            } else if is_local_max {
                resistance_levels.push(current_price);
            }
        }
        
        (support_levels, resistance_levels)
    }
    
    fn timeframe_to_ms(timeframe: &str) -> Result<i64> {
        match timeframe {
            "1m" => Ok(60 * 1000),
            "5m" => Ok(5 * 60 * 1000),
            "15m" => Ok(15 * 60 * 1000),
            "30m" => Ok(30 * 60 * 1000),
            "1h" => Ok(60 * 60 * 1000),
            "4h" => Ok(4 * 60 * 60 * 1000),
            "1d" => Ok(24 * 60 * 60 * 1000),
            "1w" => Ok(7 * 24 * 60 * 60 * 1000),
            _ => Err(anyhow::anyhow!("Unsupported timeframe: {}", timeframe)),
        }
    }
}
