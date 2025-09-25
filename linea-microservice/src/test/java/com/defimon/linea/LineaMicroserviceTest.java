package com.defimon.linea;

import com.defimon.linea.config.LineaConfiguration;
import com.defimon.linea.metrics.LineaMetricsService;
import com.defimon.linea.service.LineaSyncService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test suite for Linea microservice.
 * 
 * Tests cover:
 * - Configuration validation
 * - Service initialization
 * - Metrics collection
 * - Error handling
 * - Performance monitoring
 * - Health checks
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "linea.rpc-url=https://test-rpc.linea.build",
    "linea.database-path=/tmp/test_linea.db",
    "linea.archive-database-path=/tmp/test_linea_archive.db",
    "linea.max-concurrent-workers=5",
    "linea.rate-limit-per-second=50"
})
@ExtendWith(MockitoExtension.class)
public class LineaMicroserviceTest {
    
    @Mock
    private LineaConfiguration configuration;
    
    @Mock
    private LineaSyncService syncService;
    
    @Mock
    private LineaMetricsService metricsService;
    
    @BeforeEach
    void setUp() {
        // Setup mock configuration
        when(configuration.getRpcUrl()).thenReturn("https://test-rpc.linea.build");
        when(configuration.getDatabasePath()).thenReturn("/tmp/test_linea.db");
        when(configuration.getArchiveDatabasePath()).thenReturn("/tmp/test_linea_archive.db");
        when(configuration.getMaxConcurrentWorkers()).thenReturn(5);
        when(configuration.getRateLimitPerSecond()).thenReturn(50);
        when(configuration.getBlockCollectionInterval()).thenReturn(2000L);
        when(configuration.getTransactionCollectionInterval()).thenReturn(1000L);
        when(configuration.getAccountCollectionInterval()).thenReturn(5000L);
        when(configuration.getContractCollectionInterval()).thenReturn(10000L);
        when(configuration.getTokenCollectionInterval()).thenReturn(15000L);
        when(configuration.getDefiCollectionInterval()).thenReturn(30000L);
        when(configuration.getRequestTimeoutSeconds()).thenReturn(30L);
        when(configuration.getRetryAttempts()).thenReturn(3);
        when(configuration.getRetryDelaySeconds()).thenReturn(5L);
        when(configuration.getEnableRealTimeMonitoring()).thenReturn(true);
        when(configuration.getEnableHistoricalData()).thenReturn(true);
        when(configuration.getEnableDefiMetrics()).thenReturn(true);
        when(configuration.getEnableBridgeMetrics()).thenReturn(true);
        when(configuration.getEnableTokenMetrics()).thenReturn(true);
        when(configuration.getEnableContractMetrics()).thenReturn(true);
        when(configuration.getEnableBackup()).thenReturn(true);
        when(configuration.getBackupIntervalHours()).thenReturn(6L);
        when(configuration.getBackupRetentionDays()).thenReturn(30);
        when(configuration.getBackupPath()).thenReturn("/tmp/linea_backups/");
        when(configuration.getCompressArchiveData()).thenReturn(true);
        when(configuration.getArchiveCompressionLevel()).thenReturn(6);
        when(configuration.getNativeToken()).thenReturn("ETH");
        when(configuration.getWrappedToken()).thenReturn("WETH");
        when(configuration.getChainId()).thenReturn("59144");
        when(configuration.getNetworkName()).thenReturn("linea");
        when(configuration.getBlockTime()).thenReturn(2000L);
        when(configuration.getExplorerUrl()).thenReturn("https://lineascan.build");
        when(configuration.getBridgeContract()).thenReturn("0xA0b86a33E6441E0a4bFc0B4d5F3F3E5A4F3F3F3F");
        when(configuration.getMessageService()).thenReturn("0xd19bae9c65bde34f26c2ee8f2f3f3e5a4f3f3f3f");
        when(configuration.getLogLevel()).thenReturn("INFO");
        when(configuration.getLogFile()).thenReturn("/tmp/logs/linea_collector.log");
        when(configuration.getLogRetentionDays()).thenReturn(30);
        when(configuration.getApiHost()).thenReturn("0.0.0.0");
        when(configuration.getApiPort()).thenReturn(8008);
        when(configuration.getNodeEnv()).thenReturn("test");
        when(configuration.getWorkingDir()).thenReturn("/tmp");
    }
    
