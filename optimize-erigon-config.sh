#!/bin/bash

# ERIGON Configuration Optimization Script
# This script optimizes ERIGON configuration for better I/O performance

set -e

echo "🚀 Starting ERIGON Configuration Optimization"
echo "============================================="

# Configuration
ETHEREUM_DATA_DIR="/mnt/blockchain-disk"
ERIGON_DATA_DIR="$ETHEREUM_DATA_DIR/erigon"
CONFIG_DIR="$ERIGON_DATA_DIR/config"
BACKUP_DIR="/tmp/erigon-backup"

echo "📱 ERIGON Configuration:"
echo "  - Data directory: $ERIGON_DATA_DIR"
echo "  - Config directory: $CONFIG_DIR"

# Create directories
mkdir -p "$ERIGON_DATA_DIR"
mkdir -p "$CONFIG_DIR"
mkdir -p "$BACKUP_DIR"

# Backup existing configuration
echo "💾 Backing up existing ERIGON configuration..."
if [[ -f "$CONFIG_DIR/erigon.toml" ]]; then
    cp "$CONFIG_DIR/erigon.toml" "$BACKUP_DIR/erigon.toml.backup"
    echo "✅ Backed up erigon.toml"
fi

# Create optimized ERIGON configuration
echo "⚙️  Creating optimized ERIGON configuration..."
cat > "$CONFIG_DIR/erigon.toml" << 'EOF'
# ERIGON Optimized Configuration for High-Performance I/O
# This configuration is optimized for NVMe storage and high-throughput blockchain sync

[chain]
# Chain configuration
name = "mainnet"
id = 1

[eth]
# Ethereum configuration
private.api.addr = "0.0.0.0:9090"
http.api = ["eth", "erigon", "web3", "net", "debug", "trace", "txpool", "parity", "ots"]
ws.api = ["eth", "erigon", "web3", "net", "debug", "trace", "txpool", "parity", "ots"]

[http]
# HTTP RPC configuration
enabled = true
addr = "0.0.0.0"
port = 8545
corsdomain = ["*"]
vhosts = ["*"]

[ws]
# WebSocket RPC configuration
enabled = true
addr = "0.0.0.0"
port = 8546
origins = ["*"]

[p2p]
# P2P networking configuration
enabled = true
maxpeers = 100
maxpendpeers = 50
nat = "extip"
discovery.v5 = true
netrestrict = ""

[torrent]
# Torrent configuration for OtterSync
enabled = true
port = 42068
upload.rate = "512mb"
download.rate = "512mb"
conns = 100
seed = true

[snapshots]
# Snapshot configuration
enabled = true
keep = 2
no.downloader = false
verify = true

[db]
# Database configuration
path = "/root/.local/share/erigon"
readonly = false
batch.size = 10000
batch.delay = 100ms
cache.size = 2GB
tables.compression = "snappy"
tables.compaction = "leveled"

[txpool]
# Transaction pool configuration
enabled = true
max = 10000
maxslots = 10000
price.limit = 1000000000
price.bump = 10

[gpo]
# Gas price oracle configuration
blocks = 20
percentile = 60
max.price = 500000000000
ignore.price = 2

[clique]
# Clique consensus configuration
enabled = false

[ethash]
# Ethash consensus configuration
enabled = false

[bor]
# Bor consensus configuration
enabled = false

[heimdall]
# Heimdall configuration
enabled = false

[metrics]
# Metrics configuration
enabled = true
addr = "0.0.0.0"
port = 6060
path = "/debug/metrics"

[pprof]
# Profiling configuration
enabled = false
addr = "0.0.0.0"
port = 6060
path = "/debug/pprof"

[log]
# Logging configuration
level = "info"
format = "json"
file = "/root/.local/share/erigon/erigon.log"
max.size = 100
max.backups = 3
max.age = 28
compress = true

[download]
# Download configuration
enabled = true
max.workers = 10
max.bandwidth = "1gb"
timeout = 30s

