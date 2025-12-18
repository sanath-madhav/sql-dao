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
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import io.prometheus.client.CollectorRegistry;
import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Map;
import static org.junit.Assert.assertNotNull;

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
     * Tear up postgres server.
     */
    @AfterAll
    static void tearUpPostgresServer() {
        CollectorRegistry.defaultRegistry.clear();
        postgresqlContainer.stop();
    }
}
