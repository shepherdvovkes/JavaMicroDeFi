#!/bin/bash

# Filesystem Optimization Script for Ethereum Data
# This script optimizes filesystem settings for blockchain data processing

set -e

echo "🚀 Starting Filesystem Optimization for Ethereum Data"
echo "====================================================="

# Check if running as root
if [[ $EUID -ne 0 ]]; then
   echo "❌ This script must be run as root (use sudo)"
   exit 1
fi

# Configuration
ETHEREUM_DATA_DIR="/mnt/blockchain-disk"
FILESYSTEM_TYPE=$(df -T "$ETHEREUM_DATA_DIR" | tail -1 | awk '{print $2}')
DEVICE=$(df "$ETHEREUM_DATA_DIR" | tail -1 | awk '{print $1}')

echo "📱 Filesystem Information:"
echo "  - Device: $DEVICE"
echo "  - Type: $FILESYSTEM_TYPE"
echo "  - Mount point: $ETHEREUM_DATA_DIR"

# Backup current mount options
echo "💾 Backing up current mount options..."
mkdir -p /tmp/fs-backup
mount | grep "$ETHEREUM_DATA_DIR" > /tmp/fs-backup/mount_options
echo "✅ Backed up mount options to /tmp/fs-backup/mount_options"

# Function to optimize ext4 filesystem
optimize_ext4() {
    echo "⚙️  Optimizing ext4 filesystem..."
    
    # Check if we can remount with new options
    if mount | grep -q "$ETHEREUM_DATA_DIR.*noatime"; then
        echo "  - noatime already enabled"
    else
        echo "  - Enabling noatime (disable access time updates)"
        mount -o remount,noatime "$ETHEREUM_DATA_DIR"
    fi
    
    if mount | grep -q "$ETHEREUM_DATA_DIR.*nodiratime"; then
        echo "  - nodiratime already enabled"
    else
        echo "  - Enabling nodiratime (disable directory access time updates)"
        mount -o remount,nodiratime "$ETHEREUM_DATA_DIR"
    fi
    
    if mount | grep -q "$ETHEREUM_DATA_DIR.*data=writeback"; then
        echo "  - writeback mode already enabled"
    else
        echo "  - Enabling writeback mode (faster writes, less safety)"
        mount -o remount,data=writeback "$ETHEREUM_DATA_DIR"
    fi
    
    if mount | grep -q "$ETHEREUM_DATA_DIR.*barrier=0"; then
        echo "  - barriers already disabled"
    else
        echo "  - Disabling barriers (faster writes, less safety)"
        mount -o remount,barrier=0 "$ETHEREUM_DATA_DIR"
    fi
    
    if mount | grep -q "$ETHEREUM_DATA_DIR.*commit=60"; then
        echo "  - commit interval already optimized"
    else
        echo "  - Setting commit interval to 60 seconds"
        mount -o remount,commit=60 "$ETHEREUM_DATA_DIR"
    fi
    
    echo "✅ ext4 optimization completed"
}

# Function to optimize xfs filesystem
optimize_xfs() {
    echo "⚙️  Optimizing XFS filesystem..."
    
    # XFS optimizations
    if mount | grep -q "$ETHEREUM_DATA_DIR.*noatime"; then
        echo "  - noatime already enabled"
    else
        echo "  - Enabling noatime"
        mount -o remount,noatime "$ETHEREUM_DATA_DIR"
    fi
    
    if mount | grep -q "$ETHEREUM_DATA_DIR.*nodiratime"; then
        echo "  - nodiratime already enabled"
    else
        echo "  - Enabling nodiratime"
        mount -o remount,nodiratime "$ETHEREUM_DATA_DIR"
    fi
    
    if mount | grep -q "$ETHEREUM_DATA_DIR.*logbufs=8"; then
        echo "  - log buffers already optimized"
    else
        echo "  - Setting log buffers to 8"
        mount -o remount,logbufs=8 "$ETHEREUM_DATA_DIR"
    fi
    
    if mount | grep -q "$ETHEREUM_DATA_DIR.*logbsize=256k"; then
        echo "  - log buffer size already optimized"
    else
        echo "  - Setting log buffer size to 256k"
        mount -o remount,logbsize=256k "$ETHEREUM_DATA_DIR"
    fi
    
    echo "✅ XFS optimization completed"
}

