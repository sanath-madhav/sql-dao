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

package org.eclipse.ecsp.sql.postgress.config;

import org.eclipse.ecsp.sql.SqlDaoApplication;
import org.eclipse.ecsp.sql.authentication.CredentialsProvider;
import org.eclipse.ecsp.sql.authentication.DefaultPostgresDbCredentialsProvider;
import org.eclipse.ecsp.sql.exception.SqlDaoException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import io.prometheus.client.CollectorRegistry;
import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Map;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Test class for {@link PostgresDbConfig}.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { PostgresDbConfig.class, DefaultPostgresDbCredentialsProvider.class, SqlDaoApplication.class })
@TestPropertySource("/application-dao-test.properties")
class PostgresDbConfigTest {

    /** The default postgres db credentials provider. */
    @Autowired
    DefaultPostgresDbCredentialsProvider defaultPostgresDbCredentialsProvider;

    /** The data source. */
    @Autowired
    private Map<Object, Object> targetDataSources;

    /** The PostgresDbConfig instance. */
    @Autowired
    private PostgresDbConfig postgresDbConfig;
    
    /** The postgresql container. */
    @Container
    static PostgreSQLContainer postgresqlContainer = new PostgreSQLContainer("postgres:15").withDatabaseName("test")
            .withUsername("root").withPassword("root");

    /**
     * Sets up postgres.
     */
    @BeforeAll
    static void setUpPostgres() {
        CollectorRegistry.defaultRegistry.clear();
        postgresqlContainer.start();
        System.setProperty("DB_URL", postgresqlContainer.getJdbcUrl());
    }

    /**
     * Test connection.
     *
     * @throws SQLException the SQL exception
     */
    @Test
    void testConnection() throws SQLException {
        assertNotNull(((DataSource) targetDataSources.get("default")).getConnection());
    }

    @Test
    @DisplayName("Should add credentials provider for a new tenant")
    void testAddCredentialsProvider() {
        // Given: A new tenant ID and credentials provider bean name
        String tenantId = "testTenant1";
        String credentialProviderBeanName = "org.eclipse.ecsp.sql.authentication.DefaultPostgresDbCredentialsProvider";

        // When: Adding credentials provider
        CredentialsProvider provider = postgresDbConfig.addOrUpdateCredentialsProvider(
                tenantId, credentialProviderBeanName);

        // Then: Provider should be created and added successfully
        assertNotNull(provider, "Credentials provider should not be null");
        assertNotNull(provider.getUserName(), "Username should not be null");
        assertNotNull(provider.getPassword(), "Password should not be null");
    }

    @Test
    @DisplayName("Should update existing credentials provider")
    void testUpdateCredentialsProvider() {
        // Given: Adding initial credentials provider
        String tenantId = "testTenant2";
        String credentialProviderBeanName = "org.eclipse.ecsp.sql.authentication.DefaultPostgresDbCredentialsProvider";
        
        CredentialsProvider initialProvider = postgresDbConfig.addOrUpdateCredentialsProvider(
                tenantId, credentialProviderBeanName);
        assertNotNull(initialProvider, "Initial provider should not be null");

        // When: Updating with the same tenant ID
        CredentialsProvider updatedProvider = postgresDbConfig.addOrUpdateCredentialsProvider(
                tenantId, credentialProviderBeanName);

        // Then: Update should succeed
        assertNotNull(updatedProvider, "Updated provider should not be null");
        assertNotNull(updatedProvider.getUserName(), "Username should not be null");
        assertNotNull(updatedProvider.getPassword(), "Password should not be null");
    }

    @Test
    @DisplayName("Should throw exception for invalid credentials provider bean name")
    void testAddInvalidCredentialsProvider() {
        // Given: Invalid credentials provider bean name
        String tenantId = "testTenant3";
        String invalidBeanName = "com.invalid.NonExistentCredentialsProvider";

        // When/Then: Should throw SqlDaoException
        assertThrows(SqlDaoException.class, () -> {
            postgresDbConfig.addOrUpdateCredentialsProvider(tenantId, invalidBeanName);
        }, "Should throw SqlDaoException for invalid bean name");
    }

    @Test
    @DisplayName("Should remove credentials provider for a tenant")
    void testRemoveCredentialsProvider() {
        // Given: Adding a credentials provider first
        String tenantId = "testTenant4";
        String credentialProviderBeanName = "org.eclipse.ecsp.sql.authentication.DefaultPostgresDbCredentialsProvider";
        
        CredentialsProvider provider = postgresDbConfig.addOrUpdateCredentialsProvider(
                tenantId, credentialProviderBeanName);
        assertNotNull(provider, "Provider should be added");

        // When: Removing the credentials provider
        postgresDbConfig.removeCredentialsProvider(tenantId);

        // Then: Should complete without error (no exception thrown)
        // Verify by trying to add again - should work as if it's a new tenant
        CredentialsProvider newProvider = postgresDbConfig.addOrUpdateCredentialsProvider(
                tenantId, credentialProviderBeanName);
        assertNotNull(newProvider, "Should be able to add provider again after removal");
    }

    @Test
    @DisplayName("Should handle remove for non-existent tenant gracefully")
    void testRemoveNonExistentCredentialsProvider() {
        // Given: A tenant ID that doesn't exist
        String nonExistentTenantId = "nonExistentTenant";

        // When/Then: Removing non-existent provider should not throw exception
        assertDoesNotThrow(() -> {
            postgresDbConfig.removeCredentialsProvider(nonExistentTenantId);
        }, "Should handle removal of non-existent provider gracefully");
    }

    @Test
    @DisplayName("Should add multiple credentials providers for different tenants")
    void testAddMultipleCredentialsProviders() {
        // Given: Multiple tenant IDs
        String tenant1 = "multiTenant1";
        String tenant2 = "multiTenant2";
        String tenant3 = "multiTenant3";
        String credentialProviderBeanName = "org.eclipse.ecsp.sql.authentication.DefaultPostgresDbCredentialsProvider";

        // When: Adding credentials providers for multiple tenants
        CredentialsProvider provider1 = postgresDbConfig.addOrUpdateCredentialsProvider(
                tenant1, credentialProviderBeanName);
        CredentialsProvider provider2 = postgresDbConfig.addOrUpdateCredentialsProvider(
                tenant2, credentialProviderBeanName);
        CredentialsProvider provider3 = postgresDbConfig.addOrUpdateCredentialsProvider(
                tenant3, credentialProviderBeanName);

        // Then: All providers should be created successfully
        assertNotNull(provider1, "Provider1 should not be null");
        assertNotNull(provider2, "Provider2 should not be null");
        assertNotNull(provider3, "Provider3 should not be null");

        // Cleanup
        postgresDbConfig.removeCredentialsProvider(tenant1);
        postgresDbConfig.removeCredentialsProvider(tenant2);
        postgresDbConfig.removeCredentialsProvider(tenant3);
    }

    /**
     * Tear up postgres server.
     */
    @AfterAll
    static void tearUpPostgresServer() {
        CollectorRegistry.defaultRegistry.clear();
        postgresqlContainer.stop();
    }
}
