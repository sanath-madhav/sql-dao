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

/**
 * Database configuration properties for multi-tenant mode.
 * This class is a plain POJO used for @ConfigurationProperties binding.
 * Properties are populated automatically by Spring Boot from configuration files
 * using the property prefix defined in MultiTenantDatabaseProperties.
 * 
 * For single-tenant mode, use DefaultDbProperties which uses @Value annotations instead.
 * 
 * @author hbadshah
 * @version 1.1
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
	private String driverClassName = "org.postgresql.Driver";

	// Pool name
	private String poolName = "defaultPool";

	// Connection timeout in ms
	private int connectionTimeoutMs = 60000;

	// Minimum pool size
	private int minPoolSize = 1;

	// Maximum pool size
	private int maxPoolSize = 10;

	// Maximum idle time in seconds
	private int maxIdleTime = 600;

	// Cache prepared statements
	private String cachePrepStmts = "true";

	// Prepared statement cache size
	private int prepStmtCacheSize = 250;

	// Prepared statement cache SQL limit
	private int prepStmtCacheSqlLimit = 2048;

	// Expected 99th percentile ms value
	private String expected99thPercentileMsValue = "60000";

	// Credential provider bean name
	private String credentialProviderBeanName = "org.eclipse.ecsp.sql.authentication.DefaultPostgresDbCredentialsProvider";

	// Credentials refresh enabled
	private boolean postgresCredRefreshEnabled = false;

	// Data source retry count
	private int dataSourceRetryCount = 3;

	// Data source retry delay in ms
	private int dataSourceRetryDelay = 10;

	// Connection retry count
	private int connectionRetryCount = 3;

	// Connection retry delay in ms
	private int connectionRetryDelay = 10;

	// Auth mechanism
	private String authMechanism = "";

	// SSL mode
	private String sslMode = "prefer";

	// SSL response timeout
	private int sslResponseTimeout = 5000;

	// Root certificate path
	private String rootCrtPath = "";
}
