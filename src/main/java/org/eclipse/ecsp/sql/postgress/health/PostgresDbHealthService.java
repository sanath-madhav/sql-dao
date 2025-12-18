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

import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PostConstruct;
import org.eclipse.ecsp.healthcheck.HealthService;
import org.eclipse.ecsp.healthcheck.HealthServiceCallBack;
import org.eclipse.ecsp.sql.dao.constants.PostgresDbConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import java.util.Map;
import javax.sql.DataSource;

/**
 * Health service customization for PostgresDB.
 * <br>
 * It provides restart callback to the {@link HealthService}
 * <br>
 * This component depends on {@code postgresDbConfig} bean to ensure that the datasources
 * are initialized before health service callbacks are registered.
 */
@Component
@DependsOn("postgresDbConfig")
public class PostgresDbHealthService {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(PostgresDbHealthService.class);

    /** The datasource. */
    @Autowired
    @Qualifier("targetDataSources")
    private Map<String, DataSource> targetDataSources;

    /** The health service. */
    @Autowired
    private HealthService healthService;

    /** The restart on failure flag. */
    @Value("${sp.restart.on.failure:false}")
    private boolean restartOnFailure;

    /** The pool name. */
    @Value("${" + PostgresDbConstants.POSTGRES_POOL_NAME + "}")
    private String poolName;

    /**
     * Register callback for PostgresDB health service.
     */
    @PostConstruct
    public void init() {
        LOGGER.info("Registering callback for PostgresDbHealthService...");
        healthService.registerCallBack(new PostgresHealthServiceCallBack());
        LOGGER.info("Starting health service executor...");
        healthService.startHealthServiceExecutor();
    }

    /**
     * Postgres health service callBack.
     */
    class PostgresHealthServiceCallBack implements HealthServiceCallBack {

        /**
         * Perform restart.
         *
         * @return true, if successful
         */
        @Override
        public boolean performRestart() {
            if (restartOnFailure) {
                for (Map.Entry<String, DataSource> entry : targetDataSources.entrySet()) {
                    String tenantId = entry.getKey();
                    DataSource dataSource = entry.getValue();
                    LOGGER.info("Closing HikariDataSource for tenant: {}", tenantId);
                    ((HikariDataSource) dataSource).close();
                    LOGGER.info("HikariDataSource for tenant: {} closed successfully.", tenantId);
                    return true;
                }
            }
            return restartOnFailure;
        }
    }
}
