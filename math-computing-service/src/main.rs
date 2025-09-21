use anyhow::Result;
use axum::{
    extract::State,
    http::StatusCode,
    response::Json,
    routing::{get, post},
    Router,
};
use log::{error, info};
use std::env;
use std::sync::Arc;
use tokio::net::TcpListener;
use tower_http::cors::CorsLayer;

mod models;
mod calculations;
mod kafka_consumer;
mod math_service;
mod metrics;

use models::*;
use calculations::*;
use kafka_consumer::KafkaConsumerService;
use math_service::MathComputingService;
use metrics::{MathComputingMetrics, create_metrics_registry, register_math_metrics};

#[derive(Clone)]
pub struct AppState {
    math_service: Arc<MathComputingService>,
    metrics: Arc<MathComputingMetrics>,
}

#[tokio::main]
async fn main() -> Result<()> {
    env_logger::init();
    
    info!("Starting DEFIMON Math Computing Service");

    let kafka_brokers = env::var("KAFKA_BROKERS")
        .unwrap_or_else(|_| "localhost:9092".to_string());

    let math_service = Arc::new(MathComputingService::new(&kafka_brokers).await?);
    let metrics = Arc::new(MathComputingMetrics::new());
    
    let app_state = AppState {
        math_service: math_service.clone(),
        metrics: metrics.clone(),
    };

    // Start Kafka consumer in background
    let consumer_service = math_service.clone();
    tokio::spawn(async move {
        if let Err(e) = consumer_service.start_consumer().await {
            error!("Kafka consumer error: {}", e);
        }
    });

    // Create HTTP API routes
    let app = Router::new()
        .route("/health", get(health_check))
        .route("/metrics", get(metrics_handler))
        .route("/calculate/option-price", post(calculate_option_price_handler))
        .route("/calculate/arbitrage", post(calculate_arbitrage_handler))
        .route("/calculate/portfolio-optimization", post(optimize_portfolio_handler))
        .route("/calculate/risk-metrics", post(calculate_risk_metrics_handler))
        .route("/calculate/yield-farming", post(calculate_yield_farming_handler))
        .route("/calculate/impermanent-loss", post(calculate_impermanent_loss_handler))
        .layer(CorsLayer::permissive())
        .with_state(app_state);

    let listener = TcpListener::bind("0.0.0.0:8083").await?;
    info!("Math computing service listening on port 8083");
    
    axum::serve(listener, app).await?;

    Ok(())
}

async fn health_check() -> Json<serde_json::Value> {
    Json(serde_json::json!({
        "status": "healthy",
        "service": "math-computing",
        "timestamp": chrono::Utc::now().timestamp(),
        "cpu_cores": num_cpus::get(),
        "memory_usage": get_memory_usage()
    }))
}

async fn metrics_handler(State(state): State<AppState>) -> Result<String, StatusCode> {
    let registry = create_metrics_registry();
    if let Err(e) = register_math_metrics(&registry, &state.metrics) {
        error!("Failed to register metrics: {}", e);
        return Err(StatusCode::INTERNAL_SERVER_ERROR);
    }
    
    let metric_families = registry.gather();
    let encoder = prometheus::TextEncoder::new();
    match encoder.encode_to_string(&metric_families) {
        Ok(metrics) => Ok(metrics),
        Err(e) => {
            error!("Failed to encode metrics: {}", e);
            Err(StatusCode::INTERNAL_SERVER_ERROR)
        }
    }
}

async fn calculate_option_price_handler(
    State(state): State<AppState>,
    Json(request): Json<OptionPriceRequest>,
) -> Result<Json<OptionPriceResponse>, StatusCode> {
    match state.math_service.calculate_option_price(request).await {
        Ok(response) => Ok(Json(response)),
        Err(e) => {
            error!("Failed to calculate option price: {}", e);
            Err(StatusCode::INTERNAL_SERVER_ERROR)
        }
    }
}

async fn calculate_arbitrage_handler(
    State(state): State<AppState>,
    Json(request): Json<ArbitrageRequest>,
) -> Result<Json<ArbitrageResponse>, StatusCode> {
    match state.math_service.calculate_arbitrage_opportunity(request).await {
        Ok(response) => Ok(Json(response)),
        Err(e) => {
            error!("Failed to calculate arbitrage: {}", e);
            Err(StatusCode::INTERNAL_SERVER_ERROR)
        }
    }
}

async fn optimize_portfolio_handler(
    State(state): State<AppState>,
    Json(request): Json<PortfolioOptimizationRequest>,
) -> Result<Json<PortfolioOptimizationResponse>, StatusCode> {
    match state.math_service.optimize_portfolio(request).await {
        Ok(response) => Ok(Json(response)),
        Err(e) => {
            error!("Failed to optimize portfolio: {}", e);
            Err(StatusCode::INTERNAL_SERVER_ERROR)
        }
    }
}

async fn calculate_risk_metrics_handler(
    State(state): State<AppState>,
    Json(request): Json<RiskMetricsRequest>,
) -> Result<Json<RiskMetricsResponse>, StatusCode> {
    match state.math_service.calculate_risk_metrics(request).await {
        Ok(response) => Ok(Json(response)),
        Err(e) => {
            error!("Failed to calculate risk metrics: {}", e);
            Err(StatusCode::INTERNAL_SERVER_ERROR)
        }
    }
}

async fn calculate_yield_farming_handler(
    State(state): State<AppState>,
    Json(request): Json<YieldFarmingRequest>,
) -> Result<Json<YieldFarmingResponse>, StatusCode> {
    match state.math_service.calculate_yield_farming_returns(request).await {
        Ok(response) => Ok(Json(response)),
        Err(e) => {
            error!("Failed to calculate yield farming returns: {}", e);
            Err(StatusCode::INTERNAL_SERVER_ERROR)
        }
    }
}

async fn calculate_impermanent_loss_handler(
    State(state): State<AppState>,
    Json(request): Json<ImpermanentLossRequest>,
) -> Result<Json<ImpermanentLossResponse>, StatusCode> {
    match state.math_service.calculate_impermanent_loss(request).await {
        Ok(response) => Ok(Json(response)),
        Err(e) => {
            error!("Failed to calculate impermanent loss: {}", e);
            Err(StatusCode::INTERNAL_SERVER_ERROR)
        }
    }
}

fn get_memory_usage() -> u64 {
    // Simple memory usage estimation
    std::process::id() as u64 * 1024 // Placeholder
}
