package com.defimon.prediction;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Ethereum Price Prediction Microservice
 * 
 * This service collects comprehensive data affecting ETH price and provides
 * predictive analytics using machine learning models.
 * 
 * Features:
 * - Multi-source data collection (market data, on-chain metrics, sentiment)
 * - Real-time price prediction models
 * - Integration with existing blockchain sync services
 * - Comprehensive factor analysis
 */
@SpringBootApplication
@EnableCaching
@EnableKafka
@EnableAsync
@EnableScheduling
public class EthereumPredictionApplication {

    public static void main(String[] args) {
        SpringApplication.run(EthereumPredictionApplication.class, args);
    }
}
