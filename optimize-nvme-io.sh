#!/bin/bash

# NVMe I/O Optimization Script for ERIGON
# This script optimizes NVMe performance for blockchain data processing

set -e

echo "🚀 Starting NVMe I/O Optimization for ERIGON"
echo "=============================================="

# Check if running as root
if [[ $EUID -ne 0 ]]; then
   echo "❌ This script must be run as root (use sudo)"
   exit 1
fi

# Detect NVMe device
NVME_DEVICE=$(lsblk -d -o NAME,TYPE | grep disk | grep nvme | head -1 | awk '{print $1}')
if [[ -z "$NVME_DEVICE" ]]; then
    echo "❌ No NVMe device found"
    exit 1
fi

echo "📱 Detected NVMe device: /dev/$NVME_DEVICE"

# Backup current settings
echo "💾 Backing up current settings..."
mkdir -p /tmp/nvme-backup
echo "Current I/O scheduler: $(cat /sys/block/$NVME_DEVICE/queue/scheduler)" > /tmp/nvme-backup/scheduler
echo "Current queue depth: $(cat /sys/block/$NVME_DEVICE/queue/nr_requests)" > /tmp/nvme-backup/queue_depth
echo "Current read ahead: $(cat /sys/block/$NVME_DEVICE/queue/read_ahead_kb)" > /tmp/nvme-backup/read_ahead

# 1. Optimize I/O Scheduler
echo "⚙️  Optimizing I/O Scheduler..."
echo "none" > /sys/block/$NVME_DEVICE/queue/scheduler
echo "✅ Set I/O scheduler to 'none' (optimal for NVMe)"

# 2. Increase Queue Depth
echo "📈 Increasing queue depth..."
echo "1024" > /sys/block/$NVME_DEVICE/queue/nr_requests
echo "✅ Set queue depth to 1024"

# 3. Optimize Read Ahead
echo "🔍 Optimizing read ahead..."
echo "128" > /sys/block/$NVME_DEVICE/queue/read_ahead_kb
echo "✅ Set read ahead to 128KB"

# 4. Enable Write Caching
echo "💾 Enabling write caching..."
echo "write through" > /sys/block/$NVME_DEVICE/queue/write_cache
echo "✅ Enabled write caching"

# 5. Optimize Power Management
echo "⚡ Optimizing power management..."
echo "performance" > /sys/block/$NVME_DEVICE/queue/power/control
echo "✅ Set power management to performance mode"

# 6. Increase I/O Timeout
echo "⏱️  Increasing I/O timeout..."
echo "300" > /sys/block/$NVME_DEVICE/queue/io_timeout
echo "✅ Set I/O timeout to 300 seconds"

# 7. Optimize Merge Settings
echo "🔗 Optimizing merge settings..."
echo "0" > /sys/block/$NVME_DEVICE/queue/nomerges
echo "✅ Disabled merge operations (optimal for NVMe)"

# 8. Set Optimal Block Size
echo "📏 Setting optimal block size..."
echo "512" > /sys/block/$NVME_DEVICE/queue/logical_block_size
echo "✅ Set logical block size to 512 bytes"

# 9. Optimize CPU Affinity (if available)
if [[ -f "/sys/block/$NVME_DEVICE/queue/cpu_list" ]]; then
    echo "🖥️  Optimizing CPU affinity..."
    echo "0-$(nproc --all)" > /sys/block/$NVME_DEVICE/queue/cpu_list
    echo "✅ Set CPU affinity to all cores"
fi

# 10. Enable I/O Statistics
echo "📊 Enabling I/O statistics..."
echo "1" > /sys/block/$NVME_DEVICE/queue/iostats
echo "✅ Enabled I/O statistics"

# Create systemd service for persistent settings
echo "🔧 Creating systemd service for persistent settings..."
cat > /etc/systemd/system/nvme-optimize.service << EOF
[Unit]
Description=NVMe I/O Optimization
After=multi-user.target

[Service]
Type=oneshot
ExecStart=/bin/bash -c 'echo none > /sys/block/$NVME_DEVICE/queue/scheduler'
ExecStart=/bin/bash -c 'echo 1024 > /sys/block/$NVME_DEVICE/queue/nr_requests'
ExecStart=/bin/bash -c 'echo 128 > /sys/block/$NVME_DEVICE/queue/read_ahead_kb'
ExecStart=/bin/bash -c 'echo write through > /sys/block/$NVME_DEVICE/queue/write_cache'
ExecStart=/bin/bash -c 'echo performance > /sys/block/$NVME_DEVICE/queue/power/control'
ExecStart=/bin/bash -c 'echo 300 > /sys/block/$NVME_DEVICE/queue/io_timeout'
ExecStart=/bin/bash -c 'echo 0 > /sys/block/$NVME_DEVICE/queue/nomerges'
ExecStart=/bin/bash -c 'echo 1 > /sys/block/$NVME_DEVICE/queue/iostats'
RemainAfterExit=yes

[Install]
WantedBy=multi-user.target
EOF

# Enable the service
systemctl daemon-reload
systemctl enable nvme-optimize.service

echo "✅ Created and enabled systemd service for persistent settings"

# Display current settings
echo ""
echo "📋 Current NVMe Settings:"
echo "========================="
echo "I/O Scheduler: $(cat /sys/block/$NVME_DEVICE/queue/scheduler)"
echo "Queue Depth: $(cat /sys/block/$NVME_DEVICE/queue/nr_requests)"
echo "Read Ahead: $(cat /sys/block/$NVME_DEVICE/queue/read_ahead_kb) KB"
echo "Write Cache: $(cat /sys/block/$NVME_DEVICE/queue/write_cache)"
echo "Power Control: $(cat /sys/block/$NVME_DEVICE/queue/power/control)"
echo "I/O Timeout: $(cat /sys/block/$NVME_DEVICE/queue/io_timeout) seconds"
echo "No Merges: $(cat /sys/block/$NVME_DEVICE/queue/nomerges)"
echo "I/O Stats: $(cat /sys/block/$NVME_DEVICE/queue/iostats)"

echo ""
echo "🎉 NVMe I/O optimization completed!"
echo "💡 These settings will persist across reboots"
echo "📊 Monitor performance with: iostat -x 1"
