/*
 *
 *
 * ******************************************************************************
 *
 * Copyright (c) 2023-24 Harman International
 *
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 *
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *
 *
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 *
 * Unless required by applicable law or agreed to in writing, software
 *
 * distributed under the License is distributed on an "AS IS" BASIS,
 *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 *
 * limitations under the License.
 *
 *
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * *******************************************************************************
 *
 *
 */

package org.eclipse.ecsp.sql.postgress.config;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import jakarta.annotation.PreDestroy;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.ecsp.sql.authentication.CredentialsProvider;
import org.eclipse.ecsp.sql.dao.constants.HealthConstants;
import org.eclipse.ecsp.sql.dao.constants.MultitenantConstants;
import org.eclipse.ecsp.sql.dao.constants.PostgresDbConstants;
import org.eclipse.ecsp.sql.dao.utils.SqlDaoUtils;
import org.eclipse.ecsp.sql.exception.SqlDaoException;
import org.eclipse.ecsp.sql.multitenancy.DatabaseProperties;
import org.eclipse.ecsp.sql.multitenancy.MultiTenantDatabaseProperties;
import org.eclipse.ecsp.sql.multitenancy.TenantDatabaseProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import javax.annotation.Nullable;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import static org.eclipse.ecsp.sql.dao.constants.PostgresDbConstants.ONE_WAY_TLS_AUTH_MECHANISM;
import static org.postgresql.PGProperty.SSL;
import static org.postgresql.PGProperty.SSL_MODE;
import static org.postgresql.PGProperty.SSL_RESPONSE_TIMEOUT;
import static org.postgresql.PGProperty.SSL_ROOT_CERT;

/**
 * Configuration class for managing PostgresDB connections using HikariCP.
 *
 * <p>
 * This class provides the following functionalities:
 * <ul>
 * <li>Configures a HikariCP connection pool for PostgresDB.</li>
 * <li>Supports retry mechanisms for connection and datasource creation.</li>
 * <li>Validates PostgresDB connection properties.</li>
 * <li>Manages lifecycle events such as initialization and cleanup of connections and
 * datasource.</li>
 * <li>Supports one-way TLS authentication for secure communication with PostgresDB.</li>
 * <li>Integrates with Spring for dependency injection and bean management.</li>
 * <li>Exports health and metrics information for monitoring.</li>
 * <li>Refreshes credentials if enabled.</li>
 * </ul>
 *
 * <p>
 * This class ensures graceful shutdown of connections and the datasource during application
 * termination.
 *
 * <p>
 * Dependencies:
 * <ul>
 * <li>HikariCP for connection pooling</li>
 * <li>Dropwizard Metrics for health and metrics monitoring</li>
 * <li>Spring Framework for dependency injection</li>
 * </ul>
 *
 * <p>
 * Note: Ensure that all required PostgresDB properties are correctly configured in the environment.
 *
 * @author kaushalaroraharman
 * @version 1.1
 * @since 2025-04-15
 */
@Configuration
@EnableScheduling
@Component("postgresDbConfig")
public class PostgresDbConfig {

    /** Comma-separated list of tenant IDs */
    @Value("#{'${" + MultitenantConstants.MULTI_TENANT_IDS + "}'.split(',')}")
    private List<String> tenantIds;

    /** Flag to enable or disable multi-tenancy */
    @Value("${" + MultitenantConstants.MULTITENANCY_ENABLED + ":false}")
    private boolean isMultitenancyEnabled;

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(PostgresDbConfig.class);

    /** Holder for tenantId -> DataSource. */
    private final Map<Object, Object> targetDataSources = new ConcurrentHashMap<>();

    /** Holder for tenantId -> CredentialsProvider. */
    private final Map<String, CredentialsProvider> credsProviderMap = new ConcurrentHashMap<>();

    /** Contains a map of tenant IDs to their database properties. */
    @Autowired
    private MultiTenantDatabaseProperties multiTenantDbProperties;

