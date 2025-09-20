use anyhow::Result;
use axum::{
    extract::State,
    http::StatusCode,
    response::Json,
    routing::post,
    Router,
};
use log::{error, info};
use std::env;
use std::sync::Arc;
use tokio::net::TcpListener;
use tower_http::cors::CorsLayer;

mod models;
mod crypto;
mod kafka_consumer;
mod signing_service;

use models::*;
use crypto::CryptoService;
use kafka_consumer::KafkaConsumerService;
use signing_service::TransactionSigningService;

#[derive(Clone)]
pub struct AppState {
    signing_service: Arc<TransactionSigningService>,
}

#[tokio::main]
async fn main() -> Result<()> {
    env_logger::init();
    
    info!("Starting DEFIMON Transaction Signing Service");

    let kafka_brokers = env::var("KAFKA_BROKERS")
        .unwrap_or_else(|_| "localhost:9092".to_string());

    let signing_service = Arc::new(TransactionSigningService::new(&kafka_brokers).await?);
    
    let app_state = AppState {
        signing_service: signing_service.clone(),
    };

    // Start Kafka consumer in background
    let consumer_service = signing_service.clone();
    tokio::spawn(async move {
        if let Err(e) = consumer_service.start_consumer().await {
            error!("Kafka consumer error: {}", e);
        }
    });

    // Create HTTP API routes
    let app = Router::new()
        .route("/health", axum::routing::get(health_check))
        .route("/sign", post(sign_transaction_handler))
        .route("/create-wallet", post(create_wallet_handler))
        .route("/import-wallet", post(import_wallet_handler))
        .layer(CorsLayer::permissive())
        .with_state(app_state);

    let listener = TcpListener::bind("0.0.0.0:8082").await?;
    info!("Transaction signing service listening on port 8082");
    
    axum::serve(listener, app).await?;

    Ok(())
}

async fn health_check() -> Json<serde_json::Value> {
    Json(serde_json::json!({
        "status": "healthy",
        "service": "transaction-signing",
        "timestamp": chrono::Utc::now().timestamp()
    }))
}

async fn sign_transaction_handler(
    State(state): State<AppState>,
    Json(request): Json<SignTransactionRequest>,
) -> Result<Json<SignTransactionResponse>, StatusCode> {
    match state.signing_service.sign_transaction(request).await {
        Ok(response) => Ok(Json(response)),
        Err(e) => {
            error!("Failed to sign transaction: {}", e);
            Err(StatusCode::INTERNAL_SERVER_ERROR)
        }
    }
}

async fn create_wallet_handler(
    State(state): State<AppState>,
    Json(request): Json<CreateWalletRequest>,
) -> Result<Json<CreateWalletResponse>, StatusCode> {
    match state.signing_service.create_wallet(request).await {
        Ok(response) => Ok(Json(response)),
        Err(e) => {
            error!("Failed to create wallet: {}", e);
            Err(StatusCode::INTERNAL_SERVER_ERROR)
        }
    }
}

async fn import_wallet_handler(
    State(state): State<AppState>,
    Json(request): Json<ImportWalletRequest>,
) -> Result<Json<ImportWalletResponse>, StatusCode> {
    match state.signing_service.import_wallet(request).await {
        Ok(response) => Ok(Json(response)),
        Err(e) => {
            error!("Failed to import wallet: {}", e);
            Err(StatusCode::INTERNAL_SERVER_ERROR)
        }
    }
}
