use std::sync::Arc;
use prometheus::{Registry, Counter, Gauge, Histogram, Opts, HistogramOpts, CounterVec};
use std::collections::HashMap;

pub struct DataAggregationMetrics {
    pub data_points_processed_total: Counter,
    pub aggregation_operations_total: CounterVec,
    pub aggregation_duration: Histogram,
    pub database_queries_total: CounterVec,
    pub database_query_duration: Histogram,
    pub kafka_messages_consumed_total: Counter,
    pub active_streams: Gauge,
    pub memory_usage_bytes: Gauge,
    pub cache_hit_ratio: Gauge,
}

impl DataAggregationMetrics {
    pub fn new() -> Self {
        let data_points_processed_total = Counter::new(
            "data_aggregation_points_processed_total",
            "Total number of data points processed"
        ).unwrap();

        let aggregation_operations_total = CounterVec::new(
            Opts::new("data_aggregation_operations_total", "Total number of aggregation operations"),
            &["operation_type"]
        ).unwrap();

        let aggregation_duration = Histogram::with_opts(
            HistogramOpts::new(
                "data_aggregation_duration_seconds",
                "Duration of aggregation operations in seconds"
            ).buckets(vec![0.001, 0.005, 0.01, 0.05, 0.1, 0.5, 1.0, 5.0, 10.0])
        ).unwrap();

        let database_queries_total = CounterVec::new(
            Opts::new("data_aggregation_database_queries_total", "Total number of database queries"),
            &["query_type"]
        ).unwrap();

        let database_query_duration = Histogram::with_opts(
            HistogramOpts::new(
                "data_aggregation_database_query_duration_seconds",
                "Duration of database queries in seconds"
            ).buckets(vec![0.001, 0.005, 0.01, 0.05, 0.1, 0.5, 1.0, 5.0])
        ).unwrap();

        let kafka_messages_consumed_total = Counter::new(
            "data_aggregation_kafka_messages_consumed_total",
            "Total number of Kafka messages consumed"
        ).unwrap();

        let active_streams = Gauge::new(
            "data_aggregation_active_streams",
            "Number of currently active data streams"
        ).unwrap();

        let memory_usage_bytes = Gauge::new(
            "data_aggregation_memory_usage_bytes",
            "Memory usage in bytes"
        ).unwrap();

        let cache_hit_ratio = Gauge::new(
            "data_aggregation_cache_hit_ratio",
            "Cache hit ratio (0.0 to 1.0)"
        ).unwrap();

        Self {
            data_points_processed_total,
            aggregation_operations_total,
            aggregation_duration,
            database_queries_total,
            database_query_duration,
            kafka_messages_consumed_total,
            active_streams,
            memory_usage_bytes,
            cache_hit_ratio,
        }
    }

    pub fn record_data_point_processed(&self) {
        self.data_points_processed_total.inc();
    }

    pub fn record_aggregation_operation(&self, operation_type: &str) {
        self.aggregation_operations_total.with_label_values(&[operation_type]).inc();
    }

    pub fn record_aggregation_duration(&self, duration: f64) {
        self.aggregation_duration.observe(duration);
    }

    pub fn record_database_query(&self, query_type: &str) {
        self.database_queries_total.with_label_values(&[query_type]).inc();
    }

    pub fn record_database_query_duration(&self, duration: f64) {
        self.database_query_duration.observe(duration);
    }

    pub fn record_kafka_message_consumed(&self) {
        self.kafka_messages_consumed_total.inc();
    }

    pub fn set_active_streams(&self, count: f64) {
        self.active_streams.set(count);
    }

    pub fn update_memory_usage(&self, bytes: u64) {
        self.memory_usage_bytes.set(bytes as f64);
    }

    pub fn update_cache_hit_ratio(&self, ratio: f64) {
        self.cache_hit_ratio.set(ratio);
    }
}

pub fn create_metrics_registry() -> Registry {
    let registry = Registry::new();
    registry
}

pub fn register_aggregation_metrics(registry: &Registry, metrics: &DataAggregationMetrics) -> Result<(), prometheus::Error> {
    registry.register(Box::new(metrics.data_points_processed_total.clone()))?;
    registry.register(Box::new(metrics.aggregation_operations_total.clone()))?;
    registry.register(Box::new(metrics.aggregation_duration.clone()))?;
    registry.register(Box::new(metrics.database_queries_total.clone()))?;
    registry.register(Box::new(metrics.database_query_duration.clone()))?;
    registry.register(Box::new(metrics.kafka_messages_consumed_total.clone()))?;
    registry.register(Box::new(metrics.active_streams.clone()))?;
    registry.register(Box::new(metrics.memory_usage_bytes.clone()))?;
    registry.register(Box::new(metrics.cache_hit_ratio.clone()))?;
    Ok(())
}