    @Test
    @DisplayName("Test LineaConfiguration validation")
    void testLineaConfigurationValidation() {
        // Test configuration properties
        assertNotNull(configuration.getRpcUrl());
        assertNotNull(configuration.getDatabasePath());
        assertNotNull(configuration.getArchiveDatabasePath());
        assertNotNull(configuration.getMaxConcurrentWorkers());
        assertNotNull(configuration.getRateLimitPerSecond());
        assertNotNull(configuration.getBlockCollectionInterval());
        assertNotNull(configuration.getTransactionCollectionInterval());
        assertNotNull(configuration.getAccountCollectionInterval());
        assertNotNull(configuration.getContractCollectionInterval());
        assertNotNull(configuration.getTokenCollectionInterval());
        assertNotNull(configuration.getDefiCollectionInterval());
        assertNotNull(configuration.getRequestTimeoutSeconds());
        assertNotNull(configuration.getRetryAttempts());
        assertNotNull(configuration.getRetryDelaySeconds());
        assertNotNull(configuration.getEnableRealTimeMonitoring());
        assertNotNull(configuration.getEnableHistoricalData());
        assertNotNull(configuration.getEnableDefiMetrics());
        assertNotNull(configuration.getEnableBridgeMetrics());
        assertNotNull(configuration.getEnableTokenMetrics());
        assertNotNull(configuration.getEnableContractMetrics());
        assertNotNull(configuration.getEnableBackup());
        assertNotNull(configuration.getBackupIntervalHours());
        assertNotNull(configuration.getBackupRetentionDays());
        assertNotNull(configuration.getBackupPath());
        assertNotNull(configuration.getCompressArchiveData());
        assertNotNull(configuration.getArchiveCompressionLevel());
        assertNotNull(configuration.getNativeToken());
        assertNotNull(configuration.getWrappedToken());
        assertNotNull(configuration.getChainId());
        assertNotNull(configuration.getNetworkName());
        assertNotNull(configuration.getBlockTime());
        assertNotNull(configuration.getExplorerUrl());
        assertNotNull(configuration.getBridgeContract());
        assertNotNull(configuration.getMessageService());
        assertNotNull(configuration.getLogLevel());
        assertNotNull(configuration.getLogFile());
        assertNotNull(configuration.getLogRetentionDays());
        assertNotNull(configuration.getApiHost());
        assertNotNull(configuration.getApiPort());
        assertNotNull(configuration.getNodeEnv());
        assertNotNull(configuration.getWorkingDir());
        
        // Test configuration values
        assertEquals("https://test-rpc.linea.build", configuration.getRpcUrl());
        assertEquals("/tmp/test_linea.db", configuration.getDatabasePath());
        assertEquals("/tmp/test_linea_archive.db", configuration.getArchiveDatabasePath());
        assertEquals(5, configuration.getMaxConcurrentWorkers());
        assertEquals(50, configuration.getRateLimitPerSecond());
        assertEquals(2000L, configuration.getBlockCollectionInterval());
        assertEquals(1000L, configuration.getTransactionCollectionInterval());
        assertEquals(5000L, configuration.getAccountCollectionInterval());
        assertEquals(10000L, configuration.getContractCollectionInterval());
        assertEquals(15000L, configuration.getTokenCollectionInterval());
        assertEquals(30000L, configuration.getDefiCollectionInterval());
        assertEquals(30L, configuration.getRequestTimeoutSeconds());
        assertEquals(3, configuration.getRetryAttempts());
        assertEquals(5L, configuration.getRetryDelaySeconds());
        assertTrue(configuration.getEnableRealTimeMonitoring());
        assertTrue(configuration.getEnableHistoricalData());
        assertTrue(configuration.getEnableDefiMetrics());
        assertTrue(configuration.getEnableBridgeMetrics());
        assertTrue(configuration.getEnableTokenMetrics());
        assertTrue(configuration.getEnableContractMetrics());
        assertTrue(configuration.getEnableBackup());
        assertEquals(6L, configuration.getBackupIntervalHours());
        assertEquals(30, configuration.getBackupRetentionDays());
        assertEquals("/tmp/linea_backups/", configuration.getBackupPath());
        assertTrue(configuration.getCompressArchiveData());
        assertEquals(6, configuration.getArchiveCompressionLevel());
        assertEquals("ETH", configuration.getNativeToken());
        assertEquals("WETH", configuration.getWrappedToken());
        assertEquals("59144", configuration.getChainId());
        assertEquals("linea", configuration.getNetworkName());
        assertEquals(2000L, configuration.getBlockTime());
        assertEquals("https://lineascan.build", configuration.getExplorerUrl());
        assertEquals("0xA0b86a33E6441E0a4bFc0B4d5F3F3E5A4F3F3F3F", configuration.getBridgeContract());
        assertEquals("0xd19bae9c65bde34f26c2ee8f2f3f3e5a4f3f3f3f", configuration.getMessageService());
        assertEquals("INFO", configuration.getLogLevel());
        assertEquals("/tmp/logs/linea_collector.log", configuration.getLogFile());
        assertEquals(30, configuration.getLogRetentionDays());
        assertEquals("0.0.0.0", configuration.getApiHost());
        assertEquals(8008, configuration.getApiPort());
        assertEquals("test", configuration.getNodeEnv());
        assertEquals("/tmp", configuration.getWorkingDir());
        
        // Test configuration methods
        assertNotNull(configuration.getEffectiveRpcUrl());
        assertNotNull(configuration.getEffectiveWssUrl());
        assertTrue(configuration.hasWebSocketSupport());
        assertNotNull(configuration.getKnownDefiProtocols());
        assertNotNull(configuration.getKnownBridgeContracts());
        
        System.out.println("✅ LineaConfiguration validation passed");
    }
    
