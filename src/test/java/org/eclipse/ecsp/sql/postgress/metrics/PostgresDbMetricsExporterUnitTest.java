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

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import org.eclipse.ecsp.sql.postgress.config.DefaultDbProperties;

/**
 * Test class for {@link IgnitePostgresDbMetricsExporter}.
 */
class PostgresDbMetricsExporterUnitTest {

    /** The postgres db gauge. */
    @Mock
    private IgnitePostgresDbGuage postgresDbGauge;

    /** The datasource. */
    @Mock
    private HikariDataSource datasource;

    /** The exporter. */
    @InjectMocks
    private IgnitePostgresDbMetricsExporter exporter;

    /** The metric registry. */
    @Mock
    MetricRegistry metricRegistry;

    /** The Constant TWO. */
    public static final int TWO = 2;

    /**
     * Test metrics.
     */
    @Test
    void testMetrics() {
        MockitoAnnotations.openMocks(this);
        DefaultDbProperties tenantHealthProps = new DefaultDbProperties();
        tenantHealthProps.setPoolName("test");
        ReflectionTestUtils.setField(exporter, "defaultTenantHealthProps", tenantHealthProps);
        ReflectionTestUtils.setField(exporter, "postgresDbMetricsEnabled", true);
        ReflectionTestUtils.setField(exporter, "threadInitialDelay", 0);
        ReflectionTestUtils.setField(exporter, "threadFrequency", 1);
        ReflectionTestUtils.setField(exporter, "postgresDbGauge", postgresDbGauge);
        ReflectionTestUtils.setField(exporter, "svc", "test");
        ReflectionTestUtils.setField(exporter, "nodeName", "localhost");
        ReflectionTestUtils.setField(exporter, "prometheusEnabled", Boolean.TRUE);
        when(datasource.getMetricRegistry()).thenReturn(metricRegistry);
        Gauge gauge = new Gauge() {
            @Override
            public Object getValue() {
                return 0;
            }
        };
        when(metricRegistry.gauge(anyString())).thenReturn(gauge);
        exporter.init();
        assertNotNull(exporter.metricsList);
        assertNotNull(datasource);
        exporter.close();
    }
}