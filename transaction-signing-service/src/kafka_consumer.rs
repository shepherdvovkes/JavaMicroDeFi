use anyhow::Result;
use futures::StreamExt;
use log::{error, info, warn};
use rdkafka::config::ClientConfig;
use rdkafka::consumer::{Consumer, StreamConsumer};
use rdkafka::message::Message;
use serde_json;
use std::time::Duration;

use crate::models::{TransactionSigningTask, SigningResult};

pub struct KafkaConsumerService {
    consumer: StreamConsumer,
}

impl KafkaConsumerService {
    pub fn new(brokers: &str, group_id: &str) -> Result<Self> {
        let consumer: StreamConsumer = ClientConfig::new()
            .set("group.id", group_id)
            .set("bootstrap.servers", brokers)
            .set("enable.partition.eof", "false")
            .set("session.timeout.ms", "6000")
            .set("enable.auto.commit", "true")
            .set("auto.offset.reset", "latest")
            .create()?;

        Ok(Self { consumer })
    }

    pub async fn subscribe_to_signing_requests(&self) -> Result<()> {
        self.consumer.subscribe(&["transaction-signing-requests"])?;
        info!("Subscribed to transaction-signing-requests topic");
        Ok(())
    }

    pub async fn start_consuming<F>(&self, mut handler: F) -> Result<()>
    where
        F: FnMut(TransactionSigningTask) -> Result<SigningResult> + Send,
    {
        let mut message_stream = self.consumer.stream();

        while let Some(message) = message_stream.next().await {
            match message {
                Err(e) => {
                    error!("Kafka error: {}", e);
                    continue;
                }
                Ok(m) => {
                    let payload = match m.payload_view::<str>() {
                        None => {
                            warn!("Empty message payload");
                            continue;
                        }
                        Some(Ok(s)) => s,
                        Some(Err(e)) => {
                            error!("Error while deserializing message payload: {:?}", e);
                            continue;
                        }
                    };

                    match serde_json::from_str::<TransactionSigningTask>(payload) {
                        Ok(task) => {
                            info!("Received signing task: {}", task.task_id);
                            
                            match handler(task) {
                                Ok(result) => {
                                    info!("Successfully processed signing task: {}", result.task_id);
                                    // Send result back to Kafka
                                    if let Err(e) = self.send_signing_result(&result).await {
                                        error!("Failed to send signing result: {}", e);
                                    }
                                }
                                Err(e) => {
                                    error!("Failed to process signing task: {}", e);
                                }
                            }
                        }
                        Err(e) => {
                            error!("Failed to deserialize signing task: {}", e);
                        }
                    }
                }
            }
        }

        Ok(())
    }

    async fn send_signing_result(&self, result: &SigningResult) -> Result<()> {
        use rdkafka::producer::{FutureProducer, FutureRecord};
        
        // Create a producer for sending results
        let producer: FutureProducer = ClientConfig::new()
            .set("bootstrap.servers", std::env::var("KAFKA_BROKERS").unwrap_or_else(|_| "localhost:9092".to_string()))
            .set("message.timeout.ms", "5000")
            .set("acks", "all")
            .create()?;

        let payload = serde_json::to_string(result)?;
        let key = result.task_id.clone();

        let record = FutureRecord::to("transaction-signing-results")
            .key(&key)
            .payload(&payload);

        match producer.send(record, Duration::from_secs(0)).await {
            Ok(_) => {
                info!("Sent signing result for task: {}", result.task_id);
                Ok(())
            }
            Err((e, _)) => Err(anyhow::anyhow!("Failed to send signing result: {}", e)),
        }
    }

    pub async fn subscribe_to_batch_requests(&self) -> Result<()> {
        self.consumer.subscribe(&["batch-signing-requests"])?;
        info!("Subscribed to batch-signing-requests topic");
        Ok(())
    }

    pub async fn send_health_status(&self, status: &str) -> Result<()> {
        use rdkafka::producer::{FutureProducer, FutureRecord};
        
        let producer: FutureProducer = ClientConfig::new()
            .set("bootstrap.servers", std::env::var("KAFKA_BROKERS").unwrap_or_else(|_| "localhost:9092".to_string()))
            .set("message.timeout.ms", "5000")
            .create()?;

        let health_status = serde_json::json!({
            "service": "transaction-signing",
            "status": status,
            "timestamp": chrono::Utc::now().timestamp(),
            "memory_usage": self.get_memory_usage(),
            "active_tasks": 0 // Could be tracked
        });

        let payload = serde_json::to_string(&health_status)?;
        let key = "transaction-signing-health".to_string();

        let record = FutureRecord::to("service-status")
            .key(&key)
            .payload(&payload);

        match producer.send(record, Duration::from_secs(0)).await {
            Ok(_) => Ok(()),
            Err((e, _)) => Err(anyhow::anyhow!("Failed to send health status: {}", e)),
        }
    }

    fn get_memory_usage(&self) -> u64 {
        // Simple memory usage estimation
        // In a real implementation, you might use a more sophisticated method
        std::process::id() as u64 * 1024 // Placeholder
    }
}
