-- TimescaleDB Initialization Script for Linea Metrics
-- This script creates the necessary tables and hypertables for time-series data

-- Create the Linea metrics database
CREATE DATABASE linea_metrics;

-- Connect to the metrics database
\c linea_metrics;

-- Create extensions
CREATE EXTENSION IF NOT EXISTS timescaledb;
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;

-- Create user for the application
CREATE USER linea_user WITH PASSWORD 'linea_password';
GRANT ALL PRIVILEGES ON DATABASE linea_metrics TO linea_user;

-- System metrics table
CREATE TABLE system_metrics (
    time TIMESTAMPTZ NOT NULL,
    service_name TEXT NOT NULL,
    metric_name TEXT NOT NULL,
    metric_value DOUBLE PRECISION NOT NULL,
    tags JSONB DEFAULT '{}',
    PRIMARY KEY (time, service_name, metric_name)
);

-- Convert to hypertable
SELECT create_hypertable('system_metrics', 'time', chunk_time_interval => INTERVAL '1 hour');

-- Performance metrics table
CREATE TABLE performance_metrics (
    time TIMESTAMPTZ NOT NULL,
    service_name TEXT NOT NULL,
    operation_type TEXT NOT NULL,
    duration_ms DOUBLE PRECISION NOT NULL,
    success BOOLEAN NOT NULL,
    error_message TEXT,
    metadata JSONB DEFAULT '{}',
    PRIMARY KEY (time, service_name, operation_type)
);

-- Convert to hypertable
SELECT create_hypertable('performance_metrics', 'time', chunk_time_interval => INTERVAL '1 hour');

-- Blockchain metrics table
CREATE TABLE blockchain_metrics (
    time TIMESTAMPTZ NOT NULL,
    chain_name TEXT NOT NULL,
    block_number BIGINT NOT NULL,
    tps DOUBLE PRECISION NOT NULL,
    gas_utilization DOUBLE PRECISION NOT NULL,
    gas_price_gwei DOUBLE PRECISION NOT NULL,
    active_addresses BIGINT NOT NULL,
    total_transactions BIGINT NOT NULL,
    average_block_time DOUBLE PRECISION NOT NULL,
    network_hashrate TEXT,
    difficulty TEXT,
    PRIMARY KEY (time, chain_name, block_number)
);

-- Convert to hypertable
SELECT create_hypertable('blockchain_metrics', 'time', chunk_time_interval => INTERVAL '1 hour');

-- Error metrics table
CREATE TABLE error_metrics (
    time TIMESTAMPTZ NOT NULL,
    service_name TEXT NOT NULL,
    error_type TEXT NOT NULL,
    error_count BIGINT NOT NULL,
    error_message TEXT,
    stack_trace TEXT,
    metadata JSONB DEFAULT '{}',
    PRIMARY KEY (time, service_name, error_type)
);

-- Convert to hypertable
SELECT create_hypertable('error_metrics', 'time', chunk_time_interval => INTERVAL '1 hour');

-- Create indexes for better performance
CREATE INDEX idx_system_metrics_service_time ON system_metrics (service_name, time DESC);
CREATE INDEX idx_system_metrics_metric_time ON system_metrics (metric_name, time DESC);
CREATE INDEX idx_performance_metrics_service_time ON performance_metrics (service_name, time DESC);
CREATE INDEX idx_performance_metrics_operation_time ON performance_metrics (operation_type, time DESC);
CREATE INDEX idx_blockchain_metrics_chain_time ON blockchain_metrics (chain_name, time DESC);
CREATE INDEX idx_blockchain_metrics_block_time ON blockchain_metrics (block_number, time DESC);
CREATE INDEX idx_error_metrics_service_time ON error_metrics (service_name, time DESC);
CREATE INDEX idx_error_metrics_type_time ON error_metrics (error_type, time DESC);

-- Create continuous aggregates for real-time analytics
CREATE MATERIALIZED VIEW system_metrics_hourly
WITH (timescaledb.continuous) AS
SELECT 
    time_bucket('1 hour', time) AS hour,
    service_name,
    metric_name,
    AVG(metric_value) AS avg_value,
    MIN(metric_value) AS min_value,
    MAX(metric_value) AS max_value,
    COUNT(*) AS sample_count
