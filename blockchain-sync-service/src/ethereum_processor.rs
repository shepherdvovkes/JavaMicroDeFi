use anyhow::Result;
use chrono::Utc;
use ethabi::{Contract, Event, Function, ParamType, Token};
use hex;
use rust_decimal::Decimal;
use sha3::{Digest, Keccak256};
use std::collections::HashMap;
use web3::types::{Block, Transaction, TransactionReceipt, Log, H160, H256, U256, U64};
use web3::Web3;

use crate::models::*;

pub struct EthereumProcessor {
    known_contracts: HashMap<String, Contract>,
    token_contracts: HashMap<String, TokenContractInfo>,
    defi_protocols: HashMap<String, DeFiProtocolInfo>,
}

#[derive(Debug, Clone)]
pub struct TokenContractInfo {
    pub address: String,
    pub name: String,
    pub symbol: String,
    pub decimals: u8,
    pub token_type: TokenType,
}

#[derive(Debug, Clone)]
pub struct DeFiProtocolInfo {
    pub name: String,
    pub contract_addresses: Vec<String>,
    pub event_signatures: HashMap<String, String>, // event signature -> event name
}

impl EthereumProcessor {
    pub fn new() -> Self {
        let mut processor = Self {
            known_contracts: HashMap::new(),
            token_contracts: HashMap::new(),
            defi_protocols: HashMap::new(),
        };
        
        // Initialize with known token contracts and DeFi protocols
        processor.initialize_known_contracts();
        processor
    }

    fn initialize_known_contracts(&mut self) {
        // ERC-20 Transfer event signature
        let transfer_signature = "Transfer(address,address,uint256)";
        let transfer_hash = format!("0x{:x}", Keccak256::digest(transfer_signature.as_bytes()));
        
        // ERC-721 Transfer event signature
        let transfer_721_signature = "Transfer(address,address,uint256)";
        let transfer_721_hash = format!("0x{:x}", Keccak256::digest(transfer_721_signature.as_bytes()));
        
        // ERC-1155 TransferSingle event signature
        let transfer_single_signature = "TransferSingle(address,address,address,uint256,uint256)";
        let transfer_single_hash = format!("0x{:x}", Keccak256::digest(transfer_single_signature.as_bytes()));

        // Initialize DeFi protocols
        let uniswap_v2 = DeFiProtocolInfo {
            name: "Uniswap V2".to_string(),
            contract_addresses: vec![
                "0x7a250d5630B4cF539739dF2C5dAcb4c659F2488D".to_string(), // Router
                "0x5C69bEe701ef814a2B6a3EDD4B1652CB9cc5aA6f".to_string(), // Factory
            ],
            event_signatures: HashMap::from([
                ("0xd78ad95fa46c994b6551d0da85fc275fe613ce37657fb8d5e3d130840159d822".to_string(), "Swap".to_string()),
                ("0x4c209b5fc8ad50758f13e2e1088ba56a560dff690a1c6fef26394f4c03821c4f".to_string(), "Mint".to_string()),
                ("0xcc16f5dbb4873280815c1ee09dbd06736cffcc184412cf7a71a0fdb75d397ca5".to_string(), "Burn".to_string()),
            ]),
        };

        let uniswap_v3 = DeFiProtocolInfo {
            name: "Uniswap V3".to_string(),
            contract_addresses: vec![
                "0xE592427A0AEce92De3Edee1F18E0157C05861564".to_string(), // Router
                "0x1F98431c8aD98523631AE4a59f267346ea31F984".to_string(), // Factory
            ],
            event_signatures: HashMap::from([
                ("0xc42079f94a6350d7e6235f29174924f928cc2ac818eb64fed8004e115fbcca67".to_string(), "Swap".to_string()),
                ("0x7a53080ba414158be7ec69b987b5fb7d07dee101fe85488f0853ae16239d0bde".to_string(), "Mint".to_string()),
                ("0x0c396cd989a39f4459b5fa1aed6a9a8dcdbc45908acfd67e028cd568da98982c".to_string(), "Burn".to_string()),
            ]),
        };

        self.defi_protocols.insert("uniswap_v2".to_string(), uniswap_v2);
        self.defi_protocols.insert("uniswap_v3".to_string(), uniswap_v3);
    }

