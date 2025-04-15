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

import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.eclipse.ecsp.utils.metrics.IgniteGuage;
import org.springframework.stereotype.Component;

/**
 * Creates and registers Guage metric in Prometheus for metrics in PostgresDB,
 * with labels = serviceName and metricName(the actual name of the HikariCP datasource metric property).
 * <br/>
 *
 * @author karora
 */
@Component
public class IgnitePostgresDbGuage extends IgniteGuage {

    /** The Constant LOGGER. */
    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(IgnitePostgresDbGuage.class);

    /**
     * Create guage for postgres metrics.
     */
    public void setup() {
        createGuage("postgresdb_metric", "metric_name", "svc", "node");
        LOGGER.debug("postgresdb_metric guage successfully created.");
    }
}
