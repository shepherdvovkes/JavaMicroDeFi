package com.defimon.bitcoin.metrics.service;

import com.defimon.bitcoin.metrics.config.BitcoinMetricsConfig;
import com.defimon.bitcoin.metrics.model.*;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Bitcoin Metrics Collector Service
 */
@Service
public class BitcoinMetricsCollector {
    
    private static final Logger logger = LoggerFactory.getLogger(BitcoinMetricsCollector.class);
    
    private final BitcoinRpcClient rpcClient;
    private final MeterRegistry meterRegistry;
    private final BitcoinMetricsConfig config;
    
    // Atomic references for metrics
    private final AtomicLong blockCount = new AtomicLong(0);
    private final AtomicLong connectionCount = new AtomicLong(0);
    private final AtomicLong mempoolSize = new AtomicLong(0);
    private final AtomicLong mempoolBytes = new AtomicLong(0);
    private final AtomicReference<Double> verificationProgress = new AtomicReference<>(0.0);
    private final AtomicReference<Double> difficulty = new AtomicReference<>(0.0);
    private final AtomicReference<Double> totalFee = new AtomicReference<>(0.0);
    private final AtomicReference<Boolean> isSynced = new AtomicReference<>(false);
    
    // Timers for performance metrics
    private final Timer rpcCallTimer;
    private final Timer metricsCollectionTimer;
    
    @Autowired
    public BitcoinMetricsCollector(BitcoinRpcClient rpcClient, MeterRegistry meterRegistry, BitcoinMetricsConfig config) {
        this.rpcClient = rpcClient;
        this.meterRegistry = meterRegistry;
        this.config = config;
        
        this.rpcCallTimer = Timer.builder("bitcoin.rpc.call.duration")
                .description("Bitcoin RPC call duration")
                .register(meterRegistry);
        
        this.metricsCollectionTimer = Timer.builder("bitcoin.metrics.collection.duration")
                .description("Bitcoin metrics collection duration")
                .register(meterRegistry);
    }
    
    @PostConstruct
    public void initializeMetrics() {
        logger.info("Initializing Bitcoin metrics...");
        
        // Register gauges for blockchain metrics
        Gauge.builder("bitcoin.blockchain.blocks", blockCount, AtomicLong::get)
                .description("Current block count")
                .register(meterRegistry);
        
        Gauge.builder("bitcoin.blockchain.verification_progress", verificationProgress, AtomicReference::get)
                .description("Blockchain verification progress")
                .register(meterRegistry);
        
        Gauge.builder("bitcoin.blockchain.difficulty", difficulty, AtomicReference::get)
                .description("Current mining difficulty")
                .register(meterRegistry);
        
        Gauge.builder("bitcoin.blockchain.synced", isSynced, ref -> ref.get() ? 1.0 : 0.0)
                .description("Blockchain sync status")
                .register(meterRegistry);
        
        // Register gauges for network metrics
        Gauge.builder("bitcoin.network.connections", connectionCount, AtomicLong::get)
                .description("Number of network connections")
                .register(meterRegistry);
        
        // Register gauges for mempool metrics
        Gauge.builder("bitcoin.mempool.size", mempoolSize, AtomicLong::get)
                .description("Mempool transaction count")
                .register(meterRegistry);
        
        Gauge.builder("bitcoin.mempool.bytes", mempoolBytes, AtomicLong::get)
                .description("Mempool size in bytes")
                .register(meterRegistry);
        
        Gauge.builder("bitcoin.mempool.total_fee", totalFee, AtomicReference::get)
                .description("Total fees in mempool")
                .register(meterRegistry);
        
        logger.info("Bitcoin metrics initialized successfully");
    }
    
    /**
     * Collect Bitcoin metrics on scheduled interval
     */
    @Scheduled(fixedDelayString = "#{@bitcoinMetricsConfig.metrics.collectionInterval}")
    public void collectMetrics() {
        Timer.Sample sample = Timer.start(meterRegistry);
        
        try {
            logger.debug("Starting Bitcoin metrics collection...");
            
            if (config.getMetrics().isEnableBlockchainMetrics()) {
                collectBlockchainMetrics();
            }
            
            if (config.getMetrics().isEnableNetworkMetrics()) {
                collectNetworkMetrics();
            }
            
            if (config.getMetrics().isEnableMempoolMetrics()) {
                collectMempoolMetrics();
            }
            
            logger.debug("Bitcoin metrics collection completed");
            
        } catch (Exception e) {
            logger.error("Error collecting Bitcoin metrics", e);
            meterRegistry.counter("bitcoin.metrics.collection.errors").increment();
        } finally {
            sample.stop(metricsCollectionTimer);
        }
    }
    
