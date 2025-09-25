package com.defimon.multichain.plugin.context;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Map;

/**
 * Context object passed to plugins during initialization.
 * 
 * This record contains all the shared resources and services that plugins
 * need to function, including database connections, message queues, and
 * monitoring infrastructure.
 * 
 * @param mongoTemplate MongoDB template for document operations
 * @param kafkaTemplate Kafka template for event streaming
 * @param redisTemplate Redis template for caching and session management
 * @param meterRegistry Metrics registry for monitoring
 * @param config Shared configuration properties
 */
public record PluginContext(
    MongoTemplate mongoTemplate,
    KafkaTemplate<String, Object> kafkaTemplate,
    RedisTemplate<String, Object> redisTemplate,
    MeterRegistry meterRegistry,
    Map<String, Object> config
) {
    
    /**
     * Creates a new PluginContext with the provided components.
     * 
     * @param mongoTemplate MongoDB template
     * @param kafkaTemplate Kafka template
     * @param redisTemplate Redis template
     * @param meterRegistry Metrics registry
     * @param config Configuration map
     * @return new PluginContext instance
     */
    public static PluginContext of(
            MongoTemplate mongoTemplate,
            KafkaTemplate<String, Object> kafkaTemplate,
            RedisTemplate<String, Object> redisTemplate,
            MeterRegistry meterRegistry,
            Map<String, Object> config) {
        return new PluginContext(mongoTemplate, kafkaTemplate, redisTemplate, meterRegistry, config);
    }
    
    /**
     * Gets a configuration value by key.
     * 
     * @param key the configuration key
     * @param type the expected type
     * @param <T> the type parameter
     * @return the configuration value or null if not found
     */
    @SuppressWarnings("unchecked")
    public <T> T getConfigValue(String key, Class<T> type) {
        Object value = config.get(key);
        if (value != null && type.isAssignableFrom(value.getClass())) {
            return (T) value;
        }
        return null;
    }
    
    /**
     * Gets a configuration value by key with a default value.
     * 
     * @param key the configuration key
     * @param defaultValue the default value to return if key not found
     * @param <T> the type parameter
     * @return the configuration value or default value
     */
    @SuppressWarnings("unchecked")
    public <T> T getConfigValue(String key, T defaultValue) {
        Object value = config.get(key);
        if (value != null) {
            return (T) value;
        }
        return defaultValue;
    }
}
