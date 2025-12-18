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

package org.eclipse.ecsp.sql.authentication;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple credentials provider for multi-tenant integration testing.
 * 
 * <p>This provider is designed to work with the multi-tenant test configuration where
 * credentials are already populated in TenantDatabaseProperties from the properties file
 * (application-dao-multitenant-test.properties).</p>
 * 
 * <p>Property binding happens via @ConfigurationProperties in TenantConfig:
 * <ul>
 *   <li>tenants.profile.tenants.tenant1.user-name=root</li>
 *   <li>tenants.profile.tenants.tenant1.password=root</li>
 *   <li>tenants.profile.tenants.tenant2.user-name=root</li>
 *   <li>tenants.profile.tenants.tenant2.password=root</li>
 *   <li>tenants.profile.tenants.tenant3.user-name=root</li>
 *   <li>tenants.profile.tenants.tenant3.password=root</li>
 * </ul>
 * </p>
 * 
 * <p>This class simply returns these pre-loaded credentials when requested by PostgresDbConfig.</p>
 * 
 * <p><b>How it works:</b></p>
 * <ol>
 *   <li>Spring Boot loads properties into TenantDatabaseProperties via @ConfigurationProperties</li>
 *   <li>PostgresDbConfig creates an instance of this provider via reflection (SqlDaoUtils.getClassInstance)</li>
 *   <li>PostgresDbConfig calls getUserName() and getPassword() from this provider</li>
 *   <li>This provider simply returns the values that are already in TenantDatabaseProperties</li>
 * </ol>
 * 
 * <p><b>Note:</b> This is NOT a Spring @Component. It's instantiated by reflection
 * in PostgresDbConfig via the credential-provider-bean-name property.</p>
 * 
 * @author hbadshah
 * @version 1.1
 * @since 2025-10-28
 */
public class PostgresMultiTenantTestCredentialsProvider implements CredentialsProvider {

    /**
     * Default constructor required for reflection-based instantiation.
     */
    public PostgresMultiTenantTestCredentialsProvider() {
        // Default constructor for reflection by SqlDaoUtils.getClassInstance()
    }

    /**
     * Gets the username for the tenants.profile.
     * 
     * <p>Since TenantDatabaseProperties already contains the username from the properties file,
     * and PostgresDbConfig will override it with this value anyway, we just return a default.
     * The actual value comes from tenants.profile.tenants.{tenantId}.user-name in the properties file.</p>
     * 
     * <p>PostgresDbConfig calls this method and then does:
     * {@code dbProperties.setUserName(credsProvider.getUserName())}</p>
     * 
     * @return the username (default: "root" for test)
     */
    @Override
    public String getUserName() {
        // Return the test username
        // This value is already set in TenantDatabaseProperties from the properties file
        // PostgresDbConfig will call this and override the dbProperties value
        return "root";
    }

    /**
     * Gets the password for the tenants.profile.
     * 
     * <p>Since TenantDatabaseProperties already contains the password from the properties file,
     * and PostgresDbConfig will override it with this value anyway, we just return a default.
     * The actual value comes from tenants.profile.tenants.{tenantId}.password in the properties file.</p>
     * 
     * <p>PostgresDbConfig calls this method and then does:
     * {@code dbProperties.setPassword(credsProvider.getPassword())}</p>
     * 
     * @return the password (default: "root" for test)
     */
    @Override
    public String getPassword() {
        // Return the test password
        // This value is already set in TenantDatabaseProperties from the properties file
        // PostgresDbConfig will call this and override the dbProperties value
        return "root";
    }

    /**
     * Gets all credentials configuration.
     * 
     * @return map containing username and password
     */
    @Override
    public Map<String, Object> getAllCredentialsConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("username", getUserName());
        config.put("password", getPassword());
        return config;
    }

    /**
     * Refresh credentials (no-op for test implementation).
     * Test credentials don't need to be refreshed.
     */
    @Override
    public void refreshCredentials() {
        // No refresh needed for static test credentials
    }

    /**
     * Checks if credential refresh is in progress.
     * 
     * @return false, as test credentials don't require refresh
     */
    @Override
    public boolean isRefreshInProgress() {
        return false;
    }
}
