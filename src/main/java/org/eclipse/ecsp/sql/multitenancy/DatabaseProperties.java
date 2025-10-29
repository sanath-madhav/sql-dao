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

/**
 * Common interface for database configuration properties.
 * This interface defines the contract for both single-tenant and multi-tenant
 * database property configurations.
 * 
 * Implementations:
 * - TenantDatabaseProperties: Used for multi-tenant mode with @ConfigurationProperties binding
 * - DefaultDbProperties: Used for single-tenant mode with @Value annotations
 * 
 * @author hbadshah
 * @version 1.1
 * @since 2025-10-28
 */
public interface DatabaseProperties {
    
    /**
     * Gets the JDBC URL for the database.
     * @return the JDBC URL
     */
    String getJdbcUrl();
    
    /**
     * Sets the JDBC URL for the database.
     * @param jdbcUrl the JDBC URL to set
     */
    void setJdbcUrl(String jdbcUrl);
    
    /**
     * Gets the username for database authentication.
     * @return the username
     */
    String getUserName();
    
    /**
     * Sets the username for database authentication.
     * @param userName the username to set
     */
    void setUserName(String userName);
    
    /**
     * Gets the password for database authentication.
     * @return the password
     */
    String getPassword();
    
    /**
     * Sets the password for database authentication.
     * @param password the password to set
     */
    void setPassword(String password);
    
    /**
     * Gets the JDBC driver class name.
     * @return the driver class name
     */
    String getDriverClassName();
    
    /**
     * Sets the JDBC driver class name.
     * @param driverClassName the driver class name to set
     */
    void setDriverClassName(String driverClassName);
    
    /**
     * Gets the connection pool name.
     * @return the pool name
     */
    String getPoolName();
    
    /**
     * Sets the connection pool name.
     * @param poolName the pool name to set
     */
    void setPoolName(String poolName);
    
    /**
     * Gets the connection timeout in milliseconds.
     * @return the connection timeout
     */
    int getConnectionTimeoutMs();
    
    /**
     * Sets the connection timeout in milliseconds.
     * @param connectionTimeoutMs the connection timeout to set
     */
    void setConnectionTimeoutMs(int connectionTimeoutMs);
    
    /**
     * Gets the minimum pool size.
     * @return the minimum pool size
     */
    int getMinPoolSize();
    
    /**
     * Sets the minimum pool size.
     * @param minPoolSize the minimum pool size to set
     */
    void setMinPoolSize(int minPoolSize);
    
    /**
     * Gets the maximum pool size.
     * @return the maximum pool size
     */
    int getMaxPoolSize();
    
    /**
     * Sets the maximum pool size.
     * @param maxPoolSize the maximum pool size to set
     */
    void setMaxPoolSize(int maxPoolSize);
    
    /**
     * Gets the maximum idle time in seconds.
     * @return the maximum idle time
     */
    int getMaxIdleTime();
    
    /**
     * Sets the maximum idle time in seconds.
     * @param maxIdleTime the maximum idle time to set
     */
    void setMaxIdleTime(int maxIdleTime);
    
    /**
     * Gets whether to cache prepared statements.
     * @return the cache prepared statements setting
     */
    String getCachePrepStmts();
    
    /**
     * Sets whether to cache prepared statements.
     * @param cachePrepStmts the cache prepared statements setting to set
     */
    void setCachePrepStmts(String cachePrepStmts);
    
    /**
     * Gets the prepared statement cache size.
     * @return the prepared statement cache size
     */
    int getPrepStmtCacheSize();
    
    /**
     * Sets the prepared statement cache size.
     * @param prepStmtCacheSize the prepared statement cache size to set
     */
    void setPrepStmtCacheSize(int prepStmtCacheSize);
    
    /**
     * Gets the prepared statement cache SQL limit.
     * @return the prepared statement cache SQL limit
     */
    int getPrepStmtCacheSqlLimit();
    
