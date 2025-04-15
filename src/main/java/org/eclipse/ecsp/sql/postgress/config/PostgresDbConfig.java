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

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.ecsp.sql.authentication.CredentialsProvider;
import org.eclipse.ecsp.sql.dao.constants.CredentialsConstants;
import org.eclipse.ecsp.sql.dao.constants.HealthConstants;
import org.eclipse.ecsp.sql.dao.constants.PostgresDbConstants;
import org.eclipse.ecsp.sql.exception.SqlDaoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.eclipse.ecsp.sql.dao.constants.PostgresDbConstants.FIVE_THOUSAND;
import static org.eclipse.ecsp.sql.dao.constants.PostgresDbConstants.ONE_WAY_TLS_AUTH_MECHANISM;
import static org.postgresql.PGProperty.SSL;
import static org.postgresql.PGProperty.SSL_MODE;
import static org.postgresql.PGProperty.SSL_RESPONSE_TIMEOUT;
import static org.postgresql.PGProperty.SSL_ROOT_CERT;

/**
 * Configuration class for managing PostgresDB connections using HikariCP.
 *
 * <p>This class provides the following functionalities:
 * <ul>
 *   <li>Configures a HikariCP connection pool for PostgresDB.</li>
 *   <li>Supports retry mechanisms for connection and datasource creation.</li>
 *   <li>Validates PostgresDB connection properties.</li>
 *   <li>Manages lifecycle events such as initialization and cleanup of connections and datasource.</li>
 *   <li>Supports one-way TLS authentication for secure communication with PostgresDB.</li>
 *   <li>Integrates with Spring for dependency injection and bean management.</li>
 *   <li>Exports health and metrics information for monitoring.</li>
 *   <li>Refreshes credentials if enabled.</li>
 * </ul>
 *
 * <p>This class ensures graceful shutdown of connections and the datasource during application termination.
 *
 * <p>Dependencies:
 * <ul>
 *   <li>HikariCP for connection pooling</li>
 *   <li>Dropwizard Metrics for health and metrics monitoring</li>
 *   <li>Spring Framework for dependency injection</li>
 * </ul>
 *
 * <p>Note: Ensure that all required PostgresDB properties are correctly configured in the environment.
 *
 * @author kaushalaroraharman
 * @version 1.1
 * @since 2025-04-15
 */
@Configuration
@EnableScheduling
public class PostgresDbConfig {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(PostgresDbConfig.class);

    /** The credentials provider. */
    CredentialsProvider credentialsProvider;
    
    /** The connection. */
    Connection connection;
    
    /** The data source. */
    private DataSource dataSource;

    /** The ctx. */
    @Autowired
    private ApplicationContext ctx;

    /** Postgres DB properties. */
    @Value("${" + PostgresDbConstants.POSTGRES_JDBC_URL + "}")
    private String jdbcUrl;
    
    /** The user name. */
    @Value("${" + PostgresDbConstants.POSTGRES_USERNAME + "}")
    private String userName;
    
    /** The password. */
    @Value("${" + PostgresDbConstants.POSTGRES_PASSWORD + "}")
    private String password;
    
    /** The driver class name. */
    @Value("${" + PostgresDbConstants.POSTGRES_DRIVER_CLASS_NAME + "}")
    private String driverClassName;
    
    /** The pool name. */
    @Value("${" + PostgresDbConstants.POSTGRES_POOL_NAME + "}")
    private String poolName;
    
    /** The connection timeout ms. 
     * Default: 60000ms
     */
    @Value("${" + PostgresDbConstants.POSTGRES_CONNECTION_TIMEOUT_MS + ":60000}")
    private int connectionTimeoutMs;
    
    /**
     * Minimum number of Connections a pool will maintain at any given time.
     * Default: 1
     */
    @Value("${" + PostgresDbConstants.POSTGRES_MIN_POOL_SIZE + ":1}")
    private int minPoolSize;
    
    /**
     * Maximum number of Connections a pool will maintain at any given time.
     * Default: 10
     */
    @Value("${" + PostgresDbConstants.POSTGRES_MAX_POOL_SIZE + ":10}")
    private int maxPoolSize;
    
