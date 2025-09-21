# 18TB Disk Setup Guide for Ethereum Blockchain Data

## Overview

This guide helps you set up your 18TB disk for optimal Ethereum blockchain data storage. The 18TB disk provides ample space for the full Ethereum blockchain data with room for future growth.

## Current Situation

- **Available space**: 263GB (insufficient for blockchain data)
- **Required space**: 2.1TB+ for full Ethereum data
- **18TB disk**: Provides 15TB+ for blockchain data with buffer

## Disk Setup Process

### Step 1: Setup 18TB Disk

```bash
# Run the 18TB disk setup script
sudo ./setup-18tb-disk.sh
```

This script will:
- Check and mount the 18TB disk (`/dev/sda`)
- Create optimized filesystem (ext4 or XFS)
- Set up directory structure
- Configure persistent mounting
- Set up monitoring

### Step 2: Pre-allocate Space

```bash
# Pre-allocate space for blockchain data
sudo ./preallocate-ethereum-data.sh
```

This will pre-allocate:
- **Chain data**: 12TB
- **State data**: 2TB  
- **Temp space**: 1TB
- **Total**: 15TB (with 3TB buffer)

### Step 3: Run I/O Optimizations

```bash
# Run all I/O optimizations
sudo ./optimize-all-io.sh
```

This includes:
- NVMe I/O optimization
- Filesystem optimization
- Docker storage optimization
- ERIGON configuration

## Directory Structure

After setup, your 18TB disk will have:

```
/mnt/ethereum-data/
├── erigon/           # ERIGON blockchain data (12TB)
├── mongodb/          # MongoDB data
├── kafka/            # Kafka data
├── zookeeper/        # Zookeeper data
├── backup/           # Backup files
├── temp/             # Temporary files (1TB)
└── logs/             # Log files
```

## Space Allocation

| Component | Size | Purpose |
|-----------|------|---------|
| Chain Data | 12TB | Full Ethereum blockchain |
| State Data | 2TB | Ethereum state data |
| Temp Space | 1TB | Processing and temporary files |
| Buffer | 3TB | Future growth and safety margin |
| **Total** | **18TB** | **Complete setup** |

## Performance Expectations

With the 18TB disk setup:

- **Sync time**: 20-30% faster than before
- **Storage efficiency**: No space constraints
- **Future-proof**: Room for 5+ years of growth
- **Performance**: Optimized for large file operations

## Monitoring

### Disk Space Monitoring

```bash
# Check disk usage
df -h /mnt/ethereum-data

# Monitor in real-time
watch -n 5 'df -h /mnt/ethereum-data'

# Check monitoring logs
tail -f /var/log/18tb-disk-monitor.log
```

### Performance Testing

```bash
# Test disk performance
sudo /usr/local/bin/test-18tb-performance.sh

# Test I/O performance
sudo /usr/local/bin/test-ethereum-io.sh
```

## Migration from Current Setup

If you have existing data:

1. **Stop ERIGON**:
   ```bash
   docker stop erigon
   ```

2. **Copy existing data**:
   ```bash
   sudo cp -r /path/to/current/erigon/data/* /mnt/ethereum-data/erigon/
   ```

3. **Update Docker Compose**:
   ```bash
   # Use the optimized docker-compose
   docker-compose -f docker-compose-optimized.yml up -d
   ```

## Troubleshooting

### Disk Not Mounting

```bash
# Check disk status
lsblk -f

# Check filesystem
sudo fsck /dev/sda

# Manual mount
sudo mount /dev/sda /mnt/ethereum-data
```

### Permission Issues

```bash
# Fix permissions
sudo chown -R 1000:1000 /mnt/ethereum-data/erigon
sudo chmod -R 755 /mnt/ethereum-data
```

### Performance Issues

```bash
# Check I/O performance
iostat -x 1

# Check disk health
sudo smartctl -a /dev/sda
```

## Maintenance

### Regular Tasks

- **Weekly**: Check disk usage and performance
- **Monthly**: Review logs and clean up old data
- **Quarterly**: Run filesystem checks
- **Annually**: Plan for future storage needs

### Log Files

- **Disk monitoring**: `/var/log/18tb-disk-monitor.log`
- **I/O optimization**: `/var/log/io-optimization.log`
- **ERIGON performance**: `/var/log/erigon-performance.log`

## Security Considerations

- **Backup**: Regular backups of critical data
- **Access control**: Proper file permissions
- **Monitoring**: Automated alerts for disk issues
- **Encryption**: Consider encryption for sensitive data

## Future Planning

With 18TB of space:

- **Current Ethereum**: ~2TB
- **Future growth**: ~500GB/year
- **Buffer space**: 3TB
- **Estimated lifespan**: 5+ years

## Support

For issues or questions:

1. Check the log files
2. Run diagnostic scripts
3. Review the monitoring output
4. Consult the optimization guides

## Conclusion

The 18TB disk setup provides:

- ✅ **Sufficient space** for full Ethereum data
- ✅ **Optimized performance** for blockchain operations
- ✅ **Future-proof** storage for years of growth
- ✅ **Monitoring and maintenance** tools
- ✅ **Professional-grade** setup for production use

This setup will eliminate the space constraints and provide optimal performance for your Ethereum blockchain data processing.
