// MongoDB initialization script for DEFIMON
db = db.getSiblingDB('chaindata');

// Create collections for blockchain data
db.createCollection('blocks');
db.createCollection('transactions');
db.createCollection('events');
db.createCollection('aggregated_data');

// Create indexes for performance
db.blocks.createIndex({ "block_number": 1 });
db.blocks.createIndex({ "timestamp": 1 });
db.blocks.createIndex({ "hash": 1 }, { unique: true });

db.transactions.createIndex({ "hash": 1 }, { unique: true });
db.transactions.createIndex({ "block_number": 1 });
db.transactions.createIndex({ "from_address": 1 });
db.transactions.createIndex({ "to_address": 1 });
db.transactions.createIndex({ "timestamp": 1 });

db.events.createIndex({ "transaction_hash": 1 });
db.events.createIndex({ "block_number": 1 });
db.events.createIndex({ "contract_address": 1 });
db.events.createIndex({ "event_name": 1 });
db.events.createIndex({ "timestamp": 1 });

db.aggregated_data.createIndex({ "data_type": 1 });
db.aggregated_data.createIndex({ "timestamp": 1 });
db.aggregated_data.createIndex({ "symbol": 1 });

print('Database initialized successfully with collections and indexes');