[state]
# State configuration
enabled = true
cache.size = "1gb"
cache.blocks = 1000

[blockchain]
# Blockchain configuration
enabled = true
cache.size = "1gb"
cache.blocks = 1000

[consensus]
# Consensus configuration
enabled = true
cache.size = "512mb"
cache.blocks = 500

[execution]
# Execution configuration
enabled = true
cache.size = "512mb"
cache.blocks = 500

[storage]
# Storage configuration
enabled = true
cache.size = "2gb"
cache.blocks = 2000
compression = "snappy"
compaction = "leveled"

[network]
# Network configuration
enabled = true
max.connections = 100
max.inbound = 50
max.outbound = 50
dial.timeout = 30s
handshake.timeout = 10s

[security]
# Security configuration
enabled = true
max.request.size = "10mb"
max.response.size = "100mb"
timeout = 30s

[performance]
# Performance configuration
enabled = true
max.workers = 16
max.memory = "8gb"
max.cpu = 8
gc.interval = "5m"
gc.threshold = 0.8

[monitoring]
# Monitoring configuration
enabled = true
interval = "30s"
timeout = "10s"
retries = 3

[backup]
# Backup configuration
enabled = true
interval = "24h"
retention = "7d"
path = "/root/.local/share/erigon/backup"

[maintenance]
# Maintenance configuration
enabled = true
interval = "1h"
cleanup.interval = "24h"
vacuum.interval = "168h"
EOF

echo "✅ Created optimized ERIGON configuration"

# Create ERIGON startup script with optimized parameters
echo "⚙️  Creating optimized ERIGON startup script..."
cat > /usr/local/bin/start-erigon-optimized.sh << 'EOF'
#!/bin/bash
# Optimized ERIGON startup script

set -e

# Configuration
ETHEREUM_DATA_DIR="/mnt/blockchain-disk"
ERIGON_DATA_DIR="$ETHEREUM_DATA_DIR/erigon"
CONFIG_FILE="$ERIGON_DATA_DIR/config/erigon.toml"

# Check if data directory exists
if [[ ! -d "$ERIGON_DATA_DIR" ]]; then
    echo "❌ ERIGON data directory not found: $ERIGON_DATA_DIR"
    exit 1
fi

# Check if config file exists
if [[ ! -f "$CONFIG_FILE" ]]; then
    echo "❌ ERIGON config file not found: $CONFIG_FILE"
    exit 1
fi

# Set optimal environment variables
export GOMAXPROCS=$(nproc)
export GOGC=100
export GOMEMLIMIT=8GiB

# Set optimal ulimits
ulimit -n 1048576
ulimit -u 1048576

# Start ERIGON with optimized parameters
echo "🚀 Starting ERIGON with optimized configuration..."
echo "  - Data directory: $ERIGON_DATA_DIR"
echo "  - Config file: $CONFIG_FILE"
echo "  - Max processes: $GOMAXPROCS"
echo "  - Memory limit: $GOMEMLIMIT"

