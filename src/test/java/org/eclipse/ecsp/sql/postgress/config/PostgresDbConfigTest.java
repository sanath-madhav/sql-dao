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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import io.prometheus.client.CollectorRegistry;
import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Map;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
    @Qualifier("targetDataSources")
    private Map<String, DataSource> targetDataSources;
    
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
        assertNotNull((targetDataSources.get("default")).getConnection());
    }

    /**
     * Test adding credentials provider for a new tenant.
     */
    @Test
    void testAddOrUpdateCredentialsProvider_AddNewTenant() {
        String testTenantId = "tenant1";
        String credProviderClassName = "org.eclipse.ecsp.sql.authentication.DefaultPostgresDbCredentialsProvider";
        
        CredentialsProvider result = postgresDbConfig.addOrUpdateCredentialsProvider(testTenantId, credProviderClassName);
        
        assertNotNull(result);
        
        // Verify it was added to the credsProviderMap
        Map<String, CredentialsProvider> credsProviderMap = 
            (Map<String, CredentialsProvider>) ReflectionTestUtils.getField(postgresDbConfig, "credsProviderMap");
        assertNotNull(credsProviderMap.get(testTenantId));
    }

    /**
     * Test updating credentials provider for an existing tenant.
     */
    @Test
    void testAddOrUpdateCredentialsProvider_UpdateExistingTenant() {
        String testTenantId = "tenant2";
        String credProviderClassName = "org.eclipse.ecsp.sql.authentication.DefaultPostgresDbCredentialsProvider";
        
        // Add first time
        CredentialsProvider result1 = postgresDbConfig.addOrUpdateCredentialsProvider(testTenantId, credProviderClassName);
        assertNotNull(result1);
        
        // Update with same class name
        CredentialsProvider result2 = postgresDbConfig.addOrUpdateCredentialsProvider(testTenantId, credProviderClassName);
        assertNotNull(result2);
        
        // Verify still exists in the map
        Map<String, CredentialsProvider> credsProviderMap = 
            (Map<String, CredentialsProvider>) ReflectionTestUtils.getField(postgresDbConfig, "credsProviderMap");
        assertNotNull(credsProviderMap.get(testTenantId));
    }

    /**
     * Test adding credentials provider with invalid bean name.
     */
    @Test
    void testAddOrUpdateCredentialsProvider_InvalidBeanName() {
        String testTenantId = "tenant3";
        String invalidBeanName = "nonExistentBean";
        
        assertThrows(SqlDaoException.class, 
            () -> postgresDbConfig.addOrUpdateCredentialsProvider(testTenantId, invalidBeanName));
    }

    /**
     * Test removing credentials provider for existing tenant.
     */
    @Test
    void testRemoveCredentialsProvider_ExistingTenant() {
        String testTenantId = "tenant4";
        String credProviderClassName = "org.eclipse.ecsp.sql.authentication.DefaultPostgresDbCredentialsProvider";
        
        // First add a credentials provider
        postgresDbConfig.addOrUpdateCredentialsProvider(testTenantId, credProviderClassName);
        
        Map<String, CredentialsProvider> credsProviderMap = 
            (Map<String, CredentialsProvider>) ReflectionTestUtils.getField(postgresDbConfig, "credsProviderMap");
        assertNotNull(credsProviderMap.get(testTenantId));
        
        // Now remove it
        postgresDbConfig.removeCredentialsProvider(testTenantId);
        
        // Verify it was removed
        assertNull(credsProviderMap.get(testTenantId));
    }

    /**
     * Test removing credentials provider for non-existing tenant.
     * Should not throw any exception.
     */
    @Test
    void testRemoveCredentialsProvider_NonExistingTenant() {
        String nonExistentTenantId = "nonExistentTenant";
        
        // Should not throw any exception
        postgresDbConfig.removeCredentialsProvider(nonExistentTenantId);
        
        Map<String, CredentialsProvider> credsProviderMap = 
            (Map<String, CredentialsProvider>) ReflectionTestUtils.getField(postgresDbConfig, "credsProviderMap");
        assertNull(credsProviderMap.get(nonExistentTenantId));
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
