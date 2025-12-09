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

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.eclipse.ecsp.sql.dao.constants.MultitenantConstants;
import org.eclipse.ecsp.sql.exception.SqlDaoException;
import org.eclipse.ecsp.sql.postgress.config.PostgresDbConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Component responsible for initializing and configuring tenant-aware data source routing.
 *
 * <p>This class manages the initialization of {@link TenantRoutingDataSource} based on
 * whether multi-tenancy is enabled. In single-tenant mode, it sets up a default data source.
 * In multi-tenant mode, it configures routing to multiple tenant-specific data sources.</p>
 *
 * @author hbadshah
 */
@Component
public class TenantAwareDataSource {

    private static final Logger logger = Logger.getLogger(TenantAwareDataSource.class.getName());

    @Value("${" + MultitenantConstants.MULTITENANCY_ENABLED + ":false}")
    private boolean isMultitenancyEnabled;

    @Autowired
    @Qualifier("targetDataSources")
    private Map<Object, Object> targetDataSources;

    @Autowired
    private PostgresDbConfig postgresDbConfig;

    private TenantRoutingDataSource tenantRoutingDataSource;

    /**
     * Initializes the tenant routing data source as a Spring Bean.
     *
     * <p>This method creates and configures the {@link TenantRoutingDataSource} with appropriate 
     * target data sources based on whether multi-tenancy is enabled.</p>
     *
     * <p>For single-tenant mode, it sets both the target data sources map and the default
     * data source. For multi-tenant mode, it only sets the target data sources map, allowing
     * dynamic routing based on the current tenant context.</p>
     * 
     * @return Configured DataSource instance
     */
    @Bean
    @Primary
    @DependsOn("targetDataSources")
    public DataSource dataSource() {
        logger.info("Initializing TenantAwareDataSource");
        tenantRoutingDataSource = new TenantRoutingDataSource();
        if (!isMultitenancyEnabled) {
            logger.info("Multitenancy is disabled. Using default tenant data source.");
            tenantRoutingDataSource.setTargetDataSources(targetDataSources);
            tenantRoutingDataSource.setDefaultTargetDataSource(
                    targetDataSources.get(MultitenantConstants.DEFAULT_TENANT_ID));
        } else {
            logger.info("Multitenancy is enabled. Setting target data sources for tenants.");
            tenantRoutingDataSource.setTargetDataSources(targetDataSources);
        }
        tenantRoutingDataSource.afterPropertiesSet();
        logger.info("TenantAwareDataSource initialized successfully.");
        return tenantRoutingDataSource;
    }



    /**
     * Adds or updates a tenant-specific datasource at runtime.
     * 
     * @param tenantId
     * @param tenantDatabaseProperties
     * @return
     */
    public synchronized boolean addOrUpdateTenantDataSource(String tenantId, TenantDatabaseProperties tenantDatabaseProperties) {
        try {
            logger.info("Adding/updating tenant datasource for: " + tenantId);

            if (!isMultitenancyEnabled) {
                logger.warning("Multitenancy is disabled. Cannot add tenant datasource.");
                return false;
            }

            if (tenantId == null || tenantId.trim().isEmpty()) {
                logger.warning("Invalid tenant ID provided. Cannot add tenant datasource.");
                return false;
            }

            if (tenantDatabaseProperties == null) {
                logger.warning("Tenant database properties cannot be null for tenant: " + tenantId);
                return false;
            }
            tenantId = tenantId.trim();
            // Create new datasource directly using the provided properties
            DataSource newDataSource = postgresDbConfig.createAndGetDataSource(tenantDatabaseProperties);
            if (newDataSource == null) {
                logger.severe("Failed to create datasource for tenant: " + tenantId);
                return false;
            }
            // Close existing connection if present
            DataSource existingDataSource = (DataSource) targetDataSources.get(tenantId);
            targetDataSources.put(tenantId, newDataSource);
            // Add to target datasources
            logger.info("Successfully added tenant datasource to map: " + tenantId);
            refreshTenantRoutingDataSource();
            logger.info("Successfully added/updated datasource for tenant: " + tenantId);
            closeConnection(existingDataSource,tenantId);
            return true;

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error adding tenant datasource for " + tenantId + ": " + e.getMessage());
            /*
                Should have fall back mechanism to previous state as we are not modifying the map until
                we have successfully created the new datasource.
                1. Create new datasource - if fails, throw exception, low risk
                2. Put in map - no risk
                3. Refresh routing datasource - if fails, moderate risk - need to close new datasource
                4. Close existing datasource - if fails, high risk - need to close new datasource, existing connection leakage
            */
            return false;
        }
    }

    /**
     * Removes a tenant-specific datasource at runtime.
     * 
     * @param tenantId
     * @return
     */
    public synchronized boolean removeTenantDataSource(String tenantId) {
        try {
            logger.info("Remove tenant datasource for: " + tenantId);
            if (!isMultitenancyEnabled) {
                logger.warning("Multitenancy is disabled. Cannot remove tenant datasource.");
                return false;
            }
            if (tenantId == null || tenantId.trim().isEmpty()) {
                logger.warning("Invalid tenant ID provided. Cannot remove tenant datasource.");
                return false;
            }
            tenantId = tenantId.trim();
            DataSource existingDataSource = (DataSource) targetDataSources.get(tenantId);
            targetDataSources.remove(tenantId);
            refreshTenantRoutingDataSource();
            closeConnection(existingDataSource, tenantId);
            logger.info("Successfully removed tenant datasource from map: " + tenantId);
            return true;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error removing tenant datasource for " + tenantId + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Refreshes the tenant routing datasource to reflect changes in target datasources.
     */
    private void refreshTenantRoutingDataSource() {
        if (tenantRoutingDataSource != null) {
            tenantRoutingDataSource.setTargetDataSources(targetDataSources);
            tenantRoutingDataSource.afterPropertiesSet();
            logger.info("Tenant routing datasource refreshed successfully.");
        } else {
            logger.warning("Cannot refresh tenant routing datasource - instance not initialized.");
        }
    }

    /**
     * Closes the existing datasource for a given tenant.
     *    
     * @param tenantId
     */
    private void closeConnection(DataSource existingDataSource, String tenantId) {
        logger.info("Closing existing datasource for tenant: " + tenantId);
        try {
            if (existingDataSource != null) {
                postgresDbConfig.cleanupDataSource(existingDataSource);
                logger.info("Closed existing datasource for tenant: " + tenantId);
            }
        } catch (Exception e) {
            logger.severe("Error while closing datasource for tenant " + tenantId + ": " + e.getMessage());
            throw new SqlDaoException("Error while closing datasource for tenant : " + tenantId, e);
        }
    }
}