    /**
     * Collect blockchain-related metrics
     */
    private void collectBlockchainMetrics() {
        try {
            rpcClient.getBlockchainInfo()
                    .doOnNext(blockchainInfo -> {
                        Timer.Sample sample = Timer.start(meterRegistry);
                        try {
                            blockCount.set(blockchainInfo.getBlocks());
                            verificationProgress.set(blockchainInfo.getVerificationProgress());
                            difficulty.set(blockchainInfo.getDifficulty());
                            isSynced.set(!blockchainInfo.isInitialBlockDownload());
                            
                            // Additional blockchain metrics
                            meterRegistry.gauge("bitcoin.blockchain.headers", blockchainInfo.getHeaders());
                            meterRegistry.gauge("bitcoin.blockchain.size_on_disk_bytes", blockchainInfo.getSizeOnDisk());
                            meterRegistry.gauge("bitcoin.blockchain.time_offset", 
                                    System.currentTimeMillis() / 1000 - blockchainInfo.getTime());
                            
                            logger.info("Blockchain metrics updated - Blocks: {}, Progress: {}, Synced: {}", 
                                    blockchainInfo.getBlocks(), 
                                    blockchainInfo.getVerificationProgress(),
                                    !blockchainInfo.isInitialBlockDownload());
                            
                        } finally {
                            sample.stop(Timer.builder("bitcoin.rpc.getblockchaininfo.duration")
                                    .register(meterRegistry));
                        }
                    })
                    .doOnError(error -> {
                        logger.error("Failed to collect blockchain metrics", error);
                        meterRegistry.counter("bitcoin.rpc.getblockchaininfo.errors").increment();
                    })
                    .subscribe(
                            result -> logger.debug("Blockchain metrics collection completed"),
                            error -> logger.error("Blockchain metrics collection failed", error)
                    );
        } catch (Exception e) {
            logger.error("Error in collectBlockchainMetrics", e);
            meterRegistry.counter("bitcoin.rpc.getblockchaininfo.errors").increment();
        }
    }
    
    /**
     * Collect network-related metrics
     */
    private void collectNetworkMetrics() {
        try {
            rpcClient.getNetworkInfo()
                    .doOnNext(networkInfo -> {
                        Timer.Sample sample = Timer.start(meterRegistry);
                        try {
                            connectionCount.set(networkInfo.getConnections());
                            
                            // Additional network metrics
                            meterRegistry.gauge("bitcoin.network.version", networkInfo.getVersion());
                            meterRegistry.gauge("bitcoin.network.protocol_version", networkInfo.getProtocolversion());
                            meterRegistry.gauge("bitcoin.network.time_offset", networkInfo.getTimeoffset());
                            meterRegistry.gauge("bitcoin.network.relay_fee", networkInfo.getRelayfee());
                            meterRegistry.gauge("bitcoin.network.incremental_fee", networkInfo.getIncrementalfee());
                            
                            logger.info("Network metrics updated - Connections: {}", networkInfo.getConnections());
                            
                        } finally {
                            sample.stop(Timer.builder("bitcoin.rpc.getnetworkinfo.duration")
                                    .register(meterRegistry));
                        }
                    })
                    .doOnError(error -> {
                        logger.error("Failed to collect network metrics", error);
                        meterRegistry.counter("bitcoin.rpc.getnetworkinfo.errors").increment();
                    })
                    .subscribe(
                            result -> logger.debug("Network metrics collection completed"),
                            error -> logger.error("Network metrics collection failed", error)
                    );
        } catch (Exception e) {
            logger.error("Error in collectNetworkMetrics", e);
            meterRegistry.counter("bitcoin.rpc.getnetworkinfo.errors").increment();
        }
    }
    
    /**
     * Collect mempool-related metrics
     */
    private void collectMempoolMetrics() {
        try {
            rpcClient.getMempoolInfo()
                    .doOnNext(mempoolInfo -> {
                        Timer.Sample sample = Timer.start(meterRegistry);
                        try {
                            mempoolSize.set(mempoolInfo.getSize());
                            mempoolBytes.set(mempoolInfo.getBytes());
                            totalFee.set(mempoolInfo.getTotalFee());
                            
                            // Additional mempool metrics
                            meterRegistry.gauge("bitcoin.mempool.usage_bytes", mempoolInfo.getUsage());
                            meterRegistry.gauge("bitcoin.mempool.max_size_bytes", mempoolInfo.getMaxmempool());
                            meterRegistry.gauge("bitcoin.mempool.min_fee", mempoolInfo.getMempoolminfee());
                            meterRegistry.gauge("bitcoin.mempool.min_relay_fee", mempoolInfo.getMinrelaytxfee());
                            meterRegistry.gauge("bitcoin.mempool.unbroadcast_count", mempoolInfo.getUnbroadcastcount());
                            
                            logger.info("Mempool metrics updated - Size: {}, Bytes: {}, Total Fee: {}", 
                                    mempoolInfo.getSize(), 
                                    mempoolInfo.getBytes(),
                                    mempoolInfo.getTotalFee());
                            
                        } finally {
                            sample.stop(Timer.builder("bitcoin.rpc.getmempoolinfo.duration")
                                    .register(meterRegistry));
                        }
                    })
                    .doOnError(error -> {
                        logger.error("Failed to collect mempool metrics", error);
                        meterRegistry.counter("bitcoin.rpc.getmempoolinfo.errors").increment();
                    })
                    .subscribe(
                            result -> logger.debug("Mempool metrics collection completed"),
                            error -> logger.error("Mempool metrics collection failed", error)
                    );
        } catch (Exception e) {
            logger.error("Error in collectMempoolMetrics", e);
            meterRegistry.counter("bitcoin.rpc.getmempoolinfo.errors").increment();
        }
    }
}
