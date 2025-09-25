package com.defimon.multichain.service;

import com.defimon.multichain.model.Block;
import com.defimon.multichain.model.Transaction;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Service interface for blockchain data processing operations.
 * 
 * This interface defines the contract for processing blockchain data,
 * including event extraction, data transformation, and analytics
 * across different blockchain networks.
 */
public interface DataProcessingService {
    
    /**
     * Processes a new block and extracts relevant events and data.
     * 
     * @param block the block to process
     * @return processing result
     */
    Mono<BlockProcessingResult> processBlock(Block block);
    
    /**
     * Processes a transaction and extracts relevant events.
     * 
     * @param transaction the transaction to process
     * @return processing result
     */
    Mono<TransactionProcessingResult> processTransaction(Transaction transaction);
    
    /**
     * Extracts events from a block based on configured filters.
     * 
     * @param block the block to extract events from
     * @param eventFilters list of event filters to apply
     * @return list of extracted events
     */
    Mono<List<BlockchainEvent>> extractEvents(Block block, List<EventFilter> eventFilters);
    
    /**
     * Extracts events from a transaction.
     * 
     * @param transaction the transaction to extract events from
     * @param eventFilters list of event filters to apply
     * @return list of extracted events
     */
    Mono<List<BlockchainEvent>> extractEvents(Transaction transaction, List<EventFilter> eventFilters);
    
    /**
     * Transforms raw blockchain data into a standardized format.
     * 
     * @param rawData the raw blockchain data
     * @param transformationRules the transformation rules to apply
     * @return transformed data
     */
    Mono<Map<String, Object>> transformData(Map<String, Object> rawData, List<TransformationRule> transformationRules);
    
    /**
     * Validates blockchain data for consistency and integrity.
     * 
     * @param data the data to validate
     * @return validation result
     */
    Mono<ValidationResult> validateData(Map<String, Object> data);
    
    /**
     * Calculates analytics metrics for a block.
     * 
     * @param block the block to analyze
     * @return analytics metrics
     */
    Mono<BlockAnalytics> calculateBlockAnalytics(Block block);
    
    /**
     * Calculates analytics metrics for a transaction.
     * 
     * @param transaction the transaction to analyze
     * @return analytics metrics
     */
    Mono<TransactionAnalytics> calculateTransactionAnalytics(Transaction transaction);
    
    /**
     * Processes historical data for a range of blocks.
     * 
     * @param fromBlock the starting block number
     * @param toBlock the ending block number
     * @return flux of processing results
     */
    Flux<BlockProcessingResult> processHistoricalData(Long fromBlock, Long toBlock);
    
    /**
     * Gets processing statistics for the service.
     * 
     * @return processing statistics
     */
    Mono<ProcessingStats> getProcessingStats();
    
    /**
     * Record for block processing result.
     * 
     * @param blockNumber the block number
     * @param blockHash the block hash
     * @param processingTime the time taken to process the block
     * @param eventsExtracted number of events extracted
     * @param transactionsProcessed number of transactions processed
     * @param success whether processing was successful
     * @param errorMessage error message if processing failed
     */
    record BlockProcessingResult(
        Long blockNumber,
        String blockHash,
        Long processingTime,
        Integer eventsExtracted,
        Integer transactionsProcessed,
        Boolean success,
        String errorMessage
    ) {}
    
    /**
     * Record for transaction processing result.
     * 
     * @param transactionHash the transaction hash
     * @param processingTime the time taken to process the transaction
     * @param eventsExtracted number of events extracted
     * @param success whether processing was successful
     * @param errorMessage error message if processing failed
     */
    record TransactionProcessingResult(
        String transactionHash,
        Long processingTime,
        Integer eventsExtracted,
        Boolean success,
        String errorMessage
    ) {}
    
    /**
     * Record for blockchain event.
     * 
     * @param eventType the type of event
     * @param contractAddress the contract address that emitted the event
     * @param transactionHash the transaction hash
     * @param blockNumber the block number
     * @param logIndex the log index
     * @param topics list of indexed topics
     * @param data the event data
     * @param timestamp the event timestamp
     */
    record BlockchainEvent(
        String eventType,
        String contractAddress,
        String transactionHash,
        Long blockNumber,
        Integer logIndex,
        List<String> topics,
        String data,
        Long timestamp
    ) {}
    
    /**
     * Record for event filter.
     * 
     * @param contractAddress the contract address to filter by
     * @param eventSignature the event signature to filter by
     * @param topics list of topic filters
     */
    record EventFilter(
        String contractAddress,
        String eventSignature,
        List<String> topics
    ) {}
    
    /**
     * Record for transformation rule.
     * 
     * @param sourceField the source field to transform
     * @param targetField the target field name
     * @param transformation the transformation function
     */
    record TransformationRule(
        String sourceField,
        String targetField,
        String transformation
    ) {}
    
    /**
     * Record for validation result.
     * 
     * @param isValid whether the data is valid
     * @param validationErrors list of validation errors
     * @param validationWarnings list of validation warnings
     */
    record ValidationResult(
        Boolean isValid,
        List<String> validationErrors,
        List<String> validationWarnings
    ) {}
    
    /**
     * Record for block analytics.
     * 
     * @param blockNumber the block number
     * @param transactionCount number of transactions in the block
     * @param gasUsed total gas used in the block
     * @param gasLimit the gas limit of the block
     * @param blockSize the size of the block in bytes
     * @param processingTime the time taken to process the block
     */
    record BlockAnalytics(
        Long blockNumber,
        Integer transactionCount,
        BigDecimal gasUsed,
        BigDecimal gasLimit,
        Long blockSize,
        Long processingTime
    ) {}
    
    /**
     * Record for transaction analytics.
     * 
     * @param transactionHash the transaction hash
     * @param gasUsed gas used by the transaction
     * @param gasPrice gas price of the transaction
     * @param transactionSize size of the transaction in bytes
     * @param processingTime time taken to process the transaction
     */
    record TransactionAnalytics(
        String transactionHash,
        BigDecimal gasUsed,
        BigDecimal gasPrice,
        Long transactionSize,
        Long processingTime
    ) {}
    
    /**
     * Record for processing statistics.
     * 
     * @param totalBlocksProcessed total number of blocks processed
     * @param totalTransactionsProcessed total number of transactions processed
     * @param totalEventsExtracted total number of events extracted
     * @param averageProcessingTime average processing time per block
     * @param processingErrors total number of processing errors
     * @param lastProcessingTime timestamp of last processing activity
     */
    record ProcessingStats(
        Long totalBlocksProcessed,
        Long totalTransactionsProcessed,
        Long totalEventsExtracted,
        Double averageProcessingTime,
        Long processingErrors,
        Long lastProcessingTime
    ) {}
}
