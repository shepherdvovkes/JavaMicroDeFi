package com.defimon.multichain.manager;

import com.defimon.multichain.model.ChainType;
import com.defimon.multichain.plugin.BlockchainPlugin;
import com.defimon.multichain.plugin.PluginConfiguration;
import com.defimon.multichain.plugin.context.PluginContext;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Manages the lifecycle and operations of blockchain plugins.
 * 
 * The PluginManager is responsible for:
 * - Loading and initializing plugins
 * - Managing plugin lifecycle (start/stop)
 * - Providing plugin discovery and access
 * - Monitoring plugin health and performance
 * - Handling plugin configuration
 */
@Service
public class PluginManager {
    
    private static final Logger logger = LoggerFactory.getLogger(PluginManager.class);
    
    private final Map<String, BlockchainPlugin<?>> plugins = new ConcurrentHashMap<>();
    private final Map<String, PluginConfiguration> pluginConfigs = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();
    
    private final MongoTemplate mongoTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final MeterRegistry meterRegistry;
    
    // Metrics
    private final Counter pluginLoadCounter;
    private final Counter pluginStartCounter;
    private final Counter pluginStopCounter;
    private final Counter pluginErrorCounter;
    
    @Autowired
    public PluginManager(
            MongoTemplate mongoTemplate,
            KafkaTemplate<String, Object> kafkaTemplate,
            RedisTemplate<String, Object> redisTemplate,
            MeterRegistry meterRegistry) {
        this.mongoTemplate = mongoTemplate;
        this.kafkaTemplate = kafkaTemplate;
        this.redisTemplate = redisTemplate;
        this.meterRegistry = meterRegistry;
        
        // Initialize metrics
        this.pluginLoadCounter = Counter.builder("multichain.plugins.loaded")
                .description("Number of plugins loaded")
                .register(meterRegistry);
        
        this.pluginStartCounter = Counter.builder("multichain.plugins.started")
                .description("Number of plugins started")
                .register(meterRegistry);
        
        this.pluginStopCounter = Counter.builder("multichain.plugins.stopped")
                .description("Number of plugins stopped")
                .register(meterRegistry);
        
        this.pluginErrorCounter = Counter.builder("multichain.plugins.errors")
                .description("Number of plugin errors")
                .register(meterRegistry);
        
        // Register plugin count gauge
        Gauge.builder("multichain.plugins.count")
                .description("Total number of loaded plugins")
                .register(meterRegistry, this, PluginManager::getPluginCount);
    }
    
    /**
     * Initializes all plugins after the application is ready.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initializePlugins() {
        logger.info("Initializing Multi-Chain Service plugins...");
        
        try {
            // Auto-discover and load plugins
            loadPlugin("ethereum", createEthereumPlugin());
            loadPlugin("bitcoin", createBitcoinPlugin());
            loadPlugin("polygon", createPolygonPlugin());
            
            // Start all enabled plugins
            startEnabledPlugins();
            
            logger.info("Successfully initialized {} plugins", plugins.size());
            
        } catch (Exception e) {
            logger.error("Failed to initialize plugins", e);
            pluginErrorCounter.increment();
            throw new RuntimeException("Plugin initialization failed", e);
        }
    }
    
    /**
     * Loads a plugin with the specified chain ID.
     * 
     * @param chainId the chain ID
     * @param plugin the plugin instance
     */
    public void loadPlugin(String chainId, BlockchainPlugin<?> plugin) {
        try {
            logger.info("Loading plugin for chain: {}", chainId);
            
            // Create plugin context
            PluginContext context = PluginContext.of(
                mongoTemplate, kafkaTemplate, redisTemplate, meterRegistry, Map.of()
            );
            
            // Get plugin configuration
            PluginConfiguration config = getPluginConfiguration(chainId);
            
            // Initialize the plugin
            plugin.initialize(context, config);
            
            // Store the plugin
            plugins.put(chainId, plugin);
            pluginConfigs.put(chainId, config);
            
            pluginLoadCounter.increment("chain", chainId);
            logger.info("Successfully loaded plugin for chain: {}", chainId);
            
        } catch (Exception e) {
            logger.error("Failed to load plugin for chain: {}", chainId, e);
            pluginErrorCounter.increment("chain", chainId, "error", "load");
            throw new RuntimeException("Failed to load plugin for chain: " + chainId, e);
        }
    }
    
