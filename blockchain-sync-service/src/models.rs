use chrono::{DateTime, Utc};
use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct BlockEvent {
    pub block_number: u64,
    pub block_hash: String,
    pub timestamp: u64,
    pub transaction_count: u32,
    pub gas_used: u64,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct TransactionEvent {
    pub hash: String,
    pub block_number: u64,
    pub from: String,
    pub to: Option<String>,
    pub value: String,
    pub gas_price: String,
    pub gas_used: String,
    pub timestamp: u64,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ContractEvent {
    pub transaction_hash: String,
    pub block_number: u64,
    pub contract_address: String,
    pub topics: Vec<String>,
    pub data: String,
    pub timestamp: u64,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct StoredBlock {
    pub block_number: u64,
    pub block_hash: String,
    pub parent_hash: String,
    pub timestamp: u64,
    pub gas_limit: u64,
    pub gas_used: u64,
    pub difficulty: String,
    pub total_difficulty: String,
    pub size: u64,
    pub transaction_count: u32,
    pub created_at: DateTime<Utc>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct StoredTransaction {
    pub hash: String,
    pub block_number: u64,
    pub block_hash: String,
    pub transaction_index: u64,
    pub from: String,
    pub to: Option<String>,
    pub value: String,
    pub gas_price: String,
    pub gas_limit: String,
    pub gas_used: Option<String>,
    pub nonce: u64,
    pub input_data: String,
    pub timestamp: u64,
    pub created_at: DateTime<Utc>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct StoredEvent {
    pub transaction_hash: String,
    pub block_number: u64,
    pub contract_address: String,
    pub event_name: Option<String>,
    pub topics: Vec<String>,
    pub data: String,
    pub timestamp: u64,
    pub created_at: DateTime<Utc>,
}
