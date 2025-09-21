#!/bin/bash

# Setup 18TB Disk for Ethereum Blockchain Data
# This script configures the 18TB disk for optimal blockchain data storage

set -e

echo "🚀 Setting up 18TB disk for Ethereum blockchain data"
echo "=================================================="

# Check if running as root
if [[ $EUID -ne 0 ]]; then
   echo "❌ This script must be run as root (use sudo)"
   exit 1
fi

# Configuration
BIG_DISK="/dev/sda"
MOUNT_POINT="/mnt/blockchain-disk"
BACKUP_DIR="/tmp/disk-setup-backup"

echo "📱 Disk Information:"
echo "  - Target disk: $BIG_DISK"
echo "  - Mount point: $MOUNT_POINT"

# Create backup directory
mkdir -p "$BACKUP_DIR"

# Check if disk exists
if [[ ! -b "$BIG_DISK" ]]; then
    echo "❌ Disk $BIG_DISK not found"
    exit 1
fi

# Get disk information
DISK_SIZE=$(lsblk -d -o SIZE "$BIG_DISK" | tail -1)
echo "  - Disk size: $DISK_SIZE"

# Check current mount status
if mount | grep -q "$BIG_DISK"; then
    CURRENT_MOUNT=$(mount | grep "$BIG_DISK" | awk '{print $3}')
    echo "  - Currently mounted at: $CURRENT_MOUNT"
    
    # Unmount if mounted elsewhere
    if [[ "$CURRENT_MOUNT" != "$MOUNT_POINT" ]]; then
        echo "⏹️  Unmounting from $CURRENT_MOUNT..."
        umount "$CURRENT_MOUNT" || echo "  ⚠️  Could not unmount, continuing..."
    fi
fi

# Create mount point
echo "📁 Creating mount point..."
mkdir -p "$MOUNT_POINT"

# Check filesystem
echo "🔍 Checking filesystem..."
if blkid "$BIG_DISK" | grep -q "TYPE="; then
    FILESYSTEM_TYPE=$(blkid "$BIG_DISK" | sed 's/.*TYPE="\([^"]*\)".*/\1/')
    echo "  - Existing filesystem: $FILESYSTEM_TYPE"
    
    # Check if filesystem is healthy
    case "$FILESYSTEM_TYPE" in
        "ext4")
            echo "  - Checking ext4 filesystem..."
            e2fsck -f -y "$BIG_DISK" || echo "  ⚠️  Filesystem check completed with warnings"
            ;;
        "xfs")
            echo "  - Checking XFS filesystem..."
            xfs_repair -n "$BIG_DISK" || echo "  ⚠️  Filesystem check completed with warnings"
            ;;
        *)
            echo "  - Filesystem type: $FILESYSTEM_TYPE (no check available)"
            ;;
    esac
else
    echo "  - No filesystem found, will create ext4"
    FILESYSTEM_TYPE="ext4"
fi

# Create filesystem if needed
if [[ "$FILESYSTEM_TYPE" == "ext4" ]] && ! blkid "$BIG_DISK" | grep -q "TYPE="; then
    echo "⚙️  Creating ext4 filesystem..."
    mkfs.ext4 -F -L "ethereum-data" "$BIG_DISK"
    echo "✅ Created ext4 filesystem"
fi

# Mount the disk
echo "🔗 Mounting disk..."
mount "$BIG_DISK" "$MOUNT_POINT"
echo "✅ Disk mounted at $MOUNT_POINT"

# Set optimal permissions
echo "🔐 Setting permissions..."
chown -R root:root "$MOUNT_POINT"
chmod 755 "$MOUNT_POINT"

# Create directory structure
echo "📁 Creating directory structure..."
mkdir -p "$MOUNT_POINT/erigon"
mkdir -p "$MOUNT_POINT/mongodb"
mkdir -p "$MOUNT_POINT/kafka"
mkdir -p "$MOUNT_POINT/zookeeper"
mkdir -p "$MOUNT_POINT/backup"
mkdir -p "$MOUNT_POINT/temp"
mkdir -p "$MOUNT_POINT/logs"

