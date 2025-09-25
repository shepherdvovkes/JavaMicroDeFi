# Blockchain Sync Service Monitoring Setup

This document describes the comprehensive monitoring setup for the blockchain-sync-service using Prometheus and Grafana.

## Overview

The monitoring system provides real-time insights into:
- **Ethereum blockchain synchronization**
- **Erigon node performance** (if using Erigon)
- **Lighthouse consensus client** (if using Lighthouse)
- **MongoDB database operations**
- **Kafka message processing**
- **Service health and performance**

## Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│  Blockchain     │    │   Prometheus    │    │    Grafana      │
│  Sync Service   │───▶│   (Metrics      │───▶│  (Dashboard     │
│  (Port 9090)    │    │   Collector)    │    │   & Alerts)     │
│                 │    │   (Port 9091)   │    │   (Port 3000)   │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

## Quick Start

1. **Start the monitoring stack:**
   ```bash
   docker-compose up -d prometheus grafana
   ```

2. **Access Grafana:**
   - URL: http://localhost:3000
   - Username: `admin`
   - Password: `defimon123`

3. **Access Prometheus:**
   - URL: http://localhost:9091

4. **View the pre-configured dashboard:**
   - Go to Grafana → Dashboards → "Blockchain Sync Service Dashboard"

## Metrics Categories

### 1. Ethereum RPC Metrics
- `blockchain_sync_eth_rpc_requests_total` - Total RPC requests
- `blockchain_sync_eth_rpc_request_duration_seconds` - RPC request latency
- `blockchain_sync_eth_rpc_errors_total` - RPC errors
- `blockchain_sync_eth_latest_block_number` - Latest block number
- `blockchain_sync_eth_peer_count` - Connected peers

### 2. Erigon-Specific Metrics
- `blockchain_sync_erigon_block_processing_time_seconds` - Block processing time
- `blockchain_sync_erigon_tx_processing_time_seconds` - Transaction processing time
- `blockchain_sync_erigon_db_read_time_seconds` - Database read operations
- `blockchain_sync_erigon_db_write_time_seconds` - Database write operations

### 3. Lighthouse-Specific Metrics
- `blockchain_sync_lighthouse_slot_processing_time_seconds` - Slot processing time
- `blockchain_sync_lighthouse_epoch_processing_time_seconds` - Epoch processing time
- `blockchain_sync_lighthouse_validator_count` - Active validators
- `blockchain_sync_lighthouse_beacon_slot` - Current beacon slot
- `blockchain_sync_lighthouse_beacon_epoch` - Current beacon epoch

### 4. Database Metrics
- `blockchain_sync_mongodb_operations_total` - MongoDB operations
- `blockchain_sync_mongodb_operation_duration_seconds` - Operation latency
- `blockchain_sync_mongodb_connections_active` - Active connections
- `blockchain_sync_mongodb_collection_sizes` - Collection sizes

### 5. Blockchain Sync Metrics
- `blockchain_sync_blocks_processed_total` - Total blocks processed
- `blockchain_sync_blocks_processing_duration_seconds` - Block processing time
- `blockchain_sync_transactions_processed_total` - Total transactions
- `blockchain_sync_events_processed_total` - Contract events
- `blockchain_sync_sync_lag_blocks` - Sync lag in blocks
- `blockchain_sync_last_processed_block` - Last processed block

### 6. Kafka Metrics
- `blockchain_sync_kafka_messages_sent_total` - Messages sent
- `blockchain_sync_kafka_message_send_duration_seconds` - Send latency
- `blockchain_sync_kafka_send_errors_total` - Send errors

### 7. Service Health Metrics
- `blockchain_sync_service_uptime_seconds` - Service uptime
- `blockchain_sync_service_health_status` - Health status
- `blockchain_sync_circuit_breaker_state` - Circuit breaker state
- `blockchain_sync_memory_usage_bytes` - Memory usage
- `blockchain_sync_cpu_usage_percent` - CPU usage

### 8. DeFi Monitoring Metrics
- `blockchain_sync_defi_protocol_interactions_total` - DeFi interactions
- `blockchain_sync_token_contract_interactions_total` - Token interactions
- `blockchain_sync_gas_price_gwei` - Gas price in Gwei
- `blockchain_sync_gas_usage_per_block` - Gas usage per block
- `blockchain_sync_transaction_value_eth` - Transaction values

## Dashboard Panels

The Grafana dashboard includes:

