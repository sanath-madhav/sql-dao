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
import java.util.logging.Logger;
import org.eclipse.ecsp.sql.dao.constants.MultitenantConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

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

    /**
     * Initializes the tenant routing data source after bean construction.
     *
     * <p>This method is called automatically after dependency injection is complete.
     * It configures the {@link TenantRoutingDataSource} with appropriate target data sources
     * based on whether multi-tenancy is enabled.</p>
     *
     * <p>For single-tenant mode, it sets both the target data sources map and the default
     * data source. For multi-tenant mode, it only sets the target data sources map, allowing
     * dynamic routing based on the current tenant context.</p>
     */
    @PostConstruct
    public void initTenantRoutingDataSource() {
        logger.info("Initializing TenantAwareDataSource");
        TenantRoutingDataSource tenantRoutingDataSource = new TenantRoutingDataSource();
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
    }
}
