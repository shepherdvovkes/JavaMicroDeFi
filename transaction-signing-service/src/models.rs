use serde::{Deserialize, Serialize};
use chrono::{DateTime, Utc};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct SignTransactionRequest {
    pub wallet_id: String,
    pub to: String,
    pub value: String,
    pub gas_limit: String,
    pub gas_price: String,
    pub nonce: u64,
    pub data: Option<String>,
    pub chain_id: u64,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct SignTransactionResponse {
    pub transaction_hash: String,
    pub signed_transaction: String,
    pub signature: TransactionSignature,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct TransactionSignature {
    pub r: String,
    pub s: String,
    pub v: u64,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct CreateWalletRequest {
    pub password: String,
    pub name: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct CreateWalletResponse {
    pub wallet_id: String,
    pub address: String,
    pub mnemonic: String, // Should be returned securely and stored safely
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ImportWalletRequest {
    pub private_key: Option<String>,
    pub mnemonic: Option<String>,
    pub password: String,
    pub name: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ImportWalletResponse {
    pub wallet_id: String,
    pub address: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct TransactionSigningTask {
    pub task_id: String,
    pub wallet_id: String,
    pub transaction_data: SignTransactionRequest,
    pub created_at: DateTime<Utc>,
    pub priority: TaskPriority,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub enum TaskPriority {
    Low,
    Normal,
    High,
    Critical,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct SigningResult {
    pub task_id: String,
    pub success: bool,
    pub signed_transaction: Option<String>,
    pub transaction_hash: Option<String>,
    pub error: Option<String>,
    pub processed_at: DateTime<Utc>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct EncryptedWallet {
    pub wallet_id: String,
    pub name: Option<String>,
    pub address: String,
    pub encrypted_private_key: String,
    pub salt: String,
    pub created_at: DateTime<Utc>,
    pub last_used: Option<DateTime<Utc>>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct BatchSigningRequest {
    pub batch_id: String,
    pub transactions: Vec<SignTransactionRequest>,
    pub priority: TaskPriority,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct BatchSigningResponse {
    pub batch_id: String,
    pub results: Vec<SigningResult>,
    pub total_transactions: usize,
    pub successful_transactions: usize,
    pub failed_transactions: usize,
}
