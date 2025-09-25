# Ethereum Mainnet Full Stack

Complete Ethereum archive node stack with monitoring, management, and reverse proxy capabilities.

## 🚀 Quick Start

### Start the Stack
```bash
./start-ethereum-stack.sh
```

### Check Status
```bash
./check-ethereum-stack.sh
```

### Stop the Stack
```bash
./stop-ethereum-stack.sh
```

### Restart the Stack
```bash
./restart-ethereum-stack.sh
```

## 📋 Stack Components

### Core Services
- **ERIGON**: Ethereum execution client (archive mode)
- **Lighthouse**: Ethereum consensus client
- **Prometheus**: Metrics collection
- **Grafana**: Monitoring dashboards
- **Nginx**: Reverse proxy and load balancing
- **Monitor**: Health monitoring service

### Network Ports
- **8545**: ERIGON HTTP RPC
- **8546**: ERIGON WebSocket RPC
- **8551**: ERIGON Engine API
- **5052**: Lighthouse HTTP API
- **5054**: Lighthouse Metrics
- **6060**: ERIGON Metrics
- **9091**: Prometheus
- **3001**: Grafana Dashboard
- **80**: Nginx Reverse Proxy
- **30303**: ERIGON P2P (TCP/UDP)
- **9000**: Lighthouse P2P (TCP/UDP)

## 🔧 Management Commands

### Stack Management
```bash
# Start the entire stack
./start-ethereum-stack.sh

# Check status of all services
./check-ethereum-stack.sh

# View logs for all services
./logs-ethereum-stack.sh

# Stop the entire stack
./stop-ethereum-stack.sh

# Restart the entire stack
./restart-ethereum-stack.sh
```

### Individual Service Management
```bash
# Start specific service
docker-compose -f ethereum-mainnet-full-stack.yml -p ETHEREUM_mainnet_full up -d <service>

# Stop specific service
docker-compose -f ethereum-mainnet-full-stack.yml -p ETHEREUM_mainnet_full stop <service>

# View logs for specific service
docker logs -f ETHEREUM_mainnet_full-<service>

# Restart specific service
docker-compose -f ethereum-mainnet-full-stack.yml -p ETHEREUM_mainnet_full restart <service>
```

### Node Status Monitoring
```bash
# Quick sync status check
./quick-sync-check.sh

# Detailed node verification
./verify-node-status.sh

# Lighthouse progress monitoring
./lighthouse-progress.sh

# Real-time sync monitoring
./monitor-sync-progress.sh
```

## 🌐 Access Points

### Direct Access
- **ERIGON RPC**: http://localhost:8545
- **ERIGON WebSocket**: ws://localhost:8546
- **Lighthouse API**: http://localhost:5052
- **Prometheus**: http://localhost:9091
- **Grafana**: http://localhost:3001 (admin/admin123)

### Via Nginx Reverse Proxy
- **RPC**: http://rpc.ethereum.local
- **WebSocket**: ws://ws.ethereum.local
- **Beacon API**: http://beacon.ethereum.local
- **Metrics**: http://metrics.ethereum.local
- **Dashboard**: http://dashboard.ethereum.local
- **Health**: http://health.ethereum.local

## 📊 Monitoring

### Grafana Dashboards
Access Grafana at http://localhost:3001 with credentials:
- **Username**: admin
- **Password**: admin123

### Prometheus Metrics
Access Prometheus at http://localhost:9091 for detailed metrics.

### Health Checks
- **Node Health**: http://localhost:5052/eth/v1/node/health
- **Sync Status**: http://localhost:5052/eth/v1/node/syncing
- **ERIGON Status**: curl -X POST -H "Content-Type: application/json" -d '{"jsonrpc":"2.0","method":"eth_syncing","params":[],"id":1}' http://localhost:8545

## 💾 Data Storage

### Data Directories
- **ERIGON Data**: `/mnt/sata18tb/erigon-hot`
- **Lighthouse Data**: `/mnt/sata18tb/lighthouse-data`
- **Snapshots**: `/mnt/sata18tb/erigon_snapshots`