    /**
     * Sets the prepared statement cache SQL limit.
     * @param prepStmtCacheSqlLimit the prepared statement cache SQL limit to set
     */
    void setPrepStmtCacheSqlLimit(int prepStmtCacheSqlLimit);
    
    /**
     * Gets the expected 99th percentile milliseconds value for health checks.
     * @return the expected 99th percentile value
     */
    String getExpected99thPercentileMsValue();
    
    /**
     * Sets the expected 99th percentile milliseconds value for health checks.
     * @param expected99thPercentileMsValue the value to set
     */
    void setExpected99thPercentileMsValue(String expected99thPercentileMsValue);
    
    /**
     * Gets the credential provider bean name.
     * @return the credential provider bean name
     */
    String getCredentialProviderBeanName();
    
    /**
     * Sets the credential provider bean name.
     * @param credentialProviderBeanName the credential provider bean name to set
     */
    void setCredentialProviderBeanName(String credentialProviderBeanName);
    
    /**
     * Gets whether PostgreSQL credentials refresh is enabled.
     * @return true if credentials refresh is enabled, false otherwise
     */
    boolean isPostgresCredRefreshEnabled();
    
    /**
     * Sets whether PostgreSQL credentials refresh is enabled.
     * @param postgresCredRefreshEnabled true to enable credentials refresh, false to disable
     */
    void setPostgresCredRefreshEnabled(boolean postgresCredRefreshEnabled);
    
    /**
     * Gets the data source retry count.
     * @return the data source retry count
     */
    int getDataSourceRetryCount();
    
    /**
     * Sets the data source retry count.
     * @param dataSourceRetryCount the data source retry count to set
     */
    void setDataSourceRetryCount(int dataSourceRetryCount);
    
    /**
     * Gets the data source retry delay in milliseconds.
     * @return the data source retry delay
     */
    int getDataSourceRetryDelay();
    
    /**
     * Sets the data source retry delay in milliseconds.
     * @param dataSourceRetryDelay the data source retry delay to set
     */
    void setDataSourceRetryDelay(int dataSourceRetryDelay);
    
    /**
     * Gets the connection retry count.
     * @return the connection retry count
     */
    int getConnectionRetryCount();
    
    /**
     * Sets the connection retry count.
     * @param connectionRetryCount the connection retry count to set
     */
    void setConnectionRetryCount(int connectionRetryCount);
    
    /**
     * Gets the connection retry delay in milliseconds.
     * @return the connection retry delay
     */
    int getConnectionRetryDelay();
    
    /**
     * Sets the connection retry delay in milliseconds.
     * @param connectionRetryDelay the connection retry delay to set
     */
    void setConnectionRetryDelay(int connectionRetryDelay);
    
    /**
     * Gets the authentication mechanism.
     * @return the authentication mechanism
     */
    String getAuthMechanism();
    
    /**
     * Sets the authentication mechanism.
     * @param authMechanism the authentication mechanism to set
     */
    void setAuthMechanism(String authMechanism);
    
    /**
     * Gets the SSL mode.
     * @return the SSL mode
     */
    String getSslMode();
    
    /**
     * Sets the SSL mode.
     * @param sslMode the SSL mode to set
     */
    void setSslMode(String sslMode);
    
    /**
     * Gets the SSL response timeout in milliseconds.
     * @return the SSL response timeout
     */
    int getSslResponseTimeout();
    
    /**
     * Sets the SSL response timeout in milliseconds.
     * @param sslResponseTimeout the SSL response timeout to set
     */
    void setSslResponseTimeout(int sslResponseTimeout);
    
    /**
     * Gets the root certificate path for SSL.
     * @return the root certificate path
     */
    String getRootCrtPath();
    
    /**
     * Sets the root certificate path for SSL.
     * @param rootCrtPath the root certificate path to set
     */
    void setRootCrtPath(String rootCrtPath);
}
