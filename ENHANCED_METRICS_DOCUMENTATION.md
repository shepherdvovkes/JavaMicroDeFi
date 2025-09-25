# Enhanced Micrometer JVM Metrics Dashboard

## 🎯 Overview

This document describes the comprehensive JVM metrics monitoring system implemented for the DeFi monitoring platform. The system provides deep insights into JVM performance, business metrics, and application health.

## 📊 Metrics Architecture

### Core Components

1. **Enhanced API Gateway** - Java/Spring Boot with comprehensive Micrometer integration
2. **Prometheus** - Metrics collection and storage
3. **Grafana** - Metrics visualization and dashboards
4. **Custom Business Metrics** - DeFi-specific monitoring

### Metrics Categories

#### 🔧 JVM Metrics (40+ metrics)
- **Memory Management**
  - `jvm_memory_used_bytes{area="heap"}` - Heap memory usage
  - `jvm_memory_committed_bytes{area="heap"}` - Committed heap memory
  - `jvm_memory_max_bytes{area="heap"}` - Maximum heap memory
  - `jvm_memory_used_bytes{area="nonheap"}` - Non-heap memory usage
  - `jvm_memory_pool_used_bytes{pool="*"}` - Memory pool usage

- **Garbage Collection**
  - `jvm_gc_pause_seconds{quantile="0.5,0.95,0.99"}` - GC pause times
  - `jvm_gc_collection_seconds_count` - GC collection count
  - `rate(jvm_gc_collection_seconds_count[5m])` - GC collection rate

- **Thread Management**
  - `jvm_threads_live_threads` - Live thread count
  - `jvm_threads_daemon_threads` - Daemon thread count
  - `jvm_threads_peak_threads` - Peak thread count

- **Class Loading**
  - `jvm_classes_loaded_classes` - Loaded classes count
  - `jvm_classes_unloaded_classes_total` - Unloaded classes total

#### 🌐 HTTP Metrics (15+ metrics)
- **Request Metrics**
  - `http_server_requests_seconds{quantile="0.5,0.95,0.99"}` - Response time percentiles
  - `rate(http_server_requests_seconds_count[5m])` - Request rate
  - `http_server_requests_total{status="*"}` - Requests by status code

- **Gateway Metrics**
  - `gateway_requests_total` - Total gateway requests
  - `gateway_errors_total{error_type="*"}` - Gateway errors by type
  - `gateway_response_time` - Gateway response time
  - `gateway_active_connections` - Active connections

#### 💼 Business Metrics (20+ metrics)
- **Service Performance**
  - `business_blockchain_requests_total` - Blockchain service requests
  - `business_transaction_requests_total` - Transaction service requests
  - `business_math_requests_total` - Math service requests
  - `business_data_requests_total` - Data service requests

- **Error Tracking**
  - `business_blockchain_errors_total` - Blockchain service errors
  - `business_transaction_errors_total` - Transaction service errors
  - `business_math_errors_total` - Math service errors
  - `business_data_errors_total` - Data service errors

- **Business KPIs**
  - `business_transactions_processed_total` - Total transactions processed
  - `business_blocks_processed_total` - Total blocks processed
  - `business_calculations_performed_total` - Total calculations performed
  - `business_data_points_aggregated_total` - Total data points aggregated

- **SLA Monitoring**
  - `business_sla_compliance_ratio` - SLA compliance ratio
  - `business_circuit_breaker_failure_age_seconds` - Circuit breaker failure age

#### 🔄 Circuit Breaker Metrics (8+ metrics)
- `gateway_circuit_breaker_state{circuit_breaker="*"}` - Circuit breaker states
- Circuit breaker metrics for each service:
  - Blockchain service
  - Transaction service
  - Math service
  - Data service

#### 🖥️ System Metrics (10+ metrics)
- **CPU Usage**
  - `system_cpu_usage` - System CPU usage
  - `process_cpu_usage` - Process CPU usage

- **Load Average**
  - `system_load_average_1m` - 1-minute load average
  - `system_load_average_5m` - 5-minute load average
  - `system_load_average_15m` - 15-minute load average

- **Resource Usage**
  - `gateway_memory_usage_bytes` - Gateway memory usage
  - `gateway_thread_pool_active_threads` - Active threads
  - `gateway_thread_pool_daemon_threads` - Daemon threads

## 🚀 Setup Instructions

### Prerequisites
- Docker and Docker Compose
- Java 8+
- Maven 3.6+

### Quick Setup
```bash
# Make the setup script executable
chmod +x setup-enhanced-metrics.sh

# Run the setup script
./setup-enhanced-metrics.sh
```

### Manual Setup

1. **Build the Enhanced API Gateway**
   ```bash
   cd api-gateway
   mvn clean package -DskipTests
   cd ..
   ```

2. **Start Monitoring Stack**
   ```bash
   docker-compose up -d prometheus grafana
   ```

3. **Start API Gateway**
   ```bash
   docker-compose up -d api-gateway
   ```

4. **Import Dashboard**
   - Access Grafana at http://localhost:3000
   - Username: `admin`, Password: `defimon123`
   - Import the dashboard from `api-gateway/jvm-metrics-dashboard.json`

## 📈 Dashboard Features

### Enhanced JVM Metrics Dashboard

The dashboard includes 12 comprehensive panels:

