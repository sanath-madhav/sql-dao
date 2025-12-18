/*
 *
 *
 *   ******************************************************************************
 *
 *    Copyright (c) 2023-24 Harman International
 *
 *
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *
 *    you may not use this file except in compliance with the License.
 *
 *    You may obtain a copy of the License at
 *
 *
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *
 *    Unless required by applicable law or agreed to in writing, software
 *
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *    See the License for the specific language governing permissions and
 *
 *    limitations under the License.
 *
 *
 *
 *    SPDX-License-Identifier: Apache-2.0
 *
 *    *******************************************************************************
 *
 *
 */

package org.eclipse.ecsp.sql.postgress.multitenancy;

import io.prometheus.client.CollectorRegistry;
import org.eclipse.ecsp.sql.SqlDaoApplication;
import org.eclipse.ecsp.sql.authentication.DefaultPostgresDbCredentialsProvider;

import org.eclipse.ecsp.sql.multitenancy.TenantAwareDataSource;
import org.eclipse.ecsp.sql.multitenancy.TenantContext;
import org.eclipse.ecsp.sql.multitenancy.TenantDatabaseProperties;
import org.eclipse.ecsp.sql.postgress.config.PostgresDbConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test class for multi-tenant PostgreSQL database configuration.
 * Tests the complete multi-tenant flow including datasource routing, tenant context management,
 * and concurrent access patterns.
 *
 * @author hbadshah
 * @version 1.1
 * @since 2025-10-27
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { SqlDaoApplication.class })
@TestPropertySource(locations = "classpath:application-dao-multitenant-test.properties")
@Testcontainers
class PostgresDbConfigMultiTenantIntegrationTest {

    @Autowired
    @Qualifier("targetDataSources")
    private Map<String, DataSource> targetDataSources;

    @Autowired
    @Qualifier("tenantConfigMap")
    private Map<String, TenantDatabaseProperties> multiTenantDatabaseProperties;

    /** Testcontainer for tenant1 */
    @Container
    @SuppressWarnings("resource")
    static PostgreSQLContainer<?> tenant1Container = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("tenant1db")
            .withUsername("root")
            .withPassword("root");

    /** Testcontainer for tenant2 */
    @Container
    @SuppressWarnings("resource")
    static PostgreSQLContainer<?> tenant2Container = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("tenant2db")
            .withUsername("root")
            .withPassword("root");

    /** Testcontainer for tenant3 */
    @Container
    @SuppressWarnings("resource")
    static PostgreSQLContainer<?> tenant3Container = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("tenant3db")
            .withUsername("root")
            .withPassword("root");

    @BeforeAll
    static void setUpContainers() {
        CollectorRegistry.defaultRegistry.clear();
        
        // Enable multitenancy for TenantContext
        System.setProperty("multitenancy.enabled", "true");
        
        // Start all containers
        tenant1Container.start();
        tenant2Container.start();
        tenant3Container.start();
        
        // Set system properties for each tenant
        System.setProperty("DB_URL_TENANT1", tenant1Container.getJdbcUrl());
        System.setProperty("DB_URL_TENANT2", tenant2Container.getJdbcUrl());
        System.setProperty("DB_URL_TENANT3", tenant3Container.getJdbcUrl());
    }

    @BeforeEach
    void setUp() {
        // Clear tenant context before each test
        TenantContext.clear();
        CollectorRegistry.defaultRegistry.clear();
    }

    @AfterEach
    void tearDown() {
        // Clean up tenant context after each test
        TenantContext.clear();
    }

    @AfterAll
    static void tearDownContainers() {
        CollectorRegistry.defaultRegistry.clear();
        tenant1Container.stop();
        tenant2Container.stop();
        tenant3Container.stop();
    }

    @Test
    @DisplayName("Should initialize target datasources for all tenants")
    void testTargetDataSourcesInitialization() {
        // Given: Multi-tenancy is enabled with 3 tenants
        
        // Then: All tenant datasources should be initialized
        assertNotNull(targetDataSources, "Target datasources map should not be null");
        assertEquals(3, targetDataSources.size(), "Should have 3 tenant datasources");
        
        assertTrue(targetDataSources.containsKey("tenant1"), "Should contain tenant1 datasource");
        assertTrue(targetDataSources.containsKey("tenant2"), "Should contain tenant2 datasource");
        assertTrue(targetDataSources.containsKey("tenant3"), "Should contain tenant3 datasource");
    }