# Optimize based on filesystem type
case "$FILESYSTEM_TYPE" in
    "ext4")
        optimize_ext4
        ;;
    "xfs")
        optimize_xfs
        ;;
    *)
        echo "⚠️  Unsupported filesystem type: $FILESYSTEM_TYPE"
        echo "   Only ext4 and XFS are supported for optimization"
        exit 1
        ;;
esac

# Set up persistent mount options
echo "🔧 Setting up persistent mount options..."
FSTAB_BACKUP="/tmp/fs-backup/fstab.backup"
cp /etc/fstab "$FSTAB_BACKUP"
echo "✅ Backed up /etc/fstab to $FSTAB_BACKUP"

# Get current mount options
CURRENT_OPTIONS=$(mount | grep "$ETHEREUM_DATA_DIR" | awk '{print $6}' | sed 's/(//' | sed 's/)//')

# Create optimized mount options
case "$FILESYSTEM_TYPE" in
    "ext4")
        OPTIMIZED_OPTIONS="noatime,nodiratime,data=writeback,barrier=0,commit=60"
        ;;
    "xfs")
        OPTIMIZED_OPTIONS="noatime,nodiratime,logbufs=8,logbsize=256k"
        ;;
esac

# Update fstab with optimized options
UUID=$(blkid -s UUID -o value "$DEVICE")
if [[ -n "$UUID" ]]; then
    # Remove old entry
    sed -i "/$ETHEREUM_DATA_DIR/d" /etc/fstab
    
    # Add new optimized entry
    echo "UUID=$UUID $ETHEREUM_DATA_DIR $FILESYSTEM_TYPE $OPTIMIZED_OPTIONS 0 2" >> /etc/fstab
    echo "✅ Updated /etc/fstab with optimized mount options"
else
    echo "⚠️  Could not determine UUID for $DEVICE"
    echo "   Manual fstab update required"
fi

# Optimize kernel parameters for I/O
echo "⚙️  Optimizing kernel parameters for I/O..."
cat > /etc/sysctl.d/99-ethereum-io.conf << 'EOF'
# Ethereum I/O Optimization
# Optimize virtual memory for large file operations
vm.dirty_ratio = 15
vm.dirty_background_ratio = 5
vm.dirty_expire_centisecs = 3000
vm.dirty_writeback_centisecs = 500

# Optimize I/O scheduler
kernel.sched_rt_runtime_us = -1

# Increase file descriptor limits
fs.file-max = 2097152
fs.nr_open = 1048576

# Optimize network for P2P
net.core.rmem_max = 134217728
net.core.wmem_max = 134217728
net.core.rmem_default = 262144
net.core.wmem_default = 262144
net.core.netdev_max_backlog = 5000

# Optimize TCP for blockchain sync
net.ipv4.tcp_rmem = 4096 87380 134217728
net.ipv4.tcp_wmem = 4096 65536 134217728
net.ipv4.tcp_congestion_control = bbr
net.ipv4.tcp_slow_start_after_idle = 0

# Optimize for large files
fs.inotify.max_user_watches = 1048576
fs.inotify.max_user_instances = 8192
EOF

# Apply kernel parameters
sysctl -p /etc/sysctl.d/99-ethereum-io.conf
echo "✅ Applied kernel parameter optimizations"

# Optimize file limits
echo "⚙️  Optimizing file limits..."
cat > /etc/security/limits.d/99-ethereum.conf << 'EOF'
# Ethereum file limits
* soft nofile 1048576
* hard nofile 1048576
* soft nproc 1048576
* hard nproc 1048576
EOF

echo "✅ Set file limits for Ethereum processes"

