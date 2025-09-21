# 🦀 Rust Microservices Dashboard

## Overview

This document describes the comprehensive Grafana dashboard for monitoring Rust microservices in the DEFIMON platform. The dashboard provides real-time insights into all four Rust services: Blockchain Sync, Math Computing, Transaction Signing, and Data Aggregation.

## 🎯 Dashboard Features

### **Real-time Monitoring**
- **Service Throughput**: Operations per second for all services
- **Response Time**: 95th percentile latency across all services
- **Memory Usage**: Memory consumption tracking
- **Error Rates**: Error monitoring and database operation tracking

### **Service-Specific Metrics**
- **Blockchain Sync**: Block processing rate, sync progress, RPC performance
- **Math Computing**: Calculation types, active calculations, CPU usage
- **Transaction Signing**: Active sessions, wallet operations, signing performance
- **Data Aggregation**: Data streams, aggregation operations, cache performance

## 📊 Dashboard Panels

### 1. **Rust Services Throughput**
- **Metrics**: `rate(blockchain_blocks_processed_total[5m])`, `rate(math_calculations_total[5m])`, etc.
- **Purpose**: Monitor operations per second across all services
- **Alert Threshold**: Configurable based on service requirements

### 2. **Response Time (95th Percentile)**
- **Metrics**: `histogram_quantile(0.95, rate(*_duration_seconds_bucket[5m]))`
- **Purpose**: Track service performance and identify bottlenecks
- **Alert Threshold**: > 1 second for critical operations

### 3. **Service Activity Indicators**
- **Blockchain Sync Progress**: Latest processed block number
- **Math Computing Activity**: Active calculation count
- **Transaction Signing Sessions**: Active signing sessions
- **Data Aggregation Streams**: Active data streams

### 4. **Memory Usage**
- **Metrics**: `*_memory_usage_bytes`
- **Purpose**: Monitor memory consumption across services
- **Alert Threshold**: > 80% of allocated memory

### 5. **Error Rates & Database Operations**
- **Metrics**: `rate(*_errors_total[5m])`, `rate(*_database_queries_total[5m])`
- **Purpose**: Track service health and database performance
- **Alert Threshold**: Error rate > 1% of total operations

### 6. **Math Computing Service - Calculation Types**
- **Metrics**: `rate(math_calculations_total{calculation_type=*}[5m])`
- **Types**: Option pricing, arbitrage, portfolio optimization, risk metrics, yield farming, impermanent loss
- **Purpose**: Monitor specific calculation workloads

### 7. **Data Aggregation Service - Operation Types**
- **Metrics**: `rate(data_aggregation_operations_total{operation_type=*}[5m])`
- **Types**: Price history, volume analysis, market summary, liquidity metrics, OHLCV data, correlation analysis
- **Purpose**: Track data processing workloads

## 🔧 Metrics Implementation

### **Prometheus Metrics Added**

#### Blockchain Sync Service (Already implemented)
```rust
- blockchain_blocks_processed_total
- blockchain_last_processed_block
- blockchain_processing_errors_total
- blockchain_rpc_requests_total
- blockchain_rpc_request_duration_seconds
- blockchain_database_operations_total
- blockchain_database_operation_duration_seconds
```

#### Math Computing Service (New)
```rust
- math_calculations_total{calculation_type}
- math_calculation_duration_seconds
- math_calculation_errors_total{calculation_type, error_type}
- math_active_calculations
- math_memory_usage_bytes
- math_cpu_usage_percent
```

#### Transaction Signing Service (New)
```rust
- transaction_signing_transactions_signed_total
- transaction_signing_duration_seconds
- transaction_signing_errors_total{error_type}
- transaction_signing_wallets_created_total
- transaction_signing_wallets_imported_total
- transaction_signing_active_sessions
- transaction_signing_memory_usage_bytes
```

#### Data Aggregation Service (New)
```rust
- data_aggregation_points_processed_total
- data_aggregation_operations_total{operation_type}
- data_aggregation_duration_seconds
- data_aggregation_database_queries_total{query_type}
- data_aggregation_database_query_duration_seconds
- data_aggregation_kafka_messages_consumed_total
- data_aggregation_active_streams
- data_aggregation_memory_usage_bytes
- data_aggregation_cache_hit_ratio
```

## 🚀 Getting Started

