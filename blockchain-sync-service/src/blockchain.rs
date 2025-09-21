use anyhow::Result;
use web3::transports::Http;
use web3::types::{
    Block, BlockId, BlockNumber, Transaction, TransactionReceipt, H256, H160, U256, U64,
    CallRequest, Address, FilterBuilder, Log, BlockHeader, Uncle, SyncState, PeerInfo,
    TransactionRequest, Bytes, BlockNumber as Web3BlockNumber
};
use web3::Web3;
use std::collections::HashMap;
use chrono::{DateTime, Utc};

use crate::error_handler::{ErrorHandler, CircuitBreaker, HealthMonitor};
use crate::metrics::BlockchainMetrics;
use std::sync::Arc;

#[derive(Clone)]
pub struct BlockchainClient {
    web3: Web3<Http>,
    error_handler: ErrorHandler,
    circuit_breaker: CircuitBreaker,
    health_monitor: HealthMonitor,
    metrics: Arc<BlockchainMetrics>,
    rpc_url: String,
}

impl BlockchainClient {
    pub async fn new(rpc_url: &str, metrics: Arc<BlockchainMetrics>) -> Result<Self> {
        let transport = Http::new(rpc_url)?;
        let web3 = Web3::new(transport);
        
        let error_handler = ErrorHandler::new()
            .with_retries(3)
            .with_base_delay(std::time::Duration::from_secs(1))
            .with_max_delay(std::time::Duration::from_secs(30));
        
        let circuit_breaker = CircuitBreaker::new(
            5, // failure threshold
            std::time::Duration::from_secs(60), // recovery timeout
        );
        
        let health_monitor = HealthMonitor::new();
        
        Ok(Self { 
            web3,
            error_handler,
            circuit_breaker,
            health_monitor,
            metrics,
            rpc_url: rpc_url.to_string(),
        })
    }

    pub async fn get_latest_block_number(&self) -> Result<u64> {
        let start_time = std::time::Instant::now();
        let operation = || {
            let web3 = self.web3.clone();
            Box::pin(async move {
                let block_number = web3.eth().block_number().await?;
                Ok(block_number.as_u64())
            })
        };

        let result = self.error_handler.execute_with_retry_and_error_tracking(
            operation,
            crate::error_handler::ErrorType::RpcConnection,
            None,
            None,
        ).await;
        
        // Record metrics
        let duration = start_time.elapsed().as_secs_f64();
        let success = result.is_ok();
        self.metrics.record_eth_rpc_request("eth_blockNumber", "ethereum", duration, success);
        
        if let Err(ref e) = result {
            self.metrics.record_eth_rpc_error("rpc_error", "eth_blockNumber", "ethereum");
        }
        
        result
    }

    pub async fn get_block_with_transactions(&self, block_number: u64) -> Result<Block<Transaction>> {
        let operation = || {
            let web3 = self.web3.clone();
            let block_number = block_number;
            Box::pin(async move {
                let block_id = BlockId::Number(BlockNumber::Number(block_number.into()));
                
                match web3.eth().block_with_txs(block_id).await? {
                    Some(block) => Ok(block),
                    None => Err(anyhow::anyhow!("Block {} not found", block_number)),
                }
            })
        };

        self.error_handler.execute_with_retry_and_error_tracking(
            operation,
            crate::error_handler::ErrorType::BlockNotFound,
            Some(block_number),
            None,
        ).await
    }

    pub async fn get_block_header(&self, block_number: u64) -> Result<BlockHeader> {
        let operation = || {
            let web3 = self.web3.clone();
            let block_number = block_number;
            Box::pin(async move {
                let block_id = BlockId::Number(BlockNumber::Number(block_number.into()));
                
                match web3.eth().block_header(block_id).await? {
                    Some(header) => Ok(header),
                    None => Err(anyhow::anyhow!("Block header {} not found", block_number)),
                }
            })
        };

        self.error_handler.execute_with_retry_and_error_tracking(
            operation,
            crate::error_handler::ErrorType::BlockNotFound,
            Some(block_number),
            None,
        ).await
    }

    pub async fn get_transaction_receipt(&self, tx_hash: &H256) -> Result<Option<TransactionReceipt>> {
        let operation = || {
            let web3 = self.web3.clone();
            let tx_hash = *tx_hash;
            Box::pin(async move {
                let receipt = web3.eth().transaction_receipt(tx_hash).await?;
                Ok(receipt)
            })
        };

        self.error_handler.execute_with_retry_and_error_tracking(
            operation,
            crate::error_handler::ErrorType::ReceiptNotFound,
            None,
            Some(format!("{:?}", tx_hash)),
        ).await
    }

    pub async fn get_transaction(&self, tx_hash: &H256) -> Result<Option<Transaction>> {
        let operation = || {
            let web3 = self.web3.clone();
            let tx_hash = *tx_hash;
            Box::pin(async move {
                let tx = web3.eth().transaction(tx_hash).await?;
                Ok(tx)
            })
        };

        self.error_handler.execute_with_retry_and_error_tracking(
            operation,
            crate::error_handler::ErrorType::TransactionNotFound,
            None,
            Some(format!("{:?}", tx_hash)),
        ).await
    }

