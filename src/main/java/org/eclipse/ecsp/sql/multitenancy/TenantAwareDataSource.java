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

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
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
    private Map<String, DataSource> targetDataSources;

    @Autowired
    private PostgresDbConfig postgresDbConfig;

    @Autowired
    @Qualifier("tenantConfigMap")
    private Map<String, TenantDatabaseProperties> tenantConfigMap;

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
        TenantRoutingDataSource tenantRoutingDataSource = new TenantRoutingDataSource();
        if (!isMultitenancyEnabled) {
            logger.info("Multitenancy is disabled. Using default tenant data source.");
            tenantRoutingDataSource.setTargetDataSources(new HashMap<>(targetDataSources));
            tenantRoutingDataSource.setDefaultTargetDataSource(
                    targetDataSources.get(MultitenantConstants.DEFAULT_TENANT_ID));
        } else {
            logger.info("Multitenancy is enabled. Setting target data sources for tenants.");
            tenantRoutingDataSource.setTargetDataSources(new HashMap<>(targetDataSources));
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
        logger.info("Adding/updating tenant datasource for: " + tenantId);

        // Validating multitenancy is enabled
        if (!validateMultitenancyEnabled("add or update")) {
            return false;
        }

        Optional<String> validatedTenantId = validateAndNormalizeTenantId(tenantId);
        if (!validatedTenantId.isPresent()) {
            return false;
        }
        tenantId = validatedTenantId.get();

        if (!isTenantPropertiesNull(tenantId, tenantDatabaseProperties)) {
            return false;
        }

        try {
            return performAddOrUpdate(tenantId, tenantDatabaseProperties);
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "Error adding or update tenant datasource for " + tenantId + ": " + exception.getMessage());
            throw new SqlDaoException("Error adding or update tenant datasource for " + tenantId, exception);
        }
    }

    /**
     * Removes a tenant-specific datasource at runtime.
     * 
     * @param tenantId The tenant identifier to remove
     * @return true if operation succeeded, false otherwise
     */
    public synchronized boolean removeTenantDataSource(String tenantId) {
        logger.info("Remove tenant datasource for: " + tenantId);

        // Validate preconditions
        if (!validateMultitenancyEnabled("remove")) {
            return false;
        }

        Optional<String> normalizedTenantId = validateAndNormalizeTenantId(tenantId);
        if (!normalizedTenantId.isPresent()) {
            return false;
        }

        try {
            return performRemove(normalizedTenantId.get());
        } catch (Exception exception) {
            handleOperationError("removing", normalizedTenantId.get(), exception);
            throw new SqlDaoException("Error removing tenant datasource for " + normalizedTenantId.get(), exception);
        }
    }

    /**
     * Validates if multitenancy is enabled for the operation.
     * 
     * @param operation The operation being performed (for logging)
     * @return true if multitenancy is enabled, false otherwise
     */
    private boolean validateMultitenancyEnabled(String operation) {
        if (!isMultitenancyEnabled) {
            logger.warning("Multitenancy is disabled. Cannot " + operation + " tenant datasource.");
            return false;
        }
        return true;
    }

    /**
     * Validates and normalizes the tenant ID.
     * 
     * @param tenantId The tenant ID to validate
     * @return Optional containing normalized tenant ID if valid, empty otherwise
     */
    private Optional<String> validateAndNormalizeTenantId(String tenantId) {
        return Optional.ofNullable(tenantId)
            .map(String::trim)
            .filter(id -> !id.isEmpty())
            .or(() -> {
                logger.warning("Invalid tenant ID provided. Cannot add tenant datasource.");
                return Optional.empty();
            });
    }

    /**
     * Validates tenant database properties.
     * 
     * @param tenantId The tenant identifier
     * @param properties The properties to validate
     * @return true if properties are valid, false otherwise
     */
    private boolean isTenantPropertiesNull(String tenantId, TenantDatabaseProperties properties) {
        return Optional.ofNullable(properties)
            .map(p -> true)
            .orElseGet(() -> {
                logger.warning("Tenant database properties cannot be null for tenant: " + tenantId);
                return false;
            });
    }

    /**
     * Performs the actual add or update operation.
     * 
     * @param tenantId The normalized tenant identifier
     * @param tenantDatabaseProperties The database properties
     * @return true if operation succeeded, false otherwise
     */
    private boolean performAddOrUpdate(String tenantId, TenantDatabaseProperties tenantDatabaseProperties)
        throws InterruptedException, SQLException {

        // Create new datasource
        Optional<DataSource> newDataSource = createDataSource(tenantId, tenantDatabaseProperties);
        if (!newDataSource.isPresent()) {
            return false;
        }

        // Get existing datasource before replacement
        Optional<DataSource> existingDataSource = Optional.ofNullable(
            (DataSource) targetDataSources.get(tenantId));

        // Update datasource map
        updateDataSourceMap(tenantId, newDataSource.get());

        // Refresh routing
        refreshTenantRoutingDataSource();

        // Clean up old datasource
        existingDataSource.ifPresent(ds -> closeConnection(ds, tenantId));
        // Update multi-tenant properties
        tenantConfigMap.put(tenantId, tenantDatabaseProperties);
        logger.info("Successfully added/updated datasource for tenant: " + tenantId);
        return true;
    }

    /**
     * Performs the actual remove operation.
     * 
     * @param tenantId The normalized tenant identifier
     * @return true if operation succeeded, false otherwise
     */
    private boolean performRemove(String tenantId) {
        // Get existing datasource
        Optional<DataSource> existingDataSource = Optional.ofNullable(
            (DataSource) targetDataSources.get(tenantId));

        // Remove from map
        removeFromDataSourceMap(tenantId);

        // Refresh routing
        refreshTenantRoutingDataSource();

        // Clean up datasource
        existingDataSource.ifPresent(ds -> closeConnection(ds, tenantId));

        // Remove credentials provider for the tenant
        postgresDbConfig.removeCredentialsProvider(tenantId);
        logger.info("Successfully removed credentials provider for tenant: " + tenantId);

        // Update multi-tenant properties
        tenantConfigMap.remove(tenantId);
        return true;
    }

    /**
     * Creates a new datasource for the tenant.
     * 
     * @param tenantId The tenant identifier
     * @param properties The database properties
     * @return Optional containing created DataSource if successful, empty otherwise
     * @throws SQLException 
     * @throws InterruptedException 
     */
    private Optional<DataSource> createDataSource(String tenantId, TenantDatabaseProperties tenantDatabaseProperties) throws InterruptedException, SQLException {
        String credentialProviderBeanName = tenantDatabaseProperties.getCredentialProviderBeanName();
        if (credentialProviderBeanName != null && !credentialProviderBeanName.trim().isEmpty()) {
            postgresDbConfig.addOrUpdateCredentialsProvider(tenantId, credentialProviderBeanName);
            logger.info("Successfully added/updated credentials provider for tenant: " + tenantId);
        }
        return Optional.ofNullable(postgresDbConfig.initDataSource(tenantId, tenantDatabaseProperties));
    }

    /**
     * Updates the datasource map with the new datasource.
     * 
     * @param tenantId The tenant identifier
     * @param dataSource The datasource to add
     */
    private void updateDataSourceMap(String tenantId, DataSource dataSource) {
        targetDataSources.put(tenantId, dataSource);
        logger.info("Successfully added tenant datasource to map: " + tenantId);
    }

    /**
     * Removes a datasource from the map.
     * 
     * @param tenantId The tenant identifier
     */
    private void removeFromDataSourceMap(String tenantId) {
        targetDataSources.remove(tenantId);
    }

    /**
     * Handles errors during datasource operations.
     * 
     * @param operation The operation being performed
     * @param tenantId The tenant identifier
     * @param exception The exception that occurred
     */
    private void handleOperationError(String operation, String tenantId, Exception exception) {
        logger.log(Level.SEVERE, "Error " + operation + " tenant datasource for " 
                   + tenantId + ": " + exception.getMessage());
    }

    /**
     * Refreshes the tenant routing datasource to reflect changes in target datasources.
     */
    private void refreshTenantRoutingDataSource() {
        Optional.ofNullable(tenantRoutingDataSource)
            .ifPresentOrElse(
                routing -> {
                    routing.setTargetDataSources(new HashMap<>(targetDataSources));
                    routing.afterPropertiesSet();
                    logger.info("Tenant routing datasource refreshed successfully.");
                },
                () -> logger.warning("Cannot refresh tenant routing datasource - instance not initialized.")
            );
    }

    /**
     * Closes the existing datasource for a given tenant.
     *    
     * @param existingDataSource The datasource to close
     * @param tenantId The tenant identifier
     */
    private void closeConnection(DataSource existingDataSource, String tenantId) {
        logger.info("Closing existing datasource for tenant: " + tenantId);
        try {
            postgresDbConfig.cleanupDataSource(existingDataSource);
            logger.info("Closed existing datasource for tenant: " + tenantId);
        } catch (Exception e) {
            logger.severe("Error while closing datasource for tenant " + tenantId + ": " + e.getMessage());
            throw new SqlDaoException("Error while closing datasource for tenant : " + tenantId, e);
        }
    }
}
