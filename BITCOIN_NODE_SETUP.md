# Bitcoin Full Node Setup for Java MicroDeFi

## 🎉 Setup Complete!

Your Bitcoin full node is now running and ready for integration with your Java microservices.

## 📊 Node Status

- **Bitcoin Version**: 29.1.0 (Satoshi:29.1.0/)
- **Blockchain Height**: 915,638 blocks
- **Sync Status**: Fully synced (99.99% verification progress)
- **Network**: Mainnet
- **Connections**: 9 peers
- **Data Size**: 822GB on 18TB disk
- **Container**: bitcoin-full-node

## 🌐 RPC Endpoints

### Direct RPC Access
- **URL**: `http://localhost:8332`
- **Username**: `bitcoin`
- **Password**: `ultrafast_archive_node_2024`
- **Method**: HTTP POST with JSON-RPC 1.0

### Example RPC Calls

```bash
# Get blockchain info
curl -u bitcoin:ultrafast_archive_node_2024 -X POST -H "Content-Type: application/json" \
  -d '{"jsonrpc": "1.0", "id": "test", "method": "getblockchaininfo", "params": []}' \
  http://localhost:8332

# Get network info
curl -u bitcoin:ultrafast_archive_node_2024 -X POST -H "Content-Type: application/json" \
  -d '{"jsonrpc": "1.0", "id": "test", "method": "getnetworkinfo", "params": []}' \
  http://localhost:8332

# Get mempool info
curl -u bitcoin:ultrafast_archive_node_2024 -X POST -H "Content-Type: application/json" \
  -d '{"jsonrpc": "1.0", "id": "test", "method": "getmempoolinfo", "params": []}' \
  http://localhost:8332
```

## ☕ Java Integration

### Configuration Properties

Add these to your Java application's `application.yml` or `application.properties`:

```yaml
bitcoin:
  rpc:
    host: localhost
    port: 8332
    username: bitcoin
    password: ultrafast_archive_node_2024
    timeout: 60000
    connection-timeout: 30000
  network: mainnet
  testnet: false
```

### Java HTTP Client Example

```java
@Service
public class BitcoinService {
    
    private static final String RPC_URL = "http://localhost:8332";
    private static final String USERNAME = "bitcoin";
    private static final String PASSWORD = "ultrafast_archive_node_2024";
    
    public BitcoinBlockchainInfo getBlockchainInfo() {
        // Implement your HTTP client logic here
        // Use RestTemplate, WebClient, or your preferred HTTP client
    }
}
```

## 🔧 Management Commands

### Start Bitcoin Node
```bash
cd /home/vovkes/JavaMicroDeFi
docker-compose -f bitcoin-simple-docker-compose.yml up -d
```

### Stop Bitcoin Node
```bash
cd /home/vovkes/JavaMicroDeFi
docker-compose -f bitcoin-simple-docker-compose.yml down
```

### Check Node Status
```bash
docker-compose -f bitcoin-simple-docker-compose.yml ps
docker logs bitcoin-full-node
```

### Monitor Logs
```bash
docker logs -f bitcoin-full-node
```

## 📁 File Structure

```
/mnt/bitcoin/
├── data/                    # Bitcoin blockchain data (822GB)
│   ├── blocks/             # Block files
│   ├── chainstate/         # Chain state database
│   ├── indexes/            # Index files
│   └── mempool.dat         # Mempool data
├── bitcoin.conf            # Bitcoin configuration
├── docker-compose.yml      # Original compose file
└── logs/                   # Log files

/home/vovkes/JavaMicroDeFi/
├── bitcoin-simple-docker-compose.yml    # Simplified compose file
├── bitcoin-service-config.yml          # Java configuration template
├── start-bitcoin-node.sh               # Startup script
└── BITCOIN_NODE_SETUP.md              # This file
```

## 🚀 Performance Optimizations

The Bitcoin node is configured with:
- **txindex=1**: Full transaction index
- **blockfilterindex=1**: Block filter index
- **coinstatsindex=1**: Coin statistics index
- **dbcache=4096**: 4GB database cache
- **maxmempool=1024**: 1GB mempool
- **rpcworkqueue=32**: 32 RPC worker threads
- **rpcthreads=32**: 32 RPC processing threads

## 🔍 Monitoring

### Health Check
```bash
curl -u bitcoin:ultrafast_archive_node_2024 -X POST -H "Content-Type: application/json" \
  -d '{"jsonrpc": "1.0", "id": "health", "method": "getblockchaininfo", "params": []}' \
  http://localhost:8332
```

### Key Metrics to Monitor
- Blockchain height vs network height
- Verification progress
- Memory usage
- Disk space
- Network connections
- Mempool size

## 🛠️ Troubleshooting

### Common Issues

1. **Port 8332 not accessible**
   - Check if container is running: `docker ps`
   - Check logs: `docker logs bitcoin-full-node`

2. **Authentication failed**
   - Verify username/password in bitcoin.conf
   - Check RPC credentials in your Java application

3. **Slow RPC responses**
   - Monitor system resources
   - Check network connectivity
   - Consider increasing timeout values

### Useful Commands

```bash
# Check container status
docker ps | grep bitcoin

# View real-time logs
docker logs -f bitcoin-full-node

# Restart container
docker restart bitcoin-full-node

# Check disk usage
df -h /mnt
du -sh /mnt/bitcoin/data
```

## 📚 Additional Resources

- [Bitcoin RPC Documentation](https://developer.bitcoin.org/reference/rpc/)
- [Bitcoin Core Configuration](https://en.bitcoin.it/wiki/Running_Bitcoin)
- [Java HTTP Client Examples](https://spring.io/guides/gs/consuming-rest/)

## ✅ Next Steps

1. **Integrate with your Java microservices** using the provided configuration
2. **Set up monitoring** for the Bitcoin node health and performance
3. **Implement error handling** for RPC calls in your applications
4. **Consider implementing caching** for frequently accessed blockchain data
5. **Set up alerts** for node downtime or sync issues

Your Bitcoin full node is now ready to serve blockchain data to your Java microservices! 🚀