### Volume Management
```bash
# Check disk usage
du -sh /mnt/sata18tb/erigon-hot
du -sh /mnt/sata18tb/lighthouse-data

# Clean up old data (if needed)
docker system prune -f
```

## 🔒 Security

### JWT Secret
The JWT secret is automatically generated and stored in:
- **Path**: `./ethereum-full-archive/erigon-config/jwt.hex`
- **Permissions**: 600 (owner read/write only)

### Network Security
- All services run in isolated Docker network
- P2P ports are exposed for peer discovery
- RPC ports are accessible for API access
- Nginx provides rate limiting and access control

## 🚨 Troubleshooting

### Common Issues

#### Services Not Starting
```bash
# Check Docker status
docker info

# Check container logs
./logs-ethereum-stack.sh

# Check disk space
df -h /mnt/sata18tb
```

#### Sync Issues
```bash
# Check ERIGON sync status
curl -X POST -H "Content-Type: application/json" -d '{"jsonrpc":"2.0","method":"eth_syncing","params":[],"id":1}' http://localhost:8545

# Check Lighthouse sync status
curl http://localhost:5052/eth/v1/node/syncing

# Monitor sync progress
./lighthouse-progress.sh
```

#### Performance Issues
```bash
# Check resource usage
docker stats

# Check metrics
curl http://localhost:5054/metrics | head -20

# Check Prometheus metrics
curl http://localhost:9091/metrics
```

### Log Analysis
```bash
# Follow ERIGON logs
docker logs -f ETHEREUM_mainnet_full-erigon

# Follow Lighthouse logs
docker logs -f ETHEREUM_mainnet_full-lighthouse

# Search for errors
docker logs ETHEREUM_mainnet_full-erigon 2>&1 | grep -i error
```

## 📈 Performance Optimization

### Resource Limits
The stack is configured with optimal resource limits:
- **ERIGON**: 120GB memory limit
- **Lighthouse**: 32GB memory limit
- **Prometheus**: 8GB memory limit
- **Grafana**: 4GB memory limit

### Storage Optimization
- **ERIGON**: Archive mode for complete historical data
- **Lighthouse**: Checkpoint sync for faster initial sync
- **Snapshots**: Automatic snapshot management

## 🔄 Updates and Maintenance

### Updating Services
```bash
# Pull latest images
docker-compose -f ethereum-mainnet-full-stack.yml -p ETHEREUM_mainnet_full pull

# Restart with new images
./restart-ethereum-stack.sh
```

### Backup
```bash
# Backup configuration
tar -czf ethereum-config-backup-$(date +%Y%m%d).tar.gz ethereum-full-archive/

# Backup data (if needed)
# Note: Data is already on persistent storage
```

## 📞 Support

### Useful Commands
```bash
# Check all containers
docker ps -a

# Check network
docker network ls

# Check volumes
docker volume ls

# System information
docker system df
```

### Log Locations
- **ERIGON**: Container logs + `/mnt/sata18tb/erigon-hot/erigon.log`
- **Lighthouse**: Container logs
- **Prometheus**: Container logs
- **Grafana**: Container logs

## 🎯 Production Deployment

### Prerequisites
- Docker and Docker Compose installed
- 18TB+ storage available
- 32GB+ RAM recommended
- Stable internet connection
- Firewall configured for P2P ports

### Deployment Steps
1. Clone the repository
2. Configure data directories
3. Run `./start-ethereum-stack.sh`
4. Monitor sync progress
5. Configure monitoring dashboards
6. Set up alerts (optional)

### Monitoring Setup
1. Access Grafana at http://localhost:3001
2. Import dashboards from `./ethereum-full-archive/grafana/dashboards/`
3. Configure Prometheus data source
4. Set up alerts for sync status and performance

---

**🎉 Your Ethereum Mainnet Full Stack is ready for production use!**