    /**
     * Seconds a Connection can remain pooled but unused before being discarded. <br>
     * Zero means idle connections never expire. <br>
     * In second, after that time it will release the unused connections <br>
     */
    @Value("${" + PostgresDbConstants.POSTGRES_MAX_IDLE_TIME + "}")
    private int maxIdleTime;
    
    /** The cache prep stmts. */
    @Value("${" + PostgresDbConstants.POSTGRES_DS_CACHE_PREPARED_STATEMENTS_VALUE + "}")
    private String cachePrepStmts;
    
    /** The prep stmt cache size. */
    @Value("${" + PostgresDbConstants.POSTGRES_DS_PREPARED_STATEMENT_CACHE_SIZE_VALUE + "}")
    private int prepStmtCacheSize;
    
    /** The prep stmt cache sql limit. */
    @Value("${" + PostgresDbConstants.POSTGRES_DS_PREPARED_STATEMENT_CACHE_SQL_LIMIT_VALUE + "}")
    private int prepStmtCacheSqlLimit;
    
    /** The expected 99 th percentile ms value. */
    @Value("${" + HealthConstants.POSTGRES_EXPECTED_99_PI_MS_VALUE + ":60000}")
    private String expected99thPercentileMsValue;
    
    /** The credential provider bean name. */
    @Value("${" + CredentialsConstants.CREDENTIAL_PROVIDER_BEAN_NAME + " :defaultPostgresDbCredentialsProvider}")
    private String credentialProviderBeanName;
    
    /** The flag representing credentials refresh enabled or not for postgres. */
    @Value("${" + PostgresDbConstants.POSTGRES_CREDENTIALS_REFRESH_ENABLED + ":false}")
    private boolean postgresCredRefreshEnabled;
    
    /** The data source retry count. 
     * Default: 3
     */
    @Value("${postgres.datasource.create.retry.count:3}")
    private int dataSourceRetryCount;
    
    /** The data source retry delay. */
    @Value("${postgres.datasource.retry.delay.ms:10}")
    private int dataSourceRetryDelay;
    
    /** The connection retry count. */
    @Value("${postgres.connection.create.retry.count:3}")
    private int connectionRetryCount;
    
    /** The connection retry delay. */
    @Value("${postgres.connection.retry.delay.ms:10}")
    private int connectionRetryDelay;

    /** The auth mechanism. */
    @Value("${postgres.auth.Mechanism:}")
    private String authMechanism;

    /** The ssl mode. */
    @Value("${postgres.ssl.mode:prefer}")
    private String sslMode;

    /** The ssl response timeout. */
    @Value("${postgres.ssl.timeout:" + FIVE_THOUSAND + "}")
    private int sslResponseTimeout;

    /** The root crt path. */
    @Value("${postgres.ssl.root.crt:}")
    private String rootCrtPath;

    /**
     * Set {@link CredentialsProvider} available in Spring context.
     */
    @Autowired
    public void setCredentialsProvider() {
        credentialsProvider = (CredentialsProvider) ctx.getBean(credentialProviderBeanName);
    }