exec erigon \
    --datadir="$ERIGON_DATA_DIR" \
    --config="$CONFIG_FILE" \
    --chain=mainnet \
    --http \
    --http.addr=0.0.0.0 \
    --http.port=8545 \
    --http.api=eth,erigon,web3,net,debug,trace,txpool,parity,ots \
    --http.corsdomain=* \
    --ws \
    --ws.addr=0.0.0.0 \
    --ws.port=8546 \
    --ws.api=eth,erigon,web3,net,debug,trace,txpool,parity,ots \
    --private.api.addr=0.0.0.0:9090 \
    --torrent.port=42068 \
    --torrent.upload.rate=512mb \
    --torrent.download.rate=512mb \
    --torrent.conns=100 \
    --p2p \
    --maxpeers=100 \
    --nat=extip:$(curl -s ifconfig.me) \
    --metrics \
    --metrics.addr=0.0.0.0 \
    --metrics.port=6060 \
    --db.cache=2GB \
    --db.batch.size=10000 \
    --db.batch.delay=100ms \
    --txpool.max=10000 \
    --txpool.maxslots=10000 \
    --txpool.price.limit=1000000000 \
    --txpool.price.bump=10 \
    --gpo.blocks=20 \
    --gpo.percentile=60 \
    --gpo.max.price=500000000000 \
    --gpo.ignore.price=2 \
    --log.level=info \
    --log.format=json \
    --log.file="$ERIGON_DATA_DIR/erigon.log" \
    --log.max.size=100 \
    --log.max.backups=3 \
    --log.max.age=28 \
    --log.compress=true \
    --download.max.workers=10 \
    --download.max.bandwidth=1gb \
    --download.timeout=30s \
    --state.cache.size=1gb \
    --state.cache.blocks=1000 \
    --blockchain.cache.size=1gb \
    --blockchain.cache.blocks=1000 \
    --consensus.cache.size=512mb \
    --consensus.cache.blocks=500 \
    --execution.cache.size=512mb \
    --execution.cache.blocks=500 \
    --storage.cache.size=2gb \
    --storage.cache.blocks=2000 \
    --storage.compression=snappy \
    --storage.compaction=leveled \
    --network.max.connections=100 \
    --network.max.inbound=50 \
    --network.max.outbound=50 \
    --network.dial.timeout=30s \
    --network.handshake.timeout=10s \
    --security.max.request.size=10mb \
    --security.max.response.size=100mb \
    --security.timeout=30s \
    --performance.max.workers=16 \
    --performance.max.memory=8gb \
    --performance.max.cpu=8 \
    --performance.gc.interval=5m \
    --performance.gc.threshold=0.8 \
    --monitoring.interval=30s \
    --monitoring.timeout=10s \
    --monitoring.retries=3 \
    --backup.interval=24h \
    --backup.retention=7d \
    --backup.path="$ERIGON_DATA_DIR/backup" \
    --maintenance.interval=1h \
    --maintenance.cleanup.interval=24h \
    --maintenance.vacuum.interval=168h
EOF

chmod +x /usr/local/bin/start-erigon-optimized.sh

echo "✅ Created optimized ERIGON startup script"

# Create ERIGON performance monitoring script
echo "🔍 Creating ERIGON performance monitoring script..."
cat > /usr/local/bin/monitor-erigon-performance.sh << 'EOF'
#!/bin/bash
# ERIGON performance monitor

LOG_FILE="/var/log/erigon-performance.log"
ETHEREUM_DATA_DIR="/mnt/blockchain-disk"
ERIGON_DATA_DIR="$ETHEREUM_DATA_DIR/erigon"

