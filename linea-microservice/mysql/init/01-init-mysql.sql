-- MySQL Initialization Script for Linea Configuration Database
-- This script creates the necessary tables for configuration and metadata

-- Create the Linea configuration database
CREATE DATABASE IF NOT EXISTS linea_config CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Use the configuration database
USE linea_config;

-- Create user for the application
CREATE USER IF NOT EXISTS 'linea_user'@'%' IDENTIFIED BY 'linea123';
GRANT ALL PRIVILEGES ON linea_config.* TO 'linea_user'@'%';
FLUSH PRIVILEGES;

-- Configuration table for service settings
CREATE TABLE IF NOT EXISTS service_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    service_name VARCHAR(100) NOT NULL,
    config_key VARCHAR(200) NOT NULL,
    config_value TEXT,
    config_type ENUM('string', 'number', 'boolean', 'json') DEFAULT 'string',
    description TEXT,
    is_encrypted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_service_config (service_name, config_key),
    INDEX idx_service_name (service_name),
    INDEX idx_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Service status and health monitoring
CREATE TABLE IF NOT EXISTS service_status (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    service_name VARCHAR(100) NOT NULL,
    status ENUM('running', 'stopped', 'error', 'starting', 'stopping') NOT NULL,
    health_score INT DEFAULT 100 CHECK (health_score >= 0 AND health_score <= 100),
    last_heartbeat TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    error_message TEXT,
    metadata JSON,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_service_name (service_name),
    INDEX idx_status (status),
    INDEX idx_last_heartbeat (last_heartbeat)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Sync status tracking
CREATE TABLE IF NOT EXISTS sync_status (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    chain_name VARCHAR(50) NOT NULL,
    sync_type ENUM('realtime', 'archive', 'historical') NOT NULL,
    current_block BIGINT DEFAULT 0,
    target_block BIGINT DEFAULT 0,
    sync_percentage DECIMAL(5,2) DEFAULT 0.00,
    sync_status ENUM('syncing', 'completed', 'paused', 'error') NOT NULL,
    start_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_update TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    error_message TEXT,
    metadata JSON,
    UNIQUE KEY uk_chain_sync (chain_name, sync_type),
    INDEX idx_chain_name (chain_name),
    INDEX idx_sync_status (sync_status),
    INDEX idx_last_update (last_update)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Data collection jobs tracking
CREATE TABLE IF NOT EXISTS collection_jobs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_name VARCHAR(200) NOT NULL,
    job_type ENUM('block_collection', 'transaction_collection', 'account_collection', 'metrics_collection') NOT NULL,
    status ENUM('pending', 'running', 'completed', 'failed', 'cancelled') NOT NULL,
    start_block BIGINT DEFAULT 0,
    end_block BIGINT DEFAULT 0,
    current_block BIGINT DEFAULT 0,
    total_blocks BIGINT DEFAULT 0,
    processed_blocks BIGINT DEFAULT 0,
    progress_percentage DECIMAL(5,2) DEFAULT 0.00,
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL,
    error_message TEXT,
    metadata JSON,
    INDEX idx_job_name (job_name),
    INDEX idx_job_type (job_type),
    INDEX idx_status (status),
    INDEX idx_started_at (started_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- API keys and authentication
CREATE TABLE IF NOT EXISTS api_keys (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    key_name VARCHAR(200) NOT NULL,
    key_value VARCHAR(500) NOT NULL,
    key_type ENUM('rpc', 'api', 'webhook', 'database') NOT NULL,
    service_name VARCHAR(100) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    expires_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_key_value (key_value),
    INDEX idx_key_name (key_name),
    INDEX idx_service_name (service_name),
    INDEX idx_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- User management (for admin access)
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(200) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role ENUM('admin', 'operator', 'viewer') DEFAULT 'viewer',
    is_active BOOLEAN DEFAULT TRUE,
    last_login TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_email (email),
    INDEX idx_role (role),
    INDEX idx_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Audit log for tracking changes
CREATE TABLE IF NOT EXISTS audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    table_name VARCHAR(100) NOT NULL,
    record_id BIGINT NOT NULL,
    action ENUM('INSERT', 'UPDATE', 'DELETE') NOT NULL,
    old_values JSON,
    new_values JSON,
    changed_by VARCHAR(100),
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_table_name (table_name),
    INDEX idx_record_id (record_id),
    INDEX idx_action (action),
    INDEX idx_changed_at (changed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Insert default configuration
INSERT INTO service_config (service_name, config_key, config_value, config_type, description) VALUES
('linea-microservice', 'rpc_url', 'https://dry-special-card.linea-mainnet.quiknode.pro/your_quicknode_linea_key_here/', 'string', 'Linea RPC endpoint URL'),
('linea-microservice', 'max_concurrent_workers', '20', 'number', 'Maximum number of concurrent workers'),
('linea-microservice', 'rate_limit_per_second', '200', 'number', 'Rate limit for RPC requests per second'),
('linea-microservice', 'batch_size', '100', 'number', 'Batch size for data processing'),
('linea-microservice', 'polling_interval_ms', '5000', 'number', 'Polling interval in milliseconds'),
('linea-microservice', 'max_retries', '10', 'number', 'Maximum number of retry attempts'),
('linea-microservice', 'retry_delay_ms', '1000', 'number', 'Delay between retry attempts in milliseconds'),
('linea-microservice', 'archive_start_block', '10000', 'number', 'Starting block for archive data collection'),
('linea-microservice', 'archive_batch_size', '100', 'number', 'Batch size for archive data collection'),
('linea-microservice', 'metrics_push_interval_ms', '60000', 'number', 'Interval for pushing metrics to external systems'),
('linea-microservice', 'database_path', '/mnt/sata18tb/linea_data.db', 'string', 'Path to the main database file'),
('linea-microservice', 'archive_database_path', '/mnt/sata18tb/linea_archive_data.db', 'string', 'Path to the archive database file'),
('linea-microservice', 'service_name', 'linea-microservice', 'string', 'Name of the service'),
('linea-microservice', 'service_version', 'v1.0.0', 'string', 'Version of the service'),
('linea-microservice', 'network', 'mainnet', 'string', 'Network name (mainnet, testnet, etc.)'),
('mongodb', 'connection_string', 'mongodb://mongodb:27017/linea_blockchain', 'string', 'MongoDB connection string'),
('timescaledb', 'connection_string', 'jdbc:postgresql://timescaledb:5432/linea_metrics', 'string', 'TimescaleDB connection string'),
('redis', 'connection_string', 'redis://redis:6379', 'string', 'Redis connection string'),
('mysql', 'connection_string', 'jdbc:mysql://mysql:3306/linea_config', 'string', 'MySQL connection string');

-- Insert default service status
INSERT INTO service_status (service_name, status, health_score, last_heartbeat) VALUES
('linea-microservice', 'starting', 100, NOW()),
('mongodb', 'starting', 100, NOW()),
('timescaledb', 'starting', 100, NOW()),
('redis', 'starting', 100, NOW()),
('mysql', 'starting', 100, NOW()),
('prometheus', 'starting', 100, NOW()),
('grafana', 'starting', 100, NOW());

-- Insert default sync status
INSERT INTO sync_status (chain_name, sync_type, current_block, target_block, sync_percentage, sync_status) VALUES
('linea', 'realtime', 0, 0, 0.00, 'syncing'),
('linea', 'archive', 0, 0, 0.00, 'pending'),
('linea', 'historical', 0, 0, 0.00, 'pending');

-- Insert default admin user (password: admin123)
INSERT INTO users (username, email, password_hash, role, is_active) VALUES
('admin', 'admin@linea.local', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iKyVqU66xS3C3p5qJz7qJz7qJz7q', 'admin', TRUE);

-- Insert default API keys
INSERT INTO api_keys (key_name, key_value, key_type, service_name, is_active) VALUES
('linea_rpc_key', 'your_quicknode_linea_key_here', 'rpc', 'linea-microservice', TRUE),
('mongodb_admin', 'admin_password', 'database', 'mongodb', TRUE),
('timescaledb_admin', 'linea_password', 'database', 'timescaledb', TRUE),
('redis_admin', 'redis_password', 'database', 'redis', TRUE),
('mysql_admin', 'linea_password', 'database', 'mysql', TRUE);

-- Create triggers for audit logging
DELIMITER //

CREATE TRIGGER tr_service_config_audit_insert
AFTER INSERT ON service_config
FOR EACH ROW
BEGIN
    INSERT INTO audit_log (table_name, record_id, action, new_values, changed_by, changed_at)
    VALUES ('service_config', NEW.id, 'INSERT', JSON_OBJECT('service_name', NEW.service_name, 'config_key', NEW.config_key, 'config_value', NEW.config_value), USER(), NOW());
END//

CREATE TRIGGER tr_service_config_audit_update
AFTER UPDATE ON service_config
FOR EACH ROW
BEGIN
    INSERT INTO audit_log (table_name, record_id, action, old_values, new_values, changed_by, changed_at)
    VALUES ('service_config', NEW.id, 'UPDATE', 
        JSON_OBJECT('service_name', OLD.service_name, 'config_key', OLD.config_key, 'config_value', OLD.config_value),
        JSON_OBJECT('service_name', NEW.service_name, 'config_key', NEW.config_key, 'config_value', NEW.config_value),
        USER(), NOW());
END//

CREATE TRIGGER tr_service_config_audit_delete
AFTER DELETE ON service_config
FOR EACH ROW
BEGIN
    INSERT INTO audit_log (table_name, record_id, action, old_values, changed_by, changed_at)
    VALUES ('service_config', OLD.id, 'DELETE', 
        JSON_OBJECT('service_name', OLD.service_name, 'config_key', OLD.config_key, 'config_value', OLD.config_value),
        USER(), NOW());
END//

DELIMITER ;

-- Create stored procedures for common operations
DELIMITER //

CREATE PROCEDURE UpdateServiceStatus(
    IN p_service_name VARCHAR(100),
    IN p_status ENUM('running', 'stopped', 'error', 'starting', 'stopping'),
    IN p_health_score INT,
    IN p_error_message TEXT
)
BEGIN
    INSERT INTO service_status (service_name, status, health_score, error_message, last_heartbeat)
    VALUES (p_service_name, p_status, p_health_score, p_error_message, NOW())
    ON DUPLICATE KEY UPDATE
        status = p_status,
        health_score = p_health_score,
        error_message = p_error_message,
        last_heartbeat = NOW(),
        updated_at = NOW();
END//

CREATE PROCEDURE UpdateSyncStatus(
    IN p_chain_name VARCHAR(50),
    IN p_sync_type ENUM('realtime', 'archive', 'historical'),
    IN p_current_block BIGINT,
    IN p_target_block BIGINT,
    IN p_sync_status ENUM('syncing', 'completed', 'paused', 'error'),
    IN p_error_message TEXT
)
BEGIN
    DECLARE v_sync_percentage DECIMAL(5,2);
    
    IF p_target_block > 0 THEN
        SET v_sync_percentage = (p_current_block / p_target_block) * 100;
    ELSE
        SET v_sync_percentage = 0.00;
    END IF;
    
    INSERT INTO sync_status (chain_name, sync_type, current_block, target_block, sync_percentage, sync_status, error_message)
    VALUES (p_chain_name, p_sync_type, p_current_block, p_target_block, v_sync_percentage, p_sync_status, p_error_message)
    ON DUPLICATE KEY UPDATE
        current_block = p_current_block,
        target_block = p_target_block,
        sync_percentage = v_sync_percentage,
        sync_status = p_sync_status,
        error_message = p_error_message,
        last_update = NOW();
END//

CREATE PROCEDURE GetServiceHealth()
BEGIN
    SELECT 
        service_name,
        status,
        health_score,
        last_heartbeat,
        CASE 
            WHEN last_heartbeat < NOW() - INTERVAL 5 MINUTE THEN 'stale'
            WHEN health_score < 50 THEN 'unhealthy'
            WHEN health_score < 80 THEN 'warning'
            ELSE 'healthy'
        END as health_status
    FROM service_status
    ORDER BY service_name;
END//

DELIMITER ;

-- Grant execute permissions on stored procedures
GRANT EXECUTE ON PROCEDURE UpdateServiceStatus TO 'linea_user'@'%';
GRANT EXECUTE ON PROCEDURE UpdateSyncStatus TO 'linea_user'@'%';
GRANT EXECUTE ON PROCEDURE GetServiceHealth TO 'linea_user'@'%';

-- Final message
SELECT 'MySQL initialization completed successfully!' as message;
SELECT 'Tables created: service_config, service_status, sync_status, collection_jobs, api_keys, users, audit_log' as tables_created;
SELECT 'Stored procedures created: UpdateServiceStatus, UpdateSyncStatus, GetServiceHealth' as procedures_created;
SELECT 'Default configuration and admin user created' as initialization_complete;