    pub async fn get_balance(&self, address: &str) -> Result<String> {
        let operation = || {
            let web3 = self.web3.clone();
            let address = address.to_string();
            Box::pin(async move {
                let address: Address = address.parse()?;
                let balance = web3.eth().balance(address, None).await?;
                Ok(balance.to_string())
            })
        };

        self.error_handler.execute_with_retry_and_error_tracking(
            operation,
            crate::error_handler::ErrorType::RpcConnection,
            None,
            None,
        ).await
    }

    pub async fn get_balance_at_block(&self, address: &str, block_number: u64) -> Result<String> {
        let operation = || {
            let web3 = self.web3.clone();
            let address = address.to_string();
            let block_number = block_number;
            Box::pin(async move {
                let address: Address = address.parse()?;
                let block_id = BlockId::Number(BlockNumber::Number(block_number.into()));
                let balance = web3.eth().balance(address, Some(block_id)).await?;
                Ok(balance.to_string())
            })
        };

        self.error_handler.execute_with_retry_and_error_tracking(
            operation,
            crate::error_handler::ErrorType::RpcConnection,
            Some(block_number),
            None,
        ).await
    }

    pub async fn estimate_gas(&self, from: &str, to: &str, value: &str) -> Result<u64> {
        let operation = || {
            let web3 = self.web3.clone();
            let from = from.to_string();
            let to = to.to_string();
            let value = value.to_string();
            Box::pin(async move {
                let from_addr: Address = from.parse()?;
                let to_addr: Address = to.parse()?;
                let value_wei: U256 = value.parse()?;
                
                let call_request = CallRequest {
                    from: Some(from_addr),
                    to: Some(to_addr),
                    gas: None,
                    gas_price: None,
                    value: Some(value_wei),
                    data: None,
                    transaction_type: None,
                    access_list: None,
                    max_fee_per_gas: None,
                    max_priority_fee_per_gas: None,
                };
                
                let gas_estimate = web3.eth().estimate_gas(call_request, None).await?;
                Ok(gas_estimate.as_u64())
            })
        };

        self.error_handler.execute_with_retry_and_error_tracking(
            operation,
            crate::error_handler::ErrorType::RpcConnection,
            None,
            None,
        ).await
    }

    pub async fn get_gas_price(&self) -> Result<U256> {
        let operation = || {
            let web3 = self.web3.clone();
            Box::pin(async move {
                let gas_price = web3.eth().gas_price().await?;
                Ok(gas_price)
            })
        };

        self.error_handler.execute_with_retry_and_error_tracking(
            operation,
            crate::error_handler::ErrorType::RpcConnection,
            None,
            None,
        ).await
    }

    pub async fn get_block_gas_limit(&self, block_number: u64) -> Result<U256> {
        let operation = || {
            let web3 = self.web3.clone();
            let block_number = block_number;
            Box::pin(async move {
                let block_id = BlockId::Number(BlockNumber::Number(block_number.into()));
                let block = web3.eth().block(block_id).await?;
                match block {
                    Some(block) => Ok(block.gas_limit),
                    None => Err(anyhow::anyhow!("Block {} not found", block_number)),
                }
            })
        };

        self.error_handler.execute_with_retry_and_error_tracking(
            operation,
            crate::error_handler::ErrorType::BlockNotFound,
            Some(block_number),
            None,
        ).await
    }

    pub async fn get_logs(&self, from_block: u64, to_block: u64, addresses: Vec<H160>, topics: Vec<H256>) -> Result<Vec<Log>> {
        let operation = || {
            let web3 = self.web3.clone();
            let from_block = from_block;
            let to_block = to_block;
            let addresses = addresses.clone();
            let topics = topics.clone();
            Box::pin(async move {
                let filter = FilterBuilder::default()
                    .from_block(Web3BlockNumber::Number(from_block.into()))
                    .to_block(Web3BlockNumber::Number(to_block.into()))
                    .address(addresses)
                    .topics(Some(topics), None, None, None)
                    .build();
                
                let logs = web3.eth().logs(filter).await?;
                Ok(logs)
            })
        };

        self.error_handler.execute_with_retry_and_error_tracking(
            operation,
            crate::error_handler::ErrorType::RpcConnection,
            Some(from_block),
            None,
        ).await
    }

    pub async fn get_sync_status(&self) -> Result<SyncState> {
        let operation = || {
            let web3 = self.web3.clone();
            Box::pin(async move {
                let sync_state = web3.eth().syncing().await?;
                Ok(sync_state)
            })
        };

        self.error_handler.execute_with_retry_and_error_tracking(
            operation,
            crate::error_handler::ErrorType::RpcConnection,
            None,
            None,
        ).await
    }

