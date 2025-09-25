use std::sync::Arc;
use prometheus::{Registry, Counter, Gauge, Histogram, Opts, HistogramOpts, CounterVec};
use std::collections::HashMap;

pub struct MathComputingMetrics {
    pub calculations_total: CounterVec,
    pub calculation_duration: Histogram,
    pub calculation_errors_total: CounterVec,
    pub active_calculations: Gauge,
    pub memory_usage_bytes: Gauge,
    pub cpu_usage_percent: Gauge,
}

impl MathComputingMetrics {
    pub fn new() -> Self {
        let calculations_total = CounterVec::new(
            Opts::new("math_calculations_total", "Total number of calculations performed"),
            &["calculation_type"]
        ).unwrap();

        let calculation_duration = Histogram::with_opts(
            HistogramOpts::new(
                "math_calculation_duration_seconds",
                "Duration of calculations in seconds"
            ).buckets(vec![0.001, 0.005, 0.01, 0.05, 0.1, 0.5, 1.0, 5.0, 10.0])
        ).unwrap();

        let calculation_errors_total = CounterVec::new(
            Opts::new("math_calculation_errors_total", "Total number of calculation errors"),
            &["calculation_type", "error_type"]
        ).unwrap();

        let active_calculations = Gauge::new(
            "math_active_calculations",
            "Number of currently active calculations"
        ).unwrap();

        let memory_usage_bytes = Gauge::new(
            "math_memory_usage_bytes",
            "Memory usage in bytes"
        ).unwrap();

        let cpu_usage_percent = Gauge::new(
            "math_cpu_usage_percent",
            "CPU usage percentage"
        ).unwrap();

        Self {
            calculations_total,
            calculation_duration,
            calculation_errors_total,
            active_calculations,
            memory_usage_bytes,
            cpu_usage_percent,
        }
    }

    pub fn record_calculation(&self, calculation_type: &str) {
        self.calculations_total.with_label_values(&[calculation_type]).inc();
    }

    pub fn record_calculation_duration(&self, duration: f64) {
        self.calculation_duration.observe(duration);
    }

    pub fn record_calculation_error(&self, calculation_type: &str, error_type: &str) {
        self.calculation_errors_total.with_label_values(&[calculation_type, error_type]).inc();
    }

    pub fn set_active_calculations(&self, count: f64) {
        self.active_calculations.set(count);
    }

    pub fn update_memory_usage(&self, bytes: u64) {
        self.memory_usage_bytes.set(bytes as f64);
    }

    pub fn update_cpu_usage(&self, percent: f64) {
        self.cpu_usage_percent.set(percent);
    }
}

pub fn create_metrics_registry() -> Registry {
    let registry = Registry::new();
    registry
}

pub fn register_math_metrics(registry: &Registry, metrics: &MathComputingMetrics) -> Result<(), prometheus::Error> {
    registry.register(Box::new(metrics.calculations_total.clone()))?;
    registry.register(Box::new(metrics.calculation_duration.clone()))?;
    registry.register(Box::new(metrics.calculation_errors_total.clone()))?;
    registry.register(Box::new(metrics.active_calculations.clone()))?;
    registry.register(Box::new(metrics.memory_usage_bytes.clone()))?;
    registry.register(Box::new(metrics.cpu_usage_percent.clone()))?;
    Ok(())
}
