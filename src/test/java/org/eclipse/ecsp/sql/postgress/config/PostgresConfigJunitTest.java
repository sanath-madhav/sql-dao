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

import org.eclipse.ecsp.sql.authentication.CredentialsProvider;
import org.eclipse.ecsp.sql.authentication.DefaultPostgresDbCredentialsProvider;
import org.eclipse.ecsp.sql.exception.SqlDaoException;
import org.eclipse.ecsp.sql.multitenancy.TenantDatabaseProperties;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.Connection;
import java.util.Map;
import javax.sql.DataSource;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link PostgresDbConfig}.
 *
 */
class PostgresConfigJunitTest {

    /** The ctx. */
    @Mock
    ApplicationContext ctx;

    /** The postgres db config. */
    @InjectMocks
    PostgresDbConfig postgresDbConfig;

    /** The connection. */
    @Mock
    Connection connection;

    /** The data source. */
    @Mock
    DataSource dataSource;

    @Mock
    Map<Object, Object> targetDataSources;

    @Mock
    Map<String, CredentialsProvider> credentialsProviderMap;

    /** The default postgres db credentials provider. */
    DefaultPostgresDbCredentialsProvider defaultPostgresDbCredentialsProvider;

    /** The Constant THREE. */
    public static final int THREE = 3;
    
    /** The Constant THIRTY_THREE. */
    public static final int THIRTY_THREE = 30;

    /**
     * Test failure datasource creation.
     */
    @Test
    void testFailureDatasourceCreation() {
        MockitoAnnotations.openMocks(this);
        defaultPostgresDbCredentialsProvider = new DefaultPostgresDbCredentialsProvider();
        ReflectionTestUtils.setField(postgresDbConfig, "credsProviderMap", credentialsProviderMap);
        ReflectionTestUtils.setField(defaultPostgresDbCredentialsProvider, "userName", "testUser");
        ReflectionTestUtils.setField(defaultPostgresDbCredentialsProvider, "password", "testPassword");
        // Create a dummy TenantDatabaseProperties for the test
        TenantDatabaseProperties dbProps = new TenantDatabaseProperties();
        dbProps.setDataSourceRetryCount(THREE);
        dbProps.setDataSourceRetryDelay(THIRTY_THREE);
        dbProps.setConnectionRetryCount(THREE);
        dbProps.setConnectionRetryDelay(THIRTY_THREE);
        dbProps.setCredentialProviderBeanName(
            defaultPostgresDbCredentialsProvider.getClass().getName());
        when(credentialsProviderMap.get(anyString())).thenReturn(defaultPostgresDbCredentialsProvider);
        Exception e = assertThrows(SqlDaoException.class, () -> postgresDbConfig.initDataSource("default", dbProps));
        assertTrue(e.getMessage().contains("Retry Attempts exhausted for creating the datasource"));
    }
}
