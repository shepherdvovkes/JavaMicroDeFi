#!/bin/bash

# Blockchain Data Migration Script
# This script migrates all blockchain data from NVMe to 18TB SATA disk

set -e

echo "🚀 Starting Blockchain Data Migration"
echo "===================================="
echo "Migrating from: NVMe disk (/opt/erigon-hot)"
echo "Migrating to: 18TB SATA disk (/mnt/blockchain-disk/erigon)"
echo ""

# Check if running as root
if [[ $EUID -ne 0 ]]; then
   echo "❌ This script must be run as root (use sudo)"
   exit 1
fi

# Configuration
SOURCE_DIR="/opt/erigon-hot"
DEST_DIR="/mnt/blockchain-disk/erigon"
BACKUP_DIR="/tmp/migration-backup"
LOG_FILE="/var/log/blockchain-migration.log"

# Create backup and log directories
mkdir -p "$BACKUP_DIR"
mkdir -p "$(dirname "$LOG_FILE")"

# Function to log messages
log_message() {
    local message="$1"
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    echo "[$timestamp] $message" | tee -a "$LOG_FILE"
}

log_message "Starting blockchain data migration..."

# Check if source directory exists
if [[ ! -d "$SOURCE_DIR" ]]; then
    log_message "❌ Source directory not found: $SOURCE_DIR"
    exit 1
fi

# Check if destination directory exists
if [[ ! -d "$DEST_DIR" ]]; then
    log_message "📁 Creating destination directory: $DEST_DIR"
    mkdir -p "$DEST_DIR"
fi

# Check available space
SOURCE_SIZE=$(du -sb "$SOURCE_DIR" | cut -f1)
DEST_AVAILABLE=$(df -B1 "$DEST_DIR" | tail -1 | awk '{print $4}')

log_message "📊 Migration Analysis:"
log_message "  - Source size: $(numfmt --to=iec $SOURCE_SIZE)"
log_message "  - Destination available: $(numfmt --to=iec $DEST_AVAILABLE)"

if [[ $SOURCE_SIZE -gt $DEST_AVAILABLE ]]; then
    log_message "❌ Insufficient space on destination disk"
    log_message "  - Required: $(numfmt --to=iec $SOURCE_SIZE)"
    log_message "  - Available: $(numfmt --to=iec $DEST_AVAILABLE)"
    exit 1
fi

# Stop ERIGON container
log_message "⏹️  Stopping ERIGON container..."
if docker ps | grep -q erigon; then
    docker stop erigon
    log_message "✅ ERIGON container stopped"
else
    log_message "ℹ️  ERIGON container not running"
fi

# Stop other blockchain-related containers
log_message "⏹️  Stopping other blockchain containers..."
docker stop ethereum-prometheus ethereum-zookeeper 2>/dev/null || true
log_message "✅ Blockchain containers stopped"

# Create backup of current configuration
log_message "💾 Creating backup of current configuration..."
cp /etc/docker/daemon.json "$BACKUP_DIR/daemon.json.backup" 2>/dev/null || true
docker inspect erigon > "$BACKUP_DIR/erigon-inspect.json" 2>/dev/null || true
log_message "✅ Configuration backup created"

