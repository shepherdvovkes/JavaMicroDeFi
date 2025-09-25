#!/bin/bash

# Docker Storage Optimization Script for Ethereum Data
# This script optimizes Docker storage driver and mount options for blockchain data

set -e

echo "🚀 Starting Docker Storage Optimization for Ethereum Data"
echo "========================================================"

# Check if running as root
if [[ $EUID -ne 0 ]]; then
   echo "❌ This script must be run as root (use sudo)"
   exit 1
fi

# Check if Docker is installed
if ! command -v docker >/dev/null 2>&1; then
    echo "❌ Docker is not installed"
    exit 1
fi

# Configuration
ETHEREUM_DATA_DIR="/mnt/blockchain-disk"
DOCKER_DATA_DIR="/var/lib/docker"
BACKUP_DIR="/tmp/docker-backup"

echo "📱 Current Docker Configuration:"
echo "  - Docker version: $(docker --version)"
echo "  - Storage driver: $(docker info --format '{{.Driver}}')"
echo "  - Data directory: $DOCKER_DATA_DIR"

# Backup current Docker configuration
echo "💾 Backing up current Docker configuration..."
mkdir -p "$BACKUP_DIR"
cp /etc/docker/daemon.json "$BACKUP_DIR/daemon.json.backup" 2>/dev/null || echo "No existing daemon.json found"
docker info > "$BACKUP_DIR/docker-info.txt"
echo "✅ Backed up Docker configuration"

# Stop Docker service
echo "⏹️  Stopping Docker service..."
systemctl stop docker
echo "✅ Docker service stopped"

# Optimize Docker daemon configuration
echo "⚙️  Optimizing Docker daemon configuration..."
cat > /etc/docker/daemon.json << 'EOF'
{
  "storage-driver": "overlay2",
  "storage-opts": [
    "overlay2.override_kernel_check=true",
    "overlay2.size=20G"
  ],
  "log-driver": "json-file",
  "log-opts": {
    "max-size": "100m",
    "max-file": "3"
  },
  "data-root": "/var/lib/docker",
  "exec-opts": ["native.cgroupdriver=systemd"],
  "live-restore": true,
  "userland-proxy": false,
  "experimental": false,
  "metrics-addr": "127.0.0.1:9323",
  "default-ulimits": {
    "nofile": {
      "Name": "nofile",
      "Hard": 1048576,
      "Soft": 1048576
    },
    "nproc": {
      "Name": "nproc",
      "Hard": 1048576,
      "Soft": 1048576
    }
  },
  "default-shm-size": "2G",
  "shutdown-timeout": 30,
  "max-concurrent-downloads": 10,
  "max-concurrent-uploads": 10,
  "registry-mirrors": [],
  "insecure-registries": [],
  "dns": ["8.8.8.8", "8.8.4.4"],
  "ipv6": false,
  "fixed-cidr": "",
  "default-gateway": "",
  "bridge": "",
  "bip": "",
  "ip-masq": true,
  "iptables": true,
  "ip-forward": true,
  "ip": "0.0.0.0",
  "icc": true,
  "raw-logs": false,
  "selinux-enabled": false,
  "userns-remap": "",
  "group": "docker",
  "cgroup-parent": "",
  "default-address-pools": [
    {
      "base": "172.17.0.0/12",
      "size": 16
    }
  ]
}
EOF

echo "✅ Created optimized Docker daemon configuration"

# Optimize Docker storage directory
echo "⚙️  Optimizing Docker storage directory..."
if [[ -d "$DOCKER_DATA_DIR" ]]; then
    # Set optimal permissions
    chown -R root:docker "$DOCKER_DATA_DIR"
    chmod -R 755 "$DOCKER_DATA_DIR"
    
    # Clean up old containers and images
    echo "  - Cleaning up old Docker data..."
    docker system prune -af --volumes 2>/dev/null || echo "    No old data to clean"
    
    echo "✅ Optimized Docker storage directory"
else
    echo "  - Creating Docker storage directory..."
    mkdir -p "$DOCKER_DATA_DIR"
    chown -R root:docker "$DOCKER_DATA_DIR"
    chmod -R 755 "$DOCKER_DATA_DIR"
    echo "✅ Created Docker storage directory"
fi

# Create optimized Docker Compose configuration
echo "⚙️  Creating optimized Docker Compose configuration..."
cat > /home/vovkes/JavaMicroDeFi/docker-compose-optimized.yml << 'EOF'
version: '3.8'

