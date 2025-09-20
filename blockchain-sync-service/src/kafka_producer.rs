use anyhow::Result;
use rdkafka::config::ClientConfig;
use rdkafka::producer::{FutureProducer, FutureRecord};
use serde_json;
use std::time::Duration;

use crate::models::{BlockEvent, TransactionEvent, ContractEvent};

#[derive(Clone)]
pub struct KafkaProducerService {
    producer: FutureProducer,
}

impl KafkaProducerService {
    pub fn new(brokers: &str) -> Result<Self> {
        let producer: FutureProducer = ClientConfig::new()
            .set("bootstrap.servers", brokers)
            .set("message.timeout.ms", "5000")
            .set("acks", "all")
            .set("retries", "3")
            .set("enable.idempotence", "true")
            .create()?;

        Ok(Self { producer })
    }

    pub async fn send_block_event(&self, event: &BlockEvent) -> Result<()> {
        let payload = serde_json::to_string(event)?;
        let key = event.block_number.to_string();

        let record = FutureRecord::to("blockchain-events")
            .key(&key)
            .payload(&payload);

        match self.producer.send(record, Duration::from_secs(0)).await {
            Ok(_) => Ok(()),
            Err((e, _)) => Err(anyhow::anyhow!("Failed to send block event: {}", e)),
        }
    }

    pub async fn send_transaction_event(&self, event: &TransactionEvent) -> Result<()> {
        let payload = serde_json::to_string(event)?;
        let key = event.hash.clone();

        let record = FutureRecord::to("transaction-events")
            .key(&key)
            .payload(&payload);

        match self.producer.send(record, Duration::from_secs(0)).await {
            Ok(_) => Ok(()),
            Err((e, _)) => Err(anyhow::anyhow!("Failed to send transaction event: {}", e)),
        }
    }

    pub async fn send_contract_event(&self, event: &ContractEvent) -> Result<()> {
        let payload = serde_json::to_string(event)?;
        let key = format!("{}_{}", event.transaction_hash, event.contract_address);

        let record = FutureRecord::to("contract-events")
            .key(&key)
            .payload(&payload);

        match self.producer.send(record, Duration::from_secs(0)).await {
            Ok(_) => Ok(()),
            Err((e, _)) => Err(anyhow::anyhow!("Failed to send contract event: {}", e)),
        }
    }

    pub async fn send_sync_status(&self, block_number: u64, status: &str) -> Result<()> {
        let status_event = serde_json::json!({
            "service": "blockchain-sync",
            "block_number": block_number,
            "status": status,
            "timestamp": chrono::Utc::now().timestamp()
        });

        let payload = serde_json::to_string(&status_event)?;
        let key = "sync-status".to_string();

        let record = FutureRecord::to("service-status")
            .key(&key)
            .payload(&payload);

        match self.producer.send(record, Duration::from_secs(0)).await {
            Ok(_) => Ok(()),
            Err((e, _)) => Err(anyhow::anyhow!("Failed to send sync status: {}", e)),
        }
    }
}
