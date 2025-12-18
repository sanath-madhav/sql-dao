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

import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for multi-tenant database properties.
 * Exposes a Map bean bound to the "tenant" prefix to allow direct binding
 * of properties like tenants.profile.tenantA.jdbc-url without the redundant "tenants" level.
 * 
 * @author hbadshah
 * @version 1.1
 * @since 2025-12-18
 */
@Configuration
public class TenantConfig {

    /**
     * Creates a Map bean for tenant database properties.
     * Properties with prefix "tenant" will be bound as map entries.
     * For example: tenants.profile.tenantA.jdbc-url will create a map entry with key "tenantA".
     *
     * @return Map of tenant IDs to their database properties
     */
    @Bean("tenantConfigMap")
    @ConfigurationProperties(prefix = "tenants.profile")
    public Map<String, TenantDatabaseProperties> tenantConfigMap() {
        return new HashMap<>();
    }
}
