use anyhow::Result;
use aes_gcm::{
    aead::{Aead, AeadCore, KeyInit, OsRng},
    Aes256Gcm, Nonce,
};
use argon2::{Argon2, PasswordHasher, password_hash::{rand_core::RngCore, SaltString}};
use ethereum_types::U256;
// use rand::Rng;
use secp256k1::{PublicKey, SecretKey, Secp256k1, Message, All};
use sha3::{Digest, Keccak256};
use web3::types::{Address, TransactionParameters};
use zeroize::Zeroize;

#[derive(Clone)]
pub struct CryptoService {
    secp: Secp256k1<All>,
}

impl CryptoService {
    pub fn new() -> Self {
        Self {
            secp: Secp256k1::new(),
        }
    }

    pub fn generate_private_key(&self) -> Result<SecretKey> {
        let mut rng = rand::thread_rng();
        let secret_key = SecretKey::new(&mut rng);
        Ok(secret_key)
    }

    pub fn private_key_to_address(&self, private_key: &SecretKey) -> Result<Address> {
        let public_key = PublicKey::from_secret_key(&self.secp, private_key);
        let public_key_bytes = public_key.serialize_uncompressed();
        
        // Remove the first byte (0x04) and hash the rest
        let mut hasher = Keccak256::new();
        hasher.update(&public_key_bytes[1..]);
        let hash = hasher.finalize();
        
        // Take the last 20 bytes as the address
        let mut address_bytes = [0u8; 20];
        address_bytes.copy_from_slice(&hash[12..]);
        
        Ok(Address::from(address_bytes))
    }

    pub fn sign_transaction(
        &self,
        private_key: &SecretKey,
        transaction: &TransactionParameters,
        chain_id: u64,
    ) -> Result<(Vec<u8>, u64)> {
        let encoded = self.encode_transaction(transaction, chain_id)?;
        let hash = self.keccak256(&encoded);
        
        let message = Message::from_digest_slice(&hash)?;
        let signature = self.secp.sign_ecdsa_recoverable(&message, private_key);
        let (recovery_id, signature_bytes) = signature.serialize_compact();
        
        // Calculate v value for EIP-155
        let v = recovery_id.to_i32() as u64 + chain_id * 2 + 35;
        
        Ok((signature_bytes.to_vec(), v))
    }

    pub fn encode_transaction(&self, transaction: &TransactionParameters, chain_id: u64) -> Result<Vec<u8>> {
        use rlp::RlpStream;
        
        let mut stream = RlpStream::new();
        stream.begin_list(9);
        
        // Nonce
        stream.append(&U256::from(transaction.nonce.unwrap_or_default().as_u64()));
        
        // Gas price
        stream.append(&transaction.gas_price.unwrap_or_default());
        
        // Gas limit
        stream.append(&transaction.gas);
        
        // To address
        match transaction.to {
            Some(to) => stream.append(&to),
            None => stream.append_empty_data(),
        };
        
        // Value
        stream.append(&transaction.value);
        
        // Data
        stream.append(&transaction.data.0.as_slice());
        
        // Chain ID, r=0, s=0 for signing
        stream.append(&U256::from(chain_id));
        stream.append(&U256::zero());
        stream.append(&U256::zero());
        
        Ok(stream.out().to_vec())
    }

    pub fn keccak256(&self, data: &[u8]) -> [u8; 32] {
        let mut hasher = Keccak256::new();
        hasher.update(data);
        let hash = hasher.finalize();
        let mut result = [0u8; 32];
        result.copy_from_slice(&hash);
        result
    }

    pub fn encrypt_private_key(&self, private_key: &SecretKey, password: &str) -> Result<(String, String)> {
        // Generate salt
        let salt = SaltString::generate(&mut OsRng);
        
        // Derive key from password using Argon2
        let argon2 = Argon2::default();
        let password_hash = argon2.hash_password(password.as_bytes(), &salt)
            .map_err(|e| anyhow::anyhow!("Password hashing failed: {}", e))?;
        
        // Extract the hash bytes for encryption key
        let hash_binding = password_hash.hash.unwrap();
        let key_bytes = hash_binding.as_bytes();
        let key = &key_bytes[..32]; // Use first 32 bytes as AES-256 key
        
        // Encrypt private key
        let cipher = Aes256Gcm::new_from_slice(key)?;
        let nonce = Aes256Gcm::generate_nonce(&mut OsRng);
        
        let private_key_bytes = private_key.secret_bytes();
        let ciphertext = cipher.encrypt(&nonce, private_key_bytes.as_ref())
            .map_err(|e| anyhow::anyhow!("Encryption failed: {}", e))?;
        
        // Combine nonce and ciphertext
        let mut encrypted_data = nonce.to_vec();
        encrypted_data.extend_from_slice(&ciphertext);
        
        let encrypted_hex = hex::encode(encrypted_data);
        let salt_string = salt.to_string();
        
        Ok((encrypted_hex, salt_string))
    }

    pub fn decrypt_private_key(&self, encrypted_data: &str, password: &str, salt: &str) -> Result<SecretKey> {
        // Parse salt
        let salt = SaltString::from_b64(salt)
            .map_err(|e| anyhow::anyhow!("Salt parsing failed: {}", e))?;
        
        // Derive key from password
        let argon2 = Argon2::default();
        let password_hash = argon2.hash_password(password.as_bytes(), &salt)
            .map_err(|e| anyhow::anyhow!("Password hashing failed: {}", e))?;
        let hash_binding = password_hash.hash.unwrap();
        let key_bytes = hash_binding.as_bytes();
        let key = &key_bytes[..32];
        
        // Decrypt private key
        let encrypted_bytes = hex::decode(encrypted_data)?;
        let (nonce_bytes, ciphertext) = encrypted_bytes.split_at(12); // AES-GCM nonce is 12 bytes
        
        let cipher = Aes256Gcm::new_from_slice(key)?;
        let nonce = Nonce::from_slice(nonce_bytes);
        
        let mut decrypted_bytes = cipher.decrypt(nonce, ciphertext)
            .map_err(|e| anyhow::anyhow!("Decryption failed: {}", e))?;
        let private_key = SecretKey::from_slice(&decrypted_bytes)?;
        
        // Clear decrypted bytes from memory
        decrypted_bytes.zeroize();
        
        Ok(private_key)
    }

    pub fn verify_signature(
        &self,
        message: &[u8],
        signature: &[u8],
        recovery_id: u8,
        expected_address: &Address,
    ) -> Result<bool> {
        let message_hash = self.keccak256(message);
        let message = Message::from_digest_slice(&message_hash)?;
        
        let recovery_id = secp256k1::ecdsa::RecoveryId::from_i32(recovery_id as i32)?;
        let signature = secp256k1::ecdsa::RecoverableSignature::from_compact(&signature, recovery_id)?;
        
        let recovered_key = self.secp.recover_ecdsa(&message, &signature)?;
        let recovered_address = self.public_key_to_address(&recovered_key)?;
        
        Ok(recovered_address == *expected_address)
    }

    fn public_key_to_address(&self, public_key: &PublicKey) -> Result<Address> {
        let public_key_bytes = public_key.serialize_uncompressed();
        let mut hasher = Keccak256::new();
        hasher.update(&public_key_bytes[1..]);
        let hash = hasher.finalize();
        
        let mut address_bytes = [0u8; 20];
        address_bytes.copy_from_slice(&hash[12..]);
        
        Ok(Address::from(address_bytes))
    }
}