    pub async fn get_peer_count(&self) -> Result<U64> {
        let operation = || {
            let web3 = self.web3.clone();
            Box::pin(async move {
                let peer_count = web3.net().peer_count().await?;
                Ok(peer_count)
            })
        };

        self.error_handler.execute_with_retry_and_error_tracking(
            operation,
            crate::error_handler::ErrorType::RpcConnection,
            None,
            None,
        ).await
    }

    pub async fn get_chain_id(&self) -> Result<u64> {
        let operation = || {
            let web3 = self.web3.clone();
            Box::pin(async move {
                let chain_id = web3.eth().chain_id().await?;
                Ok(chain_id.as_u64())
            })
        };

        self.error_handler.execute_with_retry_and_error_tracking(
            operation,
            crate::error_handler::ErrorType::RpcConnection,
            None,
            None,
        ).await
    }

    pub async fn get_network_id(&self) -> Result<u64> {
        let operation = || {
            let web3 = self.web3.clone();
            Box::pin(async move {
                let network_id = web3.net().version().await?;
                Ok(network_id.parse()?)
            })
        };

        self.error_handler.execute_with_retry_and_error_tracking(
            operation,
            crate::error_handler::ErrorType::RpcConnection,
            None,
            None,
        ).await
    }

    pub async fn get_code(&self, address: &str, block_number: Option<u64>) -> Result<Bytes> {
        let operation = || {
            let web3 = self.web3.clone();
            let address = address.to_string();
            let block_number = block_number;
            Box::pin(async move {
                let address: Address = address.parse()?;
                let block_id = block_number.map(|bn| BlockId::Number(BlockNumber::Number(bn.into())));
                let code = web3.eth().code(address, block_id).await?;
                Ok(code)
            })
        };

        self.error_handler.execute_with_retry_and_error_tracking(
            operation,
            crate::error_handler::ErrorType::RpcConnection,
            block_number,
            None,
        ).await
    }

    pub async fn get_storage_at(&self, address: &str, position: H256, block_number: Option<u64>) -> Result<H256> {
        let operation = || {
            let web3 = self.web3.clone();
            let address = address.to_string();
            let position = position;
            let block_number = block_number;
            Box::pin(async move {
                let address: Address = address.parse()?;
                let block_id = block_number.map(|bn| BlockId::Number(BlockNumber::Number(bn.into())));
                let storage = web3.eth().storage(address, position, block_id).await?;
                Ok(storage)
            })
        };

        self.error_handler.execute_with_retry_and_error_tracking(
            operation,
            crate::error_handler::ErrorType::RpcConnection,
            block_number,
            None,
        ).await
    }

    pub async fn get_transaction_count(&self, address: &str, block_number: Option<u64>) -> Result<U256> {
        let operation = || {
            let web3 = self.web3.clone();
            let address = address.to_string();
            let block_number = block_number;
            Box::pin(async move {
                let address: Address = address.parse()?;
                let block_id = block_number.map(|bn| BlockId::Number(BlockNumber::Number(bn.into())));
                let count = web3.eth().transaction_count(address, block_id).await?;
                Ok(count)
            })
        };

        self.error_handler.execute_with_retry_and_error_tracking(
            operation,
            crate::error_handler::ErrorType::RpcConnection,
            block_number,
            None,
        ).await
    }

    pub async fn get_uncle_count(&self, block_number: u64) -> Result<U256> {
        let operation = || {
            let web3 = self.web3.clone();
            let block_number = block_number;
            Box::pin(async move {
                let block_id = BlockId::Number(BlockNumber::Number(block_number.into()));
                let uncle_count = web3.eth().uncle_count(block_id).await?;
                Ok(uncle_count)
            })
        };

        self.error_handler.execute_with_retry_and_error_tracking(
            operation,
            crate::error_handler::ErrorType::BlockNotFound,
            Some(block_number),
            None,
        ).await
    }

    pub async fn get_uncle(&self, block_number: u64, uncle_index: u64) -> Result<Option<Uncle>> {
        let operation = || {
            let web3 = self.web3.clone();
            let block_number = block_number;
            let uncle_index = uncle_index;
            Box::pin(async move {
                let block_id = BlockId::Number(BlockNumber::Number(block_number.into()));
                let uncle = web3.eth().uncle(block_id, uncle_index.into()).await?;
                Ok(uncle)
            })
        };

        self.error_handler.execute_with_retry_and_error_tracking(
            operation,
            crate::error_handler::ErrorType::BlockNotFound,
            Some(block_number),
            None,
        ).await
    }

    pub fn get_health_status(&self) -> (bool, f64, std::time::Duration) {
        let (success_count, error_count, error_rate, uptime) = self.health_monitor.get_stats();
        let is_healthy = self.health_monitor.is_healthy();
        (is_healthy, error_rate, uptime)
    }

    pub fn get_circuit_breaker_state(&self) -> &crate::error_handler::CircuitBreakerState {
        self.circuit_breaker.state()
    }

    pub fn record_success(&mut self) {
        self.health_monitor.record_success();
    }

    pub fn record_error(&mut self) {
        self.health_monitor.record_error();
    }
}