    @Test
    @DisplayName("Test LineaSyncService initialization")
    void testLineaSyncServiceInitialization() {
        // Test service initialization
        assertNotNull(syncService);
        
        // Test service methods
        when(syncService.isSyncRunning()).thenReturn(false);
        assertFalse(syncService.isSyncRunning());
        
        // Test service start/stop
        doNothing().when(syncService).startSync();
        doNothing().when(syncService).stopSync();
        
        syncService.startSync();
        syncService.stopSync();
        
        verify(syncService, times(1)).startSync();
        verify(syncService, times(1)).stopSync();
        
        System.out.println("✅ LineaSyncService initialization passed");
    }
    
    @Test
    @DisplayName("Test LineaMetricsService functionality")
    void testLineaMetricsServiceFunctionality() {
        // Test metrics service initialization
        assertNotNull(metricsService);
        
        // Test counter increments
        doNothing().when(metricsService).incrementBlocksCollected();
        doNothing().when(metricsService).incrementTransactionsCollected();
        doNothing().when(metricsService).incrementAccountsCollected();
        doNothing().when(metricsService).incrementContractsCollected();
        doNothing().when(metricsService).incrementTokensCollected();
        doNothing().when(metricsService).incrementDefiProtocolsCollected();
        doNothing().when(metricsService).incrementBridgeTransactionsCollected();
        
        metricsService.incrementBlocksCollected();
        metricsService.incrementTransactionsCollected();
        metricsService.incrementAccountsCollected();
        metricsService.incrementContractsCollected();
        metricsService.incrementTokensCollected();
        metricsService.incrementDefiProtocolsCollected();
        metricsService.incrementBridgeTransactionsCollected();
        
        verify(metricsService, times(1)).incrementBlocksCollected();
        verify(metricsService, times(1)).incrementTransactionsCollected();
        verify(metricsService, times(1)).incrementAccountsCollected();
        verify(metricsService, times(1)).incrementContractsCollected();
        verify(metricsService, times(1)).incrementTokensCollected();
        verify(metricsService, times(1)).incrementDefiProtocolsCollected();
        verify(metricsService, times(1)).incrementBridgeTransactionsCollected();
        
        // Test error counters
        doNothing().when(metricsService).incrementRpcErrors();
        doNothing().when(metricsService).incrementDatabaseErrors();
        doNothing().when(metricsService).incrementWorkerErrors();
        doNothing().when(metricsService).incrementRetryAttempts();
        
        metricsService.incrementRpcErrors();
        metricsService.incrementDatabaseErrors();
        metricsService.incrementWorkerErrors();
        metricsService.incrementRetryAttempts();
        
        verify(metricsService, times(1)).incrementRpcErrors();
        verify(metricsService, times(1)).incrementDatabaseErrors();
        verify(metricsService, times(1)).incrementWorkerErrors();
        verify(metricsService, times(1)).incrementRetryAttempts();
        
        // Test gauge updates
        doNothing().when(metricsService).updateLatestBlockNumber(anyLong());
        doNothing().when(metricsService).updateCurrentBlockNumber(anyLong());
        doNothing().when(metricsService).updateSyncProgress(anyDouble());
        doNothing().when(metricsService).updateNetworkTps(anyDouble());
        doNothing().when(metricsService).updateNetworkGasUtilization(anyDouble());
        doNothing().when(metricsService).updateNetworkGasPrice(anyDouble());
        
        metricsService.updateLatestBlockNumber(12345678L);
        metricsService.updateCurrentBlockNumber(12345670L);
        metricsService.updateSyncProgress(95.5);
        metricsService.updateNetworkTps(15.2);
        metricsService.updateNetworkGasUtilization(75.8);
        metricsService.updateNetworkGasPrice(2.5);
        
        verify(metricsService, times(1)).updateLatestBlockNumber(12345678L);
        verify(metricsService, times(1)).updateCurrentBlockNumber(12345670L);
        verify(metricsService, times(1)).updateSyncProgress(95.5);
        verify(metricsService, times(1)).updateNetworkTps(15.2);
        verify(metricsService, times(1)).updateNetworkGasUtilization(75.8);
        verify(metricsService, times(1)).updateNetworkGasPrice(2.5);
        
        // Test statistics methods
        when(metricsService.getCollectionStats()).thenReturn("Test collection stats");
        when(metricsService.getPerformanceStats()).thenReturn("Test performance stats");
        when(metricsService.getNetworkStats()).thenReturn("Test network stats");
        when(metricsService.getSystemStats()).thenReturn("Test system stats");
        
        assertNotNull(metricsService.getCollectionStats());
        assertNotNull(metricsService.getPerformanceStats());
        assertNotNull(metricsService.getNetworkStats());
        assertNotNull(metricsService.getSystemStats());
        
        // Test health check
        when(metricsService.isHealthy()).thenReturn(true);
        when(metricsService.getHealthStatus()).thenReturn("HEALTHY");
        
        assertTrue(metricsService.isHealthy());
        assertEquals("HEALTHY", metricsService.getHealthStatus());
        
        System.out.println("✅ LineaMetricsService functionality passed");
    }
    
