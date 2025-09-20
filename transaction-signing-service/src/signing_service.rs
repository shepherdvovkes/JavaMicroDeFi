use anyhow::Result;
use chrono::Utc;
use log::{error, info};
use std::collections::HashMap;
use std::sync::Arc;
use tokio::sync::RwLock;
use uuid::Uuid;
use web3::types::{TransactionParameters, Address, U256};

use crate::crypto::CryptoService;
use crate::kafka_consumer::KafkaConsumerService;
use crate::models::*;

#[derive(Clone)]
pub struct TransactionSigningService {
    crypto_service: CryptoService,
    kafka_consumer: KafkaConsumerService,
    wallets: Arc<RwLock<HashMap<String, EncryptedWallet>>>,
}

impl TransactionSigningService {
    pub async fn new(kafka_brokers: &str) -> Result<Self> {
        let crypto_service = CryptoService::new();
        let kafka_consumer = KafkaConsumerService::new(kafka_brokers, "transaction-signing-group")?;
        let wallets = Arc::new(RwLock::new(HashMap::new()));

        Ok(Self {
            crypto_service,
            kafka_consumer,
            wallets,
        })
    }

    pub async fn start_consumer(&self) -> Result<()> {
        self.kafka_consumer.subscribe_to_signing_requests().await?;
        
        let service = self.clone();
        self.kafka_consumer.start_consuming(move |task| {
            tokio::runtime::Handle::current().block_on(async {
                service.process_signing_task(task).await
            })
        }).await
    }

    pub async fn create_wallet(&self, request: CreateWalletRequest) -> Result<CreateWalletResponse> {
        let private_key = self.crypto_service.generate_private_key()?;
        let address = self.crypto_service.private_key_to_address(&private_key)?;
        
        let (encrypted_private_key, salt) = self.crypto_service.encrypt_private_key(&private_key, &request.password)?;
        
        let wallet_id = Uuid::new_v4().to_string();
        let encrypted_wallet = EncryptedWallet {
            wallet_id: wallet_id.clone(),
            name: request.name,
            address: format!("{:?}", address),
            encrypted_private_key,
            salt,
            created_at: Utc::now(),
            last_used: None,
        };

        // Store wallet in memory (in production, this should be persisted to a secure database)
        let mut wallets = self.wallets.write().await;
        wallets.insert(wallet_id.clone(), encrypted_wallet);

        // Generate mnemonic (simplified - in production use proper BIP39 implementation)
        let mnemonic = self.generate_mnemonic(&private_key)?;

        info!("Created new wallet: {} with address: {:?}", wallet_id, address);

        Ok(CreateWalletResponse {
            wallet_id,
            address: format!("{:?}", address),
            mnemonic,
        })
    }

    pub async fn import_wallet(&self, request: ImportWalletRequest) -> Result<ImportWalletResponse> {
        let private_key = if let Some(pk_hex) = request.private_key {
            // Import from private key
            let pk_bytes = hex::decode(pk_hex.trim_start_matches("0x"))?;
            secp256k1::SecretKey::from_slice(&pk_bytes)?
        } else if let Some(_mnemonic) = request.mnemonic {
            // Import from mnemonic (simplified implementation)
            return Err(anyhow::anyhow!("Mnemonic import not implemented yet"));
        } else {
            return Err(anyhow::anyhow!("Either private_key or mnemonic must be provided"));
        };

        let address = self.crypto_service.private_key_to_address(&private_key)?;
        let (encrypted_private_key, salt) = self.crypto_service.encrypt_private_key(&private_key, &request.password)?;
        
        let wallet_id = Uuid::new_v4().to_string();
        let encrypted_wallet = EncryptedWallet {
            wallet_id: wallet_id.clone(),
            name: request.name,
            address: format!("{:?}", address),
            encrypted_private_key,
            salt,
            created_at: Utc::now(),
            last_used: None,
        };

        let mut wallets = self.wallets.write().await;
        wallets.insert(wallet_id.clone(), encrypted_wallet);

        info!("Imported wallet: {} with address: {:?}", wallet_id, address);

        Ok(ImportWalletResponse {
            wallet_id,
            address: format!("{:?}", address),
        })
    }

