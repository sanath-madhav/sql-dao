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

package org.eclipse.ecsp.sql.dao.constants;

/**
 * Constants for multi-tenancy feature.
 */
public class MultitenantConstants {

    /**
     * Private constructor for multitenant constants.
     */
    private MultitenantConstants() {
        throw new UnsupportedOperationException("MultitenantConstants is a constants class and cannot be instantiated");
    }

    /** Multitenancy related properties. */
    public static final String MULTITENANCY_ENABLED = "multitenancy.enabled";

    /** The Constant DEFAULT_TENANT_ID. */
    public static final String DEFAULT_TENANT_ID = "default";
}