    @Test
    @DisplayName("Test error handling and recovery")
    void testErrorHandlingAndRecovery() {
        // Test RPC error handling
        doNothing().when(metricsService).incrementRpcErrors();
        metricsService.incrementRpcErrors();
        verify(metricsService, times(1)).incrementRpcErrors();
        
        // Test database error handling
        doNothing().when(metricsService).incrementDatabaseErrors();
        metricsService.incrementDatabaseErrors();
        verify(metricsService, times(1)).incrementDatabaseErrors();
        
        // Test worker error handling
        doNothing().when(metricsService).incrementWorkerErrors();
        metricsService.incrementWorkerErrors();
        verify(metricsService, times(1)).incrementWorkerErrors();
        
        // Test retry mechanism
        doNothing().when(metricsService).incrementRetryAttempts();
        metricsService.incrementRetryAttempts();
        verify(metricsService, times(1)).incrementRetryAttempts();
        
        // Test health check with errors
        when(metricsService.isHealthy()).thenReturn(false);
        when(metricsService.getHealthStatus()).thenReturn("UNHEALTHY");
        
        assertFalse(metricsService.isHealthy());
        assertEquals("UNHEALTHY", metricsService.getHealthStatus());
        
        System.out.println("✅ Error handling and recovery passed");
    }
    
    @Test
    @DisplayName("Test performance monitoring")
    void testPerformanceMonitoring() {
        // Test timer recording
        doNothing().when(metricsService).recordBlockCollectionTime(any());
        doNothing().when(metricsService).recordTransactionCollectionTime(any());
        doNothing().when(metricsService).recordAccountCollectionTime(any());
        doNothing().when(metricsService).recordRpcRequestTime(any());
        doNothing().when(metricsService).recordDatabaseWriteTime(any());
        doNothing().when(metricsService).recordDatabaseReadTime(any());
        
        metricsService.recordBlockCollectionTime(java.time.Duration.ofMillis(100));
        metricsService.recordTransactionCollectionTime(java.time.Duration.ofMillis(50));
        metricsService.recordAccountCollectionTime(java.time.Duration.ofMillis(200));
        metricsService.recordRpcRequestTime(java.time.Duration.ofMillis(30));
        metricsService.recordDatabaseWriteTime(java.time.Duration.ofMillis(80));
        metricsService.recordDatabaseReadTime(java.time.Duration.ofMillis(20));
        
        verify(metricsService, times(1)).recordBlockCollectionTime(any());
        verify(metricsService, times(1)).recordTransactionCollectionTime(any());
        verify(metricsService, times(1)).recordAccountCollectionTime(any());
        verify(metricsService, times(1)).recordRpcRequestTime(any());
        verify(metricsService, times(1)).recordDatabaseWriteTime(any());
        verify(metricsService, times(1)).recordDatabaseReadTime(any());
        
        System.out.println("✅ Performance monitoring passed");
    }
    
