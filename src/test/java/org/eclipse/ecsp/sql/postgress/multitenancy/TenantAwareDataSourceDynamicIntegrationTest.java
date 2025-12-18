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
import org.eclipse.ecsp.sql.multitenancy.MultiTenantDatabaseProperties;
import org.eclipse.ecsp.sql.multitenancy.TenantAwareDataSource;
import org.eclipse.ecsp.sql.multitenancy.TenantContext;
import org.eclipse.ecsp.sql.multitenancy.TenantDatabaseProperties;
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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test class for dynamic multi-tenant operations in TenantAwareDataSource.
 * Tests the runtime addition, update, and removal of tenant-specific datasources.
 *
 * <p>This test suite verifies:
 * <ul>
 *   <li>Adding new tenant datasources at runtime</li>
 *   <li>Updating existing tenant datasources</li>
 *   <li>Removing tenant datasources</li>
 *   <li>Connection validity after operations</li>
 *   <li>Proper cleanup and resource management</li>
 * </ul>
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { SqlDaoApplication.class })
@TestPropertySource(locations = "classpath:application-dao-dynamic-multitenant-test.properties")
@Testcontainers
class TenantAwareDataSourceDynamicIntegrationTest {

    @Autowired
    private TenantAwareDataSource tenantAwareDataSource;

    @Autowired
    @Qualifier("targetDataSources")
    private Map<Object, Object> targetDataSources;

    @Autowired
    private MultiTenantDatabaseProperties multiTenantDbProperties;

    /** Testcontainer for existing tenants */
    @Container
    @SuppressWarnings("resource")
    static PostgreSQLContainer<?> tenant1Container = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("tenant1db")
            .withUsername("root")
            .withPassword("root");

    @Container
    @SuppressWarnings("resource")
    static PostgreSQLContainer<?> tenant2Container = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("tenant2db")
            .withUsername("root")
            .withPassword("root");

    @Container
    @SuppressWarnings("resource")
    static PostgreSQLContainer<?> tenant3Container = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("tenant3db")
            .withUsername("root")
            .withPassword("root");

    /** Testcontainer for new dynamic tenant */
    @Container
    @SuppressWarnings("resource")
    static PostgreSQLContainer<?> newTenantContainer = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("newtenant_db")
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
        newTenantContainer.start();
        
