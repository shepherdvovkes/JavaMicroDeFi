package com.defimon.multichain.service.impl;

import com.defimon.multichain.config.EthereumConfiguration;
import com.defimon.multichain.model.Block;
import com.defimon.multichain.model.Transaction;
import com.defimon.multichain.plugin.context.PluginContext;
import com.defimon.multichain.service.BlockchainSyncService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.math.BigInteger;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Ethereum blockchain synchronization service.
 * 
 * This service handles real-time synchronization of Ethereum blockchain data
 * using a hybrid approach:
 * - Rust-based high-performance sync engine for data processing
 * - Java-based coordination and business logic
 * - Virtual threads for I/O-bound operations
 * - Reactive streams for high-throughput data processing
 */
public class EthereumSyncService implements BlockchainSyncService {
    
    private static final Logger logger = LoggerFactory.getLogger(EthereumSyncService.class);
    
    private final EthereumConfiguration config;
    private final PluginContext context;
    private final WebClient webClient;
    
    // Metrics
    private final Counter blockProcessedCounter;
    private final Counter transactionProcessedCounter;
    private final Timer blockProcessingTimer;
    private final Timer transactionProcessingTimer;
    
    // State management
    private final AtomicBoolean syncRunning = new AtomicBoolean(false);
    private final AtomicBoolean healthy = new AtomicBoolean(false);
    private final AtomicLong latestSyncedBlock = new AtomicLong(0);
    private final AtomicLong currentBlock = new AtomicLong(0);
    
    // Rust sync engine client
    private WebClient rustSyncEngineClient;
    
    public EthereumSyncService(EthereumConfiguration config, PluginContext context) {
        this.config = config;
        this.context = context;
        
        // Initialize WebClient for Ethereum RPC
        this.webClient = WebClient.builder()
                .baseUrl(config.getEffectiveRpcUrl())
                .build();
        
        // Initialize Rust sync engine client if enabled
        if (config.getEnableRustSyncEngine()) {
            this.rustSyncEngineClient = WebClient.builder()
                    .baseUrl(config.getRustSyncEngineUrl())
                    .build();
        }
        
        // Initialize metrics
        this.blockProcessedCounter = Counter.builder("ethereum.blocks.processed")
                .description("Number of Ethereum blocks processed")
                .register(context.meterRegistry());
        
        this.transactionProcessedCounter = Counter.builder("ethereum.transactions.processed")
                .description("Number of Ethereum transactions processed")
                .register(context.meterRegistry());
        
        this.blockProcessingTimer = Timer.builder("ethereum.block.processing.time")
                .description("Time taken to process Ethereum blocks")
                .register(context.meterRegistry());
        
        this.transactionProcessingTimer = Timer.builder("ethereum.transaction.processing.time")
                .description("Time taken to process Ethereum transactions")
                .register(context.meterRegistry());
        
        logger.info("Ethereum sync service initialized with RPC URL: {}", config.getEffectiveRpcUrl());
    }
    
    @Override
    public Mono<Long> getLatestBlockNumber() {
        return webClient.post()
                .uri("")
                .bodyValue(createJsonRpcRequest("eth_blockNumber", List.of()))
                .retrieve()
                .bodyToMono(JsonRpcResponse.class)
                .map(response -> {
                    if (response.error() != null) {
                        throw new RuntimeException("RPC Error: " + response.error().message());
                    }
                    String hexBlockNumber = (String) response.result();
                    return Long.parseLong(hexBlockNumber.substring(2), 16);
                })
                .doOnNext(blockNumber -> {
                    currentBlock.set(blockNumber);
                    healthy.set(true);
                })
                .doOnError(error -> {
                    logger.error("Failed to get latest block number", error);
                    healthy.set(false);
                })
                .subscribeOn(Schedulers.boundedElastic());
    }
    
    @Override
    public Mono<Block> getBlockByNumber(Long blockNumber) {
        return Timer.Sample.start(context.meterRegistry())
                .stop(blockProcessingTimer)
                .recordCallable(() -> {
                    String hexBlockNumber = "0x" + Long.toHexString(blockNumber);
                    
                    return webClient.post()
                            .uri("")
                            .bodyValue(createJsonRpcRequest("eth_getBlockByNumber", List.of(hexBlockNumber, true)))
                            .retrieve()
                            .bodyToMono(JsonRpcResponse.class)
                            .map(this::mapJsonRpcBlockToBlock)
                            .doOnNext(block -> {
                                blockProcessedCounter.increment();
                                logger.debug("Retrieved block {}: {}", blockNumber, block.hash());
                            })
                            .doOnError(error -> {
                                logger.error("Failed to get block by number: {}", blockNumber, error);
                                healthy.set(false);
                            })
                            .subscribeOn(Schedulers.boundedElastic());
                });
    }
    