# Set permissions for directories
chmod 755 "$MOUNT_POINT"/*
chown -R 1000:1000 "$MOUNT_POINT/erigon" 2>/dev/null || true
chown -R 999:999 "$MOUNT_POINT/mongodb" 2>/dev/null || true
chown -R 1001:1001 "$MOUNT_POINT/kafka" 2>/dev/null || true
chown -R 1001:1001 "$MOUNT_POINT/zookeeper" 2>/dev/null || true

echo "✅ Created directory structure"

# Optimize filesystem for large files
echo "⚙️  Optimizing filesystem for large files..."
case "$FILESYSTEM_TYPE" in
    "ext4")
        # Remount with optimized options
        umount "$MOUNT_POINT"
        mount -o noatime,nodiratime,data=writeback,barrier=0,commit=60 "$BIG_DISK" "$MOUNT_POINT"
        echo "✅ Remounted with optimized ext4 options"
        ;;
    "xfs")
        # Remount with optimized options
        umount "$MOUNT_POINT"
        mount -o noatime,nodiratime,logbufs=8,logbsize=256k "$BIG_DISK" "$MOUNT_POINT"
        echo "✅ Remounted with optimized XFS options"
        ;;
esac

# Update fstab for persistent mounting
echo "🔧 Updating /etc/fstab for persistent mounting..."
UUID=$(blkid -s UUID -o value "$BIG_DISK")
if [[ -n "$UUID" ]]; then
    # Remove old entries
    sed -i "/$MOUNT_POINT/d" /etc/fstab
    
    # Add new entry with optimized options
    case "$FILESYSTEM_TYPE" in
        "ext4")
            echo "UUID=$UUID $MOUNT_POINT ext4 noatime,nodiratime,data=writeback,barrier=0,commit=60 0 2" >> /etc/fstab
            ;;
        "xfs")
            echo "UUID=$UUID $MOUNT_POINT xfs noatime,nodiratime,logbufs=8,logbsize=256k 0 2" >> /etc/fstab
            ;;
    esac
    echo "✅ Updated /etc/fstab with UUID $UUID"
else
    echo "⚠️  Could not determine UUID, manual fstab update required"
fi

# Create disk space monitoring script
echo "📊 Creating disk space monitoring script..."
cat > /usr/local/bin/monitor-18tb-disk.sh << 'EOF'
#!/bin/bash
# 18TB disk space monitor

MOUNT_POINT="/mnt/blockchain-disk"
LOG_FILE="/var/log/18tb-disk-monitor.log"

monitor_disk() {
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    
    echo "[$timestamp] 18TB Disk Monitor" >> "$LOG_FILE"
    
    # Check if disk is mounted
    if mount | grep -q "$MOUNT_POINT"; then
        echo "  - Disk status: MOUNTED" >> "$LOG_FILE"
        
        # Get disk usage
        local usage=$(df -h "$MOUNT_POINT" | tail -1 | awk '{print $5}' | sed 's/%//')
        local used=$(df -h "$MOUNT_POINT" | tail -1 | awk '{print $3}')
        local available=$(df -h "$MOUNT_POINT" | tail -1 | awk '{print $4}')
        local total=$(df -h "$MOUNT_POINT" | tail -1 | awk '{print $2}')
        
        echo "  - Usage: ${usage}% (${used}/${total})" >> "$LOG_FILE"
        echo "  - Available: $available" >> "$LOG_FILE"
        
        # Check for warnings
        if [[ $usage -gt 90 ]]; then
            echo "  - WARNING: Disk usage above 90%" >> "$LOG_FILE"
            logger -t 18tb-monitor "WARNING: 18TB disk usage at ${usage}%"
        fi
        
        if [[ $usage -gt 95 ]]; then
            echo "  - CRITICAL: Disk usage above 95%" >> "$LOG_FILE"
            logger -t 18tb-monitor "CRITICAL: 18TB disk usage at ${usage}%"
        fi
        
        # Check directory sizes
        echo "  - Directory sizes:" >> "$LOG_FILE"
        du -sh "$MOUNT_POINT"/* 2>/dev/null | while read line; do
            echo "    $line" >> "$LOG_FILE"
        done
        
    else
        echo "  - Disk status: NOT MOUNTED" >> "$LOG_FILE"
        logger -t 18tb-monitor "ERROR: 18TB disk not mounted"
    fi
    
    echo "  - Monitor completed" >> "$LOG_FILE"
    echo "" >> "$LOG_FILE"
}

monitor_disk
EOF

chmod +x /usr/local/bin/monitor-18tb-disk.sh

# Create systemd service for disk monitoring
cat > /etc/systemd/system/18tb-disk-monitor.service << EOF
[Unit]
Description=18TB Disk Space Monitor
After=multi-user.target

[Service]
Type=oneshot
ExecStart=/usr/local/bin/monitor-18tb-disk.sh
User=root
EOF

# Create timer for periodic monitoring
cat > /etc/systemd/system/18tb-disk-monitor.timer << EOF
[Unit]
Description=Run 18TB Disk Space Monitor
Requires=18tb-disk-monitor.service

[Timer]
OnCalendar=*:0/10
Persistent=true

[Install]
WantedBy=timers.target
EOF

systemctl daemon-reload
systemctl enable 18tb-disk-monitor.timer
systemctl start 18tb-disk-monitor.timer

echo "✅ Set up disk space monitoring"

# Create performance test script
echo "🧪 Creating performance test script..."
cat > /usr/local/bin/test-18tb-performance.sh << 'EOF'
#!/bin/bash
# 18TB disk performance test

MOUNT_POINT="/mnt/blockchain-disk"
TEST_FILE="$MOUNT_POINT/performance_test.dat"
TEST_SIZE="10G"

echo "Testing 18TB disk performance..."

# Test write performance
echo "Testing write performance..."
time dd if=/dev/zero of="$TEST_FILE" bs=1M count=10240 oflag=direct 2>&1 | grep -E "(copied|GB/s)"

# Test read performance
echo "Testing read performance..."
time dd if="$TEST_FILE" of=/dev/null bs=1M iflag=direct 2>&1 | grep -E "(copied|GB/s)"

# Clean up
rm -f "$TEST_FILE"

echo "Performance test completed"
EOF

chmod +x /usr/local/bin/test-18tb-performance.sh

echo "✅ Created performance test script"

# Update the pre-allocation script to use the 18TB disk
echo "🔄 Updating pre-allocation script for 18TB disk..."
sed -i "s|ETHEREUM_DATA_DIR=\"/mnt/ethereum-data\"|ETHEREUM_DATA_DIR=\"$MOUNT_POINT\"|g" /home/vovkes/JavaMicroDeFi/preallocate-ethereum-data.sh
sed -i "s|ESTIMATED_SPACE=2000|ESTIMATED_SPACE=15000|g" /home/vovkes/JavaMicroDeFi/preallocate-ethereum-data.sh
sed -i "s|BUFFER_SPACE=100|BUFFER_SPACE=1000|g" /home/vovkes/JavaMicroDeFi/preallocate-ethereum-data.sh

echo "✅ Updated pre-allocation script for 18TB disk"

# Display final status
echo ""
echo "🎉 18TB Disk Setup Completed!"
echo "============================"
echo "📱 Disk: $BIG_DISK ($DISK_SIZE)"
echo "📁 Mount point: $MOUNT_POINT"
echo "💾 Filesystem: $FILESYSTEM_TYPE"
echo "🔍 Monitor script: /usr/local/bin/monitor-18tb-disk.sh"
echo "🧪 Test script: /usr/local/bin/test-18tb-performance.sh"
echo "📊 Monitor log: /var/log/18tb-disk-monitor.log"
echo ""
echo "💡 Next steps:"
echo "   1. Run: sudo /usr/local/bin/test-18tb-performance.sh"
echo "   2. Monitor: tail -f /var/log/18tb-disk-monitor.log"
echo "   3. Run: sudo ./preallocate-ethereum-data.sh"
echo "   4. Run: sudo ./optimize-all-io.sh"
echo ""
echo "📊 Current disk usage:"
df -h "$MOUNT_POINT"
