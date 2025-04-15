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

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import org.eclipse.ecsp.sql.authentication.CredentialsProvider;
import org.eclipse.ecsp.sql.authentication.DefaultPostgresDbCredentialsProvider;
import org.eclipse.ecsp.sql.exception.SqlDaoException;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link PostgresDbConfig}.
 *
 */
class PostgresConnectionsTest {

    /** The ctx. */
    @Mock
    ApplicationContext ctx;

    /** The postgres db config. */
    @InjectMocks
    PostgresDbConfig postgresDbConfig;

    /** The default postgres db credentials provider. */
    @Mock
    DefaultPostgresDbCredentialsProvider defaultPostgresDbCredentialsProvider;

    /** The credentials provider. */
    @Mock
    CredentialsProvider credentialsProvider;

    /** The connection. */
    @Mock
    Connection connection;

    /** The data source. */
    @Mock
    HikariDataSource dataSource;

    /** The Constant THREE. */
    public static final int THREE = 3;
    
    /** The Constant TWO. */
    public static final int TWO = 2;
    
    /** The Constant ZERO. */
    public static final int ZERO = 0;
    
    /** The Constant THIRTY_THREE. */
    public static final int THIRTY_THREE = 30;
    
    /** The Constant SIXTY_THOUSAND. */
    public static final int SIXTY_THOUSAND = 60000;

    /** The hikari pool mx bean. */
    HikariPoolMXBean hikariPoolMxBean = new HikariPoolMXBean() {
        @Override
        public int getIdleConnections() {
            return 0;
        }

        @Override
        public int getActiveConnections() {
            return 0;
        }

        @Override
        public int getTotalConnections() {
            return 0;
        }

        @Override
        public int getThreadsAwaitingConnection() {
            return 0;
        }

        @Override
        public void softEvictConnections() {
            // no implementation required for testing
        }

        @Override
        public void suspendPool() {
            // no implementation required for testing
        }

        @Override
        public void resumePool() {
            // no implementation required for testing
        }
    };

    /**
     * Test failure connection creation.
     *
     * @throws SQLException the SQL exception
     */
    @Test
    void testFailureConnectionCreation() throws SQLException {
        MockitoAnnotations.openMocks(this);
        postgresDbConfig.dataSource();
        when(credentialsProvider.getUserName()).thenReturn("test");
        when(credentialsProvider.getPassword()).thenReturn("pass");
        ReflectionTestUtils.setField(postgresDbConfig, "dataSourceRetryCount", ZERO);
        ReflectionTestUtils.setField(postgresDbConfig, "dataSourceRetryDelay", THIRTY_THREE);
        ReflectionTestUtils.setField(postgresDbConfig, "connectionRetryCount", THREE);
        ReflectionTestUtils.setField(postgresDbConfig, "connectionRetryDelay", THIRTY_THREE);
        ReflectionTestUtils.setField(postgresDbConfig, "jdbcUrl", "url");
        ReflectionTestUtils.setField(postgresDbConfig, "userName", "test");
        ReflectionTestUtils.setField(postgresDbConfig, "password", "pass");
        ReflectionTestUtils.setField(postgresDbConfig, "driverClassName", "org.postgresql.Driver");
        ReflectionTestUtils.setField(postgresDbConfig, "maxPoolSize", TWO);
        when(dataSource.getConnection()).thenThrow(new SQLException());
        when(dataSource.getHikariPoolMXBean()).thenReturn(hikariPoolMxBean);
        Exception e = assertThrows(SqlDaoException.class,
                () -> postgresDbConfig.initDataSource());
        assertTrue(e.getMessage().contains("All retry attempts are exhausted for connection creation"));
    }


    /**
     * Test connection creation with ssl.
     *
     * @throws SQLException the SQL exception
     * @throws InterruptedException the interrupted exception
     */
    @Test
    void testConnectionCreationWithSsl() throws SQLException, InterruptedException {
        MockitoAnnotations.openMocks(this);
        postgresDbConfig.dataSource();
        when(credentialsProvider.getUserName()).thenReturn("test");
        when(credentialsProvider.getPassword()).thenReturn("pass");
        ReflectionTestUtils.setField(postgresDbConfig, "dataSourceRetryCount", ZERO);
        ReflectionTestUtils.setField(postgresDbConfig, "dataSourceRetryDelay", THIRTY_THREE);
        ReflectionTestUtils.setField(postgresDbConfig, "connectionRetryCount", THREE);
        ReflectionTestUtils.setField(postgresDbConfig, "connectionRetryDelay", THIRTY_THREE);
        ReflectionTestUtils.setField(postgresDbConfig, "jdbcUrl", "jdbc:postgresql://localhost:5432/postgres");
        ReflectionTestUtils.setField(postgresDbConfig, "userName", "test");
        ReflectionTestUtils.setField(postgresDbConfig, "password", "pass");
        ReflectionTestUtils.setField(postgresDbConfig, "driverClassName", "org.postgresql.Driver");
        ReflectionTestUtils.setField(postgresDbConfig, "maxPoolSize", TWO);
        ReflectionTestUtils.setField(postgresDbConfig, "authMechanism", "one-way-tls");
        ReflectionTestUtils.setField(postgresDbConfig, "rootCrtPath", "src/test/root.crt");
        ReflectionTestUtils.setField(postgresDbConfig, "sslMode", "verify-full");
        ReflectionTestUtils.setField(postgresDbConfig, "expected99thPercentileMsValue", "6000");
        ReflectionTestUtils.setField(postgresDbConfig, "prepStmtCacheSqlLimit", SIXTY_THOUSAND);
        ReflectionTestUtils.setField(postgresDbConfig, "prepStmtCacheSize", SIXTY_THOUSAND);
        ReflectionTestUtils.setField(postgresDbConfig, "cachePrepStmts", "true");
        when(dataSource.getConnection()).thenReturn(connection);
        when(dataSource.getHikariPoolMXBean()).thenReturn(hikariPoolMxBean);
        postgresDbConfig.initDataSource();
        assertNotNull(postgresDbConfig.dataSource().getConnection());
    }

}
