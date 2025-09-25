#!/bin/bash

# SSD Cache Setup Script for Java Micro DeFi
# This script sets up the complete SSD cache infrastructure

set -e

echo "🚀 Setting up SSD Cache Infrastructure for Java Micro DeFi"
echo "========================================================="

# Check if running as root
if [[ $EUID -ne 0 ]]; then
   echo "❌ This script must be run as root (use sudo)"
   exit 1
fi

# Check if Docker is installed
if ! command -v docker &> /dev/null; then
    echo "❌ Docker is not installed. Please install Docker first."
    exit 1
fi

# Check if Docker Compose is installed
if ! command -v docker-compose &> /dev/null; then
    echo "❌ Docker Compose is not installed. Please install Docker Compose first."
    exit 1
fi

# Create cache directories
echo "📁 Creating cache directories..."
mkdir -p /opt/ssd-cache/{memory,ssd,metrics}
mkdir -p /var/log/ssd-cache
chmod 755 /opt/ssd-cache
chmod 755 /opt/ssd-cache/memory
chmod 755 /opt/ssd-cache/ssd
chmod 755 /opt/ssd-cache/metrics
chmod 755 /var/log/ssd-cache

# Run SSD optimization
echo "⚙️  Running SSD optimization..."
if [[ -f "/home/vovkes/JavaMicroDeFi/optimize-ssd-cache.sh" ]]; then
    /home/vovkes/JavaMicroDeFi/optimize-ssd-cache.sh
else
    echo "⚠️  SSD optimization script not found, skipping..."
fi

# Create MongoDB initialization script
echo "📝 Creating MongoDB initialization script..."
cat > /home/vovkes/JavaMicroDeFi/ssd-cache-service/mongodb-init.js << 'EOF'
// MongoDB initialization for SSD Cache Service
db = db.getSiblingDB('cache_metrics');

// Create collections
db.createCollection('cache_metrics');
db.createCollection('access_patterns');
db.createCollection('performance_data');

// Create indexes
db.cache_metrics.createIndex({ "timestamp": 1 });
db.cache_metrics.createIndex({ "key": 1 });
db.cache_metrics.createIndex({ "tier": 1 });

db.access_patterns.createIndex({ "key": 1 });
db.access_patterns.createIndex({ "timestamp": 1 });
db.access_patterns.createIndex({ "tier": 1 });

db.performance_data.createIndex({ "timestamp": 1 });
db.performance_data.createIndex({ "metric_type": 1 });

// Create user for cache service
db.createUser({
  user: "cache_user",
  pwd: "cache_password",
  roles: [
    { role: "readWrite", db: "cache_metrics" }
  ]
});

print("MongoDB initialization completed for SSD Cache Service");
EOF

# Create Prometheus configuration
echo "📊 Creating Prometheus configuration..."
cat > /home/vovkes/JavaMicroDeFi/ssd-cache-service/prometheus.yml << 'EOF'
global:
  scrape_interval: 15s
  evaluation_interval: 15s

rule_files:
  - "cache_rules.yml"

scrape_configs:
  - job_name: 'ssd-cache-service'
    static_configs:
      - targets: ['ssd-cache-service:8088']
    metrics_path: '/actuator/prometheus'
    scrape_interval: 30s

  - job_name: 'redis'
    static_configs:
      - targets: ['redis:6379']
    scrape_interval: 30s

  - job_name: 'mongodb'
    static_configs:
      - targets: ['mongodb:27017']
    scrape_interval: 30s

  - job_name: 'kafka'
    static_configs:
      - targets: ['kafka:9092']
    scrape_interval: 30s
EOF

# Create Grafana datasource configuration
echo "📈 Creating Grafana datasource configuration..."
mkdir -p /home/vovkes/JavaMicroDeFi/ssd-cache-service/grafana-datasources
cat > /home/vovkes/JavaMicroDeFi/ssd-cache-service/grafana-datasources/prometheus.yml << 'EOF'
apiVersion: 1

datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: true
    editable: true
EOF

# Create Grafana dashboard configuration
echo "📊 Creating Grafana dashboard configuration..."
mkdir -p /home/vovkes/JavaMicroDeFi/ssd-cache-service/grafana-dashboards
cat > /home/vovkes/JavaMicroDeFi/ssd-cache-service/grafana-dashboards/cache-dashboard.yml << 'EOF'
apiVersion: 1

providers:
  - name: 'SSD Cache Dashboards'
    orgId: 1
    folder: 'SSD Cache'
    type: file
    disableDeletion: false
    updateIntervalSeconds: 10
    allowUiUpdates: true
    options:
      path: /etc/grafana/provisioning/dashboards
EOF

