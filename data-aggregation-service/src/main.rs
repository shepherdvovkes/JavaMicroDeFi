use anyhow::Result;
use axum::{
    extract::{Query, State},
    http::StatusCode,
    response::Json,
    routing::{get, post},
    Router,
};
use log::{error, info};
use std::collections::HashMap;
use std::env;
use std::sync::Arc;
use tokio::net::TcpListener;
use tower_http::cors::CorsLayer;

mod models;
mod aggregation;
mod kafka_consumer;
mod mongodb_client;
mod data_service;

use models::*;
use aggregation::*;
use kafka_consumer::KafkaConsumerService;
use mongodb_client::MongoDBService;
use data_service::DataAggregationService;

#[derive(Clone)]
pub struct AppState {
    data_service: Arc<DataAggregationService>,
}

#[tokio::main]
async fn main() -> Result<()> {
    env_logger::init();
    
    info!("Starting DEFIMON Data Aggregation Service");

    let kafka_brokers = env::var("KAFKA_BROKERS")
        .unwrap_or_else(|_| "localhost:9092".to_string());
    
    let mongodb_uri = env::var("MONGODB_URI")
        .unwrap_or_else(|_| "mongodb://localhost:27017".to_string());

    let data_service = Arc::new(DataAggregationService::new(&kafka_brokers, &mongodb_uri).await?);
    
    let app_state = AppState {
        data_service: data_service.clone(),
    };

    // Start Kafka consumers in background
    let consumer_service = data_service.clone();
    tokio::spawn(async move {
        if let Err(e) = consumer_service.start_consumers().await {
            error!("Kafka consumer error: {}", e);
        }
    });

    // Start real-time aggregation in background
    let aggregation_service = data_service.clone();
    tokio::spawn(async move {
        if let Err(e) = aggregation_service.start_real_time_aggregation().await {
            error!("Real-time aggregation error: {}", e);
        }
    });

    // Create HTTP API routes
    let app = Router::new()
        .route("/health", get(health_check))
        .route("/data/price-history", get(get_price_history_handler))
        .route("/data/volume-analysis", get(get_volume_analysis_handler))
        .route("/data/market-summary", get(get_market_summary_handler))
        .route("/data/liquidity-metrics", get(get_liquidity_metrics_handler))
        .route("/data/aggregated-ohlcv", get(get_ohlcv_data_handler))
        .route("/data/real-time-feed", get(get_real_time_feed_handler))
        .route("/analytics/correlation", post(calculate_correlation_handler))
        .route("/analytics/volatility", post(calculate_volatility_handler))
        .layer(CorsLayer::permissive())
        .with_state(app_state);

    let listener = TcpListener::bind("0.0.0.0:8084").await?;
    info!("Data aggregation service listening on port 8084");
    
    axum::serve(listener, app).await?;

    Ok(())
}

async fn health_check() -> Json<serde_json::Value> {
    Json(serde_json::json!({
        "status": "healthy",
        "service": "data-aggregation",
        "timestamp": chrono::Utc::now().timestamp(),
        "memory_usage": get_memory_usage(),
        "active_streams": 0 // Could be tracked
    }))
}

async fn get_price_history_handler(
    State(state): State<AppState>,
    Query(params): Query<HashMap<String, String>>,
) -> Result<Json<PriceHistoryResponse>, StatusCode> {
    let symbol = params.get("symbol").cloned().unwrap_or_default();
    let timeframe = params.get("timeframe").cloned().unwrap_or_else(|| "1h".to_string());
    let limit: usize = params.get("limit")
        .and_then(|l| l.parse().ok())
        .unwrap_or(100);

    match state.data_service.get_price_history(&symbol, &timeframe, limit).await {
        Ok(response) => Ok(Json(response)),
        Err(e) => {
            error!("Failed to get price history: {}", e);
            Err(StatusCode::INTERNAL_SERVER_ERROR)
        }
    }
}

