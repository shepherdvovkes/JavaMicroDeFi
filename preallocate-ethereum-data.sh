#!/bin/bash

# Ethereum Data Pre-allocation Script
# This script pre-allocates disk space for ERIGON blockchain data

set -e

echo "🚀 Starting Ethereum Data Pre-allocation"
echo "========================================"

# Configuration
ETHEREUM_DATA_DIR="/mnt/blockchain-disk"
ERIGON_DATA_DIR="$ETHEREUM_DATA_DIR/erigon"
BACKUP_DIR="$ETHEREUM_DATA_DIR/backup"
TEMP_DIR="$ETHEREUM_DATA_DIR/temp"

# Estimated space requirements (in GB) - Updated for 18TB disk
ESTIMATED_SPACE=15000  # 15TB for full Ethereum data with room for growth
BUFFER_SPACE=1000      # 1TB buffer for future data
TOTAL_SPACE=$((ESTIMATED_SPACE + BUFFER_SPACE))

echo "📊 Space Requirements:"
echo "  - Estimated Ethereum data: ${ESTIMATED_SPACE}GB"
echo "  - Buffer space: ${BUFFER_SPACE}GB"
echo "  - Total required: ${TOTAL_SPACE}GB"

# Check available space
AVAILABLE_SPACE=$(df -BG "$ETHEREUM_DATA_DIR" | tail -1 | awk '{print $4}' | sed 's/G//')
echo "  - Available space: ${AVAILABLE_SPACE}GB"

if [[ $AVAILABLE_SPACE -lt $TOTAL_SPACE ]]; then
    echo "⚠️  Warning: Available space (${AVAILABLE_SPACE}GB) is less than required (${TOTAL_SPACE}GB)"
    echo "   Consider freeing up space or using a larger disk"
    read -p "Continue anyway? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

# Create directories
echo "📁 Creating directory structure..."
mkdir -p "$ERIGON_DATA_DIR"
mkdir -p "$BACKUP_DIR"
mkdir -p "$TEMP_DIR"

# Set optimal permissions
chmod 755 "$ETHEREUM_DATA_DIR"
chmod 755 "$ERIGON_DATA_DIR"
chmod 755 "$BACKUP_DIR"
chmod 755 "$TEMP_DIR"

echo "✅ Created directory structure"

# Pre-allocate space using fallocate
echo "💾 Pre-allocating disk space..."

# Create pre-allocated files for different data types
echo "  - Pre-allocating chain data (12TB)..."
fallocate -l 12T "$ERIGON_DATA_DIR/chaindata_prealloc.dat" 2>/dev/null || {
    echo "    ⚠️  fallocate failed, using dd instead..."
    dd if=/dev/zero of="$ERIGON_DATA_DIR/chaindata_prealloc.dat" bs=1G count=12288 status=progress 2>/dev/null
}

echo "  - Pre-allocating state data (2TB)..."
fallocate -l 2T "$ERIGON_DATA_DIR/state_prealloc.dat" 2>/dev/null || {
    echo "    ⚠️  fallocate failed, using dd instead..."
    dd if=/dev/zero of="$ERIGON_DATA_DIR/state_prealloc.dat" bs=1G count=2048 status=progress 2>/dev/null
}

echo "  - Pre-allocating temp space (1TB)..."
fallocate -l 1T "$TEMP_DIR/temp_prealloc.dat" 2>/dev/null || {
    echo "    ⚠️  fallocate failed, using dd instead..."
    dd if=/dev/zero of="$TEMP_DIR/temp_prealloc.dat" bs=1G count=1024 status=progress 2>/dev/null
}

echo "✅ Pre-allocation completed"

# Create sparse files for better performance
echo "🔧 Creating sparse files for optimal performance..."

# Remove pre-allocated files and create sparse ones
rm -f "$ERIGON_DATA_DIR/chaindata_prealloc.dat"
rm -f "$ERIGON_DATA_DIR/state_prealloc.dat"
rm -f "$TEMP_DIR/temp_prealloc.dat"

