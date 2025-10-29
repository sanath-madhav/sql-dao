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

import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.zaxxer.hikari.HikariDataSource;
import io.prometheus.client.CollectorRegistry;
import org.eclipse.ecsp.sql.postgress.config.DefaultDbProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.util.Map;
import java.util.Set;

/**
 *Test class for {@link PostgresDbHealthMonitor}.
 */
class PostgresDbHealthMonitorUnitTest {

    /** The postgres db health monitor. */
    @InjectMocks
    PostgresDbHealthMonitor postgresDbHealthMonitor;

    /** The postgres db health check. */
    @Mock
    PostgresDbHealthCheck postgresDbHealthCheck;

    @Mock
    Map<Object, Object> targetDataSources;

    /** The data source. */
    HikariDataSource dataSource;

    /** The health check registry. */
    @Mock
    HealthCheckRegistry healthCheckRegistry;

    /** The health check. */
    @Mock
    HealthCheck healthCheck;

    /** The result. */
    @Mock
    HealthCheck.Result result;

    /**
     * Test unhealthy.
     */
    @Test
    void testunHealthy() {
        MockitoAnnotations.openMocks(this);
        Map.Entry<Object, Object> entry = mock(Map.Entry.class);
        when(entry.getValue()).thenReturn(dataSource);
        when(targetDataSources.entrySet()).thenReturn(Set.of(entry));
        boolean healthy = postgresDbHealthMonitor.isHealthy(true);
        assertFalse(healthy);
    }

    /**
     * Test failed health check.
     */
    @Test
    void testFailedHealthCheck() {
        dataSource = mock(HikariDataSource.class);
        MockitoAnnotations.openMocks(this);
        DefaultDbProperties tenantHealthProps = new DefaultDbProperties();
        tenantHealthProps.setPoolName("testPool");
        ReflectionTestUtils.setField(postgresDbHealthMonitor, "defaultTenantHealthProps",
                tenantHealthProps);
        Map.Entry<Object, Object> entry = mock(Map.Entry.class);
        when(targetDataSources.entrySet()).thenReturn(Set.of(entry));
        when(entry.getKey()).thenReturn("default");
        when(entry.getValue()).thenReturn(dataSource);
        
        postgresDbHealthMonitor.init();
        when(dataSource.getHealthCheckRegistry()).thenReturn(healthCheckRegistry);
        when(healthCheckRegistry.getHealthCheck(anyString())).thenReturn(healthCheck);
        when(healthCheck.execute()).thenReturn(result);
        boolean healthy = postgresDbHealthMonitor.isHealthy(true);
        assertFalse(healthy);
    }
}