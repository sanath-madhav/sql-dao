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

import org.eclipse.ecsp.sql.dao.constants.MultitenantConstants;
import org.eclipse.ecsp.sql.exception.TenantNotFoundException;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;

/**
 * TenantContext is a utility class that provides a way to manage the current
 * tenant in a thread-local context.
 * 
 * @author hbadshah
 */
public class TenantContext {

    public static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(TenantContext.class);

    private TenantContext() {
        // Private constructor to prevent instantiation
    }

    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();
    private static final String DEFAULT_TENANT_ID = MultitenantConstants.DEFAULT_TENANT_ID;

    /**
     * Get the current tenant ID from thread local context.
     *
     * @return current tenant ID or default if not set
     */
    public static String getCurrentTenant() {
        String tenant = CURRENT_TENANT.get();
        if (tenant == null) {
            LOGGER.error("No tenant found in context.");
            throw new TenantNotFoundException("No tenant found in context.");
        }
        return tenant;
    }

    /**
     * Set the current tenant ID in thread local context.
     *
     * @param tenant the tenant ID to set
     */
    public static void setCurrentTenant(String tenant) {
        boolean isMultitenancyEnabled = Boolean.parseBoolean(System.getProperty(MultitenantConstants.MULTITENANCY_ENABLED));
        if (!isMultitenancyEnabled) {
            LOGGER.info("Multitenancy is disabled. Setting default tenant.");
            if (tenant == null || tenant.trim().isEmpty()) {
                tenant = DEFAULT_TENANT_ID;
            }
        } else if (tenant == null || tenant.trim().isEmpty()) {
            LOGGER.error("Attempted to set null or empty tenant in multitenant mode.");
            throw new TenantNotFoundException("Tenant ID cannot be null or empty in multitenant mode.");
        }
        CURRENT_TENANT.set(tenant);
        LOGGER.info("Current tenant set to: {}", tenant);
    }

    /**
     * Clear the current tenant from thread local context.
     * Should be called at the end of request processing to prevent memory leaks.
     */
    public static void clear() {
        String tenant = CURRENT_TENANT.get();
        CURRENT_TENANT.remove();
        LOGGER.debug("Cleared tenant context for tenant ID: {}", tenant);
    }

    /**
     * Check if a tenant is currently set.
     *
     * @return true if tenant is set, false otherwise
     */
    public static boolean hasTenant() {
        return CURRENT_TENANT.get() != null;
    }
}