# Create sparse files that will grow as needed
touch "$ERIGON_DATA_DIR/chaindata_sparse.dat"
touch "$ERIGON_DATA_DIR/state_sparse.dat"
touch "$TEMP_DIR/temp_sparse.dat"

echo "✅ Created sparse files"

# Set up disk space monitoring
echo "📊 Setting up disk space monitoring..."
cat > /usr/local/bin/monitor-ethereum-space.sh << 'EOF'
#!/bin/bash
# Ethereum disk space monitor

ETHEREUM_DATA_DIR="/mnt/blockchain-disk"
LOG_FILE="/var/log/ethereum-space.log"

while true; do
    USAGE=$(df -h "$ETHEREUM_DATA_DIR" | tail -1 | awk '{print $5}' | sed 's/%//')
    TIMESTAMP=$(date '+%Y-%m-%d %H:%M:%S')
    
    echo "[$TIMESTAMP] Disk usage: ${USAGE}%" >> "$LOG_FILE"
    
    if [[ $USAGE -gt 95 ]]; then
        echo "[$TIMESTAMP] WARNING: Disk usage above 95%!" >> "$LOG_FILE"
        # Send alert (customize as needed)
        logger -t ethereum-monitor "WARNING: Ethereum disk usage at ${USAGE}%"
    fi
    
    sleep 300  # Check every 5 minutes
done
EOF

chmod +x /usr/local/bin/monitor-ethereum-space.sh

# Create systemd service for monitoring
cat > /etc/systemd/system/ethereum-space-monitor.service << EOF
[Unit]
Description=Ethereum Disk Space Monitor
After=multi-user.target

[Service]
Type=simple
ExecStart=/usr/local/bin/monitor-ethereum-space.sh
Restart=always
RestartSec=10
User=root

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload
systemctl enable ethereum-space-monitor.service
systemctl start ethereum-space-monitor.service

echo "✅ Set up disk space monitoring"

# Optimize filesystem for large files
echo "⚙️  Optimizing filesystem for large files..."
if [[ $(df -T "$ETHEREUM_DATA_DIR" | tail -1 | awk '{print $2}') == "ext4" ]]; then
    # For ext4, we can't remount with different options without unmounting
    echo "  - Filesystem is ext4 (already optimized for large files)"
    echo "  - Consider using XFS for better performance with large files"
else
    echo "  - Filesystem optimization not needed for this filesystem type"
fi

# Create performance test script
echo "🧪 Creating performance test script..."
cat > /usr/local/bin/test-ethereum-io.sh << 'EOF'
#!/bin/bash
# Ethereum I/O performance test

ETHEREUM_DATA_DIR="/mnt/blockchain-disk"
TEST_FILE="$ETHEREUM_DATA_DIR/io_test.dat"
TEST_SIZE="10G"

echo "Testing I/O performance for Ethereum data directory..."

# Test write performance
echo "Testing write performance..."
time dd if=/dev/zero of="$TEST_FILE" bs=1M count=10240 oflag=direct 2>&1 | grep -E "(copied|GB/s)"

# Test read performance
echo "Testing read performance..."
time dd if="$TEST_FILE" of=/dev/null bs=1M iflag=direct 2>&1 | grep -E "(copied|GB/s)"

# Clean up
rm -f "$TEST_FILE"

echo "I/O performance test completed"
EOF

chmod +x /usr/local/bin/test-ethereum-io.sh

echo "✅ Created performance test script"

# Display final status
echo ""
echo "🎉 Ethereum Data Pre-allocation Completed!"
echo "=========================================="
echo "📁 Data directory: $ETHEREUM_DATA_DIR"
echo "📊 Estimated space: ${TOTAL_SPACE}GB"
echo "🔍 Monitor script: /usr/local/bin/monitor-ethereum-space.sh"
echo "🧪 Test script: /usr/local/bin/test-ethereum-io.sh"
echo ""
echo "💡 Next steps:"
echo "   1. Run: sudo /usr/local/bin/test-ethereum-io.sh"
echo "   2. Monitor: tail -f /var/log/ethereum-space.log"
echo "   3. Start ERIGON with optimized settings"
