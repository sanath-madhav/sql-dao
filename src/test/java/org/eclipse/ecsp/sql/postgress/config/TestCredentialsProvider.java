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

import org.eclipse.ecsp.sql.authentication.CredentialsProvider;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * The Class TestCredentialsProvider.
 */
@Component("testCredentialsProvider")
class TestCredentialsProvider implements CredentialsProvider {

    /** The refresh. */
    public boolean refresh = true;
    
    /** The temp. */
    int temp = 0;

    /**
     * Gets the user name.
     *
     * @return the user name
     */
    @Override
    public String getUserName() {
        return "test";
    }

    /**
     * Gets the password.
     *
     * @return the password
     */
    @Override
    public String getPassword() {
        return "password";
    }

    /**
     * Gets the all credentials config.
     *
     * @return the all credentials config
     */
    @Override
    public Map<String, Object> getAllCredentialsConfig() {
        return new HashMap<>();
    }

    /**
     * Refresh credentials.
     */
    @Override
    public void refreshCredentials() {
        // credentials are not refreshed for testing
    }

    /**
     * Checks if is refresh in progress.
     *
     * @return true, if is refresh in progress
     */
    @Override
    public boolean isRefreshInProgress() {
        if (refresh && temp == 1) {
            refresh = false;
        }
        temp++;
        return refresh;
    }

}