# Create filesystem health check script
echo "🔍 Creating filesystem health check script..."
cat > /usr/local/bin/check-ethereum-fs.sh << 'EOF'
#!/bin/bash
# Ethereum filesystem health check

ETHEREUM_DATA_DIR="/mnt/blockchain-disk"
LOG_FILE="/var/log/ethereum-fs-health.log"

check_filesystem() {
    local fs_type=$(df -T "$ETHEREUM_DATA_DIR" | tail -1 | awk '{print $2}')
    local device=$(df "$ETHEREUM_DATA_DIR" | tail -1 | awk '{print $1}')
    local usage=$(df -h "$ETHEREUM_DATA_DIR" | tail -1 | awk '{print $5}' | sed 's/%//')
    local inodes=$(df -i "$ETHEREUM_DATA_DIR" | tail -1 | awk '{print $5}' | sed 's/%//')
    
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] Filesystem health check:" >> "$LOG_FILE"
    echo "  - Type: $fs_type" >> "$LOG_FILE"
    echo "  - Device: $device" >> "$LOG_FILE"
    echo "  - Usage: ${usage}%" >> "$LOG_FILE"
    echo "  - Inodes: ${inodes}%" >> "$LOG_FILE"
    
    # Check for errors
    if [[ $usage -gt 90 ]]; then
        echo "  - WARNING: Disk usage above 90%" >> "$LOG_FILE"
        logger -t ethereum-fs "WARNING: Disk usage at ${usage}%"
    fi
    
    if [[ $inodes -gt 90 ]]; then
        echo "  - WARNING: Inode usage above 90%" >> "$LOG_FILE"
        logger -t ethereum-fs "WARNING: Inode usage at ${inodes}%"
    fi
    
    # Check filesystem errors
    case "$fs_type" in
        "ext4")
            if command -v e2fsck >/dev/null 2>&1; then
                e2fsck -n "$device" >> "$LOG_FILE" 2>&1
            fi
            ;;
        "xfs")
            if command -v xfs_repair >/dev/null 2>&1; then
                xfs_repair -n "$device" >> "$LOG_FILE" 2>&1
            fi
            ;;
    esac
    
    echo "  - Health check completed" >> "$LOG_FILE"
    echo "" >> "$LOG_FILE"
}

check_filesystem
EOF

chmod +x /usr/local/bin/check-ethereum-fs.sh

# Create systemd service for filesystem health monitoring
cat > /etc/systemd/system/ethereum-fs-monitor.service << EOF
[Unit]
Description=Ethereum Filesystem Health Monitor
After=multi-user.target

[Service]
Type=oneshot
ExecStart=/usr/local/bin/check-ethereum-fs.sh
User=root
EOF

# Create timer for periodic health checks
cat > /etc/systemd/system/ethereum-fs-monitor.timer << EOF
[Unit]
Description=Run Ethereum Filesystem Health Check
Requires=ethereum-fs-monitor.service

[Timer]
OnCalendar=daily
Persistent=true

[Install]
WantedBy=timers.target
EOF

systemctl daemon-reload
systemctl enable ethereum-fs-monitor.timer
systemctl start ethereum-fs-monitor.timer

echo "✅ Set up filesystem health monitoring"

# Display final status
echo ""
echo "🎉 Filesystem Optimization Completed!"
echo "===================================="
echo "📁 Data directory: $ETHEREUM_DATA_DIR"
echo "💾 Filesystem type: $FILESYSTEM_TYPE"
echo "⚙️  Optimized mount options: $OPTIMIZED_OPTIONS"
echo "🔍 Health check: /usr/local/bin/check-ethereum-fs.sh"
echo "📊 Health log: /var/log/ethereum-fs-health.log"
echo ""
echo "💡 Next steps:"
echo "   1. Reboot to apply all optimizations"
echo "   2. Run: sudo /usr/local/bin/check-ethereum-fs.sh"
echo "   3. Monitor: tail -f /var/log/ethereum-fs-health.log"
echo "   4. Start ERIGON with optimized settings"
