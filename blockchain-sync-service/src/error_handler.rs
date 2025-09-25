use anyhow::Result;
use chrono::Utc;
use std::time::Duration;
use tokio::time::sleep;
use uuid::Uuid;

use crate::models::ProcessingError;

#[derive(Debug, Clone)]
pub enum ErrorType {
    RpcConnection,
    BlockNotFound,
    TransactionNotFound,
    ReceiptNotFound,
    DatabaseError,
    KafkaError,
    ParsingError,
    RateLimit,
    NetworkError,
    Unknown,
}

impl std::fmt::Display for ErrorType {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            ErrorType::RpcConnection => write!(f, "RPC_CONNECTION"),
            ErrorType::BlockNotFound => write!(f, "BLOCK_NOT_FOUND"),
            ErrorType::TransactionNotFound => write!(f, "TRANSACTION_NOT_FOUND"),
            ErrorType::ReceiptNotFound => write!(f, "RECEIPT_NOT_FOUND"),
            ErrorType::DatabaseError => write!(f, "DATABASE_ERROR"),
            ErrorType::KafkaError => write!(f, "KAFKA_ERROR"),
            ErrorType::ParsingError => write!(f, "PARSING_ERROR"),
            ErrorType::RateLimit => write!(f, "RATE_LIMIT"),
            ErrorType::NetworkError => write!(f, "NETWORK_ERROR"),
            ErrorType::Unknown => write!(f, "UNKNOWN"),
        }
    }
}

pub struct ErrorHandler {
    max_retries: u32,
    base_delay: Duration,
    max_delay: Duration,
    backoff_multiplier: f64,
}

impl ErrorHandler {
    pub fn new() -> Self {
        Self {
            max_retries: 5,
            base_delay: Duration::from_secs(1),
            max_delay: Duration::from_secs(60),
            backoff_multiplier: 2.0,
        }
    }

    pub fn with_retries(mut self, max_retries: u32) -> Self {
        self.max_retries = max_retries;
        self
    }

    pub fn with_base_delay(mut self, base_delay: Duration) -> Self {
        self.base_delay = base_delay;
        self
    }

    pub fn with_max_delay(mut self, max_delay: Duration) -> Self {
        self.max_delay = max_delay;
        self
    }

    pub async fn execute_with_retry<F, T>(&self, operation: F) -> Result<T>
    where
        F: Fn() -> std::pin::Pin<Box<dyn std::future::Future<Output = Result<T>> + Send>> + Send + Sync,
    {
        let mut last_error = None;
        
        for attempt in 0..=self.max_retries {
            match operation().await {
                Ok(result) => return Ok(result),
                Err(e) => {
                    last_error = Some(e);
                    
                    if attempt < self.max_retries {
                        let delay = self.calculate_delay(attempt);
                        log::warn!("Operation failed (attempt {}/{}), retrying in {:?}: {}", 
                            attempt + 1, self.max_retries + 1, delay, last_error.as_ref().unwrap());
                        sleep(delay).await;
                    }
                }
            }
        }
        
        Err(last_error.unwrap())
    }

    pub async fn execute_with_retry_and_error_tracking<F, T>(
        &self, 
        operation: F,
        error_type: ErrorType,
        block_number: Option<u64>,
        transaction_hash: Option<String>,
    ) -> Result<T>
    where
        F: Fn() -> std::pin::Pin<Box<dyn std::future::Future<Output = Result<T>> + Send>> + Send + Sync,
    {
        let mut last_error = None;
        let error_id = Uuid::new_v4().to_string();
        
        for attempt in 0..=self.max_retries {
            match operation().await {
                Ok(result) => return Ok(result),
                Err(e) => {
                    last_error = Some(e.clone());
                    
                    // Log the error
                    let processing_error = ProcessingError {
                        error_id: error_id.clone(),
                        error_type: error_type.to_string(),
                        block_number,
                        transaction_hash: transaction_hash.clone(),
                        error_message: e.to_string(),
                        retry_count: attempt,
                        last_retry: Utc::now(),
                        created_at: Utc::now(),
                    };
                    
                    log::error!("Processing error: {:?}", processing_error);
                    
                    if attempt < self.max_retries {
                        let delay = self.calculate_delay(attempt);
                        log::warn!("Operation failed (attempt {}/{}), retrying in {:?}: {}", 
                            attempt + 1, self.max_retries + 1, delay, e);
                        sleep(delay).await;
                    }
                }
            }
        }
        
        Err(last_error.unwrap())
    }

    fn calculate_delay(&self, attempt: u32) -> Duration {
        let delay_ms = self.base_delay.as_millis() as f64 * self.backoff_multiplier.powi(attempt as i32);
        let delay_ms = delay_ms.min(self.max_delay.as_millis() as f64);
        Duration::from_millis(delay_ms as u64)
    }

    pub fn is_retryable_error(&self, error: &anyhow::Error) -> bool {
        let error_msg = error.to_string().to_lowercase();
        
        // Check for retryable error patterns
        error_msg.contains("connection") ||
        error_msg.contains("timeout") ||
        error_msg.contains("rate limit") ||
        error_msg.contains("too many requests") ||
        error_msg.contains("service unavailable") ||
        error_msg.contains("internal server error") ||
        error_msg.contains("bad gateway") ||
        error_msg.contains("gateway timeout")
    }

