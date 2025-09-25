use chrono::{DateTime, Utc};
use serde::{Deserialize, Serialize};
use rust_decimal::Decimal;
use std::collections::HashMap;

// Enhanced Block Event with more comprehensive data
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct BlockEvent {
    pub block_number: u64,
    pub block_hash: String,
    pub parent_hash: String,
    pub timestamp: u64,
    pub transaction_count: u32,
    pub gas_used: u64,
    pub gas_limit: u64,
    pub base_fee_per_gas: Option<String>,
    pub difficulty: String,
    pub total_difficulty: String,
    pub size: u64,
    pub miner: String,
    pub extra_data: String,
    pub logs_bloom: String,
    pub mix_hash: String,
    pub nonce: String,
    pub sha3_uncles: String,
    pub state_root: String,
    pub transactions_root: String,
    pub receipts_root: String,
    pub withdrawals: Option<Vec<Withdrawal>>,
    pub created_at: DateTime<Utc>,
}

// Enhanced Transaction Event with comprehensive data
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct TransactionEvent {
    pub hash: String,
    pub block_number: u64,
    pub block_hash: String,
    pub transaction_index: u64,
    pub from: String,
    pub to: Option<String>,
    pub value: String,
    pub value_eth: Decimal,
    pub gas_price: String,
    pub gas_limit: String,
    pub gas_used: Option<String>,
    pub max_fee_per_gas: Option<String>,
    pub max_priority_fee_per_gas: Option<String>,
    pub nonce: u64,
    pub input_data: String,
    pub input_data_length: usize,
    pub is_contract_creation: bool,
    pub is_contract_interaction: bool,
    pub transaction_type: u8,
    pub access_list: Option<Vec<AccessListEntry>>,
    pub chain_id: Option<u64>,
    pub v: String,
    pub r: String,
    pub s: String,
    pub timestamp: u64,
    pub created_at: DateTime<Utc>,
}

// Enhanced Contract Event with decoded data
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ContractEvent {
    pub transaction_hash: String,
    pub block_number: u64,
    pub contract_address: String,
    pub event_name: Option<String>,
    pub event_signature: Option<String>,
    pub topics: Vec<String>,
    pub data: String,
    pub decoded_data: Option<HashMap<String, serde_json::Value>>,
    pub log_index: u64,
    pub removed: bool,
    pub timestamp: u64,
    pub created_at: DateTime<Utc>,
}

// Transaction Receipt with comprehensive data
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct TransactionReceipt {
    pub transaction_hash: String,
    pub block_number: u64,
    pub block_hash: String,
    pub transaction_index: u64,
    pub from: String,
    pub to: Option<String>,
    pub gas_used: String,
    pub effective_gas_price: String,
    pub contract_address: Option<String>,
    pub logs: Vec<ContractEvent>,
    pub logs_bloom: String,
    pub status: Option<u64>, // 1 for success, 0 for failure
    pub root: Option<String>,
    pub cumulative_gas_used: String,
    pub created_at: DateTime<Utc>,
}

// Enhanced Stored Block with comprehensive Ethereum data
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct StoredBlock {
    pub block_number: u64,
    pub block_hash: String,
    pub parent_hash: String,
    pub timestamp: u64,
    pub gas_limit: u64,
    pub gas_used: u64,
    pub base_fee_per_gas: Option<String>,
    pub difficulty: String,
    pub total_difficulty: String,
    pub size: u64,
    pub transaction_count: u32,
    pub miner: String,
    pub extra_data: String,
    pub logs_bloom: String,
    pub mix_hash: String,
    pub nonce: String,
    pub sha3_uncles: String,
    pub state_root: String,
    pub transactions_root: String,
    pub receipts_root: String,
    pub withdrawals: Option<Vec<Withdrawal>>,
    pub created_at: DateTime<Utc>,
}

