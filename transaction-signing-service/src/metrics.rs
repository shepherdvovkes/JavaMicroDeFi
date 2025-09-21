use std::sync::Arc;
use prometheus::{Registry, Counter, Gauge, Histogram, Opts, HistogramOpts, CounterVec};
use std::collections::HashMap;

pub struct TransactionSigningMetrics {
    pub transactions_signed_total: Counter,
    pub signing_duration: Histogram,
    pub signing_errors_total: CounterVec,
    pub wallets_created_total: Counter,
    pub wallets_imported_total: Counter,
    pub active_sessions: Gauge,
    pub memory_usage_bytes: Gauge,
}

impl TransactionSigningMetrics {
    pub fn new() -> Self {
        let transactions_signed_total = Counter::new(
            "transaction_signing_transactions_signed_total",
            "Total number of transactions signed"
        ).unwrap();

        let signing_duration = Histogram::with_opts(
            HistogramOpts::new(
                "transaction_signing_duration_seconds",
                "Duration of transaction signing in seconds"
            ).buckets(vec![0.001, 0.005, 0.01, 0.05, 0.1, 0.5, 1.0, 5.0])
        ).unwrap();

        let signing_errors_total = CounterVec::new(
            Opts::new("transaction_signing_errors_total", "Total number of signing errors"),
            &["error_type"]
        ).unwrap();

        let wallets_created_total = Counter::new(
            "transaction_signing_wallets_created_total",
            "Total number of wallets created"
        ).unwrap();

        let wallets_imported_total = Counter::new(
            "transaction_signing_wallets_imported_total",
            "Total number of wallets imported"
        ).unwrap();

        let active_sessions = Gauge::new(
            "transaction_signing_active_sessions",
            "Number of currently active signing sessions"
        ).unwrap();

        let memory_usage_bytes = Gauge::new(
            "transaction_signing_memory_usage_bytes",
            "Memory usage in bytes"
        ).unwrap();

        Self {
            transactions_signed_total,
            signing_duration,
            signing_errors_total,
            wallets_created_total,
            wallets_imported_total,
            active_sessions,
            memory_usage_bytes,
        }
    }

    pub fn record_transaction_signed(&self) {
        self.transactions_signed_total.inc();
    }

    pub fn record_signing_duration(&self, duration: f64) {
        self.signing_duration.observe(duration);
    }

    pub fn record_signing_error(&self, error_type: &str) {
        self.signing_errors_total.with_label_values(&[error_type]).inc();
    }

    pub fn record_wallet_created(&self) {
        self.wallets_created_total.inc();
    }

    pub fn record_wallet_imported(&self) {
        self.wallets_imported_total.inc();
    }

    pub fn set_active_sessions(&self, count: f64) {
        self.active_sessions.set(count);
    }

    pub fn update_memory_usage(&self, bytes: u64) {
        self.memory_usage_bytes.set(bytes as f64);
    }
}

pub fn create_metrics_registry() -> Registry {
    let registry = Registry::new();
    registry
}

pub fn register_signing_metrics(registry: &Registry, metrics: &TransactionSigningMetrics) -> Result<(), prometheus::Error> {
    registry.register(Box::new(metrics.transactions_signed_total.clone()))?;
    registry.register(Box::new(metrics.signing_duration.clone()))?;
    registry.register(Box::new(metrics.signing_errors_total.clone()))?;
    registry.register(Box::new(metrics.wallets_created_total.clone()))?;
    registry.register(Box::new(metrics.wallets_imported_total.clone()))?;
    registry.register(Box::new(metrics.active_sessions.clone()))?;
    registry.register(Box::new(metrics.memory_usage_bytes.clone()))?;
    Ok(())
}