services:
  erigon:
    image: thorax/erigon:latest
    container_name: erigon
    restart: unless-stopped
    ports:
      - "8545:8545"
      - "8546:8546"
      - "30303:30303"
      - "30303:30303/udp"
    volumes:
      - /mnt/ethereum-data/erigon:/root/.local/share/erigon
    environment:
      - ERIGON_DATADIR=/root/.local/share/erigon
    command: >
      --datadir=/root/.local/share/erigon
      --chain=mainnet
      --http
      --http.addr=0.0.0.0
      --http.port=8545
      --http.api=eth,erigon,web3,net,debug,trace,txpool,parity,ots
      --http.corsdomain=*
      --ws
      --ws.addr=0.0.0.0
      --ws.port=8546
      --ws.api=eth,erigon,web3,net,debug,trace,txpool,parity,ots
      --private.api.addr=0.0.0.0:9090
      --torrent.port=42068
      --torrent.upload.rate=512mb
      --torrent.download.rate=512mb
      --torrent.conns=100
      --p2p
      --maxpeers=100
      --nat=extip:$(curl -s ifconfig.me)
      --metrics
      --metrics.addr=0.0.0.0
      --metrics.port=6060
    ulimits:
      nofile:
        soft: 1048576
        hard: 1048576
      nproc:
        soft: 1048576
        hard: 1048576
    shm_size: 2g
    deploy:
      resources:
        limits:
          memory: 120G
        reservations:
          memory: 100G
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8545"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s

  mongodb:
    image: mongo:7.0
    container_name: mongodb
    restart: unless-stopped
    ports:
      - "27017:27017"
    volumes:
      - mongodb_data:/data/db
      - /mnt/ethereum-data/mongodb:/data/backup
    environment:
      - MONGO_INITDB_ROOT_USERNAME=admin
      - MONGO_INITDB_ROOT_PASSWORD=password
    ulimits:
      nofile:
        soft: 65536
        hard: 65536
    deploy:
      resources:
        limits:
          memory: 8G
        reservations:
          memory: 4G

  kafka:
    image: confluentinc/cp-kafka:latest
    container_name: kafka
    restart: unless-stopped
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: true
      KAFKA_LOG_RETENTION_HOURS: 168
      KAFKA_LOG_SEGMENT_BYTES: 1073741824
      KAFKA_LOG_RETENTION_CHECK_INTERVAL_MS: 300000
    volumes:
      - kafka_data:/var/lib/kafka/data
    depends_on:
      - zookeeper
    ulimits:
      nofile:
        soft: 65536
        hard: 65536
    deploy:
      resources:
        limits:
          memory: 4G
        reservations:
          memory: 2G

  zookeeper:
    image: confluentinc/cp-zookeeper:latest
    container_name: zookeeper
    restart: unless-stopped
    ports:
      - "2181:2181"
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000
    volumes:
      - zookeeper_data:/var/lib/zookeeper/data
    ulimits:
      nofile:
        soft: 65536
        hard: 65536
    deploy:
      resources:
        limits:
          memory: 2G
        reservations:
          memory: 1G

volumes:
  mongodb_data:
    driver: local
    driver_opts:
      type: none
      o: bind
      device: /mnt/ethereum-data/mongodb
  kafka_data:
    driver: local
    driver_opts:
      type: none
      o: bind
      device: /mnt/ethereum-data/kafka
  zookeeper_data:
    driver: local
    driver_opts:
      type: none
      o: bind
      device: /mnt/ethereum-data/zookeeper
EOF

echo "✅ Created optimized Docker Compose configuration"

# Create Docker performance monitoring script
echo "🔍 Creating Docker performance monitoring script..."
cat > /usr/local/bin/monitor-docker-performance.sh << 'EOF'
#!/bin/bash
# Docker performance monitor for Ethereum services

LOG_FILE="/var/log/docker-performance.log"
ETHEREUM_DATA_DIR="/mnt/blockchain-disk"