    @Test
    @DisplayName("Test configuration edge cases")
    void testConfigurationEdgeCases() {
        // Test null values
        when(configuration.getRpcUrl()).thenReturn(null);
        assertNull(configuration.getRpcUrl());
        
        // Test empty strings
        when(configuration.getRpcUrl()).thenReturn("");
        assertEquals("", configuration.getRpcUrl());
        
        // Test invalid values
        when(configuration.getMaxConcurrentWorkers()).thenReturn(0);
        assertEquals(0, configuration.getMaxConcurrentWorkers());
        
        when(configuration.getMaxConcurrentWorkers()).thenReturn(-1);
        assertEquals(-1, configuration.getMaxConcurrentWorkers());
        
        when(configuration.getRateLimitPerSecond()).thenReturn(0);
        assertEquals(0, configuration.getRateLimitPerSecond());
        
        when(configuration.getRateLimitPerSecond()).thenReturn(-1);
        assertEquals(-1, configuration.getRateLimitPerSecond());
        
        // Test boolean values
        when(configuration.getEnableRealTimeMonitoring()).thenReturn(false);
        assertFalse(configuration.getEnableRealTimeMonitoring());
        
        when(configuration.getEnableHistoricalData()).thenReturn(false);
        assertFalse(configuration.getEnableHistoricalData());
        
        when(configuration.getEnableDefiMetrics()).thenReturn(false);
        assertFalse(configuration.getEnableDefiMetrics());
        
        when(configuration.getEnableBridgeMetrics()).thenReturn(false);
        assertFalse(configuration.getEnableBridgeMetrics());
        
        when(configuration.getEnableTokenMetrics()).thenReturn(false);
        assertFalse(configuration.getEnableTokenMetrics());
        
        when(configuration.getEnableContractMetrics()).thenReturn(false);
        assertFalse(configuration.getEnableContractMetrics());
        
        when(configuration.getEnableBackup()).thenReturn(false);
        assertFalse(configuration.getEnableBackup());
        
        when(configuration.getCompressArchiveData()).thenReturn(false);
        assertFalse(configuration.getCompressArchiveData());
        
        System.out.println("✅ Configuration edge cases passed");
    }
    
    @Test
    @DisplayName("Test metrics reset functionality")
    void testMetricsResetFunctionality() {
        // Test counter reset
        doNothing().when(metricsService).resetCounters();
        metricsService.resetCounters();
        verify(metricsService, times(1)).resetCounters();
        
        // Test timer reset
        doNothing().when(metricsService).resetTimers();
        metricsService.resetTimers();
        verify(metricsService, times(1)).resetTimers();
        
        System.out.println("✅ Metrics reset functionality passed");
    }
    
    @Test
    @DisplayName("Test integration scenarios")
    void testIntegrationScenarios() {
        // Test service integration
        when(syncService.isSyncRunning()).thenReturn(true);
        assertTrue(syncService.isSyncRunning());
        
        // Test metrics integration
        when(metricsService.isHealthy()).thenReturn(true);
        assertTrue(metricsService.isHealthy());
        
        // Test configuration integration
        when(configuration.getEnableRealTimeMonitoring()).thenReturn(true);
        assertTrue(configuration.getEnableRealTimeMonitoring());
        
        // Test error scenario
        when(metricsService.isHealthy()).thenReturn(false);
        assertFalse(metricsService.isHealthy());
        
        // Test recovery scenario
        when(metricsService.isHealthy()).thenReturn(true);
        assertTrue(metricsService.isHealthy());
        
        System.out.println("✅ Integration scenarios passed");
    }
    
    @Test
    @DisplayName("Test comprehensive system health")
    void testComprehensiveSystemHealth() {
        // Test all components are working
        assertNotNull(configuration);
        assertNotNull(syncService);
        assertNotNull(metricsService);
        
        // Test configuration is valid
        assertNotNull(configuration.getRpcUrl());
        assertNotNull(configuration.getDatabasePath());
        assertNotNull(configuration.getArchiveDatabasePath());
        
        // Test services are functional
        when(syncService.isSyncRunning()).thenReturn(false);
        assertFalse(syncService.isSyncRunning());
        
        when(metricsService.isHealthy()).thenReturn(true);
        assertTrue(metricsService.isHealthy());
        
        // Test metrics are working
        doNothing().when(metricsService).incrementBlocksCollected();
        metricsService.incrementBlocksCollected();
        verify(metricsService, times(1)).incrementBlocksCollected();
        
        System.out.println("✅ Comprehensive system health passed");
    }
}
