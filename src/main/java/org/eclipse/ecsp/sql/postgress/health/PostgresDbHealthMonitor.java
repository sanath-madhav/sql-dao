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

package org.eclipse.ecsp.sql.postgress.health;

import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PostConstruct;
import org.eclipse.ecsp.healthcheck.HealthMonitor;
import org.eclipse.ecsp.sql.dao.constants.HealthConstants;
import org.eclipse.ecsp.sql.dao.constants.MultitenantConstants;
import org.eclipse.ecsp.sql.dao.constants.PostgresDbConstants;
import org.eclipse.ecsp.sql.multitenancy.TenantDatabaseProperties;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Health monitor for PostgresDB.
 * <br>
 * {@link org.eclipse.ecsp.healthcheck.HealthService} will use this health monitor to monitor the health of
 * PostgresDB.
 * <br>
 * Callback for restart on unhealthy health monitor is implemented by {@link PostgresDbHealthService}
 * <br>
 * This component depends on PostgresDbConfig to ensure targetDataSources map is fully initialized
 * before health checks are registered.
 */
@Component
@DependsOn("postgresDbConfig")
public class PostgresDbHealthMonitor implements HealthMonitor {

    /** The logger. */
    private static IgniteLogger logger = IgniteLoggerFactory.getLogger(PostgresDbHealthMonitor.class);

    /** Configuration map for all the tenant IDs. */
    @Autowired
    @Qualifier("tenantConfigMap")
    private Map<String, TenantDatabaseProperties> tenantConfigMap;

    /** Target data sources for each tenant ID. */
    @Autowired
    @Qualifier("targetDataSources")
    private Map<String, DataSource> targetDataSources;

    /** Flag to enable or disable multi-tenancy */
    @Value("${" + MultitenantConstants.MULTITENANCY_ENABLED + ":false}")
    private boolean isMultitenancyEnabled;

        /** The postgres db health monitor enabled flag. */
    @Value("${" + HealthConstants.HEALTH_POSTGRES_DB_MONITOR_ENABLED + ": true }")
    private boolean postgresDbHealthMonitorEnabled;

    /** The postgres db restart on failure flag. */
    @Value("${" + HealthConstants.HEALTH_POSTGRES_DB_MONITOR_RESTART_ON_FAILURE + ": true }")
    private boolean postgresDbRestartOnFailure;

    /** The health check list. */
    List<String> healthCheckList;

    /**
     * Initialize health check list.
     */
    @PostConstruct
    public void init() {
        healthCheckList = new ArrayList<>();
        if (!isMultitenancyEnabled) {
            healthCheckList.add(tenantConfigMap.get(MultitenantConstants.DEFAULT_TENANT_ID).getPoolName()
                    + HealthConstants.POOL_CONNECTIVITY_HEALTH_CHECK);
            healthCheckList.add(tenantConfigMap.get(MultitenantConstants.DEFAULT_TENANT_ID).getPoolName()
                    + HealthConstants.POOL_CONNECTION_99_PERCENT_HEALTH_CHECK);
            logger.info("Initialized health check list for default tenant: {}", healthCheckList);
            return;
        }
        for (Map.Entry<String, TenantDatabaseProperties> tenantHealthProps 
                : tenantConfigMap.entrySet()) {
            healthCheckList.add(tenantHealthProps.getValue().getPoolName()
                    + HealthConstants.POOL_CONNECTIVITY_HEALTH_CHECK);
            healthCheckList.add(tenantHealthProps.getValue().getPoolName()
                    + HealthConstants.POOL_CONNECTION_99_PERCENT_HEALTH_CHECK);
        }
        logger.info("Initialized health check list for all tenants: {}", healthCheckList);
    }

    /**
     * Checks if postgres database is healthy.
     *
     * @param forceHealthCheck the force health check
     * @return true, if is healthy
     */
    @Override
    public boolean isHealthy(boolean forceHealthCheck) {
        for (Map.Entry<String, DataSource> entry : targetDataSources.entrySet()) {
            String tenantId = entry.getKey();
            DataSource datasource = entry.getValue();
            logger.info("Checking health for tenant: {}", tenantId);
            if (datasource == null) return false;
             
            HealthCheckRegistry healthCheckRegistry =
                    (HealthCheckRegistry) ((HikariDataSource) datasource).getHealthCheckRegistry();
            for (String healthCheckName : healthCheckList) {
                HealthCheck healthCheck = healthCheckRegistry.getHealthCheck(healthCheckName);
                if (healthCheck == null) {
                    logger.error("Health check '{}' not found in registry for tenantId: {}. " +
                            "Available health checks: {}", 
                            healthCheckName, tenantId, healthCheckRegistry.getNames());
                    return false;
                }
                HealthCheck.Result healthCheckResult = healthCheck.execute();
                logger.info("Health check result for {} - {} for tenantID - {} - isHealthy: {}",
                                HealthConstants.HEALTH_POSTGRES_DB_MONITOR_NAME, healthCheckName, tenantId,
                        healthCheckResult.isHealthy());
                if (Boolean.FALSE.equals(healthCheckResult.isHealthy())) {
                    logger.error("Health check failed for {} - health check name: {} " +
                                        "for tenantId - {}, with message {}, Error : {}",
                                        HealthConstants.HEALTH_POSTGRES_DB_MONITOR_NAME, healthCheckName, tenantId,
                                        healthCheckResult.getMessage(), healthCheckResult.getError());
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Monitor name.
     *
     * @return the string
     */
    @Override
    public String monitorName() {
        return HealthConstants.HEALTH_POSTGRES_DB_MONITOR_NAME;
    }

    /**
     * Needs restart on failure.
     *
     * @return true, if successful
     */
    @Override
    public boolean needsRestartOnFailure() {
        return postgresDbRestartOnFailure;
    }

    /**
     * Metric name.
     *
     * @return the string
     */
    @Override
    public String metricName() {
        return HealthConstants.HEALTH_POSTGRES_DB_MONTIOR_GUAGE;
    }

    /**
     * Checks if health monitor is enabled.
     *
     * @return true, if is enabled
     */
    @Override
    public boolean isEnabled() {
        return postgresDbHealthMonitorEnabled;
    }

}
