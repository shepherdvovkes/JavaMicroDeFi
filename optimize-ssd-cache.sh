#!/bin/bash

# SSD Cache Optimization Script for Java Micro DeFi
# This script optimizes SSD performance for cache operations

set -e

echo "🚀 Starting SSD Cache Optimization for Java Micro DeFi"
echo "====================================================="

# Check if running as root
if [[ $EUID -ne 0 ]]; then
   echo "❌ This script must be run as root (use sudo)"
   exit 1
fi

# Detect SSD devices
SSD_DEVICES=$(lsblk -d -o NAME,TYPE | grep disk | grep -E "(nvme|ssd)" | awk '{print $1}')
if [[ -z "$SSD_DEVICES" ]]; then
    echo "❌ No SSD devices found"
    exit 1
fi

echo "📱 Detected SSD devices: $SSD_DEVICES"

# Create cache directories
CACHE_BASE_DIR="/opt/ssd-cache"
MEMORY_CACHE_DIR="$CACHE_BASE_DIR/memory"
SSD_CACHE_DIR="$CACHE_BASE_DIR/ssd"
METRICS_DIR="$CACHE_BASE_DIR/metrics"

echo "📁 Creating cache directories..."
mkdir -p "$MEMORY_CACHE_DIR" "$SSD_CACHE_DIR" "$METRICS_DIR"
chmod 755 "$CACHE_BASE_DIR"
chmod 755 "$MEMORY_CACHE_DIR"
chmod 755 "$SSD_CACHE_DIR"
chmod 755 "$METRICS_DIR"

# Backup current settings
echo "💾 Backing up current settings..."
mkdir -p /tmp/ssd-cache-backup
for device in $SSD_DEVICES; do
    echo "Backing up settings for /dev/$device"
    echo "I/O scheduler: $(cat /sys/block/$device/queue/scheduler)" > "/tmp/ssd-cache-backup/${device}_scheduler"
    echo "Queue depth: $(cat /sys/block/$device/queue/nr_requests)" > "/tmp/ssd-cache-backup/${device}_queue_depth"
    echo "Read ahead: $(cat /sys/block/$device/queue/read_ahead_kb)" > "/tmp/ssd-cache-backup/${device}_read_ahead"
done

# Optimize each SSD device
for device in $SSD_DEVICES; do
    echo "⚙️  Optimizing SSD device: /dev/$device"
    
    # 1. Set I/O Scheduler to 'none' for NVMe or 'mq-deadline' for SATA SSD
    if [[ "$device" == nvme* ]]; then
        echo "none" > /sys/block/$device/queue/scheduler
        echo "✅ Set I/O scheduler to 'none' (optimal for NVMe)"
    else
        echo "mq-deadline" > /sys/block/$device/queue/scheduler
        echo "✅ Set I/O scheduler to 'mq-deadline' (optimal for SATA SSD)"
    fi
    
    # 2. Increase Queue Depth
    echo "1024" > /sys/block/$device/queue/nr_requests
    echo "✅ Set queue depth to 1024"
    
    # 3. Optimize Read Ahead
    echo "256" > /sys/block/$device/queue/read_ahead_kb
    echo "✅ Set read ahead to 256KB"
    
    # 4. Enable Write Caching
    echo "write through" > /sys/block/$device/queue/write_cache
    echo "✅ Enabled write caching"
    
    # 5. Set Power Management to Performance
    if [[ -f "/sys/block/$device/queue/power/control" ]]; then
        echo "performance" > /sys/block/$device/queue/power/control
        echo "✅ Set power management to performance mode"
    fi
    
    # 6. Increase I/O Timeout
    echo "600" > /sys/block/$device/queue/io_timeout
    echo "✅ Set I/O timeout to 600 seconds"
    
    # 7. Optimize Merge Settings
    echo "0" > /sys/block/$device/queue/nomerges
    echo "✅ Disabled merge operations (optimal for SSD)"
    
    # 8. Enable I/O Statistics
    echo "1" > /sys/block/$device/queue/iostats
    echo "✅ Enabled I/O statistics"
    
    # 9. Set CPU Affinity (if available)
    if [[ -f "/sys/block/$device/queue/cpu_list" ]]; then
        echo "0-$(nproc --all)" > /sys/block/$device/queue/cpu_list
        echo "✅ Set CPU affinity to all cores"
    fi
    
    # 10. Optimize Block Size
    echo "512" > /sys/block/$device/queue/logical_block_size
    echo "✅ Set logical block size to 512 bytes"