    /**
     * Gets a plugin by chain ID.
     * 
     * @param chainId the chain ID
     * @return the plugin instance
     */
    public BlockchainPlugin<?> getPlugin(String chainId) {
        return plugins.get(chainId);
    }
    
    /**
     * Gets all loaded plugins.
     * 
     * @return list of all plugins
     */
    public List<BlockchainPlugin<?>> getAllPlugins() {
        return List.copyOf(plugins.values());
    }
    
    /**
     * Gets all enabled plugins.
     * 
     * @return list of enabled plugins
     */
    public List<BlockchainPlugin<?>> getEnabledPlugins() {
        return plugins.values().stream()
                .filter(plugin -> plugin.getConfiguration().getEnabled())
                .collect(Collectors.toList());
    }
    
    /**
     * Gets plugins by chain type.
     * 
     * @param chainType the chain type
     * @return list of plugins of the specified type
     */
    public List<BlockchainPlugin<?>> getPluginsByType(ChainType chainType) {
        return plugins.values().stream()
                .filter(plugin -> plugin.getChainType() == chainType)
                .collect(Collectors.toList());
    }
    
    /**
     * Enables a plugin.
     * 
     * @param chainId the chain ID
     */
    public void enablePlugin(String chainId) {
        BlockchainPlugin<?> plugin = plugins.get(chainId);
        if (plugin != null) {
            try {
                plugin.start();
                plugin.getConfiguration().setEnabled(true);
                pluginStartCounter.increment("chain", chainId);
                logger.info("Enabled plugin for chain: {}", chainId);
            } catch (Exception e) {
                logger.error("Failed to enable plugin for chain: {}", chainId, e);
                pluginErrorCounter.increment("chain", chainId, "error", "start");
                throw new RuntimeException("Failed to enable plugin for chain: " + chainId, e);
            }
        } else {
            throw new IllegalArgumentException("Plugin not found for chain: " + chainId);
        }
    }
    
    /**
     * Disables a plugin.
     * 
     * @param chainId the chain ID
     */
    public void disablePlugin(String chainId) {
        BlockchainPlugin<?> plugin = plugins.get(chainId);
        if (plugin != null) {
            try {
                plugin.stop();
                plugin.getConfiguration().setEnabled(false);
                pluginStopCounter.increment("chain", chainId);
                logger.info("Disabled plugin for chain: {}", chainId);
            } catch (Exception e) {
                logger.error("Failed to disable plugin for chain: {}", chainId, e);
                pluginErrorCounter.increment("chain", chainId, "error", "stop");
                throw new RuntimeException("Failed to disable plugin for chain: " + chainId, e);
            }
        } else {
            throw new IllegalArgumentException("Plugin not found for chain: " + chainId);
        }
    }
    
    /**
     * Checks if a plugin is enabled.
     * 
     * @param chainId the chain ID
     * @return true if the plugin is enabled
     */
    public boolean isPluginEnabled(String chainId) {
        BlockchainPlugin<?> plugin = plugins.get(chainId);
        return plugin != null && plugin.getConfiguration().getEnabled();
    }
    
