use anyhow::Result;
use chrono::Utc;
use mongodb::{bson::doc, Client, Collection, Database};
use web3::types::{Block, Transaction};

use crate::models::{StoredBlock, StoredTransaction, StoredEvent, ContractEvent};

#[derive(Clone)]
pub struct MongoDBService {
    database: Database,
}

impl MongoDBService {
    pub async fn new(uri: &str) -> Result<Self> {
        let client = Client::with_uri_str(uri).await?;
        let database = client.database("chaindata");
        
        Ok(Self { database })
    }

    pub async fn get_last_processed_block(&self) -> Result<u64> {
        let collection: Collection<StoredBlock> = self.database.collection("blocks");
        
        let options = mongodb::options::FindOptions::builder()
            .sort(doc! { "block_number": -1 })
            .limit(1)
            .build();

        match collection.find_one(None, Some(options)).await? {
            Some(block) => Ok(block.block_number),
            None => Ok(0), // Start from genesis if no blocks found
        }
    }

    pub async fn store_block(&self, block: &Block<Transaction>) -> Result<()> {
        let collection: Collection<StoredBlock> = self.database.collection("blocks");
        
        let stored_block = StoredBlock {
            block_number: block.number.unwrap().as_u64(),
            block_hash: format!("{:?}", block.hash.unwrap()),
            parent_hash: format!("{:?}", block.parent_hash),
            timestamp: block.timestamp.as_u64(),
            gas_limit: block.gas_limit.as_u64(),
            gas_used: block.gas_used.as_u64(),
            difficulty: block.difficulty.to_string(),
            total_difficulty: block.total_difficulty.map(|d| d.to_string()).unwrap_or_default(),
            size: block.size.map(|s| s.as_u64()).unwrap_or(0),
            transaction_count: block.transactions.len() as u32,
            created_at: Utc::now(),
        };

        collection.insert_one(stored_block, None).await?;
        Ok(())
    }

    pub async fn store_transaction(&self, tx: &Transaction, block: &Block<Transaction>) -> Result<()> {
        let collection: Collection<StoredTransaction> = self.database.collection("transactions");
        
        let stored_tx = StoredTransaction {
            hash: format!("{:?}", tx.hash),
            block_number: block.number.unwrap().as_u64(),
            block_hash: format!("{:?}", block.hash.unwrap()),
            transaction_index: tx.transaction_index.unwrap().as_u64(),
            from: format!("{:?}", tx.from.unwrap_or_default()),
            to: tx.to.map(|addr| format!("{:?}", addr)),
            value: tx.value.to_string(),
            gas_price: tx.gas_price.unwrap_or_default().to_string(),
            gas_limit: tx.gas.to_string(),
            gas_used: None, // Will be updated when receipt is processed
            nonce: tx.nonce.as_u64(),
            input_data: format!("{:?}", tx.input.0),
            timestamp: block.timestamp.as_u64(),
            created_at: Utc::now(),
        };

        collection.insert_one(stored_tx, None).await?;
        Ok(())
    }

    pub async fn store_event(&self, event: &ContractEvent) -> Result<()> {
        let collection: Collection<StoredEvent> = self.database.collection("events");
        
        let stored_event = StoredEvent {
            transaction_hash: event.transaction_hash.clone(),
            block_number: event.block_number,
            contract_address: event.contract_address.clone(),
            event_name: None, // Could be decoded from topics
            topics: event.topics.clone(),
            data: event.data.clone(),
            timestamp: event.timestamp,
            created_at: Utc::now(),
        };

        collection.insert_one(stored_event, None).await?;
        Ok(())
    }

    pub async fn get_blocks_range(&self, start_block: u64, end_block: u64) -> Result<Vec<StoredBlock>> {
        let collection: Collection<StoredBlock> = self.database.collection("blocks");
        
        let filter = doc! {
            "block_number": {
                "$gte": start_block,
                "$lte": end_block
            }
        };

        let options = mongodb::options::FindOptions::builder()
            .sort(doc! { "block_number": 1 })
            .build();

        let mut cursor = collection.find(filter, Some(options)).await?;
        let mut blocks = Vec::new();

        while let Some(block) = cursor.next().await {
            blocks.push(block?);
        }

        Ok(blocks)
    }

    pub async fn get_transactions_by_address(&self, address: &str, limit: i64) -> Result<Vec<StoredTransaction>> {
        let collection: Collection<StoredTransaction> = self.database.collection("transactions");
        
        let filter = doc! {
            "$or": [
                { "from": address },
                { "to": address }
            ]
        };

        let options = mongodb::options::FindOptions::builder()
            .sort(doc! { "timestamp": -1 })
            .limit(limit)
            .build();

        let mut cursor = collection.find(filter, Some(options)).await?;
        let mut transactions = Vec::new();

        while let Some(tx) = cursor.next().await {
            transactions.push(tx?);
        }

        Ok(transactions)
    }
}
