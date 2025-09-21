use std::sync::Arc;
use prometheus::{Registry, Counter, Gauge, Histogram, Opts, HistogramOpts, HistogramVec};
use hyper::service::{make_service_fn, service_fn};
use hyper::{Body, Request, Response, Server};
use std::convert::Infallible;
use std::net::SocketAddr;

pub struct BlockchainMetrics {
    pub blocks_processed_total: Counter,
    pub last_processed_block: Gauge,
    pub processing_errors_total: Counter,
    pub rpc_requests_total: Counter,
    pub rpc_request_duration: Histogram,
    pub database_operations_total: Counter,
    pub database_operation_duration: Histogram,
}

impl BlockchainMetrics {
    pub fn new() -> Self {
        let blocks_processed_total = Counter::new(
            "blockchain_blocks_processed_total",
            "Total number of blocks processed"
        ).unwrap();

        let last_processed_block = Gauge::new(
            "blockchain_last_processed_block",
            "Number of the last processed block"
        ).unwrap();

        let processing_errors_total = Counter::new(
            "blockchain_processing_errors_total",
            "Total number of processing errors"
        ).unwrap();

        let rpc_requests_total = Counter::new(
            "blockchain_rpc_requests_total",
            "Total number of RPC requests made"
        ).unwrap();

        let rpc_request_duration = Histogram::with_opts(
            HistogramOpts::new(
                "blockchain_rpc_request_duration_seconds",
                "Duration of RPC requests in seconds"
            ).buckets(vec![0.001, 0.005, 0.01, 0.05, 0.1, 0.5, 1.0, 5.0])
        ).unwrap();

        let database_operations_total = Counter::new(
            "blockchain_database_operations_total",
            "Total number of database operations"
        ).unwrap();

        let database_operation_duration = Histogram::with_opts(
            HistogramOpts::new(
                "blockchain_database_operation_duration_seconds",
                "Duration of database operations in seconds"
            ).buckets(vec![0.001, 0.005, 0.01, 0.05, 0.1, 0.5, 1.0, 5.0])
        ).unwrap();

        Self {
            blocks_processed_total,
            last_processed_block,
            processing_errors_total,
            rpc_requests_total,
            rpc_request_duration,
            database_operations_total,
            database_operation_duration,
        }
    }

    pub fn record_block_processed(&self, block_number: u64) {
        self.blocks_processed_total.inc();
        self.last_processed_block.set(block_number as f64);
    }

    pub fn update_last_processed_block(&self, block_number: u64) {
        self.last_processed_block.set(block_number as f64);
    }

    pub fn record_error(&self) {
        self.processing_errors_total.inc();
    }

    pub fn record_rpc_request(&self) {
        self.rpc_requests_total.inc();
    }

    pub fn record_rpc_duration(&self, duration: f64) {
        self.rpc_request_duration.observe(duration);
    }

    pub fn record_database_operation(&self) {
        self.database_operations_total.inc();
    }

    pub fn record_database_duration(&self, duration: f64) {
        self.database_operation_duration.observe(duration);
    }
}

pub struct MetricsService {
    metrics: Arc<BlockchainMetrics>,
    addr: String,
}

impl MetricsService {
    pub fn new(metrics: Arc<BlockchainMetrics>, addr: String) -> Self {
        Self { metrics, addr }
    }

    pub async fn start(&self) -> Result<(), Box<dyn std::error::Error + Send + Sync>> {
        let addr: SocketAddr = self.addr.parse()?;
        
        let registry = Registry::new();
        
        // Register metrics with the registry
        registry.register(Box::new(self.metrics.blocks_processed_total.clone()))?;
        registry.register(Box::new(self.metrics.last_processed_block.clone()))?;
        registry.register(Box::new(self.metrics.processing_errors_total.clone()))?;
        registry.register(Box::new(self.metrics.rpc_requests_total.clone()))?;
        registry.register(Box::new(self.metrics.rpc_request_duration.clone()))?;
        registry.register(Box::new(self.metrics.database_operations_total.clone()))?;
        registry.register(Box::new(self.metrics.database_operation_duration.clone()))?;

        let make_svc = make_service_fn(move |_conn| {
            let registry = registry.clone();
            async move {
                Ok::<_, Infallible>(service_fn(move |req: Request<Body>| {
                    let registry = registry.clone();
                    async move {
                        let response = match req.uri().path() {
                            "/metrics" => {
                                let metric_families = registry.gather();
                                let encoder = prometheus::TextEncoder::new();
                                match encoder.encode_to_string(&metric_families) {
                                    Ok(metrics) => Response::builder()
                                        .status(200)
                                        .header("Content-Type", "text/plain; version=0.0.4; charset=utf-8")
                                        .body(Body::from(metrics))
                                        .unwrap(),
                                    Err(_) => Response::builder()
                                        .status(500)
                                        .body(Body::from("Failed to encode metrics"))
                                        .unwrap(),
                                }
                            }
                            "/health" => Response::builder()
                                .status(200)
                                .body(Body::from("OK"))
                                .unwrap(),
                            _ => Response::builder()
                                .status(404)
                                .body(Body::from("Not Found"))
                                .unwrap(),
                        };
                        Ok::<_, Infallible>(response)
                    }
                }))
            }
        });

        let server = Server::bind(&addr).serve(make_svc);
        
        println!("DEBUG: Starting HTTP server on {}", addr);
        
        if let Err(e) = server.await {
            eprintln!("Server error: {}", e);
        }

        Ok(())
    }
}