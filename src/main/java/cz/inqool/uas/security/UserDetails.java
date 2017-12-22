package cz.inqool.uas.security;

/**
 * Extends base spring UserDetails and adds id
 */
public interface UserDetails extends org.springframework.security.core.userdetails.UserDetails {
    default String getId() {
        return getUsername();
    }

    default String getEmail() {
        return null;
    }

    default String getFullName() { return getUsername(); }

    /**
     * In multi-tenant application distinguishes the tenant
     *
     * @return Tenant identifier
     */
    default String getTenantId() {
        return null;
    }
}