    @Override
    public Mono<Block> getBlockByHash(String blockHash) {
        return Timer.Sample.start(context.meterRegistry())
                .stop(blockProcessingTimer)
                .recordCallable(() -> {
                    return webClient.post()
                            .uri("")
                            .bodyValue(createJsonRpcRequest("eth_getBlockByHash", List.of(blockHash, true)))
                            .retrieve()
                            .bodyToMono(JsonRpcResponse.class)
                            .map(this::mapJsonRpcBlockToBlock)
                            .doOnNext(block -> {
                                blockProcessedCounter.increment();
                                logger.debug("Retrieved block by hash {}: {}", blockHash, block.number());
                            })
                            .doOnError(error -> {
                                logger.error("Failed to get block by hash: {}", blockHash, error);
                                healthy.set(false);
                            })
                            .subscribeOn(Schedulers.boundedElastic());
                });
    }
    
    @Override
    public Mono<Block> getLatestBlock() {
        return getLatestBlockNumber()
                .flatMap(this::getBlockByNumber);
    }
    
    @Override
    public Mono<Transaction> getTransaction(String transactionHash) {
        return Timer.Sample.start(context.meterRegistry())
                .stop(transactionProcessingTimer)
                .recordCallable(() -> {
                    return webClient.post()
                            .uri("")
                            .bodyValue(createJsonRpcRequest("eth_getTransactionByHash", List.of(transactionHash)))
                            .retrieve()
                            .bodyToMono(JsonRpcResponse.class)
                            .map(this::mapJsonRpcTransactionToTransaction)
                            .doOnNext(tx -> {
                                transactionProcessedCounter.increment();
                                logger.debug("Retrieved transaction: {}", transactionHash);
                            })
                            .doOnError(error -> {
                                logger.error("Failed to get transaction: {}", transactionHash, error);
                                healthy.set(false);
                            })
                            .subscribeOn(Schedulers.boundedElastic());
                });
    }
    
    @Override
    public Mono<List<Transaction>> getBlockTransactions(Long blockNumber) {
        return getBlockByNumber(blockNumber)
                .flatMap(block -> {
                    if (block.transactions().isEmpty()) {
                        return Mono.just(List.of());
                    }
                    
                    return Flux.fromIterable(block.transactions())
                            .flatMap(this::getTransaction)
                            .collectList();
                });
    }
    
    @Override
    public Mono<List<Transaction>> getBlockTransactions(String blockHash) {
        return getBlockByHash(blockHash)
                .flatMap(block -> {
                    if (block.transactions().isEmpty()) {
                        return Mono.just(List.of());
                    }
                    
                    return Flux.fromIterable(block.transactions())
                            .flatMap(this::getTransaction)
                            .collectList();
                });
    }
    
    @Override
    public Flux<Block> syncNewBlocks() {
        if (!syncRunning.compareAndSet(false, true)) {
            return Flux.error(new IllegalStateException("Sync is already running"));
        }
        
        logger.info("Starting real-time Ethereum block synchronization");
        
        return Flux.interval(Duration.ofMillis(config.getBlockTime()))
                .flatMap(tick -> getLatestBlockNumber())
                .distinctUntilChanged()
                .flatMap(latestBlock -> {
                    long fromBlock = latestSyncedBlock.get() + 1;
                    if (fromBlock <= latestBlock) {
                        return syncBlockRange(fromBlock, latestBlock);
                    }
                    return Flux.empty();
                })
                .doOnNext(block -> {
                    latestSyncedBlock.set(block.number());
                    blockProcessedCounter.increment();
                })
                .doOnError(error -> {
                    logger.error("Error during real-time sync", error);
                    syncRunning.set(false);
                    healthy.set(false);
                })
                .doOnComplete(() -> {
                    logger.info("Real-time sync completed");
                    syncRunning.set(false);
                })
                .subscribeOn(Schedulers.boundedElastic());
    }
    
    @Override
    public Flux<Block> syncBlockRange(Long fromBlock, Long toBlock) {
        logger.info("Syncing Ethereum blocks from {} to {}", fromBlock, toBlock);
        
        return Flux.range(fromBlock.intValue(), (int) (toBlock - fromBlock + 1))
                .flatMap(blockNumber -> getBlockByNumber(blockNumber.longValue()))
                .doOnNext(block -> {
                    latestSyncedBlock.set(Math.max(latestSyncedBlock.get(), block.number()));
                    blockProcessedCounter.increment();
                })
                .doOnError(error -> {
                    logger.error("Error syncing block range {}-{}", fromBlock, toBlock, error);
                    healthy.set(false);
                })
                .subscribeOn(Schedulers.boundedElastic());
    }
    
    @Override
    public Mono<SyncStatus> getSyncStatus() {
        return getLatestBlockNumber()
                .map(latestBlock -> {
                    long synced = latestSyncedBlock.get();
                    double progress = latestBlock > 0 ? (double) synced / latestBlock * 100 : 0;
                    
                    return new SyncStatus(
                        syncRunning.get(),
                        synced,
                        latestBlock,
                        progress,
                        System.currentTimeMillis(),
                        healthy.get() ? null : "Service unhealthy"
                    );
                });
    }
    
