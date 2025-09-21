#!/bin/bash

# Setup Blockchain Disk Mount Point
# This script sets up the 18TB disk at /mnt/blockchain-disk

set -e

echo "🚀 Setting up 18TB disk mount point"
echo "=================================="

# Check if running as root
if [[ $EUID -ne 0 ]]; then
   echo "❌ This script must be run as root (use sudo)"
   exit 1
fi

# Configuration
BIG_DISK="/dev/sda"
MOUNT_POINT="/mnt/blockchain-disk"
CURRENT_MOUNT="/media/vovkes/12c367d0-2e89-414c-b6fa-2fbfc8c4ee55"

echo "📱 Mount Configuration:"
echo "  - Source disk: $BIG_DISK"
echo "  - Current mount: $CURRENT_MOUNT"
echo "  - New mount point: $MOUNT_POINT"

# Check if disk exists
if [[ ! -b "$BIG_DISK" ]]; then
    echo "❌ Disk $BIG_DISK not found"
    exit 1
fi

# Create mount point
echo "📁 Creating mount point..."
mkdir -p "$MOUNT_POINT"

# Unmount from current location if mounted
if mount | grep -q "$CURRENT_MOUNT"; then
    echo "⏹️  Unmounting from current location..."
    umount "$CURRENT_MOUNT" || echo "  ⚠️  Could not unmount, continuing..."
fi

# Mount to new location
echo "🔗 Mounting to new location..."
mount "$BIG_DISK" "$MOUNT_POINT"

if mount | grep -q "$MOUNT_POINT"; then
    echo "✅ Successfully mounted to $MOUNT_POINT"
else
    echo "❌ Failed to mount to $MOUNT_POINT"
    exit 1
fi

# Set permissions
echo "🔐 Setting permissions..."
chown -R root:root "$MOUNT_POINT"
chmod 755 "$MOUNT_POINT"

# Update fstab for persistent mounting
echo "🔧 Updating /etc/fstab..."
UUID=$(blkid -s UUID -o value "$BIG_DISK")
if [[ -n "$UUID" ]]; then
    # Remove old entries
    sed -i "/$CURRENT_MOUNT/d" /etc/fstab
    sed -i "/$MOUNT_POINT/d" /etc/fstab
    
    # Add new entry
    echo "UUID=$UUID $MOUNT_POINT ext4 defaults 0 2" >> /etc/fstab
    echo "✅ Updated /etc/fstab with UUID $UUID"
else
    echo "⚠️  Could not determine UUID, manual fstab update required"
fi

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

echo "✅ Created directory structure"

# Display final status
echo ""
echo "🎉 Blockchain Disk Mount Setup Completed!"
echo "========================================"
echo "📁 Mount point: $MOUNT_POINT"
echo "💾 Disk: $BIG_DISK"
echo "🔗 UUID: $UUID"
echo ""
echo "📊 Current disk usage:"
df -h "$MOUNT_POINT"
echo ""
echo "💡 Next steps:"
echo "   1. Run: sudo ./migrate-blockchain-data.sh"
echo "   2. Verify: sudo /usr/local/bin/verify-migration.sh"
echo "   3. Start ERIGON with new configuration"