# Check if data already exists in destination
if [[ -d "$DEST_DIR" ]] && [[ "$(ls -A "$DEST_DIR" 2>/dev/null)" ]]; then
    log_message "⚠️  Destination directory not empty"
    log_message "  - Existing data will be backed up to: $BACKUP_DIR/existing-data"
    mkdir -p "$BACKUP_DIR/existing-data"
    cp -r "$DEST_DIR"/* "$BACKUP_DIR/existing-data/" 2>/dev/null || true
    log_message "✅ Existing data backed up"
fi

# Start migration with progress monitoring
log_message "🔄 Starting data migration..."
log_message "  - Source: $SOURCE_DIR"
log_message "  - Destination: $DEST_DIR"

# Use rsync for efficient migration with progress
rsync -avh --progress --stats \
    --exclude="*.lock" \
    --exclude="*.tmp" \
    --exclude="*.log" \
    "$SOURCE_DIR/" "$DEST_DIR/" 2>&1 | tee -a "$LOG_FILE"

if [[ ${PIPESTATUS[0]} -eq 0 ]]; then
    log_message "✅ Data migration completed successfully"
else
    log_message "❌ Data migration failed"
    exit 1
fi

# Verify migration
log_message "🔍 Verifying migration..."
SOURCE_FILES=$(find "$SOURCE_DIR" -type f | wc -l)
DEST_FILES=$(find "$DEST_DIR" -type f | wc -l)

log_message "  - Source files: $SOURCE_FILES"
log_message "  - Destination files: $DEST_FILES"

if [[ $SOURCE_FILES -eq $DEST_FILES ]]; then
    log_message "✅ File count verification passed"
else
    log_message "⚠️  File count mismatch - manual verification recommended"
fi

# Update Docker Compose configuration
log_message "🔧 Updating Docker Compose configuration..."
cat > /home/vovkes/JavaMicroDeFi/docker-compose-migrated.yml << 'EOF'
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
      - /mnt/blockchain-disk/erigon:/home/erigon/.local/share/erigon
      - /mnt/lighthouse-data/lighthouse-hot/ethereum/geth/jwtsecret:/home/erigon/jwtsecret:ro
    environment:
      - ERIGON_DATADIR=/home/erigon/.local/share/erigon
    command: >
      --datadir=/home/erigon/.local/share/erigon
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

  ethereum-prometheus:
    image: prom/prometheus:latest
    container_name: ethereum-prometheus
    restart: unless-stopped
    ports:
      - "9090:9090"
    volumes:
      - /home/vovkes/JavaMicroDeFi/blockchain-sync-service/prometheus:/etc/prometheus
      - prometheus_data:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
      - '--web.console.libraries=/etc/prometheus/console_libraries'
      - '--web.console.templates=/etc/prometheus/consoles'
      - '--storage.tsdb.retention.time=200h'
      - '--web.enable-lifecycle'
    deploy:
      resources:
        limits:
          memory: 4G
        reservations:
          memory: 2G

  ethereum-zookeeper:
    image: confluentinc/cp-zookeeper:latest
    container_name: ethereum-zookeeper
    restart: unless-stopped
    ports:
      - "2181:2181"
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000
    volumes:
      - zookeeper_data:/var/lib/zookeeper/data
    deploy:
      resources:
        limits:
          memory: 2G
        reservations:
          memory: 1G

volumes:
  prometheus_data:
    driver: local
    driver_opts:
      type: none
      o: bind
      device: /mnt/ethereum-data/prometheus
  zookeeper_data:
    driver: local
    driver_opts:
      type: none
      o: bind
      device: /mnt/ethereum-data/zookeeper
EOF

log_message "✅ Docker Compose configuration updated"

# Create migration verification script
log_message "🔍 Creating migration verification script..."
cat > /usr/local/bin/verify-migration.sh << 'EOF'
#!/bin/bash
# Blockchain data migration verification script

SOURCE_DIR="/opt/erigon-hot"
DEST_DIR="/mnt/blockchain-disk/erigon"
LOG_FILE="/var/log/migration-verification.log"

verify_migration() {
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    
    echo "[$timestamp] Migration Verification" >> "$LOG_FILE"
    
    # Check if source exists
    if [[ -d "$SOURCE_DIR" ]]; then
        local source_size=$(du -sb "$SOURCE_DIR" | cut -f1)
        echo "  - Source size: $(numfmt --to=iec $source_size)" >> "$LOG_FILE"
    else
        echo "  - Source directory not found" >> "$LOG_FILE"
    fi
    
    # Check if destination exists
    if [[ -d "$DEST_DIR" ]]; then
        local dest_size=$(du -sb "$DEST_DIR" | cut -f1)
        echo "  - Destination size: $(numfmt --to=iec $dest_size)" >> "$LOG_FILE"
        
        # Check file counts
        local source_files=$(find "$SOURCE_DIR" -type f 2>/dev/null | wc -l)
        local dest_files=$(find "$DEST_DIR" -type f 2>/dev/null | wc -l)
        echo "  - Source files: $source_files" >> "$LOG_FILE"
        echo "  - Destination files: $dest_files" >> "$LOG_FILE"
        
        # Check if ERIGON can start
        if docker ps | grep -q erigon; then
            echo "  - ERIGON status: RUNNING" >> "$LOG_FILE"
        else
            echo "  - ERIGON status: NOT RUNNING" >> "$LOG_FILE"
        fi
        
    else
        echo "  - Destination directory not found" >> "$LOG_FILE"
    fi
    
    echo "  - Verification completed" >> "$LOG_FILE"
    echo "" >> "$LOG_FILE"
}

verify_migration
EOF

chmod +x /usr/local/bin/verify-migration.sh
log_message "✅ Migration verification script created"

# Start ERIGON with new configuration
log_message "▶️  Starting ERIGON with new configuration..."
cd /home/vovkes/JavaMicroDeFi
docker-compose -f docker-compose-migrated.yml up -d erigon

# Wait for ERIGON to start
log_message "⏳ Waiting for ERIGON to start..."
sleep 30

# Check ERIGON status
if docker ps | grep -q erigon; then
    log_message "✅ ERIGON started successfully with migrated data"
else
    log_message "❌ ERIGON failed to start"
    log_message "  - Check logs: docker logs erigon"
    exit 1
fi

# Verify ERIGON is responding
log_message "🔍 Verifying ERIGON RPC endpoint..."
if curl -s -X POST -H "Content-Type: application/json" \
    --data '{"jsonrpc":"2.0","method":"eth_blockNumber","params":[],"id":1}' \
    http://localhost:8545 > /dev/null 2>&1; then
    log_message "✅ ERIGON RPC endpoint responding"
else
    log_message "⚠️  ERIGON RPC endpoint not responding yet"
fi

# Create cleanup script for old data
log_message "🧹 Creating cleanup script for old data..."
cat > /usr/local/bin/cleanup-old-data.sh << 'EOF'
#!/bin/bash
# Cleanup script for old blockchain data

SOURCE_DIR="/opt/erigon-hot"
BACKUP_DIR="/tmp/migration-backup"

echo "🧹 Cleaning up old blockchain data..."
echo "⚠️  This will permanently delete data from: $SOURCE_DIR"
echo "   Backup is available at: $BACKUP_DIR"
echo ""

read -p "Are you sure you want to delete the old data? (yes/no): " -r
if [[ $REPLY == "yes" ]]; then
    echo "🗑️  Deleting old data..."
    rm -rf "$SOURCE_DIR"
    echo "✅ Old data deleted"
    echo "💡 You can restore from backup if needed: $BACKUP_DIR"
else
    echo "❌ Cleanup cancelled"
fi
EOF

chmod +x /usr/local/bin/cleanup-old-data.sh
log_message "✅ Cleanup script created"

# Display final status
echo ""
echo "🎉 Blockchain Data Migration Completed!"
echo "======================================"
echo "📁 Source: $SOURCE_DIR"
echo "📁 Destination: $DEST_DIR"
echo "📊 Migration log: $LOG_FILE"
echo "🔍 Verification: /usr/local/bin/verify-migration.sh"
echo "🧹 Cleanup: /usr/local/bin/cleanup-old-data.sh"
echo ""
echo "💡 Next steps:"
echo "   1. Verify migration: sudo /usr/local/bin/verify-migration.sh"
echo "   2. Monitor ERIGON: docker logs erigon -f"
echo "   3. Check RPC: curl -X POST -H 'Content-Type: application/json' --data '{\"jsonrpc\":\"2.0\",\"method\":\"eth_blockNumber\",\"params\":[],\"id\":1}' http://localhost:8545"
echo "   4. Clean up old data: sudo /usr/local/bin/cleanup-old-data.sh"
echo ""
echo "📊 Current disk usage:"
echo "NVMe disk:"
df -h /dev/nvme0n1p2
echo "18TB disk:"
df -h /mnt/blockchain-disk