monitor_docker() {
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    
    echo "[$timestamp] Docker Performance Monitor" >> "$LOG_FILE"
    
    # Check Docker service status
    if systemctl is-active --quiet docker; then
        echo "  - Docker service: RUNNING" >> "$LOG_FILE"
    else
        echo "  - Docker service: STOPPED" >> "$LOG_FILE"
        return 1
    fi
    
    # Check container status
    local containers=$(docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" | grep -E "(erigon|mongodb|kafka|zookeeper)")
    echo "  - Containers:" >> "$LOG_FILE"
    echo "$containers" | while read line; do
        echo "    $line" >> "$LOG_FILE"
    done
    
    # Check disk usage
    local docker_usage=$(df -h /var/lib/docker | tail -1 | awk '{print $5}' | sed 's/%//')
    local ethereum_usage=$(df -h "$ETHEREUM_DATA_DIR" | tail -1 | awk '{print $5}' | sed 's/%//')
    
    echo "  - Docker disk usage: ${docker_usage}%" >> "$LOG_FILE"
    echo "  - Ethereum data usage: ${ethereum_usage}%" >> "$LOG_FILE"
    
    # Check memory usage
    local memory_usage=$(docker stats --no-stream --format "table {{.Container}}\t{{.CPUPerc}}\t{{.MemUsage}}" | grep -E "(erigon|mongodb|kafka|zookeeper)")
    echo "  - Memory usage:" >> "$LOG_FILE"
    echo "$memory_usage" | while read line; do
        echo "    $line" >> "$LOG_FILE"
    done
    
    # Check for errors
    if [[ $docker_usage -gt 90 ]]; then
        echo "  - WARNING: Docker disk usage above 90%" >> "$LOG_FILE"
        logger -t docker-monitor "WARNING: Docker disk usage at ${docker_usage}%"
    fi
    
    if [[ $ethereum_usage -gt 95 ]]; then
        echo "  - WARNING: Ethereum data usage above 95%" >> "$LOG_FILE"
        logger -t docker-monitor "WARNING: Ethereum data usage at ${ethereum_usage}%"
    fi
    
    echo "  - Monitor completed" >> "$LOG_FILE"
    echo "" >> "$LOG_FILE"
}

monitor_docker
EOF

chmod +x /usr/local/bin/monitor-docker-performance.sh

# Create systemd service for Docker performance monitoring
cat > /etc/systemd/system/docker-performance-monitor.service << EOF
[Unit]
Description=Docker Performance Monitor
After=docker.service

[Service]
Type=oneshot
ExecStart=/usr/local/bin/monitor-docker-performance.sh
User=root
EOF

# Create timer for periodic monitoring
cat > /etc/systemd/system/docker-performance-monitor.timer << EOF
[Unit]
Description=Run Docker Performance Monitor
Requires=docker-performance-monitor.service

[Timer]
OnCalendar=*:0/5
Persistent=true

[Install]
WantedBy=timers.target
EOF

systemctl daemon-reload
systemctl enable docker-performance-monitor.timer
systemctl start docker-performance-monitor.timer

echo "✅ Set up Docker performance monitoring"

# Start Docker service
echo "▶️  Starting Docker service..."
systemctl start docker
echo "✅ Docker service started"

# Wait for Docker to be ready
echo "⏳ Waiting for Docker to be ready..."
sleep 10

# Test Docker configuration
echo "🧪 Testing Docker configuration..."
docker info --format '{{.Driver}}' > /dev/null
echo "✅ Docker configuration test passed"

# Create Docker cleanup script
echo "🧹 Creating Docker cleanup script..."
cat > /usr/local/bin/cleanup-docker.sh << 'EOF'
#!/bin/bash
# Docker cleanup script for Ethereum services

echo "🧹 Starting Docker cleanup..."

# Stop all containers
echo "  - Stopping all containers..."
docker stop $(docker ps -aq) 2>/dev/null || echo "    No containers to stop"

# Remove all containers
echo "  - Removing all containers..."
docker rm $(docker ps -aq) 2>/dev/null || echo "    No containers to remove"

# Remove all images
echo "  - Removing all images..."
docker rmi $(docker images -aq) 2>/dev/null || echo "    No images to remove"

# Remove all volumes
echo "  - Removing all volumes..."
docker volume rm $(docker volume ls -q) 2>/dev/null || echo "    No volumes to remove"

# Remove all networks
echo "  - Removing all networks..."
docker network rm $(docker network ls -q) 2>/dev/null || echo "    No networks to remove"

# System prune
echo "  - Running system prune..."
docker system prune -af --volumes

echo "✅ Docker cleanup completed"
EOF

chmod +x /usr/local/bin/cleanup-docker.sh

echo "✅ Created Docker cleanup script"

# Display final status
echo ""
echo "🎉 Docker Storage Optimization Completed!"
echo "========================================"
echo "📱 Docker version: $(docker --version)"
echo "💾 Storage driver: $(docker info --format '{{.Driver}}')"
echo "📊 Data directory: $DOCKER_DATA_DIR"
echo "🔍 Monitor script: /usr/local/bin/monitor-docker-performance.sh"
echo "🧹 Cleanup script: /usr/local/bin/cleanup-docker.sh"
echo "📋 Optimized compose: docker-compose-optimized.yml"
echo ""
echo "💡 Next steps:"
echo "   1. Run: sudo /usr/local/bin/monitor-docker-performance.sh"
echo "   2. Monitor: tail -f /var/log/docker-performance.log"
echo "   3. Use: docker-compose -f docker-compose-optimized.yml up -d"
echo "   4. Check: docker ps"