    /** Configurations for default tenant. */
    @Autowired(required = false)
    private DatabaseProperties defaultDbProperties;

    /** The ctx. */
    @Autowired
    private ApplicationContext ctx;

    @Autowired
    private SqlDaoUtils utils;

    /**
     * Method for initializing and constructing target DataSources. If multitenancy is disabled,
     * only the default, i.e. single DataSource is initialized. Else, tenantId
     * specific DataSource is initialized.
     */
    @Bean("targetDataSources")
    @DependsOn("credentialsProvider")
    public Map<Object, Object> constructTargetDataSources() {
        if (!isMultitenancyEnabled) {
            LOGGER.info("Multitenancy is disabled. Default configuration will be used.");
            try {
                DataSource defaultDataSource =
                        initDataSource(MultitenantConstants.DEFAULT_TENANT_ID, defaultDbProperties);
                targetDataSources.put(MultitenantConstants.DEFAULT_TENANT_ID, defaultDataSource);
            } catch (Exception e) {
                LOGGER.error("Error initializing default DataSource", e);
            }
        } else {
            LOGGER.info("Multitenancy is enabled. Initializing tenant-specific DataSources.");
            initializeDataSourcesForTenants();
        }
        LOGGER.info("Initialization of target DataSources is complete.");
        return targetDataSources;
    }

    /**
     * Initializes DataSources for each tenant based on the provided tenant IDs.
     */
    private void initializeDataSourcesForTenants() {
        int threadCount = tenantIds != null ? tenantIds.size() : 1;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (String tenantId : tenantIds) {
            futures.add(CompletableFuture.runAsync(() -> {
                try {
                    DataSource tenantDataSource = initDataSource(tenantId,
                            multiTenantDbProperties.getTenants().get(tenantId));
                    targetDataSources.put(tenantId, tenantDataSource);
                    LOGGER.info("Configured DataSource for tenant: {}", tenantId);
                } catch (Exception e) {
                    LOGGER.error("Error initializing DataSource for tenant: {}, exception: {} ",
                            tenantId, e);
                }
            }, executor));
        }
        // Wait for all tasks to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        executor.shutdown();
    }

    /**
     * Get {@link CredentialsProvider}.
     */
    @Bean("credentialsProvider")
    public Map<String, CredentialsProvider> getCredentialsProvider() {
        if (!isMultitenancyEnabled) {
            LOGGER.info("Multitenancy is disabled. Using default tenant credentials provider.");
            credsProviderMap.put(MultitenantConstants.DEFAULT_TENANT_ID,
                    (CredentialsProvider) utils.getClassInstance(defaultDbProperties.getCredentialProviderBeanName()));
        } else {
            for (String tenantId : tenantIds) {
                DatabaseProperties tenantDbProperties =
                        multiTenantDbProperties.getTenants().get(tenantId);
                credsProviderMap.put(tenantId,
                        (CredentialsProvider) utils.getClassInstance(tenantDbProperties.getCredentialProviderBeanName()));
            }
        }
        return credsProviderMap;
    }

    /**
     * Refresh the PostgresDB credentials from a source and create pooled connections. <br>
     * Scheduled to run at a fixed delay interval. <br>
     * Retries for a specified number of times if the connection is not obtained. <br>
     *
     * @throws SQLException - if there is an exception in connecting to the database.
     * @throws InterruptedException - if the connection is interrupted.
     */
    @Scheduled(fixedDelayString = "${" + PostgresDbConstants.POSTGRES_REFRESH_CHECK_INTERVAL
            + ":86400000}")
    public void postgresCredsRefreshJob() throws SQLException, InterruptedException {
        Map<String, TenantDatabaseProperties> tenants = multiTenantDbProperties.getTenants();
        if (tenants == null || tenants.isEmpty()) {
            LOGGER.info("Executing credentials refresh job for default tenant.");
            executeCredsRefreshForTenant(MultitenantConstants.DEFAULT_TENANT_ID,
                    defaultDbProperties);
        } else {
            for (String tenantId : tenants.keySet()) {
                DatabaseProperties tenantDbProperties = tenants.get(tenantId);
                LOGGER.info("Executing credentials refresh job for tenant: {}.", tenantId);
                executeCredsRefreshForTenant(tenantId, tenantDbProperties);
            }
        }
    }