1. **Block Processing Rate** - Real-time blocks processed per second
2. **Sync Lag** - Number of blocks behind the latest
3. **Last Processed Block** - Current sync position
4. **Latest Ethereum Block** - Latest block from the network
5. **Block Processing Duration** - Latency percentiles
6. **Transaction Processing Rate** - Transactions per second
7. **Ethereum RPC Request Duration** - RPC latency
8. **MongoDB Operation Duration** - Database latency
9. **Processing Errors** - Error rates by type
10. **Kafka Message Send Rate** - Message throughput
11. **Service Health Status** - Component health
12. **Circuit Breaker State** - Circuit breaker status
13. **Gas Price** - Current gas prices
14. **Token Transfer Events** - Token transfer rates
15. **DeFi Protocol Interactions** - DeFi activity

## Alerting Rules

Create alerting rules for:

### Critical Alerts
- Sync lag > 100 blocks
- Processing errors > 10/min
- Service health status = unhealthy
- Circuit breaker open

### Warning Alerts
- Sync lag > 10 blocks
- Block processing time > 5 seconds
- RPC request duration > 2 seconds
- MongoDB operation duration > 1 second

## Configuration

### Environment Variables

```bash
# Blockchain Sync Service
ETH_RPC_URL=https://mainnet.infura.io/v3/YOUR_PROJECT_ID
METRICS_ADDR=0.0.0.0:9090
RUST_LOG=info

# Prometheus
PROMETHEUS_RETENTION_TIME=200h

# Grafana
GF_SECURITY_ADMIN_USER=admin
GF_SECURITY_ADMIN_PASSWORD=defimon123
```

### Ports
- **Blockchain Sync Service**: 9090 (metrics)
- **Prometheus**: 9091 (web UI)
- **Grafana**: 3000 (web UI)

## Troubleshooting

### Common Issues

1. **Metrics not appearing in Grafana**
   - Check Prometheus targets: http://localhost:9091/targets
   - Verify blockchain-sync service is running
   - Check logs: `docker logs blockchain-sync`

2. **High sync lag**
   - Check RPC endpoint connectivity
   - Monitor processing errors
   - Verify MongoDB performance

3. **Memory usage high**
   - Monitor `blockchain_sync_memory_usage_bytes`
   - Check for memory leaks in processing
   - Adjust batch sizes if needed

### Useful Queries

```promql
# Block processing rate
rate(blockchain_sync_blocks_processed_total{status="success"}[5m])

# Sync lag
blockchain_sync_sync_lag_blocks

# Error rate
rate(blockchain_sync_processing_errors_total[5m])

# RPC latency 95th percentile
histogram_quantile(0.95, rate(blockchain_sync_eth_rpc_request_duration_seconds_bucket[5m]))
```

## Extending Monitoring

### Adding New Metrics

1. **Define metric in `metrics.rs`:**
   ```rust
   let new_metric = register_counter_vec!(
       CounterOpts::new("new_metric_total", "Description"),
       &["label1", "label2"]
   )?;
   ```

2. **Record metrics in your code:**
   ```rust
   self.metrics.new_metric.with_label_values(&["value1", "value2"]).inc();
   ```

3. **Add to Grafana dashboard:**
   - Create new panel
   - Use appropriate query
   - Configure visualization

### Custom Dashboards

Create custom dashboards for:
- Specific DeFi protocols
- Token-specific monitoring
- Performance optimization
- Capacity planning

## Security Considerations

1. **Network Security**
   - Use internal networks for service communication
   - Restrict external access to monitoring ports
   - Use reverse proxy for external access

2. **Authentication**
   - Change default Grafana credentials
   - Use strong passwords
   - Enable HTTPS in production

3. **Data Retention**
   - Configure appropriate retention periods
   - Monitor disk usage
   - Implement backup strategies

## Performance Optimization

1. **Prometheus**
   - Adjust scrape intervals based on needs
   - Use recording rules for complex queries
   - Configure appropriate retention

2. **Grafana**
   - Optimize dashboard queries
   - Use data source caching
   - Limit dashboard refresh rates

3. **Service**
   - Minimize metric cardinality
   - Use appropriate metric types
   - Avoid high-frequency metric updates

## Integration with External Systems

### Alerting
- Configure Grafana alerting channels
- Integrate with PagerDuty, Slack, etc.
- Set up escalation policies

### Logging
- Correlate metrics with logs
- Use structured logging
- Implement log aggregation

### CI/CD
- Include monitoring in deployment pipelines
- Validate metrics in tests
- Automate dashboard updates