monitor_erigon() {
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    
    echo "[$timestamp] ERIGON Performance Monitor" >> "$LOG_FILE"
    
    # Check if ERIGON is running
    if pgrep -f "erigon" > /dev/null; then
        echo "  - ERIGON process: RUNNING" >> "$LOG_FILE"
        
        # Get process information
        local pid=$(pgrep -f "erigon")
        local cpu_usage=$(ps -p "$pid" -o %cpu --no-headers | tr -d ' ')
        local memory_usage=$(ps -p "$pid" -o %mem --no-headers | tr -d ' ')
        local memory_rss=$(ps -p "$pid" -o rss --no-headers | tr -d ' ')
        
        echo "  - CPU usage: ${cpu_usage}%" >> "$LOG_FILE"
        echo "  - Memory usage: ${memory_usage}%" >> "$LOG_FILE"
        echo "  - Memory RSS: ${memory_rss}KB" >> "$LOG_FILE"
        
        # Check RPC endpoint
        if curl -s -X POST -H "Content-Type: application/json" \
            --data '{"jsonrpc":"2.0","method":"eth_blockNumber","params":[],"id":1}' \
            http://localhost:8545 > /dev/null 2>&1; then
            echo "  - RPC endpoint: RESPONDING" >> "$LOG_FILE"
        else
            echo "  - RPC endpoint: NOT RESPONDING" >> "$LOG_FILE"
        fi
        
        # Check metrics endpoint
        if curl -s http://localhost:6060/debug/metrics > /dev/null 2>&1; then
            echo "  - Metrics endpoint: RESPONDING" >> "$LOG_FILE"
        else
            echo "  - Metrics endpoint: NOT RESPONDING" >> "$LOG_FILE"
        fi
        
        # Check disk usage
        local disk_usage=$(df -h "$ERIGON_DATA_DIR" | tail -1 | awk '{print $5}' | sed 's/%//')
        echo "  - Disk usage: ${disk_usage}%" >> "$LOG_FILE"
        
        # Check for errors in log
        if [[ -f "$ERIGON_DATA_DIR/erigon.log" ]]; then
            local error_count=$(tail -100 "$ERIGON_DATA_DIR/erigon.log" | grep -i "error" | wc -l)
            echo "  - Recent errors: $error_count" >> "$LOG_FILE"
        fi
        
        # Check for warnings in log
        if [[ -f "$ERIGON_DATA_DIR/erigon.log" ]]; then
            local warning_count=$(tail -100 "$ERIGON_DATA_DIR/erigon.log" | grep -i "warning" | wc -l)
            echo "  - Recent warnings: $warning_count" >> "$LOG_FILE"
        fi
        
        # Check for bottlenecks
        if [[ $cpu_usage -gt 80 ]]; then
            echo "  - WARNING: High CPU usage (${cpu_usage}%)" >> "$LOG_FILE"
            logger -t erigon-monitor "WARNING: High CPU usage (${cpu_usage}%)"
        fi
        
        if [[ $memory_usage -gt 80 ]]; then
            echo "  - WARNING: High memory usage (${memory_usage}%)" >> "$LOG_FILE"
            logger -t erigon-monitor "WARNING: High memory usage (${memory_usage}%)"
        fi
        
        if [[ $disk_usage -gt 95 ]]; then
            echo "  - WARNING: High disk usage (${disk_usage}%)" >> "$LOG_FILE"
            logger -t erigon-monitor "WARNING: High disk usage (${disk_usage}%)"
        fi
        
    else
        echo "  - ERIGON process: NOT RUNNING" >> "$LOG_FILE"
        logger -t erigon-monitor "ERROR: ERIGON process not running"
    fi
    
    echo "  - Monitor completed" >> "$LOG_FILE"
    echo "" >> "$LOG_FILE"
}

monitor_erigon
EOF

chmod +x /usr/local/bin/monitor-erigon-performance.sh

# Create systemd service for ERIGON performance monitoring
cat > /etc/systemd/system/erigon-performance-monitor.service << EOF
[Unit]
Description=ERIGON Performance Monitor
After=multi-user.target

[Service]
Type=oneshot
ExecStart=/usr/local/bin/monitor-erigon-performance.sh
User=root
EOF

# Create timer for periodic monitoring
cat > /etc/systemd/system/erigon-performance-monitor.timer << EOF
[Unit]
Description=Run ERIGON Performance Monitor
Requires=erigon-performance-monitor.service

[Timer]
OnCalendar=*:0/5
Persistent=true

[Install]
WantedBy=timers.target
EOF

systemctl daemon-reload
systemctl enable erigon-performance-monitor.timer
systemctl start erigon-performance-monitor.timer

echo "✅ Set up ERIGON performance monitoring"

# Create ERIGON health check script
echo "🏥 Creating ERIGON health check script..."
cat > /usr/local/bin/check-erigon-health.sh << 'EOF'
#!/bin/bash
# ERIGON health check script

ETHEREUM_DATA_DIR="/mnt/blockchain-disk"
ERIGON_DATA_DIR="$ETHEREUM_DATA_DIR/erigon"
HEALTH_LOG="/var/log/erigon-health.log"

