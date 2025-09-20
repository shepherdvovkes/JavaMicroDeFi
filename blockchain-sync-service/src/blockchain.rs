use anyhow::Result;
use web3::transports::Http;
use web3::types::{Block, BlockId, BlockNumber, Transaction, TransactionReceipt, H256};
use web3::Web3;

#[derive(Clone)]
pub struct BlockchainClient {
    web3: Web3<Http>,
}

impl BlockchainClient {
    pub async fn new(rpc_url: &str) -> Result<Self> {
        let transport = Http::new(rpc_url)?;
        let web3 = Web3::new(transport);
        
        Ok(Self { web3 })
    }

    pub async fn get_latest_block_number(&self) -> Result<u64> {
        let block_number = self.web3.eth().block_number().await?;
        Ok(block_number.as_u64())
    }

    pub async fn get_block_with_transactions(&self, block_number: u64) -> Result<Block<Transaction>> {
        let block_id = BlockId::Number(BlockNumber::Number(block_number.into()));
        
        match self.web3.eth().block_with_txs(block_id).await? {
            Some(block) => Ok(block),
            None => Err(anyhow::anyhow!("Block {} not found", block_number)),
        }
    }

    pub async fn get_transaction_receipt(&self, tx_hash: &H256) -> Result<Option<TransactionReceipt>> {
        let receipt = self.web3.eth().transaction_receipt(*tx_hash).await?;
        Ok(receipt)
    }

    pub async fn get_balance(&self, address: &str) -> Result<String> {
        let address = address.parse()?;
        let balance = self.web3.eth().balance(address, None).await?;
        Ok(balance.to_string())
    }

    pub async fn estimate_gas(&self, from: &str, to: &str, value: &str) -> Result<u64> {
        use web3::types::{CallRequest, Address};
        
        let from_addr: Address = from.parse()?;
        let to_addr: Address = to.parse()?;
        let value_wei = value.parse()?;
        
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
        
        let gas_estimate = self.web3.eth().estimate_gas(call_request, None).await?;
        Ok(gas_estimate.as_u64())
    }
}
