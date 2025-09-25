use anyhow::Result;
use chrono::Utc;
use mongodb::{bson::doc, Client, Collection, Database, IndexModel, options::{IndexOptions, CreateIndexOptions}};
use web3::types::{Block, Transaction};
use rust_decimal::Decimal;

use crate::models::*;
use crate::error_handler::{ErrorHandler, ErrorType};
use crate::metrics::BlockchainMetrics;
use std::sync::Arc;

#[derive(Clone)]
pub struct MongoDBService {
    database: Database,
    error_handler: ErrorHandler,
    metrics: Arc<BlockchainMetrics>,
}

impl MongoDBService {
    pub async fn new(uri: &str, metrics: Arc<BlockchainMetrics>) -> Result<Self> {
        let client = Client::with_uri_str(uri).await?;
        let database = client.database("ethereum_chaindata");
        
        let error_handler = ErrorHandler::new()
            .with_retries(3)
            .with_base_delay(std::time::Duration::from_secs(1))
            .with_max_delay(std::time::Duration::from_secs(10));

        let service = Self { 
            database,
            error_handler,
            metrics,
        };

        // Create indexes for better performance
        service.create_indexes().await?;
        
        Ok(service)
    }

    async fn create_indexes(&self) -> Result<()> {
        // Blocks collection indexes
        let blocks_collection: Collection<StoredBlock> = self.database.collection("blocks");
        
        let block_indexes = vec![
            IndexModel::builder()
                .keys(doc! { "block_number": 1 })
                .options(IndexOptions::builder().unique(true).build())
                .build(),
            IndexModel::builder()
                .keys(doc! { "block_hash": 1 })
                .options(IndexOptions::builder().unique(true).build())
                .build(),
            IndexModel::builder()
                .keys(doc! { "timestamp": 1 })
                .build(),
            IndexModel::builder()
                .keys(doc! { "miner": 1 })
                .build(),
        ];

        blocks_collection.create_indexes(block_indexes, None).await?;

        // Transactions collection indexes
        let transactions_collection: Collection<StoredTransaction> = self.database.collection("transactions");
        
        let tx_indexes = vec![
            IndexModel::builder()
                .keys(doc! { "hash": 1 })
                .options(IndexOptions::builder().unique(true).build())
                .build(),
            IndexModel::builder()
                .keys(doc! { "block_number": 1, "transaction_index": 1 })
                .build(),
            IndexModel::builder()
                .keys(doc! { "from": 1 })
                .build(),
            IndexModel::builder()
                .keys(doc! { "to": 1 })
                .build(),
            IndexModel::builder()
                .keys(doc! { "timestamp": 1 })
                .build(),
            IndexModel::builder()
                .keys(doc! { "is_contract_creation": 1 })
                .build(),
            IndexModel::builder()
                .keys(doc! { "is_contract_interaction": 1 })
                .build(),
        ];

        transactions_collection.create_indexes(tx_indexes, None).await?;

        // Events collection indexes
        let events_collection: Collection<StoredEvent> = self.database.collection("events");
        
        let event_indexes = vec![
            IndexModel::builder()
                .keys(doc! { "transaction_hash": 1, "log_index": 1 })
                .options(IndexOptions::builder().unique(true).build())
                .build(),
            IndexModel::builder()
                .keys(doc! { "block_number": 1 })
                .build(),
            IndexModel::builder()
                .keys(doc! { "contract_address": 1 })
                .build(),
            IndexModel::builder()
                .keys(doc! { "event_name": 1 })
                .build(),
            IndexModel::builder()
                .keys(doc! { "timestamp": 1 })
                .build(),
        ];

        events_collection.create_indexes(event_indexes, None).await?;

        Ok(())
    }

