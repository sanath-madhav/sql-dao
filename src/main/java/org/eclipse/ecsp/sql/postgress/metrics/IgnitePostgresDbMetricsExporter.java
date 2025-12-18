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

package org.eclipse.ecsp.sql.postgress.metrics;

import com.codahale.metrics.MetricRegistry;
import com.zaxxer.hikari.HikariDataSource;
import io.prometheus.client.exporter.HTTPServer;
import io.prometheus.client.hotspot.DefaultExports;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.eclipse.ecsp.healthcheck.ThreadUtils;
import org.eclipse.ecsp.sql.dao.constants.MetricsConstants;
import org.eclipse.ecsp.sql.dao.constants.MultitenantConstants;
import org.eclipse.ecsp.sql.dao.constants.PostgresDbConstants;
import org.eclipse.ecsp.sql.multitenancy.TenantDatabaseProperties;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Fetches PostgresDB's metrics from the datasource through a Scheduled thread executor
 * at configured time interval and sets the value of each metric to its corresponding Prometheus guage.
 *
 * @author karora
 */
@Component
public class IgnitePostgresDbMetricsExporter {

    /** The Constant LOGGER. */
    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(IgnitePostgresDbMetricsExporter.class);

    /** The postgres db metrics enabled flag. */
    @Value("${" + MetricsConstants.POSTGRES_DB_METRICS_ENABLED + ":false}")
    private boolean postgresDbMetricsEnabled;

    /** The prometheus enabled flag. */
    @Value("${" + MetricsConstants.PROMETHEUS_ENABLED + ":false}")
    private boolean prometheusEnabled;

    /** The thread initial delay. */
    @Value("${" + MetricsConstants.POSTGRES_DB_METRICS_THREAD_INITIAL_DELAY_MS + ":2000}")
    private int threadInitialDelay;

    /** The thread frequency. */
    @Value("${" + MetricsConstants.POSTGRES_DB_METRICS_THREAD_FREQUENCY_MS + ":5000}")
    private int threadFrequency;

    /** The svc. */
    @Value("${" + MetricsConstants.SERVICE_NAME + "}")
    private String svc;

    /** The node name. */
    @Value("${NODE_NAME:localhost}")
    private String nodeName;

    /** The shutdown buffer. */
    @Value("${" + MetricsConstants.POSTGRES_DB_METRICS_EXECUTOR_SHUTDOWN_BUFFER_MS + ":2000}")
    private int shutdownBuffer;

    /** The postgres db gauge. */
    @Autowired
    private IgnitePostgresDbGuage postgresDbGauge;

    /** Configuration map for all the tenant IDs. */
    @Autowired
    @Qualifier("tenantConfigMap")
    private Map<String, TenantDatabaseProperties> tenantConfigMap;

    /** Target data sources for each tenant ID. */
    @Autowired
    @Qualifier("targetDataSources")
    private Map<String, DataSource> targetDataSources;

    /** The postgres db metrics executor. */
    private ScheduledExecutorService postgresDbMetricsExecutor;

    /** Flag to enable or disable multi-tenancy */
    @Value("${" + MultitenantConstants.MULTITENANCY_ENABLED + ":false}")
    private boolean isMultitenancyEnabled;

    /** The prometheus export port. */
    @Value("${" + MetricsConstants.PROMETHEUS_AGENT_PORT_KEY + ":"
            + MetricsConstants.PROMETHEUS_AGENT_PORT_EXPOSED + "}")
    private int prometheusExportPort;

    /** The prometheus export server. */
    private HTTPServer prometheusExportServer;

    /** The metrics list. */
    List<String> metricsList;

    /**
     * Start prometheus server and initialize metrics executor.
     */
    @PostConstruct
    public void init() {
        if (postgresDbMetricsEnabled) {
            startPrometheusServer();
            postgresDbGauge.setup();
            createMetricsList();
            createPostgresDbMetricsExecutor();
            startPostgresDbMetricsExecutor();
        }
    }

