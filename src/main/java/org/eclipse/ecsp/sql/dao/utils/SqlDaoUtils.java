package org.eclipse.ecsp.sql.dao.utils;

import java.util.HashMap;
import org.eclipse.ecsp.sql.exception.SqlDaoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Utility class.
 */
@Component
public class SqlDaoUtils {

    private static final Logger log = LoggerFactory.getLogger(SqlDaoUtils.class);
    /** The application context. */
    @Autowired
    private ApplicationContext ctx;

    /**
     * Instantiates a class by class name.
     * @param className The name of the class to instantiate.
     * @return The instance of the class.
     */
    public Object getClassInstance(String className) {
        Object instance = null;
        Class<?> classObject = null;
        try {
            classObject = getClass().getClassLoader().loadClass(className);
            instance = ctx.getBean(classObject);
            log.info("Class {} loaded from spring application context", classObject.getName());
        } catch (Exception ex) {
            try {
                if (classObject == null) throw new IllegalArgumentException("Could not load the class " + className);
                log.info("Class {} could not be loaded as spring bean. Attempting to create new instance.", className);
                instance = classObject.getDeclaredConstructor().newInstance();
            } catch (Exception exception) {
                log.error("Class {} could not be loaded. Not found on classpath. Exception is {}", className, exception);
                throw new SqlDaoException("Class " + className + " could not be loaded. "
                        + "Exception is: " + exception);
            }
        }
        return instance;
    }
}