    pub async fn sign_transaction(&self, request: SignTransactionRequest) -> Result<SignTransactionResponse> {
        // Get wallet
        let wallets = self.wallets.read().await;
        let wallet = wallets.get(&request.wallet_id)
            .ok_or_else(|| anyhow::anyhow!("Wallet not found: {}", request.wallet_id))?;

        // For this example, we'll use a dummy password. In production, this should come from secure storage
        let password = "dummy_password"; // This should be securely retrieved
        
        let private_key = self.crypto_service.decrypt_private_key(
            &wallet.encrypted_private_key,
            password,
            &wallet.salt,
        )?;

        // Create transaction parameters
        let transaction = TransactionParameters {
            to: Some(request.to.parse()?),
            value: Some(U256::from_dec_str(&request.value)?),
            gas: U256::from_dec_str(&request.gas_limit)?,
            gas_price: Some(U256::from_dec_str(&request.gas_price)?),
            nonce: Some(U256::from(request.nonce)),
            data: request.data.map(|d| hex::decode(d.trim_start_matches("0x")).unwrap_or_default()).unwrap_or_default(),
            transaction_type: None,
            access_list: None,
            max_fee_per_gas: None,
            max_priority_fee_per_gas: None,
        };

        // Sign transaction
        let (signature_bytes, v) = self.crypto_service.sign_transaction(&private_key, &transaction, request.chain_id)?;
        
        // Extract r and s from signature
        let r = &signature_bytes[..32];
        let s = &signature_bytes[32..];

        let signature = TransactionSignature {
            r: hex::encode(r),
            s: hex::encode(s),
            v,
        };

        // Encode signed transaction
        let signed_transaction = self.encode_signed_transaction(&transaction, &signature, request.chain_id)?;
        let transaction_hash = self.calculate_transaction_hash(&signed_transaction)?;

        info!("Signed transaction for wallet: {}", request.wallet_id);

        Ok(SignTransactionResponse {
            transaction_hash: hex::encode(transaction_hash),
            signed_transaction: hex::encode(signed_transaction),
            signature,
        })
    }

    async fn process_signing_task(&self, task: TransactionSigningTask) -> Result<SigningResult> {
        info!("Processing signing task: {}", task.task_id);

        match self.sign_transaction(task.transaction_data).await {
            Ok(response) => {
                Ok(SigningResult {
                    task_id: task.task_id,
                    success: true,
                    signed_transaction: Some(response.signed_transaction),
                    transaction_hash: Some(response.transaction_hash),
                    error: None,
                    processed_at: Utc::now(),
                })
            }
            Err(e) => {
                error!("Failed to process signing task {}: {}", task.task_id, e);
                Ok(SigningResult {
                    task_id: task.task_id,
                    success: false,
                    signed_transaction: None,
                    transaction_hash: None,
                    error: Some(e.to_string()),
                    processed_at: Utc::now(),
                })
            }
        }
    }

    fn generate_mnemonic(&self, _private_key: &secp256k1::SecretKey) -> Result<String> {
        // Simplified mnemonic generation - in production use proper BIP39
        let words = vec![
            "abandon", "ability", "able", "about", "above", "absent", "absorb", "abstract",
            "absurd", "abuse", "access", "accident", "account", "accuse", "achieve", "acid",
        ];
        
        let mut mnemonic = String::new();
        for i in 0..12 {
            if i > 0 {
                mnemonic.push(' ');
            }
            mnemonic.push_str(words[i % words.len()]);
        }
        
        Ok(mnemonic)
    }

    fn encode_signed_transaction(&self, transaction: &TransactionParameters, signature: &TransactionSignature, chain_id: u64) -> Result<Vec<u8>> {
        use rlp::RlpStream;
        
        let mut stream = RlpStream::new();
        stream.begin_list(9);
        
        stream.append(&transaction.nonce.unwrap_or_default());
        stream.append(&transaction.gas_price.unwrap_or_default());
        stream.append(&transaction.gas);
        
        match transaction.to {
            Some(to) => stream.append(&to),
            None => stream.append_empty_data(),
        };
        
        stream.append(&transaction.value.unwrap_or_default());
        stream.append(&transaction.data.as_slice());
        
        // Add signature components
        stream.append(&U256::from(signature.v));
        stream.append(&U256::from_big_endian(&hex::decode(&signature.r)?));
        stream.append(&U256::from_big_endian(&hex::decode(&signature.s)?));
        
        Ok(stream.out().to_vec())
    }

    fn calculate_transaction_hash(&self, signed_transaction: &[u8]) -> Result<[u8; 32]> {
        Ok(self.crypto_service.keccak256(signed_transaction))
    }
}