        // Set system properties for existing tenants
        System.setProperty("DB_URL_TENANT1", tenant1Container.getJdbcUrl());
        System.setProperty("DB_URL_TENANT2", tenant2Container.getJdbcUrl());
        System.setProperty("DB_URL_TENANT3", tenant3Container.getJdbcUrl());
    }

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        CollectorRegistry.defaultRegistry.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        // Clean up any tenants added during tests, keep only the initial 3
        Set<String> initialTenants = new HashSet<>(Arrays.asList("tenant1", "tenant2", "tenant3"));
        Set<Object> tenantsToRemove = new HashSet<>();
        
        for (Object tenantKey : targetDataSources.keySet()) {
            if (!initialTenants.contains(tenantKey)) {
                tenantsToRemove.add(tenantKey);
            }
        }
        
        for (Object tenantKey : tenantsToRemove) {
            tenantAwareDataSource.removeTenantDataSource((String) tenantKey);
        }
    }

    @AfterAll
    static void tearDownContainers() {
        CollectorRegistry.defaultRegistry.clear();
        tenant1Container.stop();
        tenant2Container.stop();
        tenant3Container.stop();
        newTenantContainer.stop();
    }

    @Test
    @DisplayName("Should add new tenant datasource successfully")
    void testAddNewTenantDataSource() throws SQLException {
        // Given: Initial state with 3 tenants
        int initialSize = targetDataSources.size();
        assertEquals(3, initialSize, "Should start with 3 tenants");

        // When: Adding a new tenant datasource
        TenantDatabaseProperties newTenantProps = createTenantProperties(
                "tenant4",
                newTenantContainer.getJdbcUrl(),
                "root",
                "root"
        );

        boolean result = tenantAwareDataSource.addOrUpdateTenantDataSource("tenant4", newTenantProps);

        // Then: Datasource should be added successfully
        assertTrue(result, "Add operation should return true");
        assertEquals(4, targetDataSources.size(), "Should have 4 tenants now");
        assertTrue(targetDataSources.containsKey("tenant4"), "Should contain tenant4");
        assertTrue(multiTenantDbProperties.getProfile().containsKey("tenant4"), 
                   "MultiTenantDbProperties should contain tenant4");

        // Verify connection is working
        DataSource tenant4DataSource = (DataSource) targetDataSources.get("tenant4");
        assertNotNull(tenant4DataSource, "Tenant4 datasource should not be null");
        
        try (Connection connection = tenant4DataSource.getConnection()) {
            assertNotNull(connection, "Connection should not be null");
            assertTrue(connection.isValid(5), "Connection should be valid");
        }
    }

    @Test
    @DisplayName("Should update existing tenant datasource successfully")
    void testUpdateExistingTenantDataSource() throws SQLException {
        // Given: Tenant1 already exists
        assertTrue(targetDataSources.containsKey("tenant1"), "Tenant1 should exist");
        DataSource originalDataSource = (DataSource) targetDataSources.get("tenant1");

        // When: Updating tenant1 with new container configuration
        TenantDatabaseProperties updatedProps = createTenantProperties(
                "tenant1",
                newTenantContainer.getJdbcUrl(),
                "root",
                "root"
        );

        boolean result = tenantAwareDataSource.addOrUpdateTenantDataSource("tenant1", updatedProps);

        // Then: Datasource should be updated successfully
        assertTrue(result, "Update operation should return true");
        assertEquals(3, targetDataSources.size(), "Should still have 3 tenants");
        assertTrue(multiTenantDbProperties.getProfile().containsKey("tenant1"), 
                   "MultiTenantDbProperties should still contain tenant1");
        
        DataSource updatedDataSource = (DataSource) targetDataSources.get("tenant1");
        assertNotNull(updatedDataSource, "Updated tenant1 datasource should not be null");
        assertNotSame(originalDataSource, updatedDataSource, "Datasource instance should be different");

        // Verify new connection works
        try (Connection connection = updatedDataSource.getConnection()) {
            assertNotNull(connection, "Connection should not be null");
            assertTrue(connection.isValid(5), "Connection should be valid");
        }
    }

    @Test
    @DisplayName("Should remove tenant datasource successfully")
    void testRemoveTenantDataSource() {
        // Given: Adding a temporary tenant first
        TenantDatabaseProperties tempTenantProps = createTenantProperties(
                "tempTenant",
                newTenantContainer.getJdbcUrl(),
                "root",
                "root"
        );
        tenantAwareDataSource.addOrUpdateTenantDataSource("tempTenant", tempTenantProps);
        assertTrue(targetDataSources.containsKey("tempTenant"), "Temp tenant should be added");
        int sizeBeforeRemove = targetDataSources.size();

        // When: Removing the tenant datasource
        boolean result = tenantAwareDataSource.removeTenantDataSource("tempTenant");

        // Then: Datasource should be removed successfully
        assertTrue(result, "Remove operation should return true");
        assertEquals(sizeBeforeRemove - 1, targetDataSources.size(), "Should have one less tenant");
        assertFalse(targetDataSources.containsKey("tempTenant"), "Temp tenant should be removed");
        assertFalse(multiTenantDbProperties.getProfile().containsKey("tempTenant"), 
                    "MultiTenantDbProperties should not contain tempTenant after removal");
    }

    @Test
    @DisplayName("Should handle null tenant ID in add operation")
    void testAddWithNullTenantId() {
        // Given: Null tenant ID
        TenantDatabaseProperties props = createTenantProperties(
                "test",
                newTenantContainer.getJdbcUrl(),
                "root",
                "root"
        );

        // When: Attempting to add with null tenant ID
        boolean result = tenantAwareDataSource.addOrUpdateTenantDataSource(null, props);

        // Then: Operation should fail gracefully
        assertFalse(result, "Add operation with null tenant ID should return false");
    }

    @Test
    @DisplayName("Should handle empty tenant ID in add operation")
    void testAddWithEmptyTenantId() {
        // Given: Empty tenant ID
        TenantDatabaseProperties props = createTenantProperties(
                "test",
                newTenantContainer.getJdbcUrl(),
                "root",
                "root"
        );

        // When: Attempting to add with empty tenant ID
        boolean result = tenantAwareDataSource.addOrUpdateTenantDataSource("   ", props);

        // Then: Operation should fail gracefully
        assertFalse(result, "Add operation with empty tenant ID should return false");
    }

    @Test
    @DisplayName("Should handle null database properties in add operation")
    void testAddWithNullProperties() {
        // When: Attempting to add with null properties
        boolean result = tenantAwareDataSource.addOrUpdateTenantDataSource("tenant5", null);

        // Then: Operation should fail gracefully
        assertFalse(result, "Add operation with null properties should return false");
        assertFalse(targetDataSources.containsKey("tenant5"), "Tenant5 should not be added");
    }

    @Test
    @DisplayName("Should handle null tenant ID in remove operation")
    void testRemoveWithNullTenantId() {
        // When: Attempting to remove with null tenant ID
        boolean result = tenantAwareDataSource.removeTenantDataSource(null);

        // Then: Operation should fail gracefully
        assertFalse(result, "Remove operation with null tenant ID should return false");
    }

    @Test
    @DisplayName("Should handle empty tenant ID in remove operation")
    void testRemoveWithEmptyTenantId() {
        // When: Attempting to remove with empty tenant ID
        boolean result = tenantAwareDataSource.removeTenantDataSource("   ");

        // Then: Operation should fail gracefully
        assertFalse(result, "Remove operation with empty tenant ID should return false");
    }

    @Test
    @DisplayName("Should handle non-existent tenant in remove operation")
    void testRemoveNonExistentTenant() {
        // When: Attempting to remove a non-existent tenant
        boolean result = tenantAwareDataSource.removeTenantDataSource("nonExistentTenant");

        // Then: Operation should complete without error
        assertTrue(result, "Remove operation should return true even for non-existent tenant");
    }

    @Test
    @DisplayName("Should trim whitespace from tenant ID in add operation")
    void testAddWithWhitespaceTenantId() {
        // Given: Tenant ID with leading/trailing whitespace
        TenantDatabaseProperties props = createTenantProperties(
                "tenant5",
                newTenantContainer.getJdbcUrl(),
                "root",
                "root"
        );

        // When: Adding with whitespace tenant ID
        boolean result = tenantAwareDataSource.addOrUpdateTenantDataSource("  tenant5  ", props);

        // Then: Should be added with trimmed ID
        assertTrue(result, "Add operation should succeed");
        assertTrue(targetDataSources.containsKey("tenant5"), "Should contain tenant5 (trimmed)");
    }

    @Test
    @DisplayName("Should allow multiple add and remove operations")
    void testMultipleAddAndRemoveOperations() throws SQLException {
        // Given: Starting state
        int initialSize = targetDataSources.size();

        // When: Adding multiple tenants
        for (int i = 1; i <= 3; i++) {
            TenantDatabaseProperties props = createTenantProperties(
                    "dynamicTenant" + i,
                    newTenantContainer.getJdbcUrl(),
                    "root",
                    "root"
            );
            boolean addResult = tenantAwareDataSource.addOrUpdateTenantDataSource("dynamicTenant" + i, props);
            assertTrue(addResult, "Add operation " + i + " should succeed");
        }

        // Then: All should be added
        assertEquals(initialSize + 3, targetDataSources.size(), "Should have 3 more tenants");

        // When: Removing them
        for (int i = 1; i <= 3; i++) {
            boolean removeResult = tenantAwareDataSource.removeTenantDataSource("dynamicTenant" + i);
            assertTrue(removeResult, "Remove operation " + i + " should succeed");
        }

        // Then: All should be removed
        assertEquals(initialSize, targetDataSources.size(), "Should be back to initial size");
    }

    @Test
    @DisplayName("Should maintain data isolation after add operation")
    void testDataIsolationAfterAdd() throws SQLException {
        // Given: Adding a new tenant
        TenantDatabaseProperties newTenantProps = createTenantProperties(
                "isolationTest",
                newTenantContainer.getJdbcUrl(),
                "root",
                "root"
        );
        tenantAwareDataSource.addOrUpdateTenantDataSource("isolationTest", newTenantProps);

        // When: Creating tables in different tenants
        DataSource tenant1DS = (DataSource) targetDataSources.get("tenant1");
        DataSource isolationTestDS = (DataSource) targetDataSources.get("isolationTest");

        try (Connection conn1 = tenant1DS.getConnection();
             Statement stmt1 = conn1.createStatement()) {
            stmt1.execute("CREATE TABLE IF NOT EXISTS test_table1 (id INT)");
            stmt1.execute("INSERT INTO test_table1 VALUES (100)");
        }

        try (Connection conn2 = isolationTestDS.getConnection();
             Statement stmt2 = conn2.createStatement()) {
            stmt2.execute("CREATE TABLE IF NOT EXISTS test_table1 (id INT)");
            stmt2.execute("INSERT INTO test_table1 VALUES (200)");
        }

        // Then: Data should be isolated
        try (Connection conn1 = tenant1DS.getConnection();
             Statement stmt1 = conn1.createStatement();
             var rs1 = stmt1.executeQuery("SELECT id FROM test_table1")) {
            assertTrue(rs1.next());
            assertEquals(100, rs1.getInt(1), "Tenant1 should have its own data");
        }

        try (Connection conn2 = isolationTestDS.getConnection();
             Statement stmt2 = conn2.createStatement();
             var rs2 = stmt2.executeQuery("SELECT id FROM test_table1")) {
            assertTrue(rs2.next());
            assertEquals(200, rs2.getInt(1), "IsolationTest tenant should have its own data");
        }

        // Cleanup
        tenantAwareDataSource.removeTenantDataSource("isolationTest");
    }

    @Test
    @DisplayName("Should handle credentials provider during add operation")
    void testCredentialsProviderHandling() throws SQLException {
        // Given: Properties with credentials provider
        TenantDatabaseProperties propsWithProvider = createTenantProperties(
                "providerTest",
                newTenantContainer.getJdbcUrl(),
                "root",
                "root"
        );
        // Credentials provider is set in createTenantProperties

        // When: Adding tenant with credentials provider
        boolean result = tenantAwareDataSource.addOrUpdateTenantDataSource("providerTest", propsWithProvider);

        // Then: Operation should succeed
        assertTrue(result, "Add operation with credentials provider should succeed");
        assertTrue(targetDataSources.containsKey("providerTest"), "Tenant should be added");
        assertTrue(multiTenantDbProperties.getProfile().containsKey("providerTest"), 
                   "MultiTenantDbProperties should contain providerTest");

        // Verify connection works
        DataSource ds = (DataSource) targetDataSources.get("providerTest");
        assertNotNull(ds, "Datasource should not be null");
        try (Connection conn = ds.getConnection()) {
            assertTrue(conn.isValid(5), "Connection should be valid");
        }

        // Cleanup
        tenantAwareDataSource.removeTenantDataSource("providerTest");
    }

    @Test
    @DisplayName("Should update tenant configuration successfully")
    void testUpdateTenantConfiguration() throws SQLException {
        // Given: Adding initial tenant with specific pool size
        TenantDatabaseProperties initialProps = createTenantProperties(
                "configTest",
                newTenantContainer.getJdbcUrl(),
                "root",
                "root"
        );
        initialProps.setMaxPoolSize(5);
        initialProps.setPoolName("initialPool");

        tenantAwareDataSource.addOrUpdateTenantDataSource("configTest", initialProps);

        // When: Updating with different configuration
        TenantDatabaseProperties updatedProps = createTenantProperties(
                "configTest",
                newTenantContainer.getJdbcUrl(),
                "root",
                "root"
        );
        updatedProps.setMaxPoolSize(10);
        updatedProps.setPoolName("updatedPool");

        boolean result = tenantAwareDataSource.addOrUpdateTenantDataSource("configTest", updatedProps);

        // Then: Update should succeed and connection should work
        assertTrue(result, "Update operation should succeed");
        DataSource updatedDS = (DataSource) targetDataSources.get("configTest");
        assertNotNull(updatedDS, "Updated datasource should exist");

        try (Connection conn = updatedDS.getConnection()) {
            assertTrue(conn.isValid(5), "Updated datasource connection should be valid");
        }

        // Cleanup
        tenantAwareDataSource.removeTenantDataSource("configTest");
    }

    /**
     * Helper method to create TenantDatabaseProperties for testing.
     */
    private TenantDatabaseProperties createTenantProperties(
            String poolName,
            String jdbcUrl,
            String username,
            String password) {
        
        TenantDatabaseProperties props = new TenantDatabaseProperties();
        props.setJdbcUrl(jdbcUrl);
        props.setUserName(username);
        props.setPassword(password);
        props.setDriverClassName("org.postgresql.Driver");
        props.setPoolName(poolName + "Pool");
        props.setMaxPoolSize(5);
        props.setMinPoolSize(1);
        props.setConnectionTimeoutMs(30000);
        props.setCredentialProviderBeanName(
                "org.eclipse.ecsp.sql.authentication.PostgresMultiTenantCredentialsProvider");
        
        return props;
    }
}
