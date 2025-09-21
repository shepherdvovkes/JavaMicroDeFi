#!/bin/bash

# Master I/O Optimization Script for Ethereum Data
# This script runs all I/O optimizations in the correct order

set -e

echo "🚀 Starting Complete I/O Optimization for Ethereum Data"
echo "======================================================"
echo "This script will optimize:"
echo "  - NVMe I/O settings"
echo "  - Disk pre-allocation"
echo "  - Filesystem optimization"
echo "  - Docker storage optimization"
echo "  - ERIGON configuration"
echo ""

# Check if running as root
if [[ $EUID -ne 0 ]]; then
   echo "❌ This script must be run as root (use sudo)"
   exit 1
fi

# Configuration
SCRIPT_DIR="/home/vovkes/JavaMicroDeFi"
BACKUP_DIR="/tmp/io-optimization-backup"
LOG_FILE="/var/log/io-optimization.log"

# Create backup directory
mkdir -p "$BACKUP_DIR"

# Function to log messages
log_message() {
    local message="$1"
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    echo "[$timestamp] $message" | tee -a "$LOG_FILE"
}

# Function to run optimization script
run_optimization() {
    local script_name="$1"
    local script_path="$SCRIPT_DIR/$script_name"
    
    if [[ -f "$script_path" ]]; then
        log_message "Running $script_name..."
        if bash "$script_path" 2>&1 | tee -a "$LOG_FILE"; then
            log_message "✅ $script_name completed successfully"
            return 0
        else
            log_message "❌ $script_name failed"
            return 1
        fi
    else
        log_message "❌ Script not found: $script_path"
        return 1
    fi
}

# Start optimization process
log_message "Starting I/O optimization process..."

# 1. NVMe I/O Optimization
log_message "Step 1/5: NVMe I/O Optimization"
if run_optimization "optimize-nvme-io.sh"; then
    log_message "✅ NVMe I/O optimization completed"
else
    log_message "❌ NVMe I/O optimization failed"
    exit 1
fi

echo ""
log_message "Step 2/5: Disk Pre-allocation"
if run_optimization "preallocate-ethereum-data.sh"; then
    log_message "✅ Disk pre-allocation completed"
else
    log_message "❌ Disk pre-allocation failed"
    exit 1
fi

echo ""
log_message "Step 3/5: Filesystem Optimization"
if run_optimization "optimize-filesystem.sh"; then
    log_message "✅ Filesystem optimization completed"
else
    log_message "❌ Filesystem optimization failed"
    exit 1
fi

echo ""
log_message "Step 4/5: Docker Storage Optimization"
if run_optimization "optimize-docker-storage.sh"; then
    log_message "✅ Docker storage optimization completed"
else
    log_message "❌ Docker storage optimization failed"
    exit 1
fi

echo ""
log_message "Step 5/5: ERIGON Configuration Optimization"
if run_optimization "optimize-erigon-config.sh"; then
    log_message "✅ ERIGON configuration optimization completed"
else
    log_message "❌ ERIGON configuration optimization failed"
    exit 1
fi

# Final system check
log_message "Running final system check..."

# Check NVMe settings
log_message "Checking NVMe settings..."
NVME_DEVICE=$(lsblk -d -o NAME,TYPE | grep disk | grep nvme | head -1 | awk '{print $1}')
if [[ -n "$NVME_DEVICE" ]]; then
    log_message "  - I/O Scheduler: $(cat /sys/block/$NVME_DEVICE/queue/scheduler)"
    log_message "  - Queue Depth: $(cat /sys/block/$NVME_DEVICE/queue/nr_requests)"
    log_message "  - Read Ahead: $(cat /sys/block/$NVME_DEVICE/queue/read_ahead_kb) KB"
fi

# Check filesystem settings
log_message "Checking filesystem settings..."
ETHEREUM_DATA_DIR="/mnt/ethereum-data"
if mount | grep -q "$ETHEREUM_DATA_DIR"; then
    log_message "  - Mount options: $(mount | grep "$ETHEREUM_DATA_DIR" | awk '{print $6}')"
fi

# Check Docker status
log_message "Checking Docker status..."
if systemctl is-active --quiet docker; then
    log_message "  - Docker service: RUNNING"
    log_message "  - Storage driver: $(docker info --format '{{.Driver}}' 2>/dev/null || echo 'Unknown')"
else
    log_message "  - Docker service: STOPPED"
fi

# Check ERIGON configuration
log_message "Checking ERIGON configuration..."
ERIGON_CONFIG="/mnt/ethereum-data/erigon/config/erigon.toml"
if [[ -f "$ERIGON_CONFIG" ]]; then
    log_message "  - ERIGON config: EXISTS"
