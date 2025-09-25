use std::sync::Arc;
use prometheus::{Registry, Counter, Gauge, Histogram, HistogramOpts};
use axum::{
    extract::State,
    http::StatusCode,
    response::Json,
    routing::{get, post},
    Router,
};
use serde_json::json;
use tokio::net::TcpListener;

#[derive(Clone)]
pub struct AppState {
    metrics: Arc<ServiceMetrics>,
}

pub struct ServiceMetrics {
    pub requests_total: Counter,
    pub request_duration: Histogram,
    pub memory_usage: Gauge,
    pub active_connections: Gauge,
}

impl ServiceMetrics {
    pub fn new() -> Self {
        let requests_total = Counter::new(
            "service_requests_total",
            "Total number of requests"
        ).unwrap();

        let request_duration = Histogram::with_opts(
            HistogramOpts::new(
                "service_request_duration_seconds",
                "Duration of requests in seconds"
            ).buckets(vec![0.001, 0.005, 0.01, 0.05, 0.1, 0.5, 1.0, 5.0])
        ).unwrap();

        let memory_usage = Gauge::new(
            "service_memory_usage_bytes",
            "Memory usage in bytes"
        ).unwrap();

        let active_connections = Gauge::new(
            "service_active_connections",
            "Number of active connections"
        ).unwrap();

        Self {
            requests_total,
            request_duration,
            memory_usage,
            active_connections,
        }
    }
}

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error + Send + Sync>> {
    let metrics = Arc::new(ServiceMetrics::new());
    
    let app_state = AppState {
        metrics: metrics.clone(),
    };

    // Create HTTP API routes
    let app = Router::new()
        .route("/health", get(health_check))
        .route("/metrics", get(metrics_handler))
        .route("/test", post(test_handler))
        .with_state(app_state);

    let listener = TcpListener::bind("0.0.0.0:8080").await?;
    println!("Simple metrics service listening on port 8080");
    
    axum::serve(listener, app).await?;

    Ok(())
}

async fn health_check() -> Json<serde_json::Value> {
    Json(json!({
        "status": "healthy",
        "service": "simple-metrics",
        "timestamp": chrono::Utc::now().timestamp()
    }))
}

async fn metrics_handler(State(state): State<AppState>) -> Result<String, StatusCode> {
    let registry = Registry::new();
    
    // Register metrics
    if let Err(_) = registry.register(Box::new(state.metrics.requests_total.clone())) {
        return Err(StatusCode::INTERNAL_SERVER_ERROR);
    }
    if let Err(_) = registry.register(Box::new(state.metrics.request_duration.clone())) {
        return Err(StatusCode::INTERNAL_SERVER_ERROR);
    }
    if let Err(_) = registry.register(Box::new(state.metrics.memory_usage.clone())) {
        return Err(StatusCode::INTERNAL_SERVER_ERROR);
    }
    if let Err(_) = registry.register(Box::new(state.metrics.active_connections.clone())) {
        return Err(StatusCode::INTERNAL_SERVER_ERROR);
    }
    
    let metric_families = registry.gather();
    let encoder = prometheus::TextEncoder::new();
    match encoder.encode_to_string(&metric_families) {
        Ok(metrics) => Ok(metrics),
        Err(_) => Err(StatusCode::INTERNAL_SERVER_ERROR)
    }
}

async fn test_handler(State(state): State<AppState>) -> Json<serde_json::Value> {
    // Simulate some work
    state.metrics.requests_total.inc();
    state.metrics.memory_usage.set((1024 * 1024 * 10) as f64); // 10MB
    state.metrics.active_connections.set(5.0);
    
    let timer = state.metrics.request_duration.start_timer();
    tokio::time::sleep(tokio::time::Duration::from_millis(100)).await;
    timer.observe_duration();
    
    Json(json!({
        "status": "success",
        "message": "Test request processed",
        "timestamp": chrono::Utc::now().timestamp()
    }))
}
