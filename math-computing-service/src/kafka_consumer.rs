use anyhow::Result;
use futures::StreamExt;
use log::{error, info, warn};
use rdkafka::config::ClientConfig;
use rdkafka::consumer::{Consumer, StreamConsumer};
use rdkafka::message::Message;
use serde_json;
use std::time::Duration;

use crate::models::{MathComputationTask, ComputationResult};

#[derive(Clone)]
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

    pub async fn subscribe_to_computation_requests(&self) -> Result<()> {
        self.consumer.subscribe(&["math-computation-requests"])?;
        info!("Subscribed to math-computation-requests topic");
        Ok(())
    }

    pub async fn start_consuming<F>(&self, mut handler: F) -> Result<()>
    where
        F: FnMut(MathComputationTask) -> Result<ComputationResult> + Send,
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

                    match serde_json::from_str::<MathComputationTask>(payload) {
                        Ok(task) => {
                            info!("Received computation task: {} of type: {:?}", task.task_id, task.task_type);
                            
                            match handler(task) {
                                Ok(result) => {
                                    info!("Successfully processed computation task: {}", result.task_id);
                                    // Send result back to Kafka
                                    if let Err(e) = self.send_computation_result(&result).await {
                                        error!("Failed to send computation result: {}", e);
                                    }
                                }
                                Err(e) => {
                                    error!("Failed to process computation task: {}", e);
                                }
                            }
                        }
                        Err(e) => {
                            error!("Failed to deserialize computation task: {}", e);
                        }
                    }
                }
            }
        }

        Ok(())
    }

    async fn send_computation_result(&self, result: &ComputationResult) -> Result<()> {
        use rdkafka::producer::{FutureProducer, FutureRecord};
        
        // Create a producer for sending results
        let producer: FutureProducer = ClientConfig::new()
            .set("bootstrap.servers", std::env::var("KAFKA_BROKERS").unwrap_or_else(|_| "localhost:9092".to_string()))
            .set("message.timeout.ms", "5000")
            .set("acks", "all")
            .create()?;

        let payload = serde_json::to_string(result)?;
        let key = result.task_id.clone();

        let record = FutureRecord::to("math-computation-results")
            .key(&key)
            .payload(&payload);

        match producer.send(record, Duration::from_secs(0)).await {
            Ok(_) => {
                info!("Sent computation result for task: {}", result.task_id);
                Ok(())
            }
            Err((e, _)) => Err(anyhow::anyhow!("Failed to send computation result: {}", e)),
        }
    }

    pub async fn send_performance_metrics(&self, metrics: &serde_json::Value) -> Result<()> {
        use rdkafka::producer::{FutureProducer, FutureRecord};
        
        let producer: FutureProducer = ClientConfig::new()
            .set("bootstrap.servers", std::env::var("KAFKA_BROKERS").unwrap_or_else(|_| "localhost:9092".to_string()))
            .set("message.timeout.ms", "5000")
            .create()?;

        let payload = serde_json::to_string(metrics)?;
        let key = "math-computing-metrics".to_string();

        let record = FutureRecord::to("service-metrics")
            .key(&key)
            .payload(&payload);

        match producer.send(record, Duration::from_secs(0)).await {
            Ok(_) => Ok(()),
            Err((e, _)) => Err(anyhow::anyhow!("Failed to send performance metrics: {}", e)),
        }
    }

    pub async fn send_health_status(&self, status: &str) -> Result<()> {
        use rdkafka::producer::{FutureProducer, FutureRecord};
        
        let producer: FutureProducer = ClientConfig::new()
            .set("bootstrap.servers", std::env::var("KAFKA_BROKERS").unwrap_or_else(|_| "localhost:9092".to_string()))
            .set("message.timeout.ms", "5000")
            .create()?;

        let health_status = serde_json::json!({
            "service": "math-computing",
            "status": status,
            "timestamp": chrono::Utc::now().timestamp(),
            "cpu_cores": num_cpus::get(),
            "memory_usage": self.get_memory_usage(),
            "active_computations": 0 // Could be tracked
        });

        let payload = serde_json::to_string(&health_status)?;
        let key = "math-computing-health".to_string();

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
        std::process::id() as u64 * 1024 // Placeholder
    }
}