    /**
     * Creates the metrics list based on single / multi-tenant deployment.
     */
    private void createMetricsList() {
        metricsList = new ArrayList<>();
        if (!isMultitenancyEnabled) {
            TenantDatabaseProperties tenantHealthProps =
                    tenantConfigMap.get(MultitenantConstants.DEFAULT_TENANT_ID);
            metricsList.add(tenantHealthProps.getPoolName()
                    + MetricsConstants.POSTGRES_METRIC_TOTAL_CONNECTIONS);
            metricsList.add(tenantHealthProps.getPoolName()
                    + MetricsConstants.POSTGRES_METRIC_ACTIVE_CONNECTIONS);
            metricsList.add(tenantHealthProps.getPoolName()
                    + MetricsConstants.POSTGRES_METRIC_IDLE_CONNECTIONS);
            metricsList.add(tenantHealthProps.getPoolName()
                    + MetricsConstants.POSTGRES_METRIC_PENDING_CONNECTIONS);
            LOGGER.info("Created Metrics list for default tenant: {}", metricsList);
            return;
        }
        for (Map.Entry<String, TenantDatabaseProperties> entry : tenantConfigMap.entrySet()) {
            TenantDatabaseProperties tenantHealthProps = entry.getValue();
            metricsList.add(tenantHealthProps.getPoolName()
                    + MetricsConstants.POSTGRES_METRIC_TOTAL_CONNECTIONS);
            metricsList.add(tenantHealthProps.getPoolName()
                    + MetricsConstants.POSTGRES_METRIC_ACTIVE_CONNECTIONS);
            metricsList.add(tenantHealthProps.getPoolName()
                    + MetricsConstants.POSTGRES_METRIC_IDLE_CONNECTIONS);
            metricsList.add(tenantHealthProps.getPoolName()
                    + MetricsConstants.POSTGRES_METRIC_PENDING_CONNECTIONS);
            LOGGER.info("Created Metrics list for tenant {}: {}", entry.getKey(), metricsList);
        }
    }

    /**
     * Start prometheus server.
     */
    private void startPrometheusServer() {
        if (prometheusEnabled) {
            try {
                prometheusExportServer = new HTTPServer(prometheusExportPort, true);
                DefaultExports.initialize();
            } catch (IOException e) {
                LOGGER.error("Error encountered when initializing prometheus server", e);
            }
        }
    }

    /**
     * Creates the postgres db metrics executor.
     */
    private void createPostgresDbMetricsExecutor() {
        postgresDbMetricsExecutor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread t = Executors.defaultThreadFactory().newThread(runnable);
            t.setDaemon(true);
            t.setUncaughtExceptionHandler((thread, throwable) ->
                    LOGGER.error("Uncaught exception in thread {} {}", thread.getName(), throwable)
            );
            t.setName(Thread.currentThread().getName() + ":" + "postgresDBMetricsExecutor");
            return t;
        });
        LOGGER.debug("Scheduled thread executor for PostgresDB metrics created successfully.");
    }

    /**
     * Start postgres db metrics executor.
     */
    private void startPostgresDbMetricsExecutor() {
        LOGGER.info("Starting the PostgresDB metrics executor...");
        postgresDbMetricsExecutor.scheduleWithFixedDelay(this::fetchMetrics,
                threadInitialDelay, threadFrequency, TimeUnit.MILLISECONDS);
    }

    /**
     * Fetch metrics.
     */
    private void fetchMetrics() {
        for (Map.Entry<String, DataSource> entry : targetDataSources.entrySet()) {
            String tenantId = entry.getKey();
            DataSource datasource = entry.getValue();
            LOGGER.debug("Fetching PostgresDB metrics for tenant: {}", tenantId);
            if (null != datasource) {
                MetricRegistry metricRegistry =
                        ((MetricRegistry) ((HikariDataSource) datasource).getMetricRegistry());
                for (String metricName : metricsList) {
                    int metricValue = (Integer) metricRegistry.gauge(metricName).getValue();
                    LOGGER.info("Postgres Metric - {} : {}, for tenantId: {}", metricName,
                            metricValue, tenantId);
                    exportMetrics(metricValue, metricName);
                }
            }
        }
    }

    /**
     * Export metrics.
     *
     * @param val the val
     * @param metricName the metric name
     */
    private void exportMetrics(double val, String metricName) {
        postgresDbGauge.set(val, metricName, svc, nodeName);
    }

    /**
     * close metrics executor and stop postgres export server.
     */
    @PreDestroy
    public void close() {
        if (postgresDbMetricsExecutor != null && !postgresDbMetricsExecutor.isShutdown()) {
            LOGGER.info("Shutting down the PostgresDB metrics executor...");
            ThreadUtils.shutdownExecutor(postgresDbMetricsExecutor, shutdownBuffer, false);
        }
        if (prometheusExportServer != null) {
            prometheusExportServer.stop();
        }
    }
}
