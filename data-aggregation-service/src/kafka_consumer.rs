use anyhow::Result;
use futures::StreamExt;
use log::{error, info, warn};
use rdkafka::config::ClientConfig;
use rdkafka::consumer::{Consumer, StreamConsumer};
use rdkafka::message::Message;
use serde_json;

use crate::models::{StreamingDataEvent, PriceDataPoint};

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

    pub async fn subscribe_to_price_updates(&self) -> Result<()> {
        self.consumer.subscribe(&[
            "blockchain-events",
            "transaction-events", 
            "contract-events"
        ])?;
        info!("Subscribed to blockchain data topics");
        Ok(())
    }

    pub async fn start_consuming<F>(&self, mut handler: F) -> Result<()>
    where
        F: FnMut(StreamingDataEvent) -> Result<()> + Send,
    {
        let mut message_stream = self.consumer.stream();

        while let Some(message) = message_stream.next().await {
            match message {
                Err(e) => {
                    error!("Kafka error: {}", e);
                    continue;
                }
                Ok(m) => {
                    let topic = m.topic();
                    let payload = match m.payload_view::<str>() {
                        None => {
                            warn!("Empty message payload from topic: {}", topic);
                            continue;
                        }
                        Some(Ok(s)) => s,
                        Some(Err(e)) => {
                            error!("Error while deserializing message payload from {}: {:?}", topic, e);
                            continue;
                        }
                    };

                    // Convert blockchain events to streaming data events
                    match self.convert_to_streaming_event(topic, payload) {
                        Ok(Some(event)) => {
                            if let Err(e) = handler(event) {
                                error!("Failed to handle streaming event: {}", e);
                            }
                        }
                        Ok(None) => {
                            // Event not relevant for aggregation
                        }
                        Err(e) => {
                            error!("Failed to convert message to streaming event: {}", e);
                        }
                    }
                }
            }
        }

        Ok(())
    }

    fn convert_to_streaming_event(&self, topic: &str, payload: &str) -> Result<Option<StreamingDataEvent>> {
        use crate::models::StreamEventType;
        use chrono::Utc;

        match topic {
            "blockchain-events" => {
                // Parse block events and extract relevant data
                if let Ok(block_event) = serde_json::from_str::<serde_json::Value>(payload) {
                    if let Some(block_number) = block_event.get("block_number") {
                        return Ok(Some(StreamingDataEvent {
                            event_type: StreamEventType::NewBlock,
                            symbol: "ETH".to_string(), // or derive from chain
                            data: block_event,
                            timestamp: Utc::now(),
                        }));
                    }
                }
            }
            "transaction-events" => {
                // Parse transaction events for DeFi activity
                if let Ok(tx_event) = serde_json::from_str::<serde_json::Value>(payload) {
                    return Ok(Some(StreamingDataEvent {
                        event_type: StreamEventType::TradeExecution,
                        symbol: "ETH".to_string(), // or derive from transaction
                        data: tx_event,
                        timestamp: Utc::now(),
                    }));
                }
            }
            "contract-events" => {
                // Parse contract events for DEX trades, liquidity changes, etc.
                if let Ok(contract_event) = serde_json::from_str::<serde_json::Value>(payload) {
                    let event_type = if contract_event.get("topics").is_some() {
                        // Determine event type based on contract topics
                        StreamEventType::ContractEvent
                    } else {
                        StreamEventType::LiquidityChange
                    };
                    
                    return Ok(Some(StreamingDataEvent {
                        event_type,
                        symbol: "ETH".to_string(), // or derive from contract
                        data: contract_event,
                        timestamp: Utc::now(),
                    }));
                }
            }
            _ => {
                warn!("Unknown topic: {}", topic);
            }
        }

        Ok(None)
    }
}