else
    log_message "  - ERIGON config: NOT FOUND"
fi

# Performance test
log_message "Running I/O performance test..."
if [[ -f "/usr/local/bin/test-ethereum-io.sh" ]]; then
    log_message "  - Running performance test..."
    /usr/local/bin/test-ethereum-io.sh 2>&1 | tee -a "$LOG_FILE"
else
    log_message "  - Performance test script not found"
fi

# Create summary report
log_message "Creating optimization summary report..."
cat > "$BACKUP_DIR/optimization-summary.md" << EOF
# I/O Optimization Summary Report

## Optimization Date
$(date '+%Y-%m-%d %H:%M:%S')

## Completed Optimizations

### 1. NVMe I/O Optimization ✅
- I/O Scheduler: $(cat /sys/block/$NVME_DEVICE/queue/scheduler 2>/dev/null || echo 'Unknown')
- Queue Depth: $(cat /sys/block/$NVME_DEVICE/queue/nr_requests 2>/dev/null || echo 'Unknown')
- Read Ahead: $(cat /sys/block/$NVME_DEVICE/queue/read_ahead_kb 2>/dev/null || echo 'Unknown') KB

### 2. Disk Pre-allocation ✅
- Data directory: $ETHEREUM_DATA_DIR
- Pre-allocation: Completed
- Monitoring: Enabled

### 3. Filesystem Optimization ✅
- Mount options: $(mount | grep "$ETHEREUM_DATA_DIR" | awk '{print $6}' 2>/dev/null || echo 'Unknown')
- Kernel parameters: Optimized
- Health monitoring: Enabled

### 4. Docker Storage Optimization ✅
- Storage driver: $(docker info --format '{{.Driver}}' 2>/dev/null || echo 'Unknown')
- Configuration: Optimized
- Performance monitoring: Enabled

### 5. ERIGON Configuration ✅
- Config file: $ERIGON_CONFIG
- Startup script: /usr/local/bin/start-erigon-optimized.sh
- Performance monitoring: Enabled

## Monitoring Scripts
- NVMe monitoring: /usr/local/bin/monitor-ethereum-space.sh
- Filesystem monitoring: /usr/local/bin/check-ethereum-fs.sh
- Docker monitoring: /usr/local/bin/monitor-docker-performance.sh
- ERIGON monitoring: /usr/local/bin/monitor-erigon-performance.sh
- ERIGON health check: /usr/local/bin/check-erigon-health.sh

## Performance Test
- Test script: /usr/local/bin/test-ethereum-io.sh
- Results: See log file

## Log Files
- Main log: $LOG_FILE
- Ethereum space: /var/log/ethereum-space.log
- Filesystem health: /var/log/ethereum-fs-health.log
- Docker performance: /var/log/docker-performance.log
- ERIGON performance: /var/log/erigon-performance.log
- ERIGON health: /var/log/erigon-health.log

## Next Steps
1. Reboot the system to apply all optimizations
2. Start ERIGON with: sudo /usr/local/bin/start-erigon-optimized.sh
3. Monitor performance with the provided scripts
4. Check logs regularly for any issues

## Backup Information
- Backup directory: $BACKUP_DIR
- Original configurations backed up
- Rollback scripts available if needed
EOF

log_message "✅ Optimization summary report created: $BACKUP_DIR/optimization-summary.md"

# Final message
echo ""
echo "🎉 Complete I/O Optimization Finished!"
echo "====================================="
echo "📊 All optimizations completed successfully"
echo "📋 Summary report: $BACKUP_DIR/optimization-summary.md"
echo "📝 Full log: $LOG_FILE"
echo ""
echo "💡 Next steps:"
echo "   1. Reboot the system to apply all optimizations"
echo "   2. Start ERIGON with: sudo /usr/local/bin/start-erigon-optimized.sh"
echo "   3. Monitor performance with the provided scripts"
echo "   4. Check logs regularly for any issues"
echo ""
echo "🔍 Monitoring commands:"
echo "   - NVMe performance: iostat -x 1"
echo "   - Disk usage: df -h"
echo "   - ERIGON status: sudo /usr/local/bin/check-erigon-health.sh"
echo "   - Docker status: docker ps"
echo ""
echo "📚 Documentation:"
echo "   - Summary report: $BACKUP_DIR/optimization-summary.md"
echo "   - All logs: /var/log/"
echo "   - Monitoring scripts: /usr/local/bin/"
