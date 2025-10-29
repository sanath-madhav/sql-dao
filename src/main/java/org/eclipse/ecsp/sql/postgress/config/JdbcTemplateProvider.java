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

import org.eclipse.ecsp.sql.dao.constants.MultitenantConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;

/**
 * A configurable template provider for executing commands with the DB connection obtained
 * from the Hikari CP pool.
 *
 * @author karora
 */
@Configurable
@Configuration
public class JdbcTemplateProvider {

    /** The target data sources for each tenantId. */
    @Autowired
    @Qualifier("targetDataSources")
    private Map<Object, Object> targetDataSources;

    /** Flag to enable or disable multi-tenancy */
    @Value("${" + MultitenantConstants.MULTITENANCY_ENABLED + ":false}")
    private boolean isMultitenancyEnabled;

    /**
     * Provides a map of tenant ID to Jdbc template.
     *
     * @return the jdbc template
     */
    @Bean("jdbcTemplates")
    public Map<String, JdbcTemplate> constructJdbcTemplates() {
        Map<String, JdbcTemplate> jdbcTemplates = new HashMap<>();
        if (!isMultitenancyEnabled) {
            jdbcTemplates.put(MultitenantConstants.DEFAULT_TENANT_ID, new JdbcTemplate(
                    (DataSource) targetDataSources.get(MultitenantConstants.DEFAULT_TENANT_ID)));
        } else {
            for (Map.Entry<Object, Object> entry : targetDataSources.entrySet()) {
                jdbcTemplates.put((String) entry.getKey(), new JdbcTemplate((DataSource) entry.getValue()));
            }
        }
        return jdbcTemplates;
    }

    /**
     * Provides a map of NamedParameterJdbcTemplate per tenant.
     *
     * @return map of tenantId to NamedParameterJdbcTemplate
     */
    @Bean("namedParameterJdbcTemplates")
    public Map<String, NamedParameterJdbcTemplate> constructNamedParameterJdbcTemplates() {
        Map<String, NamedParameterJdbcTemplate> namedParameterJdbcTemplates = new HashMap<>();
        if (!isMultitenancyEnabled) {
            namedParameterJdbcTemplates.put(MultitenantConstants.DEFAULT_TENANT_ID,
                new NamedParameterJdbcTemplate((DataSource) targetDataSources.get(MultitenantConstants.DEFAULT_TENANT_ID)));
        } else {
            for (Map.Entry<Object, Object> entry : targetDataSources.entrySet()) {
                namedParameterJdbcTemplates.put((String) entry.getKey(),
                    new NamedParameterJdbcTemplate((DataSource) entry.getValue()));
            }
        }
        return namedParameterJdbcTemplates;
    }
}
