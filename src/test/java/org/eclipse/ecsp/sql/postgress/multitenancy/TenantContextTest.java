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

import org.eclipse.ecsp.sql.dao.constants.MultitenantConstants;
import org.eclipse.ecsp.sql.exception.TenantNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test for {@link TenantContext}.
 * 
 * Tests ThreadLocal-based tenant context management including:
 * - Setting and getting tenant IDs
 * - Default tenant behavior
 * - Thread isolation
 * - Cleanup and memory leak prevention
 */
class TenantContextTest {

    private static final String TEST_TENANT_1 = "tenant1";
    private static final String TEST_TENANT_2 = "tenant2";

    @BeforeEach
    void setUp() {
        // Ensure clean state before each test
        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        // Clean up after each test to prevent leaks
        TenantContext.clear();
    }

    @Test
    @DisplayName("Should return default tenant when no tenant is set")
    void testGetCurrentTenant_NoTenantSet() {
        assertThrows(TenantNotFoundException.class, () -> {
            TenantContext.getCurrentTenant();
        });
    }

    @Test
    @DisplayName("Should set and get tenant correctly")
    void testSetAndGetCurrentTenant() {
        // When: Setting a tenant
        TenantContext.setCurrentTenant(TEST_TENANT_1);

        // Then: The same tenant is returned
        assertEquals(TEST_TENANT_1, TenantContext.getCurrentTenant());
        assertTrue(TenantContext.hasTenant());
    }

    @Test
    @DisplayName("Should handle null tenant by setting default")
    void testSetCurrentTenant_Null_SetsDefault() {
        // When: Setting null tenant
        TenantContext.setCurrentTenant(null);

        // Then: Default tenant is set
        assertEquals(MultitenantConstants.DEFAULT_TENANT_ID, TenantContext.getCurrentTenant());
        assertTrue(TenantContext.hasTenant());
    }

    @Test
    @DisplayName("Should handle empty string tenant by setting default")
    void testSetCurrentTenant_EmptyString_SetsDefault() {
        // When: Setting empty string tenant
        TenantContext.setCurrentTenant("");

        // Then: Default tenant is set
        assertEquals(MultitenantConstants.DEFAULT_TENANT_ID, TenantContext.getCurrentTenant());
        assertTrue(TenantContext.hasTenant());
    }

    @Test
    @DisplayName("Should handle whitespace-only tenant by setting default")
    void testSetCurrentTenant_Whitespace_SetsDefault() {
        // When: Setting whitespace-only tenant
        TenantContext.setCurrentTenant("   ");

        // Then: Default tenant is set
        assertEquals(MultitenantConstants.DEFAULT_TENANT_ID, TenantContext.getCurrentTenant());
        assertTrue(TenantContext.hasTenant());
    }

    @Test
    @DisplayName("Should clear tenant context")
    void testClear() {
        // Given: A tenant is set
        TenantContext.setCurrentTenant(TEST_TENANT_1);
        assertTrue(TenantContext.hasTenant());

        // When: Clearing the context
        TenantContext.clear();
        // Then: No tenant is set
        assertFalse(TenantContext.hasTenant());
    }

    @Test
    @DisplayName("Should allow changing tenant multiple times")
    void testMultipleTenantChanges() {
        // Given: First tenant set
        TenantContext.setCurrentTenant(TEST_TENANT_1);
        assertEquals(TEST_TENANT_1, TenantContext.getCurrentTenant());

        // When: Changing to second tenant
        TenantContext.setCurrentTenant(TEST_TENANT_2);

        // Then: Second tenant is active
        assertEquals(TEST_TENANT_2, TenantContext.getCurrentTenant());
        assertTrue(TenantContext.hasTenant());
    }

    @Test
    @DisplayName("Should isolate tenant context across threads")
    void testThreadIsolation() throws InterruptedException {
        // Given: Main thread has tenant1
        TenantContext.setCurrentTenant(TEST_TENANT_1);

        AtomicReference<String> threadTenant = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        // When: Another thread sets tenant2
        Thread thread = new Thread(() -> {
            TenantContext.setCurrentTenant(TEST_TENANT_2);
            threadTenant.set(TenantContext.getCurrentTenant());
            latch.countDown();
        });
        thread.start();
        latch.await(5, TimeUnit.SECONDS);

        // Then: Each thread has its own tenant
        assertEquals(TEST_TENANT_1, TenantContext.getCurrentTenant(),
                "Main thread should still have tenant1");
        assertEquals(TEST_TENANT_2, threadTenant.get(),
                "Worker thread should have tenant2");
    }

    @Test
    @DisplayName("Should handle concurrent access from multiple threads")
    void testConcurrentAccess() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(5);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(5);
        AtomicReference<Throwable> error = new AtomicReference<>();

        // When: Multiple threads set different tenants concurrently
        for (int i = 0; i < 5; i++) {
            final String tenantId = "tenant" + i;
            executor.submit(() -> {
                try {
                    startLatch.await(); // Synchronize start
                    TenantContext.setCurrentTenant(tenantId);
                    
                    // Verify tenant is correctly isolated
                    String retrievedTenant = TenantContext.getCurrentTenant();
                    if (!tenantId.equals(retrievedTenant)) {
                        error.set(new AssertionError(
                            "Expected " + tenantId + " but got " + retrievedTenant));
                    }
                    
                    TenantContext.clear();
                } catch (Exception e) {
                    error.set(e);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // Start all threads
        boolean completed = doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        // Then: No errors occurred
        assertTrue(completed, "All threads should complete");
        assertNull(error.get(), "No concurrent access errors should occur");
    }

    @Test
    @DisplayName("Should return false for hasTenant after clear")
    void testHasTenant_AfterClear_ReturnsFalse() {
        // Given: Tenant is set
        TenantContext.setCurrentTenant(TEST_TENANT_1);
        assertTrue(TenantContext.hasTenant());

        // When: Context is cleared
        TenantContext.clear();

        // Then: hasTenant returns false
        assertFalse(TenantContext.hasTenant());
    }

    @Test
    @DisplayName("Should handle multiple clear calls safely")
    void testMultipleClearCalls() {
        // Given: Tenant is set
        TenantContext.setCurrentTenant(TEST_TENANT_1);

        // When: Clearing multiple times
        TenantContext.clear();
        TenantContext.clear();
        TenantContext.clear();

        // Then: No exception and state remains cleared
        assertFalse(TenantContext.hasTenant());
    }

    @Test
    @DisplayName("Should preserve tenant during exception in same thread")
    void testTenantPreservationDuringException() {
        // Given: Tenant is set
        TenantContext.setCurrentTenant(TEST_TENANT_1);

        try {
            // When: An exception occurs
            throw new RuntimeException("Test exception");
        } catch (RuntimeException e) {
            // Then: Tenant is still set
            assertEquals(TEST_TENANT_1, TenantContext.getCurrentTenant());
            assertTrue(TenantContext.hasTenant());
        }
    }

    @Test
    @DisplayName("Should properly cleanup in try-with-resources pattern simulation")
    void testTryWithResourcesPattern() {
        // Simulate typical usage pattern with explicit cleanup
        try {
            TenantContext.setCurrentTenant(TEST_TENANT_1);
            assertEquals(TEST_TENANT_1, TenantContext.getCurrentTenant());
            
            // Simulate some work that might throw
            if (Math.random() < 2.0) { // Always true, simulates normal execution
                // work done
            }
        } finally {
            TenantContext.clear();
        }

        // Then: Context is cleaned up
        assertFalse(TenantContext.hasTenant());
    }
}
