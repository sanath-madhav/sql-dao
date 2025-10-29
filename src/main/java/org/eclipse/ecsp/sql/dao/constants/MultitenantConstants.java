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

    /** The Constant MULTI_TENANT_IDS. */
    public static final String MULTI_TENANT_IDS = "multi.tenant.ids";
}
