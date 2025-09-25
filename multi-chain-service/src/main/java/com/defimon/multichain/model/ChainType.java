package com.defimon.multichain.model;

/**
 * Enumeration of blockchain types supported by the multi-chain service.
 * 
 * This enum helps categorize different blockchain architectures and
 * enables type-safe handling of chain-specific operations.
 */
public enum ChainType {
    
    /**
     * Ethereum Virtual Machine (EVM) compatible chains.
     * These chains support smart contracts and use account-based models.
     * Examples: Ethereum, Polygon, BSC, Arbitrum, Avalanche
     */
    EVM("Ethereum Virtual Machine"),
    
    /**
     * Unspent Transaction Output (UTXO) based chains.
     * These chains use a different transaction model without accounts.
     * Examples: Bitcoin, Litecoin, Bitcoin Cash
     */
    UTXO("Unspent Transaction Output"),
    
    /**
     * Account-based model chains that are not EVM compatible.
     * These chains use accounts but have different execution environments.
     * Examples: Solana, Cardano, Polkadot
     */
    ACCOUNT_MODEL("Account-based Model"),
    
    /**
     * Directed Acyclic Graph (DAG) based chains.
     * These chains use a different data structure for transactions.
     * Examples: IOTA, Nano
     */
    DAG("Directed Acyclic Graph");
    
    private final String description;
    
    ChainType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Checks if this chain type supports smart contracts.
     * 
     * @return true if smart contracts are supported
     */
    public boolean supportsSmartContracts() {
        return this == EVM || this == ACCOUNT_MODEL;
    }
    
    /**
     * Checks if this chain type uses an account-based model.
     * 
     * @return true if accounts are used instead of UTXO
     */
    public boolean isAccountBased() {
        return this == EVM || this == ACCOUNT_MODEL;
    }
    
    /**
     * Checks if this chain type is EVM compatible.
     * 
     * @return true if the chain can run Ethereum smart contracts
     */
    public boolean isEVMCompatible() {
        return this == EVM;
    }
}