FROM system_metrics
GROUP BY hour, service_name, metric_name;

-- Create continuous aggregate for performance metrics
CREATE MATERIALIZED VIEW performance_metrics_hourly
WITH (timescaledb.continuous) AS
SELECT 
    time_bucket('1 hour', time) AS hour,
    service_name,
    operation_type,
    AVG(duration_ms) AS avg_duration,
    MIN(duration_ms) AS min_duration,
    MAX(duration_ms) AS max_duration,
    COUNT(*) AS operation_count,
    COUNT(*) FILTER (WHERE success = true) AS success_count,
    COUNT(*) FILTER (WHERE success = false) AS error_count
FROM performance_metrics
GROUP BY hour, service_name, operation_type;

-- Create continuous aggregate for blockchain metrics
CREATE MATERIALIZED VIEW blockchain_metrics_hourly
WITH (timescaledb.continuous) AS
SELECT 
    time_bucket('1 hour', time) AS hour,
    chain_name,
    AVG(tps) AS avg_tps,
    AVG(gas_utilization) AS avg_gas_utilization,
    AVG(gas_price_gwei) AS avg_gas_price,
    AVG(active_addresses) AS avg_active_addresses,
    SUM(total_transactions) AS total_transactions,
    AVG(average_block_time) AS avg_block_time
FROM blockchain_metrics
GROUP BY hour, chain_name;

-- Set up data retention policies
SELECT add_retention_policy('system_metrics', INTERVAL '30 days');
SELECT add_retention_policy('performance_metrics', INTERVAL '30 days');
SELECT add_retention_policy('blockchain_metrics', INTERVAL '90 days');
SELECT add_retention_policy('error_metrics', INTERVAL '30 days');

-- Grant permissions to the application user
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO linea_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO linea_user;
GRANT ALL PRIVILEGES ON ALL FUNCTIONS IN SCHEMA public TO linea_user;