    pub async fn get_last_processed_block(&self) -> Result<u64> {
        let operation = || {
            let collection: Collection<StoredBlock> = self.database.collection("blocks");
            Box::pin(async move {
                let options = mongodb::options::FindOptions::builder()
                    .sort(doc! { "block_number": -1 })
                    .limit(1)
                    .build();

                match collection.find_one(None, Some(options)).await? {
                    Some(block) => Ok(block.block_number),
                    None => Ok(0), // Start from genesis if no blocks found
                }
            })
        };

        self.error_handler.execute_with_retry_and_error_tracking(
            operation,
            ErrorType::DatabaseError,
            None,
            None,
        ).await
    }

    pub async fn store_block(&self, block: &Block<Transaction>) -> Result<()> {
        let operation = || {
            let collection: Collection<StoredBlock> = self.database.collection("blocks");
            let block = block.clone();
            Box::pin(async move {
                let stored_block = StoredBlock {
                    block_number: block.number.unwrap().as_u64(),
                    block_hash: format!("{:?}", block.hash.unwrap()),
                    parent_hash: format!("{:?}", block.parent_hash),
                    timestamp: block.timestamp.as_u64(),
                    gas_limit: block.gas_limit.as_u64(),
                    gas_used: block.gas_used.as_u64(),
                    base_fee_per_gas: block.base_fee_per_gas.map(|fee| fee.to_string()),
                    difficulty: block.difficulty.to_string(),
                    total_difficulty: block.total_difficulty.map(|d| d.to_string()).unwrap_or_default(),
                    size: block.size.map(|s| s.as_u64()).unwrap_or(0),
                    transaction_count: block.transactions.len() as u32,
                    miner: format!("{:?}", block.author.unwrap_or_default()),
                    extra_data: format!("{:?}", block.extra_data.0),
                    logs_bloom: format!("{:?}", block.logs_bloom.unwrap_or_default()),
                    mix_hash: format!("{:?}", block.mix_hash.unwrap_or_default()),
                    nonce: format!("{:?}", block.nonce.unwrap_or_default()),
                    sha3_uncles: format!("{:?}", block.sha3_uncles),
                    state_root: format!("{:?}", block.state_root),
                    transactions_root: format!("{:?}", block.transactions_root),
                    receipts_root: format!("{:?}", block.receipts_root),
                    withdrawals: block.withdrawals.as_ref().map(|withdrawals| {
                        withdrawals.iter().map(|w| Withdrawal {
                            index: w.index.as_u64(),
                            validator_index: w.validator_index.as_u64(),
                            address: format!("{:?}", w.address),
                            amount: w.amount.to_string(),
                        }).collect()
                    }),
                    created_at: Utc::now(),
                };

                collection.insert_one(stored_block, None).await?;
                Ok(())
            })
        };

        self.error_handler.execute_with_retry_and_error_tracking(
            operation,
            ErrorType::DatabaseError,
            Some(block.number.unwrap().as_u64()),
            None,
        ).await
    }

