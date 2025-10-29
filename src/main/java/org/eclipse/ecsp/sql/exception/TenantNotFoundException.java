package org.eclipse.ecsp.sql.exception;

/**
 * Exception thrown when a tenant is not found in the system.
 */
public class TenantNotFoundException extends RuntimeException {

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 1L;

    /**
     * Instantiates a new tenant not found exception.
     *
     * @param message the message
     */
    public TenantNotFoundException(String message) {
        super(message);
    }

    /**
     * Instantiates a new tenant not found exception.
     *
     * @param message   the message
     * @param throwable the throwable
     */
    public TenantNotFoundException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