    pub fn classify_error(&self, error: &anyhow::Error) -> ErrorType {
        let error_msg = error.to_string().to_lowercase();
        
        if error_msg.contains("connection") || error_msg.contains("timeout") {
            ErrorType::RpcConnection
        } else if error_msg.contains("rate limit") || error_msg.contains("too many requests") {
            ErrorType::RateLimit
        } else if error_msg.contains("block not found") {
            ErrorType::BlockNotFound
        } else if error_msg.contains("transaction not found") {
            ErrorType::TransactionNotFound
        } else if error_msg.contains("receipt not found") {
            ErrorType::ReceiptNotFound
        } else if error_msg.contains("database") || error_msg.contains("mongodb") {
            ErrorType::DatabaseError
        } else if error_msg.contains("kafka") {
            ErrorType::KafkaError
        } else if error_msg.contains("parse") || error_msg.contains("decode") {
            ErrorType::ParsingError
        } else if error_msg.contains("network") {
            ErrorType::NetworkError
        } else {
            ErrorType::Unknown
        }
    }
}

pub struct CircuitBreaker {
    failure_threshold: u32,
    recovery_timeout: Duration,
    state: CircuitBreakerState,
    failure_count: u32,
    last_failure_time: Option<std::time::Instant>,
}

#[derive(Debug, Clone, PartialEq)]
pub enum CircuitBreakerState {
    Closed,    // Normal operation
    Open,      // Circuit is open, failing fast
    HalfOpen,  // Testing if service has recovered
}

impl CircuitBreaker {
    pub fn new(failure_threshold: u32, recovery_timeout: Duration) -> Self {
        Self {
            failure_threshold,
            recovery_timeout,
            state: CircuitBreakerState::Closed,
            failure_count: 0,
            last_failure_time: None,
        }
    }

    pub async fn execute<F, T>(&mut self, operation: F) -> Result<T>
    where
        F: Fn() -> std::pin::Pin<Box<dyn std::future::Future<Output = Result<T>> + Send>> + Send + Sync,
    {
        match self.state {
            CircuitBreakerState::Open => {
                if let Some(last_failure) = self.last_failure_time {
                    if last_failure.elapsed() >= self.recovery_timeout {
                        self.state = CircuitBreakerState::HalfOpen;
                        log::info!("Circuit breaker transitioning to HalfOpen state");
                    } else {
                        return Err(anyhow::anyhow!("Circuit breaker is open"));
                    }
                } else {
                    return Err(anyhow::anyhow!("Circuit breaker is open"));
                }
            }
            CircuitBreakerState::HalfOpen => {
                // Allow one request to test if service has recovered
            }
            CircuitBreakerState::Closed => {
                // Normal operation
            }
        }

        match operation().await {
            Ok(result) => {
                self.on_success();
                Ok(result)
            }
            Err(e) => {
                self.on_failure();
                Err(e)
            }
        }
    }

    fn on_success(&mut self) {
        self.failure_count = 0;
        self.state = CircuitBreakerState::Closed;
        self.last_failure_time = None;
    }

    fn on_failure(&mut self) {
        self.failure_count += 1;
        self.last_failure_time = Some(std::time::Instant::now());

        if self.failure_count >= self.failure_threshold {
            self.state = CircuitBreakerState::Open;
            log::warn!("Circuit breaker opened after {} failures", self.failure_count);
        }
    }

    pub fn state(&self) -> &CircuitBreakerState {
        &self.state
    }

    pub fn failure_count(&self) -> u32 {
        self.failure_count
    }
}

pub struct HealthMonitor {
    start_time: std::time::Instant,
    error_count: u64,
    success_count: u64,
    last_error_time: Option<std::time::Instant>,
}

impl HealthMonitor {
    pub fn new() -> Self {
        Self {
            start_time: std::time::Instant::now(),
            error_count: 0,
            success_count: 0,
            last_error_time: None,
        }
    }

    pub fn record_success(&mut self) {
        self.success_count += 1;
    }

    pub fn record_error(&mut self) {
        self.error_count += 1;
        self.last_error_time = Some(std::time::Instant::now());
    }

    pub fn uptime(&self) -> Duration {
        self.start_time.elapsed()
    }

    pub fn error_rate(&self) -> f64 {
        let total = self.error_count + self.success_count;
        if total == 0 {
            0.0
        } else {
            self.error_count as f64 / total as f64
        }
    }

    pub fn is_healthy(&self) -> bool {
        self.error_rate() < 0.1 && // Less than 10% error rate
        (self.last_error_time.is_none() || 
         self.last_error_time.unwrap().elapsed() > Duration::from_secs(60)) // No errors in last minute
    }

    pub fn get_stats(&self) -> (u64, u64, f64, Duration) {
        (self.success_count, self.error_count, self.error_rate(), self.uptime())
    }
}