    @Test
    @DisplayName("Should get connection for tenant1 datasource")
    void testTenant1Connection() throws SQLException {
        // Given: Tenant1 datasource exists
        DataSource tenant1DataSource = targetDataSources.get("tenant1");
        assertNotNull(tenant1DataSource, "Tenant1 datasource should not be null");
        
        // When: Getting a connection
        try (Connection connection = tenant1DataSource.getConnection()) {
            // Then: Connection should be valid
            assertNotNull(connection, "Connection should not be null");
            assertTrue(connection.isValid(5), "Connection should be valid");
            assertFalse(connection.isClosed(), "Connection should not be closed");
        }
    }

    @Test
    @DisplayName("Should get connection for tenant2 datasource")
    void testTenant2Connection() throws SQLException {
        // Given: Tenant2 datasource exists
        DataSource tenant2DataSource = targetDataSources.get("tenant2");
        assertNotNull(tenant2DataSource, "Tenant2 datasource should not be null");
        
        // When: Getting a connection
        try (Connection connection = tenant2DataSource.getConnection()) {
            // Then: Connection should be valid
            assertNotNull(connection, "Connection should not be null");
            assertTrue(connection.isValid(5), "Connection should be valid");
            assertFalse(connection.isClosed(), "Connection should not be closed");
        }
    }

    @Test
    @DisplayName("Should get connection for tenant3 datasource")
    void testTenant3Connection() throws SQLException {
        // Given: Tenant3 datasource exists
        DataSource tenant3DataSource = targetDataSources.get("tenant3");
        assertNotNull(tenant3DataSource, "Tenant3 datasource should not be null");
        
        // When: Getting a connection
        try (Connection connection = tenant3DataSource.getConnection()) {
            // Then: Connection should be valid
            assertNotNull(connection, "Connection should not be null");
            assertTrue(connection.isValid(5), "Connection should be valid");
            assertFalse(connection.isClosed(), "Connection should not be closed");
        }
    }

    @Test
    @DisplayName("Should isolate data between different tenants")
    void testTenantDataIsolation() throws SQLException {
        // Given: Create test tables and insert data in each tenant database
        createTestTableAndInsertData("tenant1", "Tenant1Data");
        createTestTableAndInsertData("tenant2", "Tenant2Data");
        createTestTableAndInsertData("tenant3", "Tenant3Data");
        
        // When & Then: Each tenant should only see their own data
        String tenant1Data = queryTestData("tenant1");
        assertEquals("Tenant1Data", tenant1Data, "Tenant1 should see only its data");
        
        String tenant2Data = queryTestData("tenant2");
        assertEquals("Tenant2Data", tenant2Data, "Tenant2 should see only its data");
        
        String tenant3Data = queryTestData("tenant3");
        assertEquals("Tenant3Data", tenant3Data, "Tenant3 should see only its data");
    }

