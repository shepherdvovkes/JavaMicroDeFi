package com.defimon.bitcoin.metrics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Bitcoin Metrics Service Application
 * Collects and exports Bitcoin node metrics to Prometheus/Grafana
 */
@SpringBootApplication
@EnableScheduling
public class BitcoinMetricsApplication {

    public static void main(String[] args) {
        SpringApplication.run(BitcoinMetricsApplication.class, args);
    }
}