    pub async fn store_transaction(&self, tx: &Transaction, block: &Block<Transaction>) -> Result<()> {
        let operation = || {
            let collection: Collection<StoredTransaction> = self.database.collection("transactions");
            let tx = tx.clone();
            let block = block.clone();
            Box::pin(async move {
                let wei_to_eth = Decimal::new(1, 18);
                let value_eth = Decimal::from_str_exact(&tx.value.to_string()).unwrap_or_default() / wei_to_eth;
                
                let is_contract_creation = tx.to.is_none();
                let is_contract_interaction = !tx.input.0.is_empty() && tx.to.is_some();
                
                let access_list = tx.access_list.as_ref().map(|list| {
                    list.iter().map(|entry| AccessListEntry {
                        address: format!("{:?}", entry.address),
                        storage_keys: entry.storage_keys.iter().map(|key| format!("{:?}", key)).collect(),
                    }).collect()
                });

                let stored_tx = StoredTransaction {
                    hash: format!("{:?}", tx.hash),
                    block_number: block.number.unwrap().as_u64(),
                    block_hash: format!("{:?}", block.hash.unwrap()),
                    transaction_index: tx.transaction_index.unwrap().as_u64(),
                    from: format!("{:?}", tx.from.unwrap_or_default()),
                    to: tx.to.map(|addr| format!("{:?}", addr)),
                    value: tx.value.to_string(),
                    value_eth,
                    gas_price: tx.gas_price.unwrap_or_default().to_string(),
                    gas_limit: tx.gas.to_string(),
                    gas_used: None, // Will be updated when receipt is processed
                    max_fee_per_gas: tx.max_fee_per_gas.map(|fee| fee.to_string()),
                    max_priority_fee_per_gas: tx.max_priority_fee_per_gas.map(|fee| fee.to_string()),
                    nonce: tx.nonce.as_u64(),
                    input_data: format!("{:?}", tx.input.0),
                    input_data_length: tx.input.0.len(),
                    is_contract_creation,
                    is_contract_interaction,
                    transaction_type: tx.transaction_type.unwrap_or(0).as_u64() as u8,
                    access_list,
                    chain_id: tx.chain_id.map(|id| id.as_u64()),
                    v: format!("{:?}", tx.v),
                    r: format!("{:?}", tx.r),
                    s: format!("{:?}", tx.s),
                    timestamp: block.timestamp.as_u64(),
                    created_at: Utc::now(),
                };

                collection.insert_one(stored_tx, None).await?;
                Ok(())
            })
        };

        self.error_handler.execute_with_retry_and_error_tracking(
            operation,
            ErrorType::DatabaseError,
            Some(block.number.unwrap().as_u64()),
            Some(format!("{:?}", tx.hash)),
        ).await
    }

    pub async fn store_event(&self, event: &ContractEvent) -> Result<()> {
        let operation = || {
            let collection: Collection<StoredEvent> = self.database.collection("events");
            let event = event.clone();
            Box::pin(async move {
                let stored_event = StoredEvent {
                    transaction_hash: event.transaction_hash.clone(),
                    block_number: event.block_number,
                    contract_address: event.contract_address.clone(),
                    event_name: event.event_name.clone(),
                    event_signature: event.event_signature.clone(),
                    topics: event.topics.clone(),
                    data: event.data.clone(),
                    decoded_data: event.decoded_data.clone(),
                    log_index: event.log_index,
                    removed: event.removed,
                    timestamp: event.timestamp,
                    created_at: Utc::now(),
                };

                collection.insert_one(stored_event, None).await?;
                Ok(())
            })
        };

        self.error_handler.execute_with_retry_and_error_tracking(
            operation,
            ErrorType::DatabaseError,
            Some(event.block_number),
            Some(event.transaction_hash.clone()),
        ).await
    }

    pub async fn store_token_transfer(&self, transfer: &TokenTransferEvent) -> Result<()> {
        let operation = || {
            let collection: Collection<TokenTransferEvent> = self.database.collection("token_transfers");
            let transfer = transfer.clone();
            Box::pin(async move {
                collection.insert_one(transfer, None).await?;
                Ok(())
            })
        };

        self.error_handler.execute_with_retry_and_error_tracking(
            operation,
            ErrorType::DatabaseError,
            Some(transfer.block_number),
            Some(transfer.transaction_hash.clone()),
        ).await
    }

    pub async fn store_defi_event(&self, event: &DeFiEvent) -> Result<()> {
        let operation = || {
            let collection: Collection<DeFiEvent> = self.database.collection("defi_events");
            let event = event.clone();
            Box::pin(async move {
                collection.insert_one(event, None).await?;
                Ok(())
            })
        };

        self.error_handler.execute_with_retry_and_error_tracking(
            operation,
            ErrorType::DatabaseError,
            Some(event.block_number),
            Some(event.transaction_hash.clone()),
        ).await
    }

