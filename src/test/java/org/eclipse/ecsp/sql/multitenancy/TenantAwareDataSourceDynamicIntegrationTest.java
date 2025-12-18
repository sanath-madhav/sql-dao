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

package org.eclipse.ecsp.sql.multitenancy;

import io.prometheus.client.CollectorRegistry;
import org.eclipse.ecsp.sql.SqlDaoApplication;
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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test class for TenantAwareDataSource with dynamic tenant management.
 * Tests adding, updating, and removing tenant datasources at runtime, as well as
 * tenant context switching and data isolation.
 *
 * @author smadhavmv
 * @version 1.0
 * @since 2025-12-18
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { SqlDaoApplication.class })
@TestPropertySource(locations = "classpath:application-dao-dynamic-tenant-test.properties")
@Testcontainers
class TenantAwareDataSourceDynamicIntegrationTest {

    @Autowired
    private TenantAwareDataSource tenantAwareDataSource;

    @Autowired
    @Qualifier("targetDataSources")
    private Map<String, DataSource> targetDataSources;

    @Autowired
    @Qualifier("tenantConfigMap")
    private Map<String, TenantDatabaseProperties> tenantConfigMap;

    @Autowired
    private PostgresDbConfig postgresDbConfig;

    @Autowired
    @Qualifier("dataSource")
    private DataSource routingDataSource;

    /** Testcontainer for initial tenant1 */
    @Container
    @SuppressWarnings("resource")
    static PostgreSQLContainer<?> tenant1Container = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("tenant1db")
            .withUsername("root")
            .withPassword("root");

    /** Testcontainer for initial tenant2 */
    @Container
    @SuppressWarnings("resource")
    static PostgreSQLContainer<?> tenant2Container = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("tenant2db")
            .withUsername("root")
            .withPassword("root");

    /** Testcontainer for dynamically added tenant (tenant4) */
    @Container
    @SuppressWarnings("resource")
    static PostgreSQLContainer<?> tenant4Container = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("tenant4db")
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
        tenant4Container.start();
        