### 1. **Access the Dashboard**
- **URL**: http://localhost:3000
- **Username**: `admin`
- **Password**: `defimon123`
- **Dashboard**: "Rust Microservices Dashboard"

### 2. **Start Services with Metrics**
```bash
# Start all services including the new metrics endpoints
cd /home/vovkes/JavaMicroDeFi
docker-compose up -d

# Verify metrics endpoints are accessible
curl http://localhost:8083/metrics  # Math Computing Service
curl http://localhost:8082/metrics  # Transaction Signing Service
curl http://localhost:8084/metrics  # Data Aggregation Service
curl http://localhost:9090/metrics  # Blockchain Sync Service
```

### 3. **Prometheus Configuration**
The Prometheus configuration has been updated to scrape all Rust services:
```yaml
# Math Computing Service metrics
- job_name: 'math-computing-service'
  static_configs:
    - targets: ['172.19.0.1:8083']
  scrape_interval: 15s
  metrics_path: /metrics

# Transaction Signing Service metrics
- job_name: 'transaction-signing-service'
  static_configs:
    - targets: ['172.19.0.1:8082']
  scrape_interval: 15s
  metrics_path: /metrics

# Data Aggregation Service metrics
- job_name: 'data-aggregation-service'
  static_configs:
    - targets: ['172.19.0.1:8084']
  scrape_interval: 15s
  metrics_path: /metrics
```

## 📈 Performance Monitoring

### **Key Performance Indicators (KPIs)**

1. **Throughput**
   - Blockchain Sync: > 10 blocks/second
   - Math Computing: > 100 calculations/second
   - Transaction Signing: > 50 transactions/second
   - Data Aggregation: > 1000 data points/second

2. **Latency**
   - All services: < 100ms (95th percentile)
   - Critical operations: < 50ms

3. **Error Rates**
   - All services: < 0.1% error rate
   - Database operations: < 1% timeout rate

4. **Resource Usage**
   - Memory: < 80% of allocated
   - CPU: < 70% average usage

## 🔔 Alerting Rules (Recommended)

### **High Priority Alerts**
```yaml
- alert: HighErrorRate
  expr: rate(*_errors_total[5m]) / rate(*_total[5m]) > 0.01
  for: 2m
  labels:
    severity: critical
  annotations:
    summary: "High error rate detected"

- alert: HighLatency
  expr: histogram_quantile(0.95, rate(*_duration_seconds_bucket[5m])) > 1
  for: 5m
  labels:
    severity: warning
  annotations:
    summary: "High latency detected"

- alert: HighMemoryUsage
  expr: *_memory_usage_bytes / 1024 / 1024 / 1024 > 8
  for: 10m
  labels:
    severity: warning
  annotations:
    summary: "High memory usage detected"
```

## 🛠️ Troubleshooting

### **Common Issues**

1. **Metrics Not Appearing**
   - Check service logs for metrics registration errors
   - Verify Prometheus can reach the metrics endpoints
   - Ensure services are running on expected ports

2. **Dashboard Not Loading**
   - Verify Grafana is running: `docker ps | grep grafana`
   - Check dashboard JSON syntax
   - Reload Grafana: `docker restart grafana`

3. **High Memory Usage**
   - Monitor specific service memory consumption
   - Check for memory leaks in calculation operations
   - Consider increasing container memory limits

### **Debug Commands**
```bash
# Check metrics endpoint
curl -s http://localhost:8083/metrics | grep math_calculations_total

# Check Prometheus targets
curl -s http://localhost:9090/api/v1/targets | jq '.data.activeTargets[]'

# Check service health
curl -s http://localhost:8083/health | jq .
```

## 📚 Additional Resources

- **Grafana Documentation**: https://grafana.com/docs/
- **Prometheus Metrics**: https://prometheus.io/docs/concepts/metric_types/
- **Rust Prometheus Client**: https://docs.rs/prometheus/
- **DEFIMON API Documentation**: See `API_DOCUMENTATION.md`

## 🎉 Success!

Your Rust microservices are now fully monitored with comprehensive metrics and real-time dashboards! The system provides complete visibility into service performance, health, and resource utilization.

**Next Steps:**
1. Set up alerting rules based on your requirements
2. Create custom dashboards for specific use cases
3. Monitor and optimize service performance based on metrics
4. Scale services based on throughput and resource usage patterns