1. **JVM Heap Memory Usage** - Heap memory trends with used/committed/max
2. **JVM Non-Heap Memory Usage** - Non-heap memory monitoring
3. **JVM Thread Metrics** - Live, daemon, and peak thread counts
4. **Garbage Collection Pause Times** - GC performance with percentiles
5. **Garbage Collection Activity** - GC collection rates and counts
6. **JVM Class Loading** - Class loading and unloading metrics
7. **CPU Usage** - System and process CPU utilization
8. **System Load Average** - 1m, 5m, and 15m load averages
9. **HTTP Server Response Times** - Response time percentiles
10. **HTTP Request Rates** - Request throughput monitoring
11. **Circuit Breaker States** - Circuit breaker status for all services
12. **Gateway Resource Usage** - Memory and connection monitoring

### Dashboard Features
- **Auto-refresh**: 5-second intervals for real-time monitoring
- **Time Range**: Configurable (default: last 1 hour)
- **Dark Theme**: Professional appearance
- **Responsive Layout**: Optimized for different screen sizes
- **Alert-ready**: Prepared for alerting rules

## 🔧 Configuration Details

### Application Configuration

The enhanced configuration in `application.yml` includes:

```yaml
management:
  metrics:
    export:
      prometheus:
        enabled: true
        step: 15s
        descriptions: true
        histogram-flavor: prometheus
    distribution:
      percentiles-histogram:
        http.server.requests: true
        http.client.requests: true
      percentiles:
        http.server.requests: 0.5, 0.95, 0.99
        http.client.requests: 0.5, 0.95, 0.99
        jvm.gc.pause: 0.5, 0.95, 0.99
      slo:
        http.server.requests: 50ms, 100ms, 200ms, 300ms, 400ms, 500ms, 1s, 2s, 5s
    tags:
      application: api-gateway
      environment: production
      instance: ${HOSTNAME}:${server.port}
```

### Prometheus Configuration

Enhanced scraping configuration with metric filtering:

```yaml
- job_name: 'api-gateway'
  static_configs:
    - targets: ['api-gateway:8080']
  scrape_interval: 15s
  metrics_path: /actuator/prometheus
  scrape_timeout: 10s
  metric_relabel_configs:
    - source_labels: [__name__]
      regex: 'jvm_.*|http_server_requests.*|gateway_.*|system_.*|process_.*'
      action: keep
```

## 📊 Metrics Endpoints

### Available Endpoints

- **Prometheus Metrics**: `http://localhost:8080/actuator/prometheus`
- **Health Check**: `http://localhost:8080/actuator/health`
- **Application Info**: `http://localhost:8080/actuator/info`
- **Environment**: `http://localhost:8080/actuator/env`
- **Beans**: `http://localhost:8080/actuator/beans`
- **Mappings**: `http://localhost:8080/actuator/mappings`

### Metrics Query Examples

```promql
# JVM Memory Usage
jvm_memory_used_bytes{area="heap"}

# Response Time 95th Percentile
http_server_requests_seconds{quantile="0.95"}

# Request Rate
rate(http_server_requests_seconds_count[5m])

# Error Rate
rate(gateway_errors_total[5m])

# Circuit Breaker State
gateway_circuit_breaker_state{circuit_breaker="blockchain"}

# Business KPI
business_transactions_processed_total
```

## 🚨 Alerting Rules

### Recommended Alerts

```yaml
groups:
  - name: jvm_alerts
    rules:
      - alert: HighMemoryUsage
        expr: jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} > 0.8
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High JVM heap memory usage"
          
      - alert: HighGCUsage
        expr: rate(jvm_gc_collection_seconds_count[5m]) > 10
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High garbage collection activity"
          
      - alert: CircuitBreakerOpen
        expr: gateway_circuit_breaker_state > 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Circuit breaker is open"
```

## 🔍 Troubleshooting

### Common Issues

1. **Metrics not appearing in Prometheus**
   - Check if API Gateway is running: `docker-compose ps`
   - Verify metrics endpoint: `curl http://localhost:8080/actuator/prometheus`
   - Check Prometheus targets: `http://localhost:9091/targets`

2. **Dashboard not loading**
   - Verify Grafana is running: `curl http://localhost:3000/api/health`
   - Check Prometheus datasource configuration
   - Import dashboard manually if needed

3. **High memory usage**
   - Monitor JVM heap usage in dashboard
   - Check for memory leaks using heap dumps
   - Adjust JVM memory settings if needed

### Debug Commands

```bash
# Check service status
docker-compose ps

# View API Gateway logs
docker-compose logs api-gateway

# Check Prometheus targets
curl http://localhost:9091/api/v1/targets

# Test metrics endpoint
curl http://localhost:8080/actuator/prometheus | head -20

# Check Grafana health
curl http://localhost:3000/api/health
```

## 📚 Additional Resources

- [Micrometer Documentation](https://micrometer.io/docs)
- [Prometheus Query Language](https://prometheus.io/docs/prometheus/latest/querying/)
- [Grafana Dashboard Documentation](https://grafana.com/docs/grafana/latest/dashboards/)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)

## 🎯 Performance Optimization

### JVM Tuning Recommendations

```bash
# Add to application startup
JAVA_OPTS="-Xms2g -Xmx4g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+UseStringDeduplication"
```

### Monitoring Best Practices

1. **Set up proper alerting** for critical metrics
2. **Monitor trends** rather than absolute values
3. **Use percentiles** for response time monitoring
4. **Track business KPIs** alongside technical metrics
5. **Regular capacity planning** based on metrics trends

---

**Total Metrics Available: 100+ comprehensive metrics covering JVM, HTTP, business, and system performance.**