    pub async fn store_smart_contract(&self, contract: &SmartContract) -> Result<()> {
        let operation = || {
            let collection: Collection<SmartContract> = self.database.collection("smart_contracts");
            let contract = contract.clone();
            Box::pin(async move {
                // Use upsert to avoid duplicates
                let filter = doc! { "address": &contract.address };
                let options = mongodb::options::ReplaceOptions::builder()
                    .upsert(true)
                    .build();
                
                collection.replace_one(filter, contract, Some(options)).await?;
                Ok(())
            })
        };

        self.error_handler.execute_with_retry_and_error_tracking(
            operation,
            ErrorType::DatabaseError,
            Some(contract.first_seen_block),
            None,
        ).await
    }

    pub async fn store_address_balance(&self, balance: &AddressBalance) -> Result<()> {
        let operation = || {
            let collection: Collection<AddressBalance> = self.database.collection("address_balances");
            let balance = balance.clone();
            Box::pin(async move {
                // Use upsert to update existing balance
                let filter = doc! { 
                    "address": &balance.address,
                    "token_address": balance.token_address.as_deref().unwrap_or("ETH")
                };
                let options = mongodb::options::ReplaceOptions::builder()
                    .upsert(true)
                    .build();
                
                collection.replace_one(filter, balance, Some(options)).await?;
                Ok(())
            })
        };

        self.error_handler.execute_with_retry_and_error_tracking(
            operation,
            ErrorType::DatabaseError,
            Some(balance.block_number),
            None,
        ).await
    }

    pub async fn store_sync_status(&self, status: &SyncStatus) -> Result<()> {
        let operation = || {
            let collection: Collection<SyncStatus> = self.database.collection("sync_status");
            let status = status.clone();
            Box::pin(async move {
                let filter = doc! { "service_name": &status.service_name };
                let options = mongodb::options::ReplaceOptions::builder()
                    .upsert(true)
                    .build();
                
                collection.replace_one(filter, status, Some(options)).await?;
                Ok(())
            })
        };

        self.error_handler.execute_with_retry_and_error_tracking(
            operation,
            ErrorType::DatabaseError,
            Some(status.last_processed_block),
            None,
        ).await
    }

    pub async fn store_processing_error(&self, error: &ProcessingError) -> Result<()> {
        let operation = || {
            let collection: Collection<ProcessingError> = self.database.collection("processing_errors");
            let error = error.clone();
            Box::pin(async move {
                collection.insert_one(error, None).await?;
                Ok(())
            })
        };

        self.error_handler.execute_with_retry_and_error_tracking(
            operation,
            ErrorType::DatabaseError,
            error.block_number,
            error.transaction_hash.clone(),
        ).await
    }

    pub async fn get_blocks_range(&self, start_block: u64, end_block: u64) -> Result<Vec<StoredBlock>> {
        let operation = || {
            let collection: Collection<StoredBlock> = self.database.collection("blocks");
            let start_block = start_block;
            let end_block = end_block;
            Box::pin(async move {
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
            })
        };

        self.error_handler.execute_with_retry_and_error_tracking(
            operation,
            ErrorType::DatabaseError,
            Some(start_block),
            None,
        ).await
    }

