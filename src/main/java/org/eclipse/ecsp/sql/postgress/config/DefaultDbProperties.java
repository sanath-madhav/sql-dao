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

import org.eclipse.ecsp.sql.dao.constants.CredentialsConstants;
import org.eclipse.ecsp.sql.dao.constants.HealthConstants;
import org.eclipse.ecsp.sql.dao.constants.PostgresDbConstants;
import org.eclipse.ecsp.sql.multitenancy.DatabaseProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import lombok.Getter;
import lombok.Setter;

/**
 * Default database properties bean for single-tenant mode.
 * This class uses @Value annotations to read properties from configuration files.
 * Implements DatabaseProperties interface to maintain a consistent contract with
 * TenantDatabaseProperties used in multi-tenant mode.
 * 
 * Property values are injected at bean creation time from application.properties
 * or environment variables using Spring's @Value annotation.
 * 
 * @author hbadshah
 * @version 1.1
 * @since 2025-10-28
 */
@Getter
@Setter
@Component
public class DefaultDbProperties implements DatabaseProperties {
    
    // JDBC URL for the database
    @Value("${" + PostgresDbConstants.POSTGRES_JDBC_URL + ":}")
    private String jdbcUrl;

    // Username for the database
    @Value("${" + PostgresDbConstants.POSTGRES_USERNAME + ":}")
    private String userName;

    // Password for the database
    @Value("${" + PostgresDbConstants.POSTGRES_PASSWORD + ":}")
    private String password;

    // Driver class name
    @Value("${" + PostgresDbConstants.POSTGRES_DRIVER_CLASS_NAME + ":org.postgresql.Driver}")
    private String driverClassName;

    // Pool name
    @Value("${" + PostgresDbConstants.POSTGRES_POOL_NAME + "}")
    private String poolName;

    // Connection timeout in ms
    @Value("${" + PostgresDbConstants.POSTGRES_CONNECTION_TIMEOUT_MS + ":60000}")
    private int connectionTimeoutMs;

    // Minimum pool size
    @Value("${" + PostgresDbConstants.POSTGRES_MIN_POOL_SIZE + ":1}")
    private int minPoolSize;

    // Maximum pool size
    @Value("${" + PostgresDbConstants.POSTGRES_MAX_POOL_SIZE + ":10}")
    private int maxPoolSize;

    // Maximum idle time in seconds
    @Value("${" + PostgresDbConstants.POSTGRES_MAX_IDLE_TIME + ":600}")
    private int maxIdleTime;

    // Cache prepared statements
    @Value("${" + PostgresDbConstants.POSTGRES_DS_CACHE_PREPARED_STATEMENTS_VALUE + ":true}")
    private String cachePrepStmts;

    // Prepared statement cache size
    @Value("${" + PostgresDbConstants.POSTGRES_DS_PREPARED_STATEMENT_CACHE_SIZE_VALUE + ":250}")
    private int prepStmtCacheSize;

    // Prepared statement cache SQL limit
    @Value("${" + PostgresDbConstants.POSTGRES_DS_PREPARED_STATEMENT_CACHE_SQL_LIMIT_VALUE + ":2048}")
    private int prepStmtCacheSqlLimit;

    // Expected 99th percentile ms value
    @Value("${" + HealthConstants.POSTGRES_EXPECTED_99_PI_MS_VALUE + ":60000}")
    private String expected99thPercentileMsValue;

    // Credential provider bean name
    @Value("${" + CredentialsConstants.CREDENTIAL_PROVIDER_BEAN_NAME + ":org.eclipse.ecsp.sql.authentication.DefaultPostgresDbCredentialsProvider}")
    private String credentialProviderBeanName;

    // Credentials refresh enabled
    @Value("${" + PostgresDbConstants.POSTGRES_CREDENTIALS_REFRESH_ENABLED + ":false}")
    private boolean postgresCredRefreshEnabled;

    // Data source retry count
    @Value("${postgres.datasource.create.retry.count:3}")
    private int dataSourceRetryCount;

    // Data source retry delay in ms
    @Value("${postgres.datasource.retry.delay.ms:10}")
    private int dataSourceRetryDelay;

    // Connection retry count
    @Value("${postgres.connection.create.retry.count:3}")
    private int connectionRetryCount;

    // Connection retry delay in ms
    @Value("${postgres.connection.retry.delay.ms:10}")
    private int connectionRetryDelay;

    // Auth mechanism
    @Value("${postgres.auth.Mechanism:}")
    private String authMechanism;

    // SSL mode
    @Value("${postgres.ssl.mode:prefer}")
    private String sslMode;

    // SSL response timeout
    @Value("${postgres.ssl.timeout:5000}")
    private int sslResponseTimeout;

    // Root certificate path
    @Value("${postgres.ssl.root.crt:}")
    private String rootCrtPath;
}