-- Create a function to get real-time metrics
CREATE OR REPLACE FUNCTION get_realtime_metrics(service_name_param TEXT DEFAULT NULL)
RETURNS TABLE (
    metric_name TEXT,
    current_value DOUBLE PRECISION,
    avg_value_1h DOUBLE PRECISION,
    avg_value_24h DOUBLE PRECISION
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        sm.metric_name,
        sm.metric_value as current_value,
        COALESCE(hourly.avg_value, 0) as avg_value_1h,
        COALESCE(daily.avg_value, 0) as avg_value_24h
    FROM system_metrics sm
    LEFT JOIN system_metrics_hourly hourly ON sm.service_name = hourly.service_name 
        AND sm.metric_name = hourly.metric_name 
        AND hourly.hour = date_trunc('hour', NOW())
    LEFT JOIN (
        SELECT 
            service_name,
            metric_name,
            AVG(avg_value) as avg_value
        FROM system_metrics_hourly
        WHERE hour >= NOW() - INTERVAL '24 hours'
        GROUP BY service_name, metric_name
    ) daily ON sm.service_name = daily.service_name 
        AND sm.metric_name = daily.metric_name
    WHERE sm.time >= NOW() - INTERVAL '5 minutes'
        AND (service_name_param IS NULL OR sm.service_name = service_name_param)
    ORDER BY sm.metric_name;
END;
$$ LANGUAGE plpgsql;

-- Grant execute permission on the function
GRANT EXECUTE ON FUNCTION get_realtime_metrics(TEXT) TO linea_user;

-- Create a function to get blockchain statistics
CREATE OR REPLACE FUNCTION get_blockchain_stats(chain_name_param TEXT DEFAULT 'linea')
RETURNS TABLE (
    current_block BIGINT,
    avg_tps_1h DOUBLE PRECISION,
    avg_tps_24h DOUBLE PRECISION,
    avg_gas_utilization_1h DOUBLE PRECISION,
    avg_gas_utilization_24h DOUBLE PRECISION,
    total_transactions_24h BIGINT
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        bm.block_number as current_block,
        COALESCE(hourly.avg_tps, 0) as avg_tps_1h,
        COALESCE(daily.avg_tps, 0) as avg_tps_24h,
        COALESCE(hourly.avg_gas_utilization, 0) as avg_gas_utilization_1h,
        COALESCE(daily.avg_gas_utilization, 0) as avg_gas_utilization_24h,
        COALESCE(daily.total_transactions, 0) as total_transactions_24h
    FROM blockchain_metrics bm
    LEFT JOIN blockchain_metrics_hourly hourly ON bm.chain_name = hourly.chain_name 
        AND hourly.hour = date_trunc('hour', NOW())
    LEFT JOIN (
        SELECT 
            chain_name,
            AVG(avg_tps) as avg_tps,
            AVG(avg_gas_utilization) as avg_gas_utilization,
            SUM(total_transactions) as total_transactions
        FROM blockchain_metrics_hourly
        WHERE hour >= NOW() - INTERVAL '24 hours'
        GROUP BY chain_name
    ) daily ON bm.chain_name = daily.chain_name
    WHERE bm.time >= NOW() - INTERVAL '5 minutes'
        AND bm.chain_name = chain_name_param
    ORDER BY bm.time DESC
    LIMIT 1;
END;
$$ LANGUAGE plpgsql;

-- Grant execute permission on the function
GRANT EXECUTE ON FUNCTION get_blockchain_stats(TEXT) TO linea_user;

-- Insert some initial data
INSERT INTO system_metrics (time, service_name, metric_name, metric_value) VALUES
    (NOW(), 'linea-microservice', 'startup_time', EXTRACT(EPOCH FROM NOW())),
    (NOW(), 'linea-microservice', 'memory_usage_mb', 512),
    (NOW(), 'linea-microservice', 'cpu_usage_percent', 15.5);

INSERT INTO blockchain_metrics (time, chain_name, block_number, tps, gas_utilization, gas_price_gwei, active_addresses, total_transactions, average_block_time, network_hashrate, difficulty) VALUES
    (NOW(), 'linea', 0, 0.0, 0.0, 0.0, 0, 0, 0.0, '0', '0');

-- Create a view for dashboard queries
CREATE VIEW dashboard_metrics AS
SELECT 
    'system' as metric_type,
    service_name,
    metric_name,
    metric_value as current_value,
    time
FROM system_metrics
WHERE time >= NOW() - INTERVAL '1 hour'
UNION ALL
SELECT 
    'blockchain' as metric_type,
    chain_name as service_name,
    'tps' as metric_name,
    tps as current_value,
    time
FROM blockchain_metrics
WHERE time >= NOW() - INTERVAL '1 hour';

-- Grant access to the view
GRANT SELECT ON dashboard_metrics TO linea_user;

-- Create a function to clean up old data
CREATE OR REPLACE FUNCTION cleanup_old_metrics()
RETURNS void AS $$
BEGIN
    -- Delete system metrics older than 30 days
    DELETE FROM system_metrics WHERE time < NOW() - INTERVAL '30 days';
    
    -- Delete performance metrics older than 30 days
    DELETE FROM performance_metrics WHERE time < NOW() - INTERVAL '30 days';
    
    -- Delete blockchain metrics older than 90 days
    DELETE FROM blockchain_metrics WHERE time < NOW() - INTERVAL '90 days';
    
    -- Delete error metrics older than 30 days
    DELETE FROM error_metrics WHERE time < NOW() - INTERVAL '30 days';
END;
$$ LANGUAGE plpgsql;

-- Grant execute permission on the cleanup function
GRANT EXECUTE ON FUNCTION cleanup_old_metrics() TO linea_user;

-- Create a scheduled job to run cleanup (requires pg_cron extension)
-- This would be set up in production with pg_cron
-- SELECT cron.schedule('cleanup-metrics', '0 2 * * *', 'SELECT cleanup_old_metrics();');

-- Final message
DO $$
BEGIN
    RAISE NOTICE 'TimescaleDB initialization completed successfully!';
    RAISE NOTICE 'Tables created: system_metrics, performance_metrics, blockchain_metrics, error_metrics';
    RAISE NOTICE 'Hypertables created with 1-hour chunk intervals';
    RAISE NOTICE 'Continuous aggregates created for hourly rollups';
    RAISE NOTICE 'Retention policies set: 30 days for most metrics, 90 days for blockchain metrics';
    RAISE NOTICE 'Helper functions created: get_realtime_metrics(), get_blockchain_stats(), cleanup_old_metrics()';
END $$;