// Enhanced Stored Transaction with comprehensive data
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct StoredTransaction {
    pub hash: String,
    pub block_number: u64,
    pub block_hash: String,
    pub transaction_index: u64,
    pub from: String,
    pub to: Option<String>,
    pub value: String,
    pub value_eth: Decimal,
    pub gas_price: String,
    pub gas_limit: String,
    pub gas_used: Option<String>,
    pub max_fee_per_gas: Option<String>,
    pub max_priority_fee_per_gas: Option<String>,
    pub nonce: u64,
    pub input_data: String,
    pub input_data_length: usize,
    pub is_contract_creation: bool,
    pub is_contract_interaction: bool,
    pub transaction_type: u8,
    pub access_list: Option<Vec<AccessListEntry>>,
    pub chain_id: Option<u64>,
    pub v: String,
    pub r: String,
    pub s: String,
    pub timestamp: u64,
    pub created_at: DateTime<Utc>,
}

// Enhanced Stored Event with decoded data
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct StoredEvent {
    pub transaction_hash: String,
    pub block_number: u64,
    pub contract_address: String,
    pub event_name: Option<String>,
    pub event_signature: Option<String>,
    pub topics: Vec<String>,
    pub data: String,
    pub decoded_data: Option<HashMap<String, serde_json::Value>>,
    pub log_index: u64,
    pub removed: bool,
    pub timestamp: u64,
    pub created_at: DateTime<Utc>,
}

// Supporting data structures
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Withdrawal {
    pub index: u64,
    pub validator_index: u64,
    pub address: String,
    pub amount: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct AccessListEntry {
    pub address: String,
    pub storage_keys: Vec<String>,
}

// Token Transfer Event (ERC-20/ERC-721/ERC-1155)
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct TokenTransferEvent {
    pub transaction_hash: String,
    pub block_number: u64,
    pub contract_address: String,
    pub token_type: TokenType,
    pub from: String,
    pub to: String,
    pub value: Option<String>, // For ERC-20
    pub token_id: Option<String>, // For ERC-721/ERC-1155
    pub amount: Option<String>, // For ERC-1155
    pub timestamp: u64,
    pub created_at: DateTime<Utc>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub enum TokenType {
    ERC20,
    ERC721,
    ERC1155,
}

// DeFi Protocol Events
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct DeFiEvent {
    pub transaction_hash: String,
    pub block_number: u64,
    pub protocol: String,
    pub event_type: DeFiEventType,
    pub user: String,
    pub amount: Option<String>,
    pub token: Option<String>,
    pub pool: Option<String>,
    pub timestamp: u64,
    pub created_at: DateTime<Utc>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub enum DeFiEventType {
    Swap,
    LiquidityAdd,
    LiquidityRemove,
    Lending,
    Borrowing,
    Repayment,
    Liquidation,
    Staking,
    Unstaking,
    RewardClaim,
}

// Address Balance Tracking
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct AddressBalance {
    pub address: String,
    pub token_address: Option<String>, // None for ETH
    pub balance: String,
    pub balance_eth: Decimal,
    pub block_number: u64,
    pub timestamp: u64,
    pub created_at: DateTime<Utc>,
}

// Smart Contract Information
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct SmartContract {
    pub address: String,
    pub name: Option<String>,
    pub symbol: Option<String>,
    pub decimals: Option<u8>,
    pub contract_type: ContractType,
    pub is_verified: bool,
    pub abi: Option<String>,
    pub source_code: Option<String>,
    pub compiler_version: Option<String>,
    pub first_seen_block: u64,
    pub created_at: DateTime<Utc>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub enum ContractType {
    Token,
    DEX,
    Lending,
    Staking,
    Governance,
    NFT,
    Other,
}

// Sync Status and Health Monitoring
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct SyncStatus {
    pub service_name: String,
    pub last_processed_block: u64,
    pub latest_block: u64,
    pub sync_lag: u64,
    pub is_syncing: bool,
    pub last_sync_time: DateTime<Utc>,
    pub total_blocks_processed: u64,
    pub total_transactions_processed: u64,
    pub total_events_processed: u64,
    pub error_count: u64,
    pub uptime_seconds: u64,
}

// Error and Retry Information
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ProcessingError {
    pub error_id: String,
    pub error_type: String,
    pub block_number: Option<u64>,
    pub transaction_hash: Option<String>,
    pub error_message: String,
    pub retry_count: u32,
    pub last_retry: DateTime<Utc>,
    pub created_at: DateTime<Utc>,
}
