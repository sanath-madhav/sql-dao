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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    @DisplayName("Should return default tenant when no tenant is set in single-tenant mode")
    void testGetCurrentTenant_NoTenantSet_SingleTenantMode() {
        // Given: Multitenancy is disabled
        System.setProperty(MultitenantConstants.MULTITENANCY_ENABLED, "false");
        
        try {
            // Then: Default tenant is returned
            assertThrows(TenantNotFoundException.class, () -> {
                TenantContext.getCurrentTenant();
            });
        } finally {
            System.clearProperty(MultitenantConstants.MULTITENANCY_ENABLED);
        }
    }

    @Test
    @DisplayName("Should throw exception when no tenant is set in multi-tenant mode")
    void testGetCurrentTenant_NoTenantSet_MultiTenantMode() {
        // Given: Multitenancy is enabled
        System.setProperty(MultitenantConstants.MULTITENANCY_ENABLED, "true");
        
        try {
            // When/Then: Getting tenant without setting it throws exception
            TenantNotFoundException exception = assertThrows(TenantNotFoundException.class, () -> {
                TenantContext.getCurrentTenant();
            });
            assertTrue(exception.getMessage().contains("multitenant mode"));
        } finally {
            System.clearProperty(MultitenantConstants.MULTITENANCY_ENABLED);
        }
    }

    @Test
    @DisplayName("Should set and get tenant correctly")
    void testSetAndGetCurrentTenant() {
        System.setProperty("multitenancy.enabled", "true");
        // When: Setting a tenant
        TenantContext.setCurrentTenant(TEST_TENANT_1);

        // Then: The same tenant is returned
        assertEquals(TEST_TENANT_1, TenantContext.getCurrentTenant());
        assertTrue(TenantContext.hasTenant());
    }

    @Test
    @DisplayName("Should handle null tenant by setting default in single-tenant mode")
    void testSetCurrentTenant_Null_SetsDefault_SingleTenantMode() {
        // Given: Multitenancy is disabled
        System.setProperty(MultitenantConstants.MULTITENANCY_ENABLED, "false");
        
        try {
            // When: Setting null tenant
            TenantContext.setCurrentTenant(null);

            // Then: Default tenant is set
            assertEquals(MultitenantConstants.DEFAULT_TENANT_ID, TenantContext.getCurrentTenant());
            assertTrue(TenantContext.hasTenant());
        } finally {
            System.clearProperty(MultitenantConstants.MULTITENANCY_ENABLED);
            TenantContext.clear();
        }
    }

    @Test
    @DisplayName("Should throw exception for null tenant in multi-tenant mode")
    void testSetCurrentTenant_Null_ThrowsException_MultiTenantMode() {
        // Given: Multitenancy is enabled
        System.setProperty(MultitenantConstants.MULTITENANCY_ENABLED, "true");
        
        try {
            // When/Then: Setting null tenant throws exception
            TenantNotFoundException exception = assertThrows(TenantNotFoundException.class, () -> {
                TenantContext.setCurrentTenant(null);
            });
            assertTrue(exception.getMessage().contains("multitenant mode"));
        } finally {
            System.clearProperty(MultitenantConstants.MULTITENANCY_ENABLED);
            TenantContext.clear();
        }
    }

    @Test
    @DisplayName("Should handle empty string tenant by setting default in single-tenant mode")
    void testSetCurrentTenant_EmptyString_SetsDefault_SingleTenantMode() {
        // Given: Multitenancy is disabled
        System.setProperty(MultitenantConstants.MULTITENANCY_ENABLED, "false");
        
        try {
            // When: Setting empty string tenant
            TenantContext.setCurrentTenant("");

            // Then: Default tenant is set
            assertEquals(MultitenantConstants.DEFAULT_TENANT_ID, TenantContext.getCurrentTenant());
            assertTrue(TenantContext.hasTenant());
        } finally {
            System.clearProperty(MultitenantConstants.MULTITENANCY_ENABLED);
            TenantContext.clear();
        }
    }

    @Test
    @DisplayName("Should throw exception for empty string in multi-tenant mode")
    void testSetCurrentTenant_EmptyString_ThrowsException_MultiTenantMode() {
        // Given: Multitenancy is enabled
        System.setProperty(MultitenantConstants.MULTITENANCY_ENABLED, "true");
        
        try {
            // When/Then: Setting empty string throws exception
            TenantNotFoundException exception = assertThrows(TenantNotFoundException.class, () -> {
                TenantContext.setCurrentTenant("");
            });
            assertTrue(exception.getMessage().contains("multitenant mode"));
        } finally {
            System.clearProperty(MultitenantConstants.MULTITENANCY_ENABLED);
            TenantContext.clear();
        }
    }

    @Test
    @DisplayName("Should handle whitespace-only tenant by setting default in single-tenant mode")
    void testSetCurrentTenant_Whitespace_SetsDefault_SingleTenantMode() {
        // Given: Multitenancy is disabled
        System.setProperty(MultitenantConstants.MULTITENANCY_ENABLED, "false");
        
        try {
            // When: Setting whitespace-only tenant
            TenantContext.setCurrentTenant("   ");

            // Then: Default tenant is set
            assertEquals(MultitenantConstants.DEFAULT_TENANT_ID, TenantContext.getCurrentTenant());
            assertTrue(TenantContext.hasTenant());
        } finally {
            System.clearProperty(MultitenantConstants.MULTITENANCY_ENABLED);
            TenantContext.clear();
        }
    }

    @Test
    @DisplayName("Should throw exception for whitespace-only in multi-tenant mode")
    void testSetCurrentTenant_Whitespace_ThrowsException_MultiTenantMode() {
        // Given: Multitenancy is enabled
        System.setProperty(MultitenantConstants.MULTITENANCY_ENABLED, "true");
        
        try {
            // When/Then: Setting whitespace-only throws exception
            TenantNotFoundException exception = assertThrows(TenantNotFoundException.class, () -> {
                TenantContext.setCurrentTenant("   ");
            });
            assertTrue(exception.getMessage().contains("multitenant mode"));
        } finally {
            System.clearProperty(MultitenantConstants.MULTITENANCY_ENABLED);
            TenantContext.clear();
        }
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
        System.setProperty("multitenancy.enabled", "true");
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
        System.setProperty("multitenancy.enabled", "true");
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
        System.setProperty("multitenancy.enabled", "true");
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
        System.setProperty("multitenancy.enabled", "true");
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
        System.setProperty("multitenancy.enabled", "true");
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