    pub fn process_block(&self, block: &Block<Transaction>) -> Result<BlockEvent> {
        let wei_to_eth = Decimal::new(1, 18); // 1 ETH = 10^18 wei
        
        Ok(BlockEvent {
            block_number: block.number.unwrap().as_u64(),
            block_hash: format!("{:?}", block.hash.unwrap()),
            parent_hash: format!("{:?}", block.parent_hash),
            timestamp: block.timestamp.as_u64(),
            transaction_count: block.transactions.len() as u32,
            gas_used: block.gas_used.as_u64(),
            gas_limit: block.gas_limit.as_u64(),
            base_fee_per_gas: block.base_fee_per_gas.map(|fee| fee.to_string()),
            difficulty: block.difficulty.to_string(),
            total_difficulty: block.total_difficulty.map(|d| d.to_string()).unwrap_or_default(),
            size: block.size.map(|s| s.as_u64()).unwrap_or(0),
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
        })
    }

    pub fn process_transaction(&self, tx: &Transaction, block: &Block<Transaction>) -> Result<TransactionEvent> {
        let wei_to_eth = Decimal::new(1, 18);
        let value_eth = Decimal::from_str_exact(&tx.value.to_string())? / wei_to_eth;
        
        let is_contract_creation = tx.to.is_none();
        let is_contract_interaction = !tx.input.0.is_empty() && tx.to.is_some();
        
        let access_list = tx.access_list.as_ref().map(|list| {
            list.iter().map(|entry| AccessListEntry {
                address: format!("{:?}", entry.address),
                storage_keys: entry.storage_keys.iter().map(|key| format!("{:?}", key)).collect(),
            }).collect()
        });

        Ok(TransactionEvent {
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
            gas_used: None, // Will be filled from receipt
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
        })
    }

    pub fn process_transaction_receipt(&self, receipt: &TransactionReceipt, block: &Block<Transaction>) -> Result<TransactionReceipt> {
        let processed_logs: Result<Vec<ContractEvent>> = receipt.logs.iter()
            .map(|log| self.process_log(log, block))
            .collect();

        Ok(TransactionReceipt {
            transaction_hash: format!("{:?}", receipt.transaction_hash),
            block_number: block.number.unwrap().as_u64(),
            block_hash: format!("{:?}", block.hash.unwrap()),
            transaction_index: receipt.transaction_index.as_u64(),
            from: format!("{:?}", receipt.from),
            to: receipt.to.map(|addr| format!("{:?}", addr)),
            gas_used: receipt.gas_used.to_string(),
            effective_gas_price: receipt.effective_gas_price.to_string(),
            contract_address: receipt.contract_address.map(|addr| format!("{:?}", addr)),
            logs: processed_logs?,
            logs_bloom: format!("{:?}", receipt.logs_bloom),
            status: receipt.status.map(|s| s.as_u64()),
            root: receipt.root.map(|r| format!("{:?}", r)),
            cumulative_gas_used: receipt.cumulative_gas_used.to_string(),
            created_at: Utc::now(),
        })
    }

    pub fn process_log(&self, log: &Log, block: &Block<Transaction>) -> Result<ContractEvent> {
        let contract_address = format!("{:?}", log.address);
        let topics: Vec<String> = log.topics.iter().map(|topic| format!("{:?}", topic)).collect();
        
        // Try to decode the event
        let (event_name, event_signature, decoded_data) = self.decode_event(&contract_address, &topics, &log.data.0)?;
        
        // Check if this is a token transfer
        if let Some(token_transfer) = self.detect_token_transfer(&topics, &log.data.0)? {
            // Process token transfer separately
            self.process_token_transfer(&token_transfer, block)?;
        }
        
        // Check if this is a DeFi event
        if let Some(defi_event) = self.detect_defi_event(&contract_address, &topics)? {
            // Process DeFi event separately
            self.process_defi_event(&defi_event, block)?;
        }

        Ok(ContractEvent {
            transaction_hash: format!("{:?}", log.transaction_hash),
            block_number: block.number.unwrap().as_u64(),
            contract_address,
            event_name,
            event_signature,
            topics,
            data: format!("{:?}", log.data.0),
            decoded_data,
            log_index: log.log_index.as_u64(),
            removed: log.removed.unwrap_or(false),
            timestamp: block.timestamp.as_u64(),
            created_at: Utc::now(),
        })
    }

