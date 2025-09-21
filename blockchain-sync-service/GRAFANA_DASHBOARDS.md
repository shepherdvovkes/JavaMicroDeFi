# Grafana Dashboards for Blockchain Sync Service

This document describes the comprehensive Grafana dashboards created for monitoring the blockchain synchronization service with micrometer metrics and Prometheus.

## 📊 Dashboard Overview

### 1. Blockchain Sync Service Dashboard
**File:** `blockchain-sync-dashboard.json`  
**Focus:** Core blockchain synchronization metrics and performance

#### Key Panels:
- **Block Processing Rate** - Real-time blocks processed per second
- **Last Processed Block** - Current blockchain sync position
- **Ethereum RPC Request Latency** - API response times (95th and 50th percentiles)
- **Processing Errors** - Error rates by type and operation
- **MongoDB Operation Latency** - Database performance metrics
- **Kafka Message Throughput** - Message streaming rates
- **Transaction Processing Rate** - Transaction throughput
- **Event Processing Rate** - Blockchain event processing
- **MongoDB Connection Pool** - Database connection monitoring
- **Circuit Breaker Status** - Service resilience metrics
- **Blockchain Sync Lag** - Sync delay monitoring
- **Memory Usage** - System resource utilization

### 2. Ethereum Infrastructure Dashboard
**File:** `ethereum-infrastructure-dashboard.json`  
**Focus:** Ethereum node performance and DeFi monitoring

#### Key Panels:
- **Ethereum RPC Request Rate** - API call frequency by method
- **Ethereum RPC Errors** - Error tracking and classification
- **Ethereum RPC Latency** - Response time distribution
- **Erigon Block Processing** - Execution layer performance
- **Erigon Database Operations** - Node database activity
- **Lighthouse Slot Processing** - Consensus layer metrics
- **Lighthouse Validator Count** - Network validator statistics
- **DeFi Token Transfers** - Token transfer activity
- **Ethereum Gas Price** - Network gas price trends
- **DeFi Protocol Interactions** - Protocol usage metrics

### 3. System Metrics Dashboard
**File:** `system-metrics-dashboard.json`  
**Focus:** Infrastructure and system health monitoring

#### Key Panels:
- **Memory Usage** - Process memory consumption
- **CPU Usage** - System CPU utilization
- **MongoDB Connection Pool** - Database connection management
- **MongoDB Operation Latency** - Database performance
- **MongoDB Operations Rate** - Database activity
- **Kafka Message Throughput** - Message streaming performance
- **Service Uptime** - Service availability tracking
- **Circuit Breaker Failures** - Resilience monitoring
- **File Descriptor Usage** - System resource monitoring
- **MongoDB Errors** - Database error tracking

## 🎯 Metrics Categories

### Blockchain Metrics
- `blockchain_blocks_processed_total` - Total blocks processed
- `blockchain_last_processed_block_number` - Latest sync position
- `blockchain_transactions_processed_total` - Transaction throughput
- `blockchain_events_processed_total` - Event processing rate
- `blockchain_sync_lag_seconds` - Sync delay measurement
- `blockchain_processing_errors_total` - Error tracking

### Ethereum RPC Metrics
- `ethereum_rpc_requests_total` - API request counts
- `ethereum_rpc_request_duration_seconds` - Response times
- `ethereum_rpc_errors_total` - Error classification
- `ethereum_gas_price_gwei` - Network gas prices

### Erigon Node Metrics
- `erigon_block_processing_duration_seconds_total` - Block processing time
- `erigon_database_operations_total` - Database activity

### Lighthouse Metrics
- `lighthouse_slot_processing_duration_seconds` - Consensus timing
- `lighthouse_validator_count` - Network validator count

### MongoDB Metrics
- `mongodb_operations_total` - Operation counts
- `mongodb_operation_duration_seconds` - Operation latency
- `mongodb_connections_active` - Active connections
- `mongodb_connections_available` - Available connections
- `mongodb_errors_total` - Database errors

### Kafka Metrics
- `kafka_messages_produced_total` - Message production
- `kafka_messages_consumed_total` - Message consumption

### DeFi Metrics
- `defi_token_transfers_total` - Token transfer activity
- `defi_protocol_interactions_total` - Protocol usage

### System Metrics
- `process_resident_memory_bytes` - Memory usage
- `process_cpu_seconds_total` - CPU utilization
- `service_uptime_seconds` - Service availability
- `circuit_breaker_state` - Circuit breaker status
- `circuit_breaker_failures_total` - Failure tracking

## 🔧 Configuration

### Grafana Setup
1. **Data Source:** Prometheus at `http://prometheus:9090`
2. **Refresh Rate:** 5 seconds
3. **Time Range:** Last 1 hour (default)
4. **Theme:** Dark mode

### Dashboard Organization
- **Folder:** "Blockchain Monitoring"
- **Auto-refresh:** 5 seconds
- **Time picker:** Enabled
- **Annotations:** Available for alerting

## 📈 Key Performance Indicators

### Critical Metrics to Monitor:
1. **Block Processing Rate** - Should maintain steady throughput
2. **Sync Lag** - Should remain minimal (< 30 seconds)
3. **RPC Latency** - 95th percentile should be < 1 second
4. **Error Rates** - Should be < 1% for all operations
5. **Memory Usage** - Should not exceed 80% of available memory
6. **Database Latency** - 95th percentile should be < 100ms

### Alerting Recommendations:
- **High Error Rate:** > 5% error rate for any operation
- **Sync Lag:** > 60 seconds behind latest block
- **Memory Usage:** > 90% memory utilization
- **RPC Latency:** > 2 seconds for 95th percentile
- **Circuit Breaker Open:** Any circuit breaker in open state

## 🚀 Usage Instructions

### Accessing Dashboards:
1. Open Grafana: http://localhost:3000
2. Login: admin / defimon123
3. Navigate to "Blockchain Monitoring" folder
4. Select desired dashboard

### Dashboard Navigation:
- **Time Range:** Use top-right time picker
- **Refresh:** Manual refresh or auto-refresh every 5s
- **Drill-down:** Click on metrics for detailed views
- **Export:** Use dashboard export for sharing

### Customization:
- **Variables:** Add dashboard variables for filtering
- **Panels:** Modify queries for specific metrics
- **Alerts:** Set up alerting rules for critical metrics
- **Annotations:** Add deployment and incident markers

## 🔍 Troubleshooting

### Common Issues:
1. **No Data:** Check if blockchain-sync-service is running
2. **Missing Metrics:** Verify Prometheus is scraping the service
3. **Dashboard Not Loading:** Check Grafana logs for errors
4. **High Latency:** Monitor RPC endpoint health

### Debug Steps:
1. Check service status: `docker-compose ps`
2. Verify metrics endpoint: `curl http://localhost:9090/metrics`
3. Check Prometheus targets: http://localhost:9091/targets
4. Review service logs: `docker-compose logs blockchain-sync`

## 📚 Additional Resources

- [Prometheus Documentation](https://prometheus.io/docs/)
- [Grafana Dashboard Best Practices](https://grafana.com/docs/grafana/latest/best-practices/)
- [Micrometer Documentation](https://micrometer.io/docs)
- [Ethereum RPC API Reference](https://ethereum.org/en/developers/docs/apis/json-rpc/)

---

**Dashboard Version:** 1.0  
**Last Updated:** September 2024  
**Compatible with:** Grafana 10.1.0+, Prometheus 2.45.0+
