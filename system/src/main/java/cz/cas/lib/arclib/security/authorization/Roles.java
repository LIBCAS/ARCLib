package cz.cas.lib.arclib.security.authorization;

/**
 * List of all Roles
 * <p>
 * Important! Every permission needs to have ROLE_ prefix for spring security to work.
 */
public class Roles {
    public static final String ARCHIVIST = "ROLE_ARCHIVIST";
    public static final String ADMIN = "ROLE_ADMIN";
    public static final String SUPER_ADMIN = "ROLE_SUPER_ADMIN";
    public static final String ANALYST = "ROLE_ANALYST";
    public static final String EDITOR = "ROLE_EDITOR";
    public static final String DELETION_ACKNOWLEDGE = "ROLE_DELETION_ACKNOWLEDGE";
}
