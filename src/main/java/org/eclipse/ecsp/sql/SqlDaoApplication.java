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

package org.eclipse.ecsp.sql;

import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

/**
 * Main application class for configuring and initializing the SQL DAO module.
 *
 * <p>This class is responsible for:
 * <ul>
 *   <li>Scanning and loading Spring components from the specified base packages.</li>
 *   <li>Providing configuration for the SQL DAO module and its dependencies.</li>
 *   <li>Enabling the integration of utility libraries and dependent beans into the Spring context.</li>
 * </ul>
 *
 * <p>The `@ComponentScan` annotation ensures that all classes within the `org.eclipse.ecsp` package
 * and its sub-packages are scanned and registered as Spring beans.
 *
 * <p>Annotations used:
 * <ul>
 *   <li>`@ComponentScan` - Specifies the base packages to scan for Spring components.</li>
 *   <li>`@Component` - Marks this class as a Spring component.</li>
 *   <li>`@Configuration` - Indicates that this class contains Spring configuration.</li>
 * </ul>
 *
 * <p>This class serves as the entry point for setting up the SQL DAO module in a Spring-based application.
 *
 * @author kaushalaroraharman
 * @version 1.1
 * @since 2025-04-15
 */
@ComponentScan(basePackages = {"org.eclipse.ecsp"})
@ConfigurationPropertiesScan(basePackages = {"org.eclipse.ecsp"})
@Component
@Configuration
public class SqlDaoApplication {

}