    fn decode_event(&self, contract_address: &str, topics: &[String], data: &[u8]) -> Result<(Option<String>, Option<String>, Option<HashMap<String, serde_json::Value>>)> {
        if topics.is_empty() {
            return Ok((None, None, None));
        }

        let event_signature = topics[0].clone();
        
        // Try to find the contract in known contracts
        if let Some(contract) = self.known_contracts.get(contract_address) {
            // Try to decode with known contract ABI
            for event in contract.events() {
                let signature = format!("0x{:x}", Keccak256::digest(event.signature().as_bytes()));
                if signature == event_signature {
                    // Try to decode the event data
                    if let Ok(decoded) = event.parse_log(ethabi::RawLog {
                        topics: topics.iter().skip(1).map(|t| {
                            let hex_str = t.trim_start_matches("0x");
                            H256::from_slice(&hex::decode(hex_str).unwrap_or_default())
                        }).collect(),
                        data: data.to_vec(),
                    }) {
                        let mut decoded_data = HashMap::new();
                        for (param, token) in event.inputs.iter().zip(decoded.params) {
                            decoded_data.insert(param.name.clone(), self.token_to_json_value(&token));
                        }
                        return Ok((Some(event.name.clone()), Some(event.signature().to_string()), Some(decoded_data)));
                    }
                }
            }
        }

        Ok((None, Some(event_signature), None))
    }

    fn token_to_json_value(&self, token: &Token) -> serde_json::Value {
        match token {
            Token::Address(addr) => serde_json::Value::String(format!("{:?}", addr)),
            Token::Bytes(bytes) => serde_json::Value::String(hex::encode(bytes)),
            Token::Int(int) => serde_json::Value::String(int.to_string()),
            Token::Uint(uint) => serde_json::Value::String(uint.to_string()),
            Token::Bool(b) => serde_json::Value::Bool(*b),
            Token::String(s) => serde_json::Value::String(s.clone()),
            Token::Array(tokens) => {
                let values: Vec<serde_json::Value> = tokens.iter().map(|t| self.token_to_json_value(t)).collect();
                serde_json::Value::Array(values)
            },
            Token::FixedArray(tokens) => {
                let values: Vec<serde_json::Value> = tokens.iter().map(|t| self.token_to_json_value(t)).collect();
                serde_json::Value::Array(values)
            },
            Token::Tuple(tokens) => {
                let mut map = serde_json::Map::new();
                for (i, token) in tokens.iter().enumerate() {
                    map.insert(i.to_string(), self.token_to_json_value(token));
                }
                serde_json::Value::Object(map)
            },
        }
    }

    fn detect_token_transfer(&self, topics: &[String], data: &[u8]) -> Result<Option<TokenTransferEvent>> {
        if topics.is_empty() {
            return Ok(None);
        }

        let event_signature = &topics[0];
        
        // ERC-20 Transfer event signature
        let transfer_signature = "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef";
        
        if event_signature == transfer_signature && topics.len() >= 4 {
            // This is likely an ERC-20 Transfer event
            let from = format!("0x{}", &topics[1][26..]); // Remove padding
            let to = format!("0x{}", &topics[2][26..]); // Remove padding
            let value = U256::from_big_endian(&hex::decode(&topics[3][2..]).unwrap_or_default());
            
            return Ok(Some(TokenTransferEvent {
                transaction_hash: "".to_string(), // Will be filled by caller
                block_number: 0, // Will be filled by caller
                contract_address: "".to_string(), // Will be filled by caller
                token_type: TokenType::ERC20,
                from,
                to,
                value: Some(value.to_string()),
                token_id: None,
                amount: None,
                timestamp: 0, // Will be filled by caller
                created_at: Utc::now(),
            }));
        }

        Ok(None)
    }

