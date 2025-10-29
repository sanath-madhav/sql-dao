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

import javax.sql.DataSource;
import org.eclipse.ecsp.sql.dao.constants.MultitenantConstants;
import org.eclipse.ecsp.sql.exception.TargetDataSourceNotFoundException;
import org.eclipse.ecsp.sql.exception.TenantNotFoundException;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * Custom AbstractRoutingDataSource for Multi-tenancy which routes database
 * connections based on the current tenant context.
 * This class extends Spring's AbstractRoutingDataSource to provide
 * tenant-specific database routing. The tenant context is determined by the
 * TenantContext class.
 * Database routing flow:
 * <li>1. TenantContext.setCurrentTenant(tenantId) stores tenant in
 * thread-local.</li>
 * <li>2. This router's determineCurrentLookupKey() returns current tenant
 * ID.</li>
 * <li>3. Spring routes database operations to tenant-specific DataSource</li>
 * <li>4. TenantContext.clear() clears context after request completion</li>
 *
 * @author hbadshah
 */
public class TenantRoutingDataSource extends AbstractRoutingDataSource {

    /** Logger for this class */
    public static final IgniteLogger LOGGER =
            IgniteLoggerFactory.getLogger(TenantRoutingDataSource.class);

    /**
     * Determine the current lookup key for routing to the appropriate
     * DataSource. This method is called by Spring's AbstractRoutingDataSource
     * for every database operation.
     *
     * @return the tenant ID from current thread context, used as DataSource
     *         lookup key
     */
    @Override
    protected Object determineCurrentLookupKey() {
        boolean isMultitenancyEnabled = Boolean.parseBoolean(System.getProperty(MultitenantConstants.MULTITENANCY_ENABLED));
        if (!isMultitenancyEnabled) {
            LOGGER.info("Multitenancy is disabled. Using default tenant.");
            return MultitenantConstants.DEFAULT_TENANT_ID;
        } else {
            return TenantContext.getCurrentTenant();
        }
    }

    /**
     * Override to provide custom DataSource resolution when lookup key is not
     * found. This is called when determineCurrentLookupKey() returns a key
     * that doesn't exist in the configured target DataSources map.
     *
     * @return DataSource for the current tenant context
     */
    @Override
    protected DataSource determineTargetDataSource() {
        try {
            return super.determineTargetDataSource();
        } catch (IllegalStateException e) {
            String tenantId = TenantContext.getCurrentTenant();
            LOGGER.error("Failed to determine target DataSource for tenant '{}': {}", tenantId,
                    e.getMessage());
            throw new TargetDataSourceNotFoundException(
                    String.format(
                            "No DataSource configured for tenant '%s'. "
                                    + "Please verify tenant configuration and database setup.",
                            tenantId), e);
        }
    }
}
