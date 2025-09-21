# Basic JVM Metrics Setup - Java 8 Compatible

## 🎯 Overview

This document describes the basic JVM metrics monitoring system implemented for the DeFi monitoring platform. The system provides essential JVM insights using Spring Boot Actuator and Micrometer with Java 8 compatibility.

## 📊 Metrics Architecture

### Core Components

1. **API Gateway** - Java 8/Spring Boot with basic Micrometer integration
2. **Prometheus** - Metrics collection and storage
3. **Grafana** - Metrics visualization and dashboards

### Metrics Categories

#### 🔧 JVM Metrics (Available via Spring Boot Actuator)
- **Memory Management**
  - `jvm_memory_used_bytes{area="heap"}` - Heap memory usage
  - `jvm_memory_committed_bytes{area="heap"}` - Committed heap memory
  - `jvm_memory_max_bytes{area="heap"}` - Maximum heap memory
  - `jvm_memory_used_bytes{area="nonheap"}` - Non-heap memory usage

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

#### 🌐 HTTP Metrics
- **Request Metrics**
  - `http_server_requests_seconds{quantile="0.5,0.95,0.99"}` - Response time percentiles
  - `rate(http_server_requests_seconds_count[5m])` - Request rate
  - `http_server_requests_total{status="*"}` - Requests by status code

#### 🖥️ System Metrics
- **CPU Usage**
  - `system_cpu_usage` - System CPU usage
  - `process_cpu_usage` - Process CPU usage

- **Load Average**
  - `system_load_average_1m` - 1-minute load average
  - `system_load_average_5m` - 5-minute load average
  - `system_load_average_15m` - 15-minute load average

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

1. **Build the API Gateway**
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
   - Import the dashboard from `api-gateway/basic-jvm-metrics-dashboard.json`

## 📈 Dashboard Features

### Basic JVM Metrics Dashboard

The dashboard includes 6 comprehensive panels:

1. **JVM Heap Memory Usage** - Heap memory trends with used/committed/max
2. **JVM Thread Metrics** - Live, daemon, and peak thread counts
3. **Garbage Collection Pause Times** - GC performance with percentiles
4. **CPU Usage** - System and process CPU utilization
5. **HTTP Server Response Times** - Response time percentiles
6. **HTTP Request Rate** - Request throughput monitoring

### Dashboard Features
- **Auto-refresh**: 5-second intervals for real-time monitoring
- **Time Range**: Configurable (default: last 1 hour)
- **Dark Theme**: Professional appearance
- **Responsive Layout**: Optimized for different screen sizes

## 🔧 Configuration Details

### Application Configuration

The basic configuration in `application.yml`:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
    metrics:
      enabled: true
    prometheus:
      enabled: true
  metrics:
    export:
      prometheus:
        enabled: true
        step: 15s
        descriptions: true
    tags:
      application: ${spring.application.name}
      environment: ${spring.profiles.active:default}
```

### Prometheus Configuration

Basic scraping configuration:

```yaml
- job_name: 'api-gateway'
  static_configs:
    - targets: ['api-gateway:8080']
  scrape_interval: 15s
  metrics_path: /actuator/prometheus
  scrape_timeout: 10s
```

## 📊 Metrics Endpoints

### Available Endpoints

- **Prometheus Metrics**: `http://localhost:8080/actuator/prometheus`
- **Health Check**: `http://localhost:8080/actuator/health`
- **Application Info**: `http://localhost:8080/actuator/info`

### Metrics Query Examples

```promql
# JVM Memory Usage
jvm_memory_used_bytes{area="heap"}

# Response Time 95th Percentile
http_server_requests_seconds{quantile="0.95"}

# Request Rate
rate(http_server_requests_seconds_count[5m])

# CPU Usage
system_cpu_usage
```

## 🚨 Alerting Rules

### Recommended Alerts

```yaml
groups:
  - name: basic_jvm_alerts
    rules:
      - alert: HighMemoryUsage
        expr: jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} > 0.8
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High JVM heap memory usage"
          
      - alert: HighResponseTime
        expr: http_server_requests_seconds{quantile="0.95"} > 5
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High HTTP response time"
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
JAVA_OPTS="-Xms2g -Xmx4g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
```

### Monitoring Best Practices

1. **Set up proper alerting** for critical metrics
2. **Monitor trends** rather than absolute values
3. **Use percentiles** for response time monitoring
4. **Regular capacity planning** based on metrics trends

---

**Total Metrics Available: 20+ essential JVM, HTTP, and system metrics covering core application performance.**