    private void executeCredsRefreshForTenant(String tenantId, DatabaseProperties props) {
        CredentialsProvider credentialsProvider = credsProviderMap.get(tenantId);
        if (!props.isPostgresCredRefreshEnabled()) {
            LOGGER.info("Postgres credentials refresh is disabled. Skipping the scheduled job.");
            return;
        }
        LOGGER.info("Starting Postgres refresh Job...");
        credentialsProvider.refreshCredentials();
        LOGGER.info("Completed Postgres refresh Job for tenant ID: {}.", tenantId);

        props.setUserName(credentialsProvider.getUserName());
        props.setPassword(credentialsProvider.getPassword());

        LOGGER.debug("Cleaning up existing connections for tenant ID: {}.", tenantId);
        cleanupConnections((DataSource) targetDataSources.get(tenantId));
        LOGGER.info("Creating connection with refreshed credentials...");
        try {
            Connection connection = createConnections(tenantId, props);
            LOGGER.info(
                    "Connection created successfully with refreshed credentials for tenant ID: {}.",
                    tenantId);
        } catch (Exception exception) {
            try {
                this.retryConnectionCreation(tenantId, props);
            } catch (InterruptedException | SQLException e) {
                LOGGER.error(
                        "Exception occurred while retrying connection creation for tenant ID: {} "
                                + "when refreshing credentials. Error is: {}",
                        tenantId, e);
            }
        }
    }

    /**
     * Retry connection creation.
     *
     * @throws InterruptedException the interrupted exception
     * @throws SQLException the SQL exception
     */
    private void retryConnectionCreation(String tenantId, DatabaseProperties dbProperties)
            throws InterruptedException, SQLException {
        int connectionRetryCount = dbProperties.getConnectionRetryCount();
        int connectionRetryDelay = dbProperties.getConnectionRetryDelay();
        Connection connection;
        do {
            try {
                LOGGER.info("Retrying the connection creation");
                connectionRetryCount--;
                dbProperties.setConnectionRetryCount(connectionRetryCount);
                connection = createConnections(tenantId, dbProperties);
                if (connection != null && !connection.isValid(1)) {
                    Thread.sleep(connectionRetryDelay);
                    continue;
                }
            } catch (Exception e) {
                if (connectionRetryCount == 0) {
                    LOGGER.error("All retry attempts are exhausted for connection creation with exception ", e);
                    throw new SqlDaoException("All retry attempts are exhausted for connection creation");
                }
                LOGGER.error("Exception occurred in retrying the connection creation", e);
                Thread.sleep(connectionRetryDelay);
            }
        } while (connectionRetryCount > 0);
    }
    /**
     * Creates the connections.
     *
     * @return the connection
     */
    private Connection createConnections(String tenantId, DatabaseProperties dbProperties) {
        HikariDataSource hds = ((HikariDataSource) targetDataSources.get(tenantId));
        hds.setUsername(dbProperties.getUserName());
        hds.setPassword(dbProperties.getPassword());
        Connection conn = null;
        try {
            conn = hds.getConnection();
        } catch (Exception exception) {
            throw new SqlDaoException("Exception occurred in creating the connection", exception);
        } finally {
            LOGGER.info("Printing connection details for tenant ID: {}.", tenantId);
            this.printConnections(hds.getHikariPoolMXBean());
        }
        return conn;
    }