    fn detect_defi_event(&self, contract_address: &str, topics: &[String]) -> Result<Option<DeFiEvent>> {
        if topics.is_empty() {
            return Ok(None);
        }

        let event_signature = &topics[0];
        
        // Check against known DeFi protocols
        for (protocol_name, protocol_info) in &self.defi_protocols {
            if protocol_info.contract_addresses.contains(&contract_address.to_lowercase()) {
                if let Some(event_name) = protocol_info.event_signatures.get(event_signature) {
                    return Ok(Some(DeFiEvent {
                        transaction_hash: "".to_string(), // Will be filled by caller
                        block_number: 0, // Will be filled by caller
                        protocol: protocol_name.clone(),
                        event_type: self.map_defi_event_type(event_name),
                        user: "".to_string(), // Would need to decode from topics/data
                        amount: None,
                        token: None,
                        pool: None,
                        timestamp: 0, // Will be filled by caller
                        created_at: Utc::now(),
                    }));
                }
            }
        }

        Ok(None)
    }

    fn map_defi_event_type(&self, event_name: &str) -> DeFiEventType {
        match event_name.to_lowercase().as_str() {
            "swap" => DeFiEventType::Swap,
            "mint" => DeFiEventType::LiquidityAdd,
            "burn" => DeFiEventType::LiquidityRemove,
            "lend" | "deposit" => DeFiEventType::Lending,
            "borrow" => DeFiEventType::Borrowing,
            "repay" | "repayment" => DeFiEventType::Repayment,
            "liquidate" | "liquidation" => DeFiEventType::Liquidation,
            "stake" => DeFiEventType::Staking,
            "unstake" => DeFiEventType::Unstaking,
            "claim" | "reward" => DeFiEventType::RewardClaim,
            _ => DeFiEventType::Swap, // Default fallback
        }
    }

    fn process_token_transfer(&self, transfer: &TokenTransferEvent, block: &Block<Transaction>) -> Result<()> {
        // This would typically send the token transfer event to Kafka
        // For now, we'll just log it
        log::info!("Token transfer detected: {} -> {} ({} {})", 
            transfer.from, transfer.to, 
            transfer.value.as_ref().unwrap_or(&"0".to_string()),
            transfer.token_type);
        Ok(())
    }

    fn process_defi_event(&self, event: &DeFiEvent, block: &Block<Transaction>) -> Result<()> {
        // This would typically send the DeFi event to Kafka
        // For now, we'll just log it
        log::info!("DeFi event detected: {} {} on {}", 
            event.protocol, 
            format!("{:?}", event.event_type),
            event.transaction_hash);
        Ok(())
    }

    pub fn get_contract_type(&self, address: &str) -> ContractType {
        // Check if it's a known token contract
        if self.token_contracts.contains_key(&address.to_lowercase()) {
            return ContractType::Token;
        }

        // Check if it's a known DeFi protocol
        for protocol_info in self.defi_protocols.values() {
            if protocol_info.contract_addresses.contains(&address.to_lowercase()) {
                return match protocol_info.name.to_lowercase().as_str() {
                    name if name.contains("uniswap") || name.contains("sushiswap") => ContractType::DEX,
                    name if name.contains("aave") || name.contains("compound") => ContractType::Lending,
                    name if name.contains("stake") => ContractType::Staking,
                    _ => ContractType::Other,
                };
            }
        }

        ContractType::Other
    }

    pub fn add_known_contract(&mut self, address: String, contract: Contract) {
        self.known_contracts.insert(address, contract);
    }

    pub fn add_token_contract(&mut self, address: String, info: TokenContractInfo) {
        self.token_contracts.insert(address, info);
    }
}
