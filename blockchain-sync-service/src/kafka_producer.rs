use anyhow::Result;
use rdkafka::config::ClientConfig;
use rdkafka::producer::{FutureProducer, FutureRecord};
use serde_json;
use std::time::Duration;
use chrono::Utc;
use uuid::Uuid;

use crate::models::*;
use crate::error_handler::{ErrorHandler, ErrorType};

#[derive(Clone)]
pub struct KafkaProducerService {
    producer: FutureProducer,
    error_handler: ErrorHandler,
}

impl KafkaProducerService {
    pub fn new(brokers: &str) -> Result<Self> {
        let producer: FutureProducer = ClientConfig::new()
            .set("bootstrap.servers", brokers)
            .set("message.timeout.ms", "10000")
            .set("acks", "all")
            .set("retries", "5")
            .set("enable.idempotence", "true")
            .set("compression.type", "snappy")
            .set("batch.size", "16384")
            .set("linger.ms", "5")
            .set("buffer.memory", "33554432")
            .create()?;

        let error_handler = ErrorHandler::new()
            .with_retries(3)
            .with_base_delay(Duration::from_secs(1))
            .with_max_delay(Duration::from_secs(10));

        Ok(Self { 
            producer,
            error_handler,
        })
    }

    pub async fn send_block_event(&self, event: &BlockEvent) -> Result<()> {
        let operation = || {
            let producer = self.producer.clone();
            let event = event.clone();
            Box::pin(async move {
                let payload = serde_json::to_string(&event)?;
                let key = event.block_number.to_string();

                let record = FutureRecord::to("ethereum-blocks")
                    .key(&key)
                    .payload(&payload);

                match producer.send(record, Duration::from_secs(5)).await {
                    Ok(_) => Ok(()),
                    Err((e, _)) => Err(anyhow::anyhow!("Failed to send block event: {}", e)),
                }
            })
        };

        self.error_handler.execute_with_retry_and_error_tracking(
            operation,
            ErrorType::KafkaError,
            Some(event.block_number),
            None,
        ).await
    }

    pub async fn send_transaction_event(&self, event: &TransactionEvent) -> Result<()> {
        let operation = || {
            let producer = self.producer.clone();
            let event = event.clone();
            Box::pin(async move {
                let payload = serde_json::to_string(&event)?;
                let key = event.hash.clone();

                let record = FutureRecord::to("ethereum-transactions")
                    .key(&key)
                    .payload(&payload);

                match producer.send(record, Duration::from_secs(5)).await {
                    Ok(_) => Ok(()),
                    Err((e, _)) => Err(anyhow::anyhow!("Failed to send transaction event: {}", e)),
                }
            })
        };

        self.error_handler.execute_with_retry_and_error_tracking(
            operation,
            ErrorType::KafkaError,
            Some(event.block_number),
            Some(event.hash.clone()),
        ).await
    }

    pub async fn send_contract_event(&self, event: &ContractEvent) -> Result<()> {
        let operation = || {
            let producer = self.producer.clone();
            let event = event.clone();
            Box::pin(async move {
                let payload = serde_json::to_string(&event)?;
                let key = format!("{}_{}", event.transaction_hash, event.contract_address);

                let record = FutureRecord::to("ethereum-events")
                    .key(&key)
                    .payload(&payload);

                match producer.send(record, Duration::from_secs(5)).await {
                    Ok(_) => Ok(()),
                    Err((e, _)) => Err(anyhow::anyhow!("Failed to send contract event: {}", e)),
                }
            })
        };

        self.error_handler.execute_with_retry_and_error_tracking(
            operation,
            ErrorType::KafkaError,
            Some(event.block_number),
            Some(event.transaction_hash.clone()),
        ).await
    }

    pub async fn send_token_transfer_event(&self, event: &TokenTransferEvent) -> Result<()> {
        let operation = || {
            let producer = self.producer.clone();
            let event = event.clone();
            Box::pin(async move {
                let payload = serde_json::to_string(&event)?;
                let key = format!("{}_{}", event.transaction_hash, event.contract_address);

                let record = FutureRecord::to("ethereum-token-transfers")
                    .key(&key)
                    .payload(&payload);

                match producer.send(record, Duration::from_secs(5)).await {
                    Ok(_) => Ok(()),
                    Err((e, _)) => Err(anyhow::anyhow!("Failed to send token transfer event: {}", e)),
                }
            })
        };

        self.error_handler.execute_with_retry_and_error_tracking(
            operation,
            ErrorType::KafkaError,
            Some(event.block_number),
            Some(event.transaction_hash.clone()),
        ).await
    }

    pub async fn send_defi_event(&self, event: &DeFiEvent) -> Result<()> {
        let operation = || {
            let producer = self.producer.clone();
            let event = event.clone();
            Box::pin(async move {
                let payload = serde_json::to_string(&event)?;
                let key = format!("{}_{}", event.transaction_hash, event.protocol);

                let record = FutureRecord::to("ethereum-defi-events")
                    .key(&key)
                    .payload(&payload);

                match producer.send(record, Duration::from_secs(5)).await {
                    Ok(_) => Ok(()),
                    Err((e, _)) => Err(anyhow::anyhow!("Failed to send DeFi event: {}", e)),
                }
            })
        };

        self.error_handler.execute_with_retry_and_error_tracking(
            operation,
            ErrorType::KafkaError,
            Some(event.block_number),
            Some(event.transaction_hash.clone()),
        ).await
    }

    pub async fn send_sync_status(&self, status: &SyncStatus) -> Result<()> {
        let operation = || {
            let producer = self.producer.clone();
            let status = status.clone();
            Box::pin(async move {
                let payload = serde_json::to_string(&status)?;
                let key = format!("sync-status-{}", status.service_name);

                let record = FutureRecord::to("ethereum-sync-status")
                    .key(&key)
                    .payload(&payload);

                match producer.send(record, Duration::from_secs(5)).await {
                    Ok(_) => Ok(()),
                    Err((e, _)) => Err(anyhow::anyhow!("Failed to send sync status: {}", e)),
                }
            })
        };

        self.error_handler.execute_with_retry_and_error_tracking(
            operation,
            ErrorType::KafkaError,
            Some(status.last_processed_block),
            None,
        ).await
    }

    pub async fn send_health_check(&self, service_name: &str, is_healthy: bool, error_rate: f64, uptime: Duration) -> Result<()> {
        let health_data = serde_json::json!({
            "service_name": service_name,
            "is_healthy": is_healthy,
            "error_rate": error_rate,
            "uptime_seconds": uptime.as_secs(),
            "timestamp": Utc::now().timestamp(),
            "event_type": "health_check"
        });

        let operation = || {
            let producer = self.producer.clone();
            let health_data = health_data.clone();
            Box::pin(async move {
                let payload = serde_json::to_string(&health_data)?;
                let key = format!("health-{}", service_name);

                let record = FutureRecord::to("ethereum-service-health")
                    .key(&key)
                    .payload(&payload);

                match producer.send(record, Duration::from_secs(5)).await {
                    Ok(_) => Ok(()),
                    Err((e, _)) => Err(anyhow::anyhow!("Failed to send health check: {}", e)),
                }
            })
        };

        self.error_handler.execute_with_retry_and_error_tracking(
            operation,
            ErrorType::KafkaError,
            None,
            None,
        ).await
    }
}