    /**
     * Prints the connections.
     *
     * @param hikariPoolMxBean the hikari pool mx bean
     */
    private void printConnections(HikariPoolMXBean hikariPoolMxBean) {
        LOGGER.debug("Total connections {}", hikariPoolMxBean.getTotalConnections());
        LOGGER.debug("Active connections {}", hikariPoolMxBean.getActiveConnections());
        LOGGER.debug("Idle connections {}", hikariPoolMxBean.getIdleConnections());
    }

    /**
     * This method will create datasource and connection using the provided parameters.
     *
     * @throws InterruptedException , SQLException
     * @throws SQLException the SQL exception
     */
    public DataSource initDataSource(String tenantId, DatabaseProperties dbProperties)
            throws InterruptedException, SQLException {
        CredentialsProvider credsProvider = credsProviderMap.get(tenantId);
        LOGGER.info("Initializing datasource for tenant: {}", tenantId);
        try {
            dbProperties.setUserName(credsProvider.getUserName());
            dbProperties.setPassword(credsProvider.getPassword());
            validate(dbProperties);
            return createAndGetDataSource(dbProperties);
        } catch (Exception exception) {
            LOGGER.error("Error occurred while creating the datasource: {} for tenantId: {}",
                    exception, tenantId);
            int dataSourceRetryCount = dbProperties.getDataSourceRetryCount();
            DataSource dataSource = (DataSource) targetDataSources.get(tenantId);
            while (dataSourceRetryCount > 0 && dataSource == null) {
                dbProperties.setUserName(credsProvider.getUserName());
                dbProperties.setPassword(credsProvider.getPassword());
                try {
                    --dataSourceRetryCount;
                    LOGGER.info("Retrying datasource creation for tenant: {}, attempts left: {}",
                            tenantId, dataSourceRetryCount);
                    dataSource = createAndGetDataSource(dbProperties);
                    return dataSource;
                } catch (Exception ex) {
                    if (dataSourceRetryCount == 0) {
                        LOGGER.error("Failed to create the datasource for tenant: {}", tenantId,
                                ex);
                        throw new SqlDaoException(
                                "Retry Attempts exhausted for creating the datasource", ex);
                    }
                    LOGGER.error(
                            "Error occurred in creating the datasource for tenant: {}, exception is: {}",
                            tenantId, ex);
                    Thread.sleep(dbProperties.getDataSourceRetryDelay());
                }
            }
        }
        try {
            LOGGER.info("Creating datasource connection");
            Connection connection = ((DataSource) targetDataSources.get(tenantId)).getConnection();
        } catch (Exception exception) {
            LOGGER.error("Exception occurred while creating connection ", exception);
            this.retryConnectionCreation(tenantId, dbProperties);
        }
        return null;
    }

    /**
     * Cleanup connections and datasource.
     */
    @PreDestroy
    private void destroy() {
        LOGGER.info("Destroying Postgres connections and dataSources...");
        for (Map.Entry<Object, Object> entry : targetDataSources.entrySet()) {
            cleanupConnections((DataSource) entry.getValue());
            cleanupDataSource((DataSource) entry.getValue());
        }
    }

    /**
     * Validate Postgres configurations.
     */
    private void validate(DatabaseProperties dbProperties) {

        List<String> inValidConfAttributes = new ArrayList<>();

        if (StringUtils.isEmpty(dbProperties.getJdbcUrl())) {
            inValidConfAttributes.add(PostgresDbConstants.POSTGRES_JDBC_URL);
        }
        if (StringUtils.isEmpty(dbProperties.getUserName())) {
            inValidConfAttributes.add(PostgresDbConstants.POSTGRES_USERNAME);
        }
        if (StringUtils.isEmpty(dbProperties.getPassword())) {
            inValidConfAttributes.add(PostgresDbConstants.POSTGRES_PASSWORD);
        }
        if (StringUtils.isEmpty(dbProperties.getDriverClassName())) {
            inValidConfAttributes.add(PostgresDbConstants.POSTGRES_DRIVER_CLASS_NAME);
        }
        if (dbProperties.getMaxPoolSize() == 0) {
            inValidConfAttributes.add(PostgresDbConstants.POSTGRES_MAX_POOL_SIZE);
        }
        if (!inValidConfAttributes.isEmpty()) {
            throw new IllegalArgumentException(
                    "Missing or Invalid PostgresDB connection properties: "
                            + inValidConfAttributes.toString());
        }
    }