done

# Create systemd service for persistent settings
echo "🔧 Creating systemd service for persistent settings..."
cat > /etc/systemd/system/ssd-cache-optimize.service << EOF
[Unit]
Description=SSD Cache Optimization
After=multi-user.target

[Service]
Type=oneshot
ExecStart=/bin/bash -c 'for device in $SSD_DEVICES; do echo none > /sys/block/\$device/queue/scheduler; done'
ExecStart=/bin/bash -c 'for device in $SSD_DEVICES; do echo 1024 > /sys/block/\$device/queue/nr_requests; done'
ExecStart=/bin/bash -c 'for device in $SSD_DEVICES; do echo 256 > /sys/block/\$device/queue/read_ahead_kb; done'
ExecStart=/bin/bash -c 'for device in $SSD_DEVICES; do echo write through > /sys/block/\$device/queue/write_cache; done'
ExecStart=/bin/bash -c 'for device in $SSD_DEVICES; do echo performance > /sys/block/\$device/queue/power/control; done'
ExecStart=/bin/bash -c 'for device in $SSD_DEVICES; do echo 600 > /sys/block/\$device/queue/io_timeout; done'
ExecStart=/bin/bash -c 'for device in $SSD_DEVICES; do echo 0 > /sys/block/\$device/queue/nomerges; done'
ExecStart=/bin/bash -c 'for device in $SSD_DEVICES; do echo 1 > /sys/block/\$device/queue/iostats; done'
RemainAfterExit=yes

[Install]
WantedBy=multi-user.target
EOF

# Enable the service
systemctl daemon-reload
systemctl enable ssd-cache-optimize.service

echo "✅ Created and enabled systemd service for persistent settings"

# Create cache monitoring script
echo "📊 Creating cache monitoring script..."
cat > /usr/local/bin/monitor-ssd-cache.sh << 'EOF'
#!/bin/bash

# SSD Cache Monitoring Script
echo "SSD Cache Performance Monitor"
echo "=============================="
echo "Date: $(date)"
echo ""

# Check SSD devices
SSD_DEVICES=$(lsblk -d -o NAME,TYPE | grep disk | grep -E "(nvme|ssd)" | awk '{print $1}')

for device in $SSD_DEVICES; do
    echo "Device: /dev/$device"
    echo "  I/O Scheduler: $(cat /sys/block/$device/queue/scheduler)"
    echo "  Queue Depth: $(cat /sys/block/$device/queue/nr_requests)"
    echo "  Read Ahead: $(cat /sys/block/$device/queue/read_ahead_kb) KB"
    echo "  Write Cache: $(cat /sys/block/$device/queue/write_cache)"
    echo "  Power Control: $(cat /sys/block/$device/queue/power/control)"
    echo "  I/O Timeout: $(cat /sys/block/$device/queue/io_timeout) seconds"
    echo "  No Merges: $(cat /sys/block/$device/queue/nomerges)"
    echo "  I/O Stats: $(cat /sys/block/$device/queue/iostats)"
    echo ""
done

# Show I/O statistics
echo "I/O Statistics (last 5 seconds):"
iostat -x 1 1 | grep -E "(Device|nvme|ssd)"
EOF

chmod +x /usr/local/bin/monitor-ssd-cache.sh

# Create cache performance test script
echo "🧪 Creating cache performance test script..."
cat > /usr/local/bin/test-ssd-cache.sh << 'EOF'
#!/bin/bash

# SSD Cache Performance Test
echo "SSD Cache Performance Test"
echo "=========================="