    pub async fn get_transactions_by_address(&self, address: &str, limit: i64) -> Result<Vec<StoredTransaction>> {
        let operation = || {
            let collection: Collection<StoredTransaction> = self.database.collection("transactions");
            let address = address.to_string();
            let limit = limit;
            Box::pin(async move {
                let filter = doc! {
                    "$or": [
                        { "from": &address },
                        { "to": &address }
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
            })
        };

        self.error_handler.execute_with_retry_and_error_tracking(
            operation,
            ErrorType::DatabaseError,
            None,
            None,
        ).await
    }

    pub async fn get_events_by_contract(&self, contract_address: &str, limit: i64) -> Result<Vec<StoredEvent>> {
        let operation = || {
            let collection: Collection<StoredEvent> = self.database.collection("events");
            let contract_address = contract_address.to_string();
            let limit = limit;
            Box::pin(async move {
                let filter = doc! { "contract_address": &contract_address };

                let options = mongodb::options::FindOptions::builder()
                    .sort(doc! { "timestamp": -1 })
                    .limit(limit)
                    .build();

                let mut cursor = collection.find(filter, Some(options)).await?;
                let mut events = Vec::new();

                while let Some(event) = cursor.next().await {
                    events.push(event?);
                }

                Ok(events)
            })
        };

        self.error_handler.execute_with_retry_and_error_tracking(
            operation,
            ErrorType::DatabaseError,
            None,
            None,
        ).await
    }

    pub async fn get_token_transfers_by_address(&self, address: &str, limit: i64) -> Result<Vec<TokenTransferEvent>> {
        let operation = || {
            let collection: Collection<TokenTransferEvent> = self.database.collection("token_transfers");
            let address = address.to_string();
            let limit = limit;
            Box::pin(async move {
                let filter = doc! {
                    "$or": [
                        { "from": &address },
                        { "to": &address }
                    ]
                };

                let options = mongodb::options::FindOptions::builder()
                    .sort(doc! { "timestamp": -1 })
                    .limit(limit)
                    .build();

                let mut cursor = collection.find(filter, Some(options)).await?;
                let mut transfers = Vec::new();

                while let Some(transfer) = cursor.next().await {
                    transfers.push(transfer?);
                }

                Ok(transfers)
            })
        };

        self.error_handler.execute_with_retry_and_error_tracking(
            operation,
            ErrorType::DatabaseError,
            None,
            None,
        ).await
    }

    pub async fn get_defi_events_by_protocol(&self, protocol: &str, limit: i64) -> Result<Vec<DeFiEvent>> {
        let operation = || {
            let collection: Collection<DeFiEvent> = self.database.collection("defi_events");
            let protocol = protocol.to_string();
            let limit = limit;
            Box::pin(async move {
                let filter = doc! { "protocol": &protocol };

                let options = mongodb::options::FindOptions::builder()
                    .sort(doc! { "timestamp": -1 })
                    .limit(limit)
                    .build();

                let mut cursor = collection.find(filter, Some(options)).await?;
                let mut events = Vec::new();

                while let Some(event) = cursor.next().await {
                    events.push(event?);
                }

                Ok(events)
            })
        };

        self.error_handler.execute_with_retry_and_error_tracking(
            operation,
            ErrorType::DatabaseError,
            None,
            None,
        ).await
    }

    pub async fn get_sync_status(&self, service_name: &str) -> Result<Option<SyncStatus>> {
        let operation = || {
            let collection: Collection<SyncStatus> = self.database.collection("sync_status");
            let service_name = service_name.to_string();
            Box::pin(async move {
                let filter = doc! { "service_name": &service_name };
                let status = collection.find_one(filter, None).await?;
                Ok(status)
            })
        };

        self.error_handler.execute_with_retry_and_error_tracking(
            operation,
            ErrorType::DatabaseError,
            None,
            None,
        ).await
    }

    pub async fn get_processing_errors(&self, limit: i64) -> Result<Vec<ProcessingError>> {
        let operation = || {
            let collection: Collection<ProcessingError> = self.database.collection("processing_errors");
            let limit = limit;
            Box::pin(async move {
                let options = mongodb::options::FindOptions::builder()
                    .sort(doc! { "created_at": -1 })
                    .limit(limit)
                    .build();

                let mut cursor = collection.find(None, Some(options)).await?;
                let mut errors = Vec::new();

                while let Some(error) = cursor.next().await {
                    errors.push(error?);
                }

                Ok(errors)
            })
        };

        self.error_handler.execute_with_retry_and_error_tracking(
            operation,
            ErrorType::DatabaseError,
            None,
            None,
        ).await
    }
}
