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

import lombok.Getter;
import lombok.Setter;
import org.eclipse.ecsp.sql.dao.constants.PostgresDbConstants;

/**
 * Database configuration properties for all tenants (including 'default').
 * This class is a plain POJO used for @ConfigurationProperties binding.
 * Properties are populated automatically by Spring Boot from configuration files
 * using the property prefix "tenant" defined in TenantConfig.
 * 
 * Used for both single-tenant and multi-tenant modes:
 * - Single-tenant: tenant.default.* properties
 * - Multi-tenant: tenant.{tenantId}.* properties
 * 
 * @author hbadshah
 * @version 2.0
 * @since 2025-10-28
 */
@Getter
@Setter
public class TenantDatabaseProperties implements DatabaseProperties {
	// JDBC URL for the tenant's database
	private String jdbcUrl;

	// Username for the tenant's database
	private String userName;

	// Password for the tenant's database
	private String password;

	// Driver class name
	private String driverClassName = PostgresDbConstants.DEFAULT_DRIVER_CLASS_NAME;

	// Pool name
	private String poolName = PostgresDbConstants.DEFAULT_POOL_NAME;

	// Connection timeout in ms
	private int connectionTimeoutMs = PostgresDbConstants.DEFAULT_CONNECTION_TIMEOUT_MS;

	// Minimum pool size
	private int minPoolSize = PostgresDbConstants.DEFAULT_MIN_POOL_SIZE;

	// Maximum pool size
	private int maxPoolSize = PostgresDbConstants.DEFAULT_MAX_POOL_SIZE;

	// Maximum idle time in seconds
	private int maxIdleTime = PostgresDbConstants.DEFAULT_MAX_IDLE_TIME;

	// Cache prepared statements
	private String cachePrepStmts = PostgresDbConstants.DEFAULT_CACHE_PREP_STMTS;

	// Prepared statement cache size
	private int prepStmtCacheSize = PostgresDbConstants.DEFAULT_PREP_STMT_CACHE_SIZE;

	// Prepared statement cache SQL limit
	private int prepStmtCacheSqlLimit = PostgresDbConstants.DEFAULT_PREP_STMT_CACHE_SQL_LIMIT;

	// Expected 99th percentile ms value
	private String expected99thPercentileMsValue = PostgresDbConstants.DEFAULT_EXPECTED_99TH_PERCENTILE_MS;

	// Credential provider bean name
	private String credentialProviderBeanName = PostgresDbConstants.DEFAULT_CREDENTIAL_PROVIDER_BEAN_NAME;

	// Credentials refresh enabled
	private boolean postgresCredRefreshEnabled = PostgresDbConstants.DEFAULT_CREDS_REFRESH_ENABLED;

	// Data source retry count
	private int dataSourceRetryCount = PostgresDbConstants.DEFAULT_DATASOURCE_RETRY_COUNT;

	// Data source retry delay in ms
	private int dataSourceRetryDelay = PostgresDbConstants.DEFAULT_DATASOURCE_RETRY_DELAY;

	// Connection retry count
	private int connectionRetryCount = PostgresDbConstants.DEFAULT_CONNECTION_RETRY_COUNT;

	// Connection retry delay in ms
	private int connectionRetryDelay = PostgresDbConstants.DEFAULT_CONNECTION_RETRY_DELAY;

	// Auth mechanism
	private String authMechanism = "";

	// SSL mode
	private String sslMode = PostgresDbConstants.DEFAULT_SSL_MODE;

	// SSL response timeout
	private int sslResponseTimeout = PostgresDbConstants.DEFAULT_SSL_RESPONSE_TIMEOUT;

	// Root certificate path
	private String rootCrtPath = "";
}