    @Test
    @DisplayName("Should handle concurrent access from multiple tenants")
    void testConcurrentMultiTenantAccess() throws InterruptedException {
        // Given: Multiple threads accessing different tenants
        int threadCount = 6;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicReference<Throwable> error = new AtomicReference<>();

        // When: Concurrent access from multiple threads
        for (int i = 0; i < threadCount; i++) {
            final String tenantId = "tenant" + ((i % 3) + 1);
            executor.submit(() -> {
                try {
                    startLatch.await(); // Synchronize start
                    TenantContext.setCurrentTenant(tenantId);
                    
                    // Verify connection can be obtained
                    DataSource ds = targetDataSources.get(tenantId);
                    try (Connection conn = ds.getConnection()) {
                        assertTrue(conn.isValid(5), "Connection should be valid for " + tenantId);
                    }
                    
                    TenantContext.clear();
                } catch (Exception e) {
                    error.set(e);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Start all threads
        startLatch.countDown();
        
        // Wait for completion
        boolean completed = doneLatch.await(30, TimeUnit.SECONDS);

        // Then: All threads should complete successfully
        executor.shutdown();
        assertTrue(completed, "All threads should complete within timeout");
        assertNull(error.get(), "No errors should occur during concurrent access");
    }

    @Test
    @DisplayName("Should verify tenant-specific connection pool configuration")
    void testTenantSpecificPoolConfiguration() {
        // Given: Each tenant has different pool configuration
        Map<String, TenantDatabaseProperties> tenants = multiTenantDatabaseProperties;
        
        // Then: Verify tenant configurations are loaded correctly
        assertNotNull(tenants, "Tenants configuration should not be null");
        assertEquals(3, tenants.size(), "Should have 3 tenant configurations");
        
        TenantDatabaseProperties tenant1Props = tenants.get("tenant1");
        assertNotNull(tenant1Props, "Tenant1 properties should not be null");
        assertEquals("tenant1Pool", tenant1Props.getPoolName(), "Tenant1 pool name should match");
        assertEquals(5, tenant1Props.getMaxPoolSize(), "Tenant1 max pool size should be 5");
        
        TenantDatabaseProperties tenant2Props = tenants.get("tenant2");
        assertNotNull(tenant2Props, "Tenant2 properties should not be null");
        assertEquals("tenant2Pool", tenant2Props.getPoolName(), "Tenant2 pool name should match");
        
        TenantDatabaseProperties tenant3Props = tenants.get("tenant3");
        assertNotNull(tenant3Props, "Tenant3 properties should not be null");
        assertEquals("tenant3Pool", tenant3Props.getPoolName(), "Tenant3 pool name should match");
    }

    @Test
    @DisplayName("Should execute queries on different tenant databases")
    void testQueriesOnDifferentTenants() throws SQLException {
        // Given: Create different tables in each tenant database
        for (String tenantId : new String[]{"tenant1", "tenant2", "tenant3"}) {
            DataSource ds = targetDataSources.get(tenantId);
            try (Connection conn = ds.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE TABLE IF NOT EXISTS test_table_" + tenantId 
                    + " (id SERIAL PRIMARY KEY, name VARCHAR(100))");
                stmt.execute("INSERT INTO test_table_" + tenantId 
                    + " (name) VALUES ('" + tenantId + "_data')");
            }
        }
        
        // When & Then: Each tenant should be able to query their own table
        for (String tenantId : new String[]{"tenant1", "tenant2", "tenant3"}) {
            DataSource ds = targetDataSources.get(tenantId);
            try (Connection conn = ds.getConnection();
                 Statement stmt = conn.createStatement();
                 var rs = stmt.executeQuery("SELECT COUNT(*) as cnt FROM test_table_" + tenantId)) {
                assertTrue(rs.next(), "Should have results for " + tenantId);
                assertEquals(1, rs.getInt("cnt"), "Should have 1 row for " + tenantId);
            }
        }
    }

    @Test
    @DisplayName("Should handle tenant context switching correctly")
    void testTenantContextSwitching() throws SQLException {
        // Given: Set tenant context to tenant1
        TenantContext.setCurrentTenant("tenant1");
        assertEquals("tenant1", TenantContext.getCurrentTenant(), "Current tenant should be tenant1");
        
        // When: Switch to tenant2
        TenantContext.setCurrentTenant("tenant2");
        
        // Then: Current tenant should be updated
        assertEquals("tenant2", TenantContext.getCurrentTenant(), "Current tenant should be tenant2");
        
        // When: Switch to tenant3
        TenantContext.setCurrentTenant("tenant3");
        
        // Then: Current tenant should be updated again
        assertEquals("tenant3", TenantContext.getCurrentTenant(), "Current tenant should be tenant3");
        
        // When: Clear context
        TenantContext.clear();
    }

    @Test
    @DisplayName("Should verify all datasources are HikariCP instances")
    void testDataSourceTypes() {
        // When & Then: All datasources should be HikariCP instances
        for (Object key : targetDataSources.keySet()) {
            DataSource ds = targetDataSources.get(key);
            assertNotNull(ds, "Datasource for " + key + " should not be null");
            assertEquals("com.zaxxer.hikari.HikariDataSource", 
                ds.getClass().getName(), 
                "Datasource should be HikariDataSource");
        }
    }

    // Helper methods

    /**
     * Creates a test table and inserts data for the specified tenant.
     */
    private void createTestTableAndInsertData(String tenantId, String data) throws SQLException {
        DataSource ds = targetDataSources.get(tenantId);
        try (Connection conn = ds.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS tenant_test (id SERIAL PRIMARY KEY, data VARCHAR(100))");
            stmt.execute("INSERT INTO tenant_test (data) VALUES ('" + data + "')");
        }
    }

    /**
     * Queries test data from the specified tenant's database.
     */
    private String queryTestData(String tenantId) throws SQLException {
        DataSource ds = targetDataSources.get(tenantId);
        try (Connection conn = ds.getConnection();
             Statement stmt = conn.createStatement();
             var rs = stmt.executeQuery("SELECT data FROM tenant_test LIMIT 1")) {
            if (rs.next()) {
                return rs.getString("data");
            }
            return null;
        }
    }
}