# Create cache dashboard JSON
cat > /home/vovkes/JavaMicroDeFi/ssd-cache-service/grafana-dashboards/ssd-cache-dashboard.json << 'EOF'
{
  "dashboard": {
    "id": null,
    "title": "SSD Cache Performance",
    "tags": ["cache", "ssd", "performance"],
    "style": "dark",
    "timezone": "browser",
    "panels": [
      {
        "id": 1,
        "title": "Cache Hit Ratio",
        "type": "stat",
        "targets": [
          {
            "expr": "cache_hit_ratio",
            "legendFormat": "Hit Ratio"
          }
        ],
        "fieldConfig": {
          "defaults": {
            "unit": "percent",
            "min": 0,
            "max": 100
          }
        },
        "gridPos": {"h": 8, "w": 12, "x": 0, "y": 0}
      },
      {
        "id": 2,
        "title": "Cache Operations",
        "type": "graph",
        "targets": [
          {
            "expr": "cache_operations_total",
            "legendFormat": "Total Operations"
          },
          {
            "expr": "cache_hits_total",
            "legendFormat": "Cache Hits"
          },
          {
            "expr": "cache_misses_total",
            "legendFormat": "Cache Misses"
          }
        ],
        "gridPos": {"h": 8, "w": 12, "x": 12, "y": 0}
      },
      {
        "id": 3,
        "title": "Cache Latency",
        "type": "graph",
        "targets": [
          {
            "expr": "cache_latency_seconds",
            "legendFormat": "Average Latency"
          }
        ],
        "gridPos": {"h": 8, "w": 12, "x": 0, "y": 8}
      },
      {
        "id": 4,
        "title": "Cache Memory Usage",
        "type": "graph",
        "targets": [
          {
            "expr": "cache_memory_usage_bytes",
            "legendFormat": "Memory Usage"
          }
        ],
        "gridPos": {"h": 8, "w": 12, "x": 12, "y": 8}
      }
    ],
    "time": {
      "from": "now-1h",
      "to": "now"
    },
    "refresh": "30s"
  }
}
EOF

# Build the SSD cache service
echo "🔨 Building SSD cache service..."
cd /home/vovkes/JavaMicroDeFi/ssd-cache-service

# Check if Maven is available
if command -v mvn &> /dev/null; then
    echo "📦 Building with Maven..."
    mvn clean package -DskipTests
else
    echo "⚠️  Maven not found, using Docker build..."
fi

# Start the services
echo "🚀 Starting SSD cache services..."
docker-compose up -d

# Wait for services to be ready
echo "⏳ Waiting for services to be ready..."
sleep 30

# Check service health
echo "🔍 Checking service health..."
services=("ssd-cache-service" "redis" "mongodb" "kafka" "prometheus" "grafana")

for service in "${services[@]}"; do
    if docker ps | grep -q "$service"; then
        echo "✅ $service is running"
    else
        echo "❌ $service is not running"
    fi
done

# Test cache service
echo "🧪 Testing cache service..."
if curl -f http://localhost:8088/api/cache/health > /dev/null 2>&1; then
    echo "✅ Cache service is responding"
else
    echo "❌ Cache service is not responding"
fi

# Display service URLs
echo ""
echo "🎉 SSD Cache Infrastructure Setup Complete!"
echo "=========================================="
echo ""
echo "Service URLs:"
echo "  Cache Service: http://localhost:8088"
echo "  Cache Health: http://localhost:8088/api/cache/health"
echo "  Cache Statistics: http://localhost:8088/api/cache/statistics"
echo "  Prometheus: http://localhost:9090"
echo "  Grafana: http://localhost:3000 (admin/admin123)"
echo ""
echo "Cache Directories:"
echo "  Memory Cache: /opt/ssd-cache/memory"
echo "  SSD Cache: /opt/ssd-cache/ssd"
echo "  Metrics: /opt/ssd-cache/metrics"
echo "  Logs: /var/log/ssd-cache"
echo ""
echo "Monitoring Commands:"
echo "  Monitor SSD Cache: /usr/local/bin/monitor-ssd-cache.sh"
echo "  Test SSD Cache: /usr/local/bin/test-ssd-cache.sh"
echo "  Optimize SSD Cache: /usr/local/bin/optimize-ssd-cache.sh"
echo ""
echo "Docker Commands:"
echo "  View logs: docker-compose logs -f ssd-cache-service"
echo "  Restart service: docker-compose restart ssd-cache-service"
echo "  Stop services: docker-compose down"
echo "  Start services: docker-compose up -d"
echo ""
echo "📊 Performance Monitoring:"
echo "  - Check Grafana dashboard for cache performance metrics"
echo "  - Monitor Prometheus for detailed metrics"
echo "  - Use cache statistics API for real-time monitoring"
echo ""
echo "🔧 Optimization:"
echo "  - Cache optimization runs automatically every 5 minutes"
echo "  - Manual optimization: curl -X POST http://localhost:8088/api/cache/optimize"
echo "  - Clear cache: curl -X DELETE http://localhost:8088/api/cache/{key}"
echo ""
echo "💡 Next Steps:"
echo "  1. Configure your microservices to use the cache service"
echo "  2. Set up cache integration in your blockchain sync services"
echo "  3. Monitor performance and adjust cache settings as needed"
echo "  4. Review cache statistics regularly for optimization opportunities"