    /**
     * Gets the health status of all plugins.
     * 
     * @return map of chain ID to health status
     */
    public Map<String, Boolean> getPluginHealthStatus() {
        return plugins.entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> entry.getValue().isHealthy()
                ));
    }
    
    /**
     * Gets plugin information for all loaded plugins.
     * 
     * @return list of plugin information
     */
    public List<PluginInfo> getPluginInfo() {
        return plugins.entrySet().stream()
                .map(entry -> {
                    BlockchainPlugin<?> plugin = entry.getValue();
                    PluginConfiguration config = plugin.getConfiguration();
                    
                    return new PluginInfo(
                        entry.getKey(),
                        plugin.getChainName(),
                        plugin.getChainType(),
                        plugin.getVersion(),
                        config.getEnabled(),
                        plugin.isHealthy(),
                        config.getRpcUrl(),
                        config.getTechnology()
                    );
                })
                .collect(Collectors.toList());
    }
    
    /**
     * Starts all enabled plugins.
     */
    private void startEnabledPlugins() {
        getEnabledPlugins().forEach(plugin -> {
            try {
                plugin.start();
                pluginStartCounter.increment("chain", plugin.getChainId());
                logger.info("Started plugin for chain: {}", plugin.getChainId());
            } catch (Exception e) {
                logger.error("Failed to start plugin for chain: {}", plugin.getChainId(), e);
                pluginErrorCounter.increment("chain", plugin.getChainId(), "error", "start");
            }
        });
    }
    
    /**
     * Gets the plugin count for metrics.
     * 
     * @return the number of loaded plugins
     */
    private int getPluginCount() {
        return plugins.size();
    }
    
    /**
     * Creates an Ethereum plugin instance.
     * 
     * @return the Ethereum plugin
     */
    private BlockchainPlugin<?> createEthereumPlugin() {
        // This will be implemented in the Ethereum plugin class
        return new com.defimon.multichain.plugin.impl.EthereumPlugin();
    }
    
    /**
     * Creates a Bitcoin plugin instance.
     * 
     * @return the Bitcoin plugin
     */
    private BlockchainPlugin<?> createBitcoinPlugin() {
        // This will be implemented in the Bitcoin plugin class
        return new com.defimon.multichain.plugin.impl.BitcoinPlugin();
    }
    
    /**
     * Creates a Polygon plugin instance.
     * 
     * @return the Polygon plugin
     */
    private BlockchainPlugin<?> createPolygonPlugin() {
        // This will be implemented in the Polygon plugin class
        return new com.defimon.multichain.plugin.impl.PolygonPlugin();
    }
    
    /**
     * Gets plugin configuration by chain ID.
     * 
     * @param chainId the chain ID
     * @return the plugin configuration
     */
    private PluginConfiguration getPluginConfiguration(String chainId) {
        // This would typically load from application.yml or database
        // For now, return default configurations
        return switch (chainId) {
            case "ethereum" -> createEthereumConfiguration();
            case "bitcoin" -> createBitcoinConfiguration();
            case "polygon" -> createPolygonConfiguration();
            default -> throw new IllegalArgumentException("Unknown chain ID: " + chainId);
        };
    }
    
    /**
     * Creates Ethereum plugin configuration.
     * 
     * @return Ethereum configuration
     */
    private PluginConfiguration createEthereumConfiguration() {
        PluginConfiguration config = new PluginConfiguration() {};
        config.setChainId("1");
        config.setChainName("Ethereum");
        config.setEnabled(true);
        config.setRpcUrl("https://mainnet.infura.io/v3/your-key");
        config.setBlockTime(12000L);
        config.setSyncStrategy("realtime");
        config.setTechnology("rust-java-hybrid");
        return config;
    }
    
    /**
     * Creates Bitcoin plugin configuration.
     * 
     * @return Bitcoin configuration
     */
    private PluginConfiguration createBitcoinConfiguration() {
        PluginConfiguration config = new PluginConfiguration() {};
        config.setChainId("bitcoin");
        config.setChainName("Bitcoin");
        config.setEnabled(true);
        config.setRpcUrl("http://localhost:8332");
        config.setBlockTime(600000L);
        config.setSyncStrategy("batch");
        config.setTechnology("java21");
        return config;
    }
    
    /**
     * Creates Polygon plugin configuration.
     * 
     * @return Polygon configuration
     */
    private PluginConfiguration createPolygonConfiguration() {
        PluginConfiguration config = new PluginConfiguration() {};
        config.setChainId("137");
        config.setChainName("Polygon");
        config.setEnabled(true);
        config.setRpcUrl("https://polygon-rpc.com");
        config.setBlockTime(2000L);
        config.setSyncStrategy("realtime");
        config.setTechnology("java21");
        return config;
    }
    
    /**
     * Record for plugin information.
     * 
     * @param chainId the chain ID
     * @param chainName the chain name
     * @param chainType the chain type
     * @param version the plugin version
     * @param enabled whether the plugin is enabled
     * @param healthy whether the plugin is healthy
     * @param rpcUrl the RPC URL
     * @param technology the technology used
     */
    public record PluginInfo(
        String chainId,
        String chainName,
        ChainType chainType,
        String version,
        Boolean enabled,
        Boolean healthy,
        String rpcUrl,
        String technology
    ) {}
}
