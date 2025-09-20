use anyhow::Result;
use chrono::{DateTime, Utc};
use futures::StreamExt;
use log::{error, info, warn};
use mongodb::{Client as MongoClient, Collection, Database};
use rdkafka::config::ClientConfig;
use rdkafka::producer::{FutureProducer, FutureRecord};
use serde::{Deserialize, Serialize};
use std::env;
use std::time::Duration;
use tokio::time::sleep;
use web3::types::{Block, BlockId, BlockNumber, Transaction, H256, U256, U64};
use web3::Web3;

mod models;
mod blockchain;
mod kafka_producer;
mod mongodb_client;

use models::*;
use blockchain::BlockchainClient;
use kafka_producer::KafkaProducerService;
use mongodb_client::MongoDBService;

#[derive(Clone)]
pub struct BlockchainSyncService {
    blockchain_client: BlockchainClient,
    kafka_producer: KafkaProducerService,
    mongodb_service: MongoDBService,
    last_processed_block: u64,
}

impl BlockchainSyncService {
    pub async fn new() -> Result<Self> {
        let rpc_url = env::var("ETH_RPC_URL")
            .unwrap_or_else(|_| "https://mainnet.infura.io/v3/YOUR_PROJECT_ID".to_string());
        
        let kafka_brokers = env::var("KAFKA_BROKERS")
            .unwrap_or_else(|_| "localhost:9092".to_string());
        
        let mongodb_uri = env::var("MONGODB_URI")
            .unwrap_or_else(|_| "mongodb://localhost:27017".to_string());

        let blockchain_client = BlockchainClient::new(&rpc_url).await?;
        let kafka_producer = KafkaProducerService::new(&kafka_brokers)?;
        let mongodb_service = MongoDBService::new(&mongodb_uri).await?;

        // Get last processed block from database
        let last_processed_block = mongodb_service.get_last_processed_block().await?;

        Ok(Self {
            blockchain_client,
            kafka_producer,
            mongodb_service,
            last_processed_block,
        })
    }

    pub async fn start_sync(&mut self) -> Result<()> {
        info!("Starting blockchain synchronization from block {}", self.last_processed_block);

        loop {
            match self.sync_next_block().await {
                Ok(block_number) => {
                    self.last_processed_block = block_number;
                    info!("Successfully processed block {}", block_number);
                }
                Err(e) => {
                    error!("Error processing block: {}", e);
                    sleep(Duration::from_secs(5)).await;
                }
            }

            // Small delay to prevent overwhelming the RPC endpoint
            sleep(Duration::from_millis(100)).await;
        }
    }

    async fn sync_next_block(&mut self) -> Result<u64> {
        let latest_block_number = self.blockchain_client.get_latest_block_number().await?;
        
        if self.last_processed_block >= latest_block_number {
            // We're caught up, wait a bit
            sleep(Duration::from_secs(1)).await;
            return Ok(self.last_processed_block);
        }

        let block_number = self.last_processed_block + 1;
        let block = self.blockchain_client.get_block_with_transactions(block_number).await?;

        // Store block in MongoDB
        self.mongodb_service.store_block(&block).await?;

        // Process transactions
        for tx in &block.transactions {
            self.process_transaction(tx, &block).await?;
        }

        // Send block event to Kafka
        let block_event = BlockEvent {
            block_number: block.number.unwrap().as_u64(),
            block_hash: format!("{:?}", block.hash.unwrap()),
            timestamp: block.timestamp.as_u64(),
            transaction_count: block.transactions.len() as u32,
            gas_used: block.gas_used.as_u64(),
        };

        self.kafka_producer.send_block_event(&block_event).await?;

        Ok(block_number)
    }

    async fn process_transaction(&self, tx: &Transaction, block: &Block<Transaction>) -> Result<()> {
        // Store transaction in MongoDB
        self.mongodb_service.store_transaction(tx, block).await?;

        // Send transaction event to Kafka
        let tx_event = TransactionEvent {
            hash: format!("{:?}", tx.hash),
            block_number: block.number.unwrap().as_u64(),
            from: format!("{:?}", tx.from.unwrap_or_default()),
            to: tx.to.map(|addr| format!("{:?}", addr)),
            value: tx.value.to_string(),
            gas_price: tx.gas_price.unwrap_or_default().to_string(),
            gas_used: tx.gas.to_string(),
            timestamp: block.timestamp.as_u64(),
        };

        self.kafka_producer.send_transaction_event(&tx_event).await?;

        // Process contract events if this is a contract interaction
        if let Some(receipt) = self.blockchain_client.get_transaction_receipt(&tx.hash).await? {
            for log in receipt.logs {
                let event = ContractEvent {
                    transaction_hash: format!("{:?}", tx.hash),
                    block_number: block.number.unwrap().as_u64(),
                    contract_address: format!("{:?}", log.address),
                    topics: log.topics.iter().map(|topic| format!("{:?}", topic)).collect(),
                    data: format!("{:?}", log.data.0),
                    timestamp: block.timestamp.as_u64(),
                };

                self.mongodb_service.store_event(&event).await?;
                self.kafka_producer.send_contract_event(&event).await?;
            }
        }

        Ok(())
    }
}

#[tokio::main]
async fn main() -> Result<()> {
    env_logger::init();
    
    info!("Starting DEFIMON Blockchain Sync Service");

    let mut sync_service = BlockchainSyncService::new().await?;
    
    match sync_service.start_sync().await {
        Ok(_) => info!("Blockchain sync service completed successfully"),
        Err(e) => error!("Blockchain sync service failed: {}", e),
    }

    Ok(())
}