    @Override
    public void startSync() {
        if (config.getEnableRustSyncEngine()) {
            startRustSyncEngine();
        } else {
            startJavaSync();
        }
    }
    
    @Override
    public void stopSync() {
        syncRunning.set(false);
        healthy.set(false);
        logger.info("Ethereum sync stopped");
    }
    
    @Override
    public boolean isSyncRunning() {
        return syncRunning.get();
    }
    
    /**
     * Starts the Rust sync engine for high-performance processing.
     */
    private void startRustSyncEngine() {
        logger.info("Starting Rust sync engine for Ethereum");
        
        // Send start command to Rust sync engine
        rustSyncEngineClient.post()
                .uri("/start")
                .bodyValue(Map.of("chain_id", "1", "rpc_url", config.getEffectiveRpcUrl()))
                .retrieve()
                .bodyToMono(String.class)
                .subscribe(
                    response -> {
                        logger.info("Rust sync engine started: {}", response);
                        healthy.set(true);
                    },
                    error -> {
                        logger.error("Failed to start Rust sync engine", error);
                        startJavaSync(); // Fallback to Java sync
                    }
                );
    }
    
    /**
     * Starts Java-based sync as fallback.
     */
    private void startJavaSync() {
        logger.info("Starting Java sync for Ethereum");
        syncNewBlocks().subscribe();
    }
    
    /**
     * Creates a JSON-RPC request.
     */
    private Map<String, Object> createJsonRpcRequest(String method, List<Object> params) {
        return Map.of(
            "jsonrpc", "2.0",
            "method", method,
            "params", params,
            "id", System.currentTimeMillis()
        );
    }
    
    /**
     * Maps JSON-RPC block response to Block model.
     */
    private Block mapJsonRpcBlockToBlock(JsonRpcResponse response) {
        if (response.error() != null) {
            throw new RuntimeException("RPC Error: " + response.error().message());
        }
        
        Map<String, Object> blockData = (Map<String, Object>) response.result();
        
        return new Block(
            Long.parseLong(((String) blockData.get("number")).substring(2), 16),
            (String) blockData.get("hash"),
            (String) blockData.get("parentHash"),
            LocalDateTime.now(), // TODO: Parse timestamp from block data
            (String) blockData.get("nonce"),
            blockData.get("difficulty") != null ? 
                new BigInteger(((String) blockData.get("difficulty")).substring(2), 16).doubleValue() : null,
            blockData.get("gasLimit") != null ? 
                new BigInteger(((String) blockData.get("gasLimit")).substring(2), 16) : null,
            blockData.get("gasUsed") != null ? 
                new BigInteger(((String) blockData.get("gasUsed")).substring(2), 16) : null,
            (String) blockData.get("miner"),
            null, // size
            ((List<?>) blockData.get("transactions")).size(),
            (List<String>) blockData.get("transactions"),
            Map.of(),
            config.getChainId()
        );
    }
    
    /**
     * Maps JSON-RPC transaction response to Transaction model.
     */
    private Transaction mapJsonRpcTransactionToTransaction(JsonRpcResponse response) {
        if (response.error() != null) {
            throw new RuntimeException("RPC Error: " + response.error().message());
        }
        
        Map<String, Object> txData = (Map<String, Object>) response.result();
        
        return new Transaction(
            (String) txData.get("hash"),
            txData.get("blockNumber") != null ? 
                Long.parseLong(((String) txData.get("blockNumber")).substring(2), 16) : null,
            (String) txData.get("blockHash"),
            txData.get("transactionIndex") != null ? 
                Integer.parseInt(((String) txData.get("transactionIndex")).substring(2), 16) : null,
            (String) txData.get("from"),
            (String) txData.get("to"),
            txData.get("value") != null ? 
                new BigInteger(((String) txData.get("value")).substring(2), 16) : null,
            txData.get("gas") != null ? 
                new BigInteger(((String) txData.get("gas")).substring(2), 16) : null,
            txData.get("gasPrice") != null ? 
                new BigInteger(((String) txData.get("gasPrice")).substring(2), 16) : null,
            null, // gasUsed
            txData.get("nonce") != null ? 
                Long.parseLong(((String) txData.get("nonce")).substring(2), 16) : null,
            (String) txData.get("input"),
            null, // output
            Transaction.TransactionStatus.PENDING,
            LocalDateTime.now(), // TODO: Parse timestamp
            config.getChainId(),
            Map.of()
        );
    }
    
    /**
     * Checks if the service is healthy.
     * 
     * @return true if the service is healthy
     */
    public boolean isHealthy() {
        return healthy.get();
    }
    
    /**
     * Record for JSON-RPC response.
     * 
     * @param jsonrpc the JSON-RPC version
     * @param result the result data
     * @param error the error data
     * @param id the request ID
     */
    private record JsonRpcResponse(
        String jsonrpc,
        Object result,
        JsonRpcError error,
        Object id
    ) {}
    
    /**
     * Record for JSON-RPC error.
     * 
     * @param code the error code
     * @param message the error message
     */
    private record JsonRpcError(
        Integer code,
        String message
    ) {}
}