    /**
     * Creates the and get data source.
     *
     * @return the data source
     */
    private DataSource createAndGetDataSource(DatabaseProperties dbProperties) {

        HikariConfig config = new HikariConfig();

        config.setJdbcUrl(dbProperties.getJdbcUrl());
        config.setUsername(dbProperties.getUserName());
        config.setPassword(dbProperties.getPassword());
        config.setDriverClassName(dbProperties.getDriverClassName());
        config.setMaximumPoolSize(dbProperties.getMaxPoolSize());
        config.setMinimumIdle(dbProperties.getMinPoolSize());
        config.setIdleTimeout(dbProperties.getMaxIdleTime());
        config.setPoolName(dbProperties.getPoolName());
        config.setConnectionTimeout(dbProperties.getConnectionTimeoutMs());
        config.addHealthCheckProperty(HealthConstants.POSTGRES_EXPECTED_99_PI_MS,
                dbProperties.getExpected99thPercentileMsValue());
        config.addDataSourceProperty(PostgresDbConstants.POSTGRES_DS_CACHE_PREPARED_STATEMENTS,
                dbProperties.getCachePrepStmts());
        config.addDataSourceProperty(PostgresDbConstants.POSTGRES_DS_PREPARED_STATEMENT_CACHE_SIZE,
                dbProperties.getPrepStmtCacheSize());
        config.addDataSourceProperty(
                PostgresDbConstants.POSTGRES_DS_PREPARED_STATEMENT_CACHE_SQL_LIMIT,
                dbProperties.getPrepStmtCacheSqlLimit());
        config.setHealthCheckRegistry(new HealthCheckRegistry());
        config.setMetricRegistry(new MetricRegistry());

        if (dbProperties.getAuthMechanism().equalsIgnoreCase(ONE_WAY_TLS_AUTH_MECHANISM)) {
            LOGGER.debug("One way ssl communication is enabled with postgres");
            config.addDataSourceProperty(SSL.getName(), Boolean.TRUE);
            config.addDataSourceProperty(SSL_MODE.getName(), dbProperties.getSslMode());
            config.addDataSourceProperty(SSL_RESPONSE_TIMEOUT.getName(),
                    dbProperties.getSslResponseTimeout());
            config.addDataSourceProperty(SSL_ROOT_CERT.getName(), dbProperties.getRootCrtPath());
            LOGGER.debug(
                    "Parameters passed are sslmode: {} , sslResponseTimeout: {} ,"
                            + "sslRootCrtPath: {} ",
                    dbProperties.getSslMode(), dbProperties.getSslResponseTimeout(),
                    dbProperties.getRootCrtPath());
        }
        return new HikariDataSource(config);
    }

    /**
     * Cleanup data source.
     */
    private void cleanupDataSource(DataSource dataSource) {
        if (dataSource instanceof HikariDataSource hikariDataSource) {
            LOGGER.info("Closing data source...");
            hikariDataSource.close();
        }
    }

    /**
     * Cleanup connections.
     */
    private void cleanupConnections(DataSource dataSource) {
        LOGGER.info("Closing the connections");
        HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
        HikariPoolMXBean hikariPoolMxBean = hikariDataSource.getHikariPoolMXBean();
        hikariPoolMxBean.softEvictConnections();
        try {
            Connection conn = hikariDataSource.getConnection();
            hikariDataSource.evictConnection(conn);
            dataSource.getConnection().close();
            conn.close();
            LOGGER.info("connection closed successfully");
        } catch (Exception exception) {
            LOGGER.error("Exception occurred while closing the connection", exception);
        }
        this.printConnections(hikariPoolMxBean);
    }
}
