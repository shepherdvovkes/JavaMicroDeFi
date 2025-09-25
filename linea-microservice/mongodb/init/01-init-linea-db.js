// MongoDB Initialization Script for Linea Blockchain Data
// This script creates the necessary collections and indexes for optimal performance

// Switch to the Linea blockchain database
db = db.getSiblingDB('linea_blockchain');

// Create collections with proper indexes
print('Creating Linea blockchain collections...');

// Blocks collection
db.createCollection('blocks', {
  validator: {
    $jsonSchema: {
      bsonType: 'object',
      required: ['number', 'hash', 'timestamp', 'parentHash'],
      properties: {
        number: { bsonType: 'long' },
        hash: { bsonType: 'string' },
        timestamp: { bsonType: 'long' },
        parentHash: { bsonType: 'string' },
        gasLimit: { bsonType: 'long' },
        gasUsed: { bsonType: 'long' },
        size: { bsonType: 'long' },
        difficulty: { bsonType: 'string' },
        totalDifficulty: { bsonType: 'string' },
        nonce: { bsonType: 'string' },
        extraData: { bsonType: 'string' },
        miner: { bsonType: 'string' },
        mixHash: { bsonType: 'string' },
        receiptsRoot: { bsonType: 'string' },
        sha3Uncles: { bsonType: 'string' },
        stateRoot: { bsonType: 'string' },
        transactionsRoot: { bsonType: 'string' },
        uncles: { bsonType: 'array' },
        transactions: { bsonType: 'array' },
        createdAt: { bsonType: 'date' },
        updatedAt: { bsonType: 'date' }
      }
    }
  }
});

// Transactions collection
db.createCollection('transactions', {
  validator: {
    $jsonSchema: {
      bsonType: 'object',
      required: ['hash', 'blockNumber', 'blockHash', 'from', 'to'],
      properties: {
        hash: { bsonType: 'string' },
        blockNumber: { bsonType: 'long' },
        blockHash: { bsonType: 'string' },
        transactionIndex: { bsonType: 'long' },
        from: { bsonType: 'string' },
        to: { bsonType: 'string' },
        value: { bsonType: 'string' },
        gas: { bsonType: 'long' },
        gasPrice: { bsonType: 'string' },
        nonce: { bsonType: 'long' },
        input: { bsonType: 'string' },
        v: { bsonType: 'string' },
        r: { bsonType: 'string' },
        s: { bsonType: 'string' },
        type: { bsonType: 'string' },
        accessList: { bsonType: 'array' },
        chainId: { bsonType: 'string' },
        maxFeePerGas: { bsonType: 'string' },
        maxPriorityFeePerGas: { bsonType: 'string' },
        createdAt: { bsonType: 'date' },
        updatedAt: { bsonType: 'date' }
      }
    }
  }
});

// Accounts collection
db.createCollection('accounts', {
  validator: {
    $jsonSchema: {
      bsonType: 'object',
      required: ['address', 'balance', 'nonce'],
      properties: {
        address: { bsonType: 'string' },
        balance: { bsonType: 'string' },
        nonce: { bsonType: 'long' },
        code: { bsonType: 'string' },
        storage: { bsonType: 'object' },
        isContract: { bsonType: 'bool' },
        createdAt: { bsonType: 'date' },
        updatedAt: { bsonType: 'date' }
      }
    }
  }
});

// Network metrics collection
db.createCollection('network_metrics', {
  validator: {
    $jsonSchema: {
      bsonType: 'object',
      required: ['timestamp', 'blockNumber', 'tps'],
      properties: {
        timestamp: { bsonType: 'date' },
        blockNumber: { bsonType: 'long' },
        tps: { bsonType: 'double' },
        gasUtilization: { bsonType: 'double' },
        gasPrice: { bsonType: 'string' },
        activeAddresses: { bsonType: 'long' },
        totalTransactions: { bsonType: 'long' },
        totalBlocks: { bsonType: 'long' },
        averageBlockTime: { bsonType: 'double' },
        networkHashrate: { bsonType: 'string' },
        difficulty: { bsonType: 'string' },
        createdAt: { bsonType: 'date' }
      }
    }
  }
});

print('Creating indexes for optimal performance...');

// Blocks indexes
db.blocks.createIndex({ "number": 1 }, { unique: true, name: "idx_blocks_number" });
db.blocks.createIndex({ "hash": 1 }, { unique: true, name: "idx_blocks_hash" });
db.blocks.createIndex({ "timestamp": 1 }, { name: "idx_blocks_timestamp" });
db.blocks.createIndex({ "miner": 1 }, { name: "idx_blocks_miner" });
db.blocks.createIndex({ "number": 1, "timestamp": 1 }, { name: "idx_blocks_number_timestamp" });

// Transactions indexes
db.transactions.createIndex({ "hash": 1 }, { unique: true, name: "idx_transactions_hash" });
db.transactions.createIndex({ "blockNumber": 1 }, { name: "idx_transactions_block_number" });
db.transactions.createIndex({ "from": 1 }, { name: "idx_transactions_from" });
db.transactions.createIndex({ "to": 1 }, { name: "idx_transactions_to" });
db.transactions.createIndex({ "blockNumber": 1, "transactionIndex": 1 }, { name: "idx_transactions_block_tx_index" });
db.transactions.createIndex({ "from": 1, "to": 1 }, { name: "idx_transactions_from_to" });

// Accounts indexes
db.accounts.createIndex({ "address": 1 }, { unique: true, name: "idx_accounts_address" });
db.accounts.createIndex({ "isContract": 1 }, { name: "idx_accounts_is_contract" });
db.accounts.createIndex({ "balance": 1 }, { name: "idx_accounts_balance" });

// Network metrics indexes
db.network_metrics.createIndex({ "timestamp": 1 }, { name: "idx_network_metrics_timestamp" });
db.network_metrics.createIndex({ "blockNumber": 1 }, { name: "idx_network_metrics_block_number" });
db.network_metrics.createIndex({ "timestamp": 1, "blockNumber": 1 }, { name: "idx_network_metrics_timestamp_block" });

print('MongoDB initialization completed successfully!');
print('Collections created: blocks, transactions, accounts, network_metrics');
print('Indexes created for optimal query performance');
