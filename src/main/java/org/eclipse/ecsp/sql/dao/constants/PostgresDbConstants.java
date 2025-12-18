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

package org.eclipse.ecsp.sql.dao.constants;

/**
 * Constants for feature implementing a pooled connection to Postgres DB.
 */
public class PostgresDbConstants {

    /**
     * Private constructor for postgres db constants.
     */
    private PostgresDbConstants() {
        throw new UnsupportedOperationException("PostgresDbConstants is a constants class and cannot be instantiated");
    }

    /** Postgres DB pooled connection properties. */
    public static final String POSTGRES_JDBC_URL = "postgres.jdbc.url";
    
    /** The Constant POSTGRES_USERNAME. */
    public static final String POSTGRES_USERNAME = "postgres.username";
    
    /** The Constant POSTGRES_PASSWORD. */
    public static final String POSTGRES_PASSWORD = "postgres.password";
    
    /** The Constant POSTGRES_DRIVER_CLASS_NAME. */
    public static final String POSTGRES_DRIVER_CLASS_NAME = "postgres.driver.class.name";
    
    /** The Constant POSTGRES_POOL_NAME. */
    public static final String POSTGRES_POOL_NAME = "postgres.pool.name";
    
    /** The Constant POSTGRES_MIN_POOL_SIZE. */
    public static final String POSTGRES_MIN_POOL_SIZE = "postgres.min.pool.size";
    
    /** The Constant POSTGRES_MAX_POOL_SIZE. */
    public static final String POSTGRES_MAX_POOL_SIZE = "postgres.max.pool.size";
    
    /** The Constant POSTGRES_MAX_IDLE_TIME. */
    public static final String POSTGRES_MAX_IDLE_TIME = "postgres.max.idle.time";
    
    /** The Constant POSTGRES_CONNECTION_TIMEOUT_MS. */
    public static final String POSTGRES_CONNECTION_TIMEOUT_MS = "postgres.connection.timeout.ms";
    
    /** The Constant POSTGRES_REFRESH_CHECK_INTERVAL. */
    public static final String POSTGRES_REFRESH_CHECK_INTERVAL = "postgres.refreshCheckInterval";
    
    /** The Constant POSTGRES_CREDENTIALS_REFRESH_ENABLED. */
    public static final String POSTGRES_CREDENTIALS_REFRESH_ENABLED = "postgres.credentials.refresh.enabled";
    
    /** The Constant POSTGRES_DS_CACHE_PREPARED_STATEMENTS. */
    public static final String POSTGRES_DS_CACHE_PREPARED_STATEMENTS = "cachePrepStmts";
    
    /** The Constant POSTGRES_DS_PREPARED_STATEMENT_CACHE_SIZE. */
    public static final String POSTGRES_DS_PREPARED_STATEMENT_CACHE_SIZE = "prepStmtCacheSize";
    
    /** The Constant POSTGRES_DS_PREPARED_STATEMENT_CACHE_SQL_LIMIT. */
    public static final String POSTGRES_DS_PREPARED_STATEMENT_CACHE_SQL_LIMIT = "prepStmtCacheSqlLimit";
    
    /** The Constant POSTGRES_DS_CACHE_PREPARED_STATEMENTS_VALUE. */
    public static final String POSTGRES_DS_CACHE_PREPARED_STATEMENTS_VALUE

            = "postgres.data-source-properties.cachePrepStmts";
    
    /** The Constant POSTGRES_DS_PREPARED_STATEMENT_CACHE_SIZE_VALUE. */
    public static final String POSTGRES_DS_PREPARED_STATEMENT_CACHE_SIZE_VALUE
            = "postgres.data-source-properties.prepStmtCacheSize";
    
    /** The Constant POSTGRES_DS_PREPARED_STATEMENT_CACHE_SQL_LIMIT_VALUE. */
    public static final String POSTGRES_DS_PREPARED_STATEMENT_CACHE_SQL_LIMIT_VALUE
            = "postgres.data-source-properties.prepStmtCacheSqlLimit";

    /** The Constant PROMETHEUS_ENABLED. */
    public static final String PROMETHEUS_ENABLED = "";
    
    /** The Constant ONE_WAY_TLS_AUTH_MECHANISM. */
    public static final String ONE_WAY_TLS_AUTH_MECHANISM = "one-way-tls";
    
    /** The Constant FIVE_THOUSAND. */
    public static final int FIVE_THOUSAND = 5000;
    
    /** Default driver class name. */
    public static final String DEFAULT_DRIVER_CLASS_NAME = "org.postgresql.Driver";
    
    /** Default pool name. */
    public static final String DEFAULT_POOL_NAME = "defaultPool";
    
    /** Default connection timeout in milliseconds. */
    public static final int DEFAULT_CONNECTION_TIMEOUT_MS = 60000;
    
    /** Default minimum pool size. */
    public static final int DEFAULT_MIN_POOL_SIZE = 1;
    
    /** Default maximum pool size. */
    public static final int DEFAULT_MAX_POOL_SIZE = 10;
    
    /** Default maximum idle time in seconds. */
    public static final int DEFAULT_MAX_IDLE_TIME = 600;
    
    /** Default cache prepared statements value. */
    public static final String DEFAULT_CACHE_PREP_STMTS = "true";
    
    /** Default prepared statement cache size. */
    public static final int DEFAULT_PREP_STMT_CACHE_SIZE = 250;
    
    /** Default prepared statement cache SQL limit. */
    public static final int DEFAULT_PREP_STMT_CACHE_SQL_LIMIT = 2048;
    
    /** Default expected 99th percentile milliseconds value. */
    public static final String DEFAULT_EXPECTED_99TH_PERCENTILE_MS = "60000";
    
    /** Default credential provider bean class name. */
    public static final String DEFAULT_CREDENTIAL_PROVIDER_BEAN_NAME = "org.eclipse.ecsp.sql.authentication.DefaultPostgresDbCredentialsProvider";
    
    /** Default credentials refresh enabled flag. */
    public static final boolean DEFAULT_CREDS_REFRESH_ENABLED = false;
    
    /** Default data source retry count. */
    public static final int DEFAULT_DATASOURCE_RETRY_COUNT = 3;
    
    /** Default data source retry delay in milliseconds. */
    public static final int DEFAULT_DATASOURCE_RETRY_DELAY = 10;
    
    /** Default connection retry count. */
    public static final int DEFAULT_CONNECTION_RETRY_COUNT = 3;
    
    /** Default connection retry delay in milliseconds. */
    public static final int DEFAULT_CONNECTION_RETRY_DELAY = 10;
    
    /** Default SSL mode. */
    public static final String DEFAULT_SSL_MODE = "prefer";
    
    /** Default SSL response timeout in milliseconds. */
    public static final int DEFAULT_SSL_RESPONSE_TIMEOUT = 5000;
}