check_erigon_health() {
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    local health_status="HEALTHY"
    local issues=()
    
    echo "[$timestamp] ERIGON Health Check" >> "$HEALTH_LOG"
    
    # Check if ERIGON is running
    if ! pgrep -f "erigon" > /dev/null; then
        health_status="UNHEALTHY"
        issues+=("ERIGON process not running")
    fi
    
    # Check RPC endpoint
    if ! curl -s -X POST -H "Content-Type: application/json" \
        --data '{"jsonrpc":"2.0","method":"eth_blockNumber","params":[],"id":1}' \
        http://localhost:8545 > /dev/null 2>&1; then
        health_status="UNHEALTHY"
        issues+=("RPC endpoint not responding")
    fi
    
    # Check metrics endpoint
    if ! curl -s http://localhost:6060/debug/metrics > /dev/null 2>&1; then
        health_status="UNHEALTHY"
        issues+=("Metrics endpoint not responding")
    fi
    
    # Check disk space
    local disk_usage=$(df -h "$ERIGON_DATA_DIR" | tail -1 | awk '{print $5}' | sed 's/%//')
    if [[ $disk_usage -gt 95 ]]; then
        health_status="UNHEALTHY"
        issues+=("Disk usage above 95% (${disk_usage}%)")
    fi
    
    # Check for critical errors in log
    if [[ -f "$ERIGON_DATA_DIR/erigon.log" ]]; then
        local critical_errors=$(tail -1000 "$ERIGON_DATA_DIR/erigon.log" | grep -i "critical\|fatal\|panic" | wc -l)
        if [[ $critical_errors -gt 0 ]]; then
            health_status="UNHEALTHY"
            issues+=("Critical errors in log ($critical_errors)")
        fi
    fi
    
    # Log health status
    echo "  - Health status: $health_status" >> "$HEALTH_LOG"
    
    if [[ ${#issues[@]} -gt 0 ]]; then
        echo "  - Issues found:" >> "$HEALTH_LOG"
        for issue in "${issues[@]}"; do
            echo "    * $issue" >> "$HEALTH_LOG"
        done
        
        # Send alert for unhealthy status
        if [[ "$health_status" == "UNHEALTHY" ]]; then
            logger -t erigon-health "ALERT: ERIGON health check failed - ${issues[*]}"
        fi
    else
        echo "  - No issues found" >> "$HEALTH_LOG"
    fi
    
    echo "  - Health check completed" >> "$HEALTH_LOG"
    echo "" >> "$HEALTH_LOG"
    
    # Return exit code based on health status
    if [[ "$health_status" == "HEALTHY" ]]; then
        exit 0
    else
        exit 1
    fi
}

check_erigon_health
EOF

chmod +x /usr/local/bin/check-erigon-health.sh

echo "✅ Created ERIGON health check script"

# Display final status
echo ""
echo "🎉 ERIGON Configuration Optimization Completed!"
echo "=============================================="
echo "📁 Data directory: $ERIGON_DATA_DIR"
echo "⚙️  Config file: $CONFIG_DIR/erigon.toml"
echo "🚀 Startup script: /usr/local/bin/start-erigon-optimized.sh"
echo "🔍 Monitor script: /usr/local/bin/monitor-erigon-performance.sh"
echo "🏥 Health check: /usr/local/bin/check-erigon-health.sh"
echo "📊 Performance log: /var/log/erigon-performance.log"
echo "🏥 Health log: /var/log/erigon-health.log"
echo ""
echo "💡 Next steps:"
echo "   1. Run: sudo /usr/local/bin/start-erigon-optimized.sh"
echo "   2. Monitor: tail -f /var/log/erigon-performance.log"
echo "   3. Health check: sudo /usr/local/bin/check-erigon-health.sh"
echo "   4. Check status: curl -X POST -H 'Content-Type: application/json' --data '{\"jsonrpc\":\"2.0\",\"method\":\"eth_blockNumber\",\"params\":[],\"id\":1}' http://localhost:8545"