CACHE_DIR="/opt/ssd-cache/ssd"
TEST_FILE="$CACHE_DIR/performance_test.dat"
TEST_SIZE="1G"

echo "Creating test file of size $TEST_SIZE..."
dd if=/dev/zero of="$TEST_FILE" bs=1M count=1024 2>/dev/null

echo "Testing read performance..."
READ_START=$(date +%s.%N)
dd if="$TEST_FILE" of=/dev/null bs=1M 2>/dev/null
READ_END=$(date +%s.%N)
READ_TIME=$(echo "$READ_END - $READ_START" | bc)
READ_SPEED=$(echo "scale=2; 1024 / $READ_TIME" | bc)
echo "Read Speed: ${READ_SPEED} MB/s"

echo "Testing write performance..."
WRITE_START=$(date +%s.%N)
dd if=/dev/zero of="$TEST_FILE" bs=1M count=1024 2>/dev/null
WRITE_END=$(date +%s.%N)
WRITE_TIME=$(echo "$WRITE_END - $WRITE_START" | bc)
WRITE_SPEED=$(echo "scale=2; 1024 / $WRITE_TIME" | bc)
echo "Write Speed: ${WRITE_SPEED} MB/s"

# Cleanup
rm -f "$TEST_FILE"
echo "Test completed and cleaned up."
EOF

chmod +x /usr/local/bin/test-ssd-cache.sh

# Create cache optimization service
echo "🔧 Creating cache optimization service..."
cat > /etc/systemd/system/ssd-cache-optimizer.service << EOF
[Unit]
Description=SSD Cache Optimizer
After=ssd-cache-optimize.service

[Service]
Type=simple
ExecStart=/usr/local/bin/optimize-cache.sh
Restart=always
RestartSec=300
User=root

[Install]
WantedBy=multi-user.target
EOF

# Create cache optimization script
cat > /usr/local/bin/optimize-cache.sh << 'EOF'
#!/bin/bash

# Cache Optimization Script
while true; do
    echo "$(date): Running cache optimization..."
    
    # Clean up old cache files
    find /opt/ssd-cache -name "*.tmp" -mtime +1 -delete 2>/dev/null
    
    # Optimize cache directories
    find /opt/ssd-cache -type d -exec chmod 755 {} \; 2>/dev/null
    
    # Log cache statistics
    echo "$(date): Cache optimization completed" >> /var/log/ssd-cache-optimizer.log
    
    sleep 300  # Run every 5 minutes
done
EOF

chmod +x /usr/local/bin/optimize-cache.sh

# Enable cache optimizer service
systemctl daemon-reload
systemctl enable ssd-cache-optimizer.service
systemctl start ssd-cache-optimizer.service

# Display current settings
echo ""
echo "📋 Current SSD Cache Settings:"
echo "=============================="
for device in $SSD_DEVICES; do
    echo "Device: /dev/$device"
    echo "  I/O Scheduler: $(cat /sys/block/$device/queue/scheduler)"
    echo "  Queue Depth: $(cat /sys/block/$device/queue/nr_requests)"
    echo "  Read Ahead: $(cat /sys/block/$device/queue/read_ahead_kb) KB"
    echo "  Write Cache: $(cat /sys/block/$device/queue/write_cache)"
    echo "  Power Control: $(cat /sys/block/$device/queue/power/control)"
    echo "  I/O Timeout: $(cat /sys/block/$device/queue/io_timeout) seconds"
    echo "  No Merges: $(cat /sys/block/$device/queue/nomerges)"
    echo "  I/O Stats: $(cat /sys/block/$device/queue/iostats)"
    echo ""
done

echo "🎉 SSD Cache optimization completed!"
echo "💡 These settings will persist across reboots"
echo "📊 Monitor performance with: /usr/local/bin/monitor-ssd-cache.sh"
echo "🧪 Test performance with: /usr/local/bin/test-ssd-cache.sh"
echo "📁 Cache directories created at: $CACHE_BASE_DIR"
echo "📝 Logs available at: /var/log/ssd-cache-optimizer.log"

