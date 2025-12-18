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

package org.eclipse.ecsp.sql.dao.utils;

import org.eclipse.ecsp.sql.exception.SqlDaoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SqlDaoUtils}.
 * 
 * Tests the utility class functionality for dynamic class loading and instantiation,
 * including both Spring bean retrieval and direct instantiation fallback.
 */
@ExtendWith(MockitoExtension.class)
class SqlDaoUtilsTest {

    @Mock
    private ApplicationContext applicationContext;

    @InjectMocks
    private SqlDaoUtils sqlDaoUtils;

    // Test class with public no-arg constructor
    public static class TestClassWithNoArgConstructor {
        public TestClassWithNoArgConstructor() {
            // Default constructor
        }
    }

    // Test class without public no-arg constructor
    public static class TestClassWithoutNoArgConstructor {
        private final String value;

        public TestClassWithoutNoArgConstructor(String value) {
            this.value = value;
        }
    }

    @BeforeEach
    void setUp() {
        // Reset mocks before each test
        reset(applicationContext);
    }

    @Test
    @DisplayName("Should load class from Spring context when bean exists")
    void testGetClassInstance_LoadFromSpringContext() {
        // Given: A class that exists as a Spring bean
        String className = TestClassWithNoArgConstructor.class.getName();
        TestClassWithNoArgConstructor expectedBean = new TestClassWithNoArgConstructor();
        
        when(applicationContext.getBean(any(Class.class))).thenReturn(expectedBean);

        // When: Getting class instance
        Object result = sqlDaoUtils.getClassInstance(className);

        // Then: Bean is loaded from Spring context
        assertNotNull(result);
        assertSame(expectedBean, result);
        verify(applicationContext).getBean(any(Class.class));
    }

    @Test
    @DisplayName("Should create new instance when not found in Spring context")
    void testGetClassInstance_CreateNewInstanceWhenNotInContext() {
        // Given: A class that is not in Spring context but has no-arg constructor
        String className = TestClassWithNoArgConstructor.class.getName();
        
        when(applicationContext.getBean(any(Class.class)))
                .thenThrow(new RuntimeException("Bean not found"));

        // When: Getting class instance
        Object result = sqlDaoUtils.getClassInstance(className);

        // Then: New instance is created
        assertNotNull(result);
        assertInstanceOf(TestClassWithNoArgConstructor.class, result);
        verify(applicationContext).getBean(any(Class.class));
    }

    @Test
    @DisplayName("Should throw SqlDaoException when class not found on classpath")
    void testGetClassInstance_ClassNotFoundOnClasspath() {
        // Given: A class name that doesn't exist
        String className = "com.nonexistent.ClassThatDoesNotExist";

        // When/Then: SqlDaoException is thrown
        SqlDaoException exception = assertThrows(SqlDaoException.class, () -> {
            sqlDaoUtils.getClassInstance(className);
        });

        assertTrue(exception.getMessage().contains(className));
        assertTrue(exception.getMessage().contains("could not be loaded"));
    }

    @Test
    @DisplayName("Should throw SqlDaoException when class has no accessible constructor")
    void testGetClassInstance_NoAccessibleConstructor() {
        // Given: A class without no-arg constructor
        String className = TestClassWithoutNoArgConstructor.class.getName();
        
        when(applicationContext.getBean(any(Class.class)))
                .thenThrow(new RuntimeException("Bean not found"));

        // When/Then: SqlDaoException is thrown
        SqlDaoException exception = assertThrows(SqlDaoException.class, () -> {
            sqlDaoUtils.getClassInstance(className);
        });

        assertTrue(exception.getMessage().contains(className));
        assertTrue(exception.getMessage().contains("could not be loaded"));
    }

    @Test
    @DisplayName("Should handle null class name gracefully")
    void testGetClassInstance_NullClassName() {
        // Given: A null class name
        String className = null;

        // When/Then: SqlDaoException is thrown
        SqlDaoException exception = assertThrows(SqlDaoException.class, () -> {
            sqlDaoUtils.getClassInstance(className);
        });

        assertTrue(exception.getMessage().contains("could not be loaded"));
    }

    @Test
    @DisplayName("Should handle empty class name gracefully")
    void testGetClassInstance_EmptyClassName() {
        // Given: An empty class name
        String className = "";

        // When/Then: SqlDaoException is thrown
        SqlDaoException exception = assertThrows(SqlDaoException.class, () -> {
            sqlDaoUtils.getClassInstance(className);
        });

        assertTrue(exception.getMessage().contains("could not be loaded"));
    }

    @Test
    @DisplayName("Should handle Spring context exception and fallback to direct instantiation")
    void testGetClassInstance_SpringContextExceptionWithFallback() {
        // Given: Spring context throws exception but class has no-arg constructor
        String className = TestClassWithNoArgConstructor.class.getName();
        
        when(applicationContext.getBean(any(Class.class)))
                .thenThrow(new IllegalStateException("Context initialization failed"));

        // When: Getting class instance
        Object result = sqlDaoUtils.getClassInstance(className);

        // Then: Falls back to direct instantiation
        assertNotNull(result);
        assertInstanceOf(TestClassWithNoArgConstructor.class, result);
        verify(applicationContext).getBean(any(Class.class));
    }

    @Test
    @DisplayName("Should load standard Java class when available")
    void testGetClassInstance_StandardJavaClass() {
        // Given: A standard Java class name
        String className = HashMap.class.getName();
        
        when(applicationContext.getBean(any(Class.class)))
                .thenThrow(new RuntimeException("Bean not found"));

        // When: Getting class instance
        Object result = sqlDaoUtils.getClassInstance(className);

        // Then: HashMap instance is created
        assertNotNull(result);
        assertInstanceOf(HashMap.class, result);
    }

    @Test
    @DisplayName("Should preserve exception message when class loading fails")
    void testGetClassInstance_PreservesExceptionMessage() {
        // Given: A class that will fail to load
        String className = "invalid..ClassName";

        // When/Then: Exception message contains details
        SqlDaoException exception = assertThrows(SqlDaoException.class, () -> {
            sqlDaoUtils.getClassInstance(className);
        });

        assertTrue(exception.getMessage().contains("Class " + className + " could not be loaded"));
        assertTrue(exception.getMessage().contains("Exception is:"));
    }

    @Test
    @DisplayName("Should handle concurrent calls safely")
    void testGetClassInstance_ConcurrentCalls() throws InterruptedException {
        // Given: Multiple threads requesting instances
        String className = TestClassWithNoArgConstructor.class.getName();
        when(applicationContext.getBean(any(Class.class)))
                .thenThrow(new RuntimeException("Bean not found"));

        // When: Multiple threads call getClassInstance
        Thread thread1 = new Thread(() -> {
            Object result = sqlDaoUtils.getClassInstance(className);
            assertNotNull(result);
        });

        Thread thread2 = new Thread(() -> {
            Object result = sqlDaoUtils.getClassInstance(className);
            assertNotNull(result);
        });

        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();

        // Then: Both threads successfully create instances
        verify(applicationContext, atLeast(2)).getBean(any(Class.class));
    }
}