        // Set system properties for each tenant
        System.setProperty("DB_URL_TENANT1", tenant1Container.getJdbcUrl());
        System.setProperty("DB_URL_TENANT2", tenant2Container.getJdbcUrl());
        System.setProperty("DB_URL_TENANT4", tenant4Container.getJdbcUrl());
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
        tenant4Container.stop();
    }

    @Test
    @DisplayName("Should initialize with initial tenant datasources")
    void testInitialTenantDataSourcesInitialization() {
        // Then: Initial tenant datasources should be initialized
        assertNotNull(targetDataSources, "Target datasources map should not be null");
        assertTrue(targetDataSources.size() >= 2, "Should have at least 2 initial tenant datasources");
        
        assertTrue(targetDataSources.containsKey("tenant1"), "Should contain tenant1 datasource");
        assertTrue(targetDataSources.containsKey("tenant2"), "Should contain tenant2 datasource");
    }

    @Test
    @DisplayName("Should add new tenant datasource dynamically")
    void testAddNewTenantDatasource() throws SQLException {
        // Given: Tenant4 does not exist initially
        assertFalse(targetDataSources.containsKey("tenant4"), 
            "Tenant4 should not exist initially");
        
        // When: Adding tenant4 datasource
        TenantDatabaseProperties tenant4Props = createTenantProperties(
            System.getProperty("DB_URL_TENANT4"), "tenant4Pool");
        
        boolean result = tenantAwareDataSource.addOrUpdateTenantDataSource("tenant4", tenant4Props);
        
        // Then: Tenant4 should be added successfully
        assertTrue(result, "Adding tenant4 should succeed");
        assertTrue(targetDataSources.containsKey("tenant4"), 
            "Tenant4 datasource should be added");
        
        // Verify connection works directly from targetDataSources
        DataSource tenant4DataSource = targetDataSources.get("tenant4");
        try (Connection connection = tenant4DataSource.getConnection()) {
            assertNotNull(connection, "Connection should not be null");
            assertTrue(connection.isValid(5), "Connection should be valid");
        }
        
        // Cleanup
        tenantAwareDataSource.removeTenantDataSource("tenant4");
    }

    @Test
    @DisplayName("Should update existing tenant datasource")
    void testUpdateExistingTenantDatasource() throws SQLException {
        // Given: Tenant1 exists
        assertTrue(targetDataSources.containsKey("tenant1"), "Tenant1 should exist");
        
        // When: Updating tenant1 datasource with new properties
        TenantDatabaseProperties updatedProps = createTenantProperties(
            System.getProperty("DB_URL_TENANT1"), "tenant1PoolUpdated");
        updatedProps.setMaxPoolSize(10);
        
        boolean result = tenantAwareDataSource.addOrUpdateTenantDataSource("tenant1", updatedProps);
        
        // Then: Tenant1 should be updated successfully
        assertTrue(result, "Updating tenant1 should succeed");
        assertTrue(targetDataSources.containsKey("tenant1"), 
            "Tenant1 datasource should still exist");
        
        // Verify new connection works directly from targetDataSources
        DataSource updatedDataSource = targetDataSources.get("tenant1");
        try (Connection connection = updatedDataSource.getConnection()) {
            assertNotNull(connection, "Connection should not be null");
            assertTrue(connection.isValid(5), "Connection should be valid");
        }
    }

    @Test
    @DisplayName("Should remove tenant datasource dynamically")
    void testRemoveTenantDatasource() {
        // Given: Add tenant4 first
        TenantDatabaseProperties tenant4Props = createTenantProperties(
            System.getProperty("DB_URL_TENANT4"), "tenant4Pool");
        tenantAwareDataSource.addOrUpdateTenantDataSource("tenant4", tenant4Props);
        assertTrue(targetDataSources.containsKey("tenant4"), "Tenant4 should exist");
        
        // When: Removing tenant4
        boolean result = tenantAwareDataSource.removeTenantDataSource("tenant4");
        
        // Then: Tenant4 should be removed successfully
        assertTrue(result, "Removing tenant4 should succeed");
        assertFalse(targetDataSources.containsKey("tenant4"), 
            "Tenant4 datasource should be removed");
    }

    @Test
    @DisplayName("Should isolate data between tenants after dynamic addition")
    void testDataIsolationWithDynamicTenants() throws SQLException {
        // Given: Add tenant4 dynamically
        TenantDatabaseProperties tenant4Props = createTenantProperties(
            System.getProperty("DB_URL_TENANT4"), "tenant4Pool");
        tenantAwareDataSource.addOrUpdateTenantDataSource("tenant4", tenant4Props);
        
        // When: Create test data in each tenant
        createTestTableAndInsertData("tenant1", "Tenant1Data");
        createTestTableAndInsertData("tenant2", "Tenant2Data");
        createTestTableAndInsertData("tenant4", "Tenant4Data");
        
        // Then: Each tenant should only see their own data
        String tenant1Data = queryTestData("tenant1");
        assertEquals("Tenant1Data", tenant1Data, "Tenant1 should see only its data");
        
        String tenant2Data = queryTestData("tenant2");
        assertEquals("Tenant2Data", tenant2Data, "Tenant2 should see only its data");
        
        String tenant4Data = queryTestData("tenant4");
        assertEquals("Tenant4Data", tenant4Data, "Tenant4 should see only its data");
        
        // Cleanup
        tenantAwareDataSource.removeTenantDataSource("tenant4");
    }

    @Test
    @DisplayName("Should switch tenant context and route to correct datasource")
    void testTenantContextSwitching() throws SQLException {
        // Given: Multiple tenants with test data
        createTestTableAndInsertData("tenant1", "FromTenant1");
        createTestTableAndInsertData("tenant2", "FromTenant2");
        
        // When & Then: Query directly from each tenant's datasource
        String data1 = queryTestData("tenant1");
        assertEquals("FromTenant1", data1, "Should get data from tenant1");
        
        String data2 = queryTestData("tenant2");
        assertEquals("FromTenant2", data2, "Should get data from tenant2");
    }

    @Test
    @DisplayName("Should handle adding tenant with null properties")
    void testAddTenantWithNullProperties() {
        // When: Attempting to add tenant with null properties
        boolean result = tenantAwareDataSource.addOrUpdateTenantDataSource("tenant5", null);
        
        // Then: Operation should fail gracefully
        assertFalse(result, "Adding tenant with null properties should fail");
        assertFalse(targetDataSources.containsKey("tenant5"), 
            "Tenant5 should not be added");
    }

    @Test
    @DisplayName("Should handle adding tenant with empty tenant ID")
    void testAddTenantWithEmptyId() {
        // When: Attempting to add tenant with empty ID
        TenantDatabaseProperties props = createTenantProperties(
            System.getProperty("DB_URL_TENANT4"), "emptyIdPool");
        boolean result = tenantAwareDataSource.addOrUpdateTenantDataSource("", props);
        
        // Then: Operation should fail gracefully
        assertFalse(result, "Adding tenant with empty ID should fail");
    }

    @Test
    @DisplayName("Should handle removing non-existent tenant")
    void testRemoveNonExistentTenant() {
        // Given: Tenant99 does not exist
        assertFalse(targetDataSources.containsKey("tenant99"), 
            "Tenant99 should not exist");
        
        // When: Attempting to remove non-existent tenant
        boolean result = tenantAwareDataSource.removeTenantDataSource("tenant99");
        
        // Then: Operation should succeed without errors
        assertTrue(result, "Removing non-existent tenant should succeed");
    }

    @Test
    @DisplayName("Should verify routing datasource is properly initialized")
    void testRoutingDataSourceInitialization() {
        // Then: Routing datasource should be initialized
        assertNotNull(routingDataSource, "Routing datasource should not be null");
        assertTrue(routingDataSource instanceof TenantRoutingDataSource, 
            "Should be instance of TenantRoutingDataSource");
    }

    @Test
    @DisplayName("Should get valid connections for all initial tenants")
    void testConnectionsForAllInitialTenants() throws SQLException {
        // When & Then: All initial tenants should provide valid connections
        for (String tenantId : targetDataSources.keySet()) {
            DataSource ds = targetDataSources.get(tenantId);
            assertNotNull(ds, "Datasource for " + tenantId + " should not be null");
            
            try (Connection connection = ds.getConnection()) {
                assertNotNull(connection, "Connection for " + tenantId + " should not be null");
                assertTrue(connection.isValid(5), 
                    "Connection for " + tenantId + " should be valid");
                assertFalse(connection.isClosed(), 
                    "Connection for " + tenantId + " should not be closed");
            }
        }
    }

    @Test
    @DisplayName("Should handle multiple dynamic additions and removals")
    void testMultipleDynamicOperations() throws SQLException {
        // When: Perform multiple add/remove operations
        TenantDatabaseProperties tenant4Props = createTenantProperties(
            System.getProperty("DB_URL_TENANT4"), "tenant4Pool");
        
        // Add tenant4
        assertTrue(tenantAwareDataSource.addOrUpdateTenantDataSource("tenant4", tenant4Props), 
            "First add should succeed");
        assertTrue(targetDataSources.containsKey("tenant4"), "Tenant4 should exist");
        
        // Verify it works directly
        DataSource tenant4DataSource = targetDataSources.get("tenant4");
        try (Connection conn = tenant4DataSource.getConnection()) {
            assertTrue(conn.isValid(5), "Connection should be valid");
        }
        
        // Remove tenant4
        assertTrue(tenantAwareDataSource.removeTenantDataSource("tenant4"), 
            "Remove should succeed");
        assertFalse(targetDataSources.containsKey("tenant4"), 
            "Tenant4 should be removed");
        
        // Add tenant4 again
        assertTrue(tenantAwareDataSource.addOrUpdateTenantDataSource("tenant4", tenant4Props), 
            "Second add should succeed");
        assertTrue(targetDataSources.containsKey("tenant4"), 
            "Tenant4 should exist again");
        
        // Cleanup
        tenantAwareDataSource.removeTenantDataSource("tenant4");
    }

    // Helper methods

    /**
     * Creates tenant database properties for testing.
     */
    private TenantDatabaseProperties createTenantProperties(String jdbcUrl, String poolName) {
        TenantDatabaseProperties props = new TenantDatabaseProperties();
        props.setJdbcUrl(jdbcUrl);
        props.setUserName("root");
        props.setPassword("root");
        props.setDriverClassName("org.postgresql.Driver");
        props.setPoolName(poolName);
        props.setMaxPoolSize(5);
        props.setCredentialProviderBeanName(
            "org.eclipse.ecsp.sql.authentication.PostgresMultiTenantTestCredentialsProvider");
        return props;
    }

    /**
     * Creates a test table and inserts data for the specified tenant.
     */
    private void createTestTableAndInsertData(String tenantId, String data) throws SQLException {
        DataSource ds = targetDataSources.get(tenantId);
        try (Connection conn = ds.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS tenant_test");
            stmt.execute("CREATE TABLE tenant_test (id SERIAL PRIMARY KEY, data VARCHAR(100))");
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
             ResultSet rs = stmt.executeQuery("SELECT data FROM tenant_test LIMIT 1")) {
            if (rs.next()) {
                return rs.getString("data");
            }
            return null;
        }
    }

    /**
     * Queries test data using the routing datasource (respects TenantContext).
     */
    private String queryTestDataWithRoutingDataSource() throws SQLException {
        try (Connection conn = routingDataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT data FROM tenant_test LIMIT 1")) {
            if (rs.next()) {
                return rs.getString("data");
            }
            return null;
        }
    }
}
