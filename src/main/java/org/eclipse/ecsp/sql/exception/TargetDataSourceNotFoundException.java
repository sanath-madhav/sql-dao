package org.eclipse.ecsp.sql.exception;

/**
 * Exception thrown when a target DataSource for the current tenant is not
 * found.
 */
public class TargetDataSourceNotFoundException extends RuntimeException {

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 1L;

    /**
     * Instantiates a new target data source not found exception.
     *
     * @param message the message
     */
    public TargetDataSourceNotFoundException(String message) {
        super(message);
    }

    /**
     * Instantiates a new target data source not found exception.
     *
     * @param message   the message
     * @param throwable the throwable
     */
    public TargetDataSourceNotFoundException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