    /**
     * Required Spring datasource bean for connecting to the database.
     * <br>
     *
     * @return datasource
     */
    @Bean
    @Scope(scopeName = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public DataSource dataSource() {
        LOGGER.info("Fetching dataSource bean...");
        while (credentialsProvider.isRefreshInProgress()) {
            LOGGER.info("DataSource credentials refresh is in progress."
                    + " The current thread will wait for refresh to complete.");
            try {
                final int sleepTime = 50;
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                LOGGER.error("Thread initializing dataSource waiting for refresh interrupted", e);
                Thread.currentThread().interrupt();
            }
        }
        if (null == dataSource) {
            LOGGER.error("Datasource bean is not initialized");
        }
        return dataSource;
    }

    /**
     * Refresh the PostgresDB credentials from a source and create pooled connections.
     * <br>
     * Scheduled to run at a fixed delay interval.
     * <br>
     * Retries for a specified number of times if the connection is not obtained.
     * <br>
     *
     * @throws SQLException - if there is an exception in connecting to the database.
     * @throws InterruptedException - if the connection is interrupted.
     */
    @Scheduled(fixedDelayString = "${" + PostgresDbConstants.POSTGRES_REFRESH_CHECK_INTERVAL + ":86400000}")
    public void postgresCredsRefreshJob() throws SQLException, InterruptedException {
        if (!postgresCredRefreshEnabled) {
            LOGGER.info("Postgres credentials refresh is disabled. Skipping the scheduled job.");
            return;
        }
        LOGGER.info("Starting Postgres refresh Job...");
        credentialsProvider.refreshCredentials();
        LOGGER.info("Completed Postgres refresh Job.");

        userName = credentialsProvider.getUserName();
        password = credentialsProvider.getPassword();

        cleanupConnections();
        LOGGER.info("Creating connection with refreshed credentials...");
        try {
            connection = createConnections();
            LOGGER.info("Connection created successfully with refreshed credentials.");
        } catch (Exception exception) {
            this.retryConnectionCreation();
        }
    }

    /**
     * Retry connection creation.
     *
     * @throws InterruptedException the interrupted exception
     * @throws SQLException the SQL exception
     */
    private void retryConnectionCreation() throws InterruptedException, SQLException {
        while (connectionRetryCount > 0 && !connection.isValid(1)) {
            LOGGER.info("Retrying the connection creation");
            connectionRetryCount--;
            try {
                connection = createConnections();
            } catch (Exception e) {
                if (connectionRetryCount == 0) {
                    LOGGER.error("All retry attempts are exhausted for connection creation with exception ", e);
                    throw new SqlDaoException("All retry attempts are exhausted for connection creation");
                }
                LOGGER.error("Exception occurred in retrying the connection creation", e);
                Thread.sleep(connectionRetryDelay);
            }
        }
    }

    /**
     * Creates the connections.
     *
     * @return the connection
     */
    private Connection createConnections()  {
        HikariDataSource hds = ((HikariDataSource) dataSource);
        hds.setUsername(userName);
        hds.setPassword(password);
        Connection conn = null;
        try {
            conn = hds.getConnection();
        } catch (Exception exception) {
            throw new SqlDaoException("Exception occurred in creating the connection", exception);
        } finally {
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
    @PostConstruct
    public void initDataSource() throws InterruptedException, SQLException {
        try {
            userName = credentialsProvider.getUserName();
            password = credentialsProvider.getPassword();
            validate();
            dataSource = createAndGetDataSource();
        } catch (Exception exception) {
            LOGGER.error("Error occurred while creating the datasource", exception);
            while (dataSourceRetryCount > 0 && dataSource == null) {
                userName = credentialsProvider.getUserName();
                password = credentialsProvider.getPassword();
                try {
                    --dataSourceRetryCount;
                    LOGGER.info("Retrying datasource creation");
                    dataSource = createAndGetDataSource();
                } catch (Exception ex) {
                    if (dataSourceRetryCount == 0) {
                        LOGGER.error("Failed to creating the datasource ", ex);
                        throw new SqlDaoException("Retry Attempts exhausted for creating the datasource", ex);
                    }
                    LOGGER.error("Error occurred in creating the datasource with exception ", ex);
                    Thread.sleep(dataSourceRetryDelay);
                }
            }
        }
        try {
            LOGGER.info("Creating datasource connection");
            connection = dataSource.getConnection();
        } catch (Exception exception) {
            LOGGER.error("Exception occurred while creating connection ", exception);
            this.retryConnectionCreation();
        }
    }

    /**
     * Cleanup connections and datasource.
     */
    @PreDestroy
    private void destroy() {
        cleanupConnections();
        cleanupDataSource();
    }

    /**
     * Validate Postgres configurations.
     */
    private void validate() {

        List<String> inValidConfAttributes = new ArrayList<>();

        if (StringUtils.isEmpty(jdbcUrl)) {
            inValidConfAttributes.add(PostgresDbConstants.POSTGRES_JDBC_URL);
        }
        if (StringUtils.isEmpty(userName)) {
            inValidConfAttributes.add(PostgresDbConstants.POSTGRES_USERNAME);
        }
        if (StringUtils.isEmpty(password)) {
            inValidConfAttributes.add(PostgresDbConstants.POSTGRES_PASSWORD);
        }
        if (StringUtils.isEmpty(driverClassName)) {
            inValidConfAttributes.add(PostgresDbConstants.POSTGRES_DRIVER_CLASS_NAME);
        }
        if (maxPoolSize == 0) {
            inValidConfAttributes.add(PostgresDbConstants.POSTGRES_MAX_POOL_SIZE);
        }
        if (!inValidConfAttributes.isEmpty()) {
            throw new IllegalArgumentException("Missing or Invalid PostgresDB connection properties: "
                    + inValidConfAttributes.toString());
        }
    }

    /**
     * Creates the and get data source.
     *
     * @return the data source
     */
    private DataSource createAndGetDataSource() {

        HikariConfig config = new HikariConfig();

        config.setJdbcUrl(jdbcUrl);
        config.setUsername(userName);
        config.setPassword(password);
        config.setDriverClassName(driverClassName);
        config.setMaximumPoolSize(maxPoolSize);
        config.setMinimumIdle(minPoolSize);
        config.setIdleTimeout(maxIdleTime);
        config.setPoolName(poolName);
        config.setConnectionTimeout(connectionTimeoutMs);
        config.addHealthCheckProperty(HealthConstants.POSTGRES_EXPECTED_99_PI_MS, expected99thPercentileMsValue);
        config.addDataSourceProperty(PostgresDbConstants.POSTGRES_DS_CACHE_PREPARED_STATEMENTS, cachePrepStmts);
        config.addDataSourceProperty(PostgresDbConstants.POSTGRES_DS_PREPARED_STATEMENT_CACHE_SIZE, prepStmtCacheSize);
        config.addDataSourceProperty(PostgresDbConstants.POSTGRES_DS_PREPARED_STATEMENT_CACHE_SQL_LIMIT,
                prepStmtCacheSqlLimit);
        config.setHealthCheckRegistry(new HealthCheckRegistry());
        config.setMetricRegistry(new MetricRegistry());

        if (authMechanism.equalsIgnoreCase(ONE_WAY_TLS_AUTH_MECHANISM)) {
            LOGGER.debug("One way ssl communication is enabled with postgres");
            config.addDataSourceProperty(SSL.getName(), Boolean.TRUE);
            config.addDataSourceProperty(SSL_MODE.getName(), sslMode);
            config.addDataSourceProperty(SSL_RESPONSE_TIMEOUT.getName(), sslResponseTimeout);
            config.addDataSourceProperty(SSL_ROOT_CERT.getName(), rootCrtPath);
            LOGGER.debug("Parameters passed are sslmode: {} , sslResponseTimeout: {} ,"
                    + "sslRootCrtPath: {} ", sslMode, sslResponseTimeout, rootCrtPath);
        }
        return new HikariDataSource(config);
    }

    /**
     * Cleanup data source.
     */
    private void cleanupDataSource() {
        if (dataSource instanceof HikariDataSource hikariDataSource) {
            LOGGER.info("Closing data source...");
            hikariDataSource.close();
        }
    }

    /**
     * Cleanup connections.
     */
    private void cleanupConnections() {
        LOGGER.info("Closing the connections");
        HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
        HikariPoolMXBean hikariPoolMxBean = hikariDataSource.getHikariPoolMXBean();
        hikariPoolMxBean.softEvictConnections();
        hikariDataSource.evictConnection(connection);
        try {
            dataSource.getConnection().close();
            connection.close();
            LOGGER.info("connection closed successfully");
        } catch (Exception exception) {
            LOGGER.error("Exception occurred while closing the connection", exception);
        }
        this.printConnections(hikariPoolMxBean);
    }
}