async fn get_volume_analysis_handler(
    State(state): State<AppState>,
    Query(params): Query<HashMap<String, String>>,
) -> Result<Json<VolumeAnalysisResponse>, StatusCode> {
    let symbol = params.get("symbol").cloned().unwrap_or_default();
    let period_hours: u64 = params.get("period_hours")
        .and_then(|p| p.parse().ok())
        .unwrap_or(24);

    match state.data_service.get_volume_analysis(&symbol, period_hours).await {
        Ok(response) => Ok(Json(response)),
        Err(e) => {
            error!("Failed to get volume analysis: {}", e);
            Err(StatusCode::INTERNAL_SERVER_ERROR)
        }
    }
}

async fn get_market_summary_handler(
    State(state): State<AppState>,
) -> Result<Json<MarketSummaryResponse>, StatusCode> {
    match state.data_service.get_market_summary().await {
        Ok(response) => Ok(Json(response)),
        Err(e) => {
            error!("Failed to get market summary: {}", e);
            Err(StatusCode::INTERNAL_SERVER_ERROR)
        }
    }
}

async fn get_liquidity_metrics_handler(
    State(state): State<AppState>,
    Query(params): Query<HashMap<String, String>>,
) -> Result<Json<LiquidityMetricsResponse>, StatusCode> {
    let symbol = params.get("symbol").cloned().unwrap_or_default();

    match state.data_service.get_liquidity_metrics(&symbol).await {
        Ok(response) => Ok(Json(response)),
        Err(e) => {
            error!("Failed to get liquidity metrics: {}", e);
            Err(StatusCode::INTERNAL_SERVER_ERROR)
        }
    }
}

async fn get_ohlcv_data_handler(
    State(state): State<AppState>,
    Query(params): Query<HashMap<String, String>>,
) -> Result<Json<OHLCVResponse>, StatusCode> {
    let symbol = params.get("symbol").cloned().unwrap_or_default();
    let timeframe = params.get("timeframe").cloned().unwrap_or_else(|| "1h".to_string());
    let limit: usize = params.get("limit")
        .and_then(|l| l.parse().ok())
        .unwrap_or(100);

    match state.data_service.get_ohlcv_data(&symbol, &timeframe, limit).await {
        Ok(response) => Ok(Json(response)),
        Err(e) => {
            error!("Failed to get OHLCV data: {}", e);
            Err(StatusCode::INTERNAL_SERVER_ERROR)
        }
    }
}

async fn get_real_time_feed_handler(
    State(state): State<AppState>,
    Query(params): Query<HashMap<String, String>>,
) -> Result<Json<RealTimeFeedResponse>, StatusCode> {
    let symbols: Vec<String> = params.get("symbols")
        .map(|s| s.split(',').map(|sym| sym.trim().to_string()).collect())
        .unwrap_or_default();

    match state.data_service.get_real_time_feed(&symbols).await {
        Ok(response) => Ok(Json(response)),
        Err(e) => {
            error!("Failed to get real-time feed: {}", e);
            Err(StatusCode::INTERNAL_SERVER_ERROR)
        }
    }
}

async fn calculate_correlation_handler(
    State(state): State<AppState>,
    Json(request): Json<CorrelationRequest>,
) -> Result<Json<CorrelationResponse>, StatusCode> {
    match state.data_service.calculate_correlation(request).await {
        Ok(response) => Ok(Json(response)),
        Err(e) => {
            error!("Failed to calculate correlation: {}", e);
            Err(StatusCode::INTERNAL_SERVER_ERROR)
        }
    }
}

async fn calculate_volatility_handler(
    State(state): State<AppState>,
    Json(request): Json<VolatilityRequest>,
) -> Result<Json<VolatilityResponse>, StatusCode> {
    match state.data_service.calculate_volatility(request).await {
        Ok(response) => Ok(Json(response)),
        Err(e) => {
            error!("Failed to calculate volatility: {}", e);
            Err(StatusCode::INTERNAL_SERVER_ERROR)
        }
    }
}

fn get_memory_usage() -> u64 {
    // Simple memory usage estimation
    std::process::id() as u64 * 1024 // Placeholder
}
