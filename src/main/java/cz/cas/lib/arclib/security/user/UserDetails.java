package cz.cas.lib.arclib.security.user;

/**
 * Extends base spring UserDetails and adds id
 */
public interface UserDetails extends org.springframework.security.core.userdetails.UserDetails {
    default String getId() {
        return null;
    }

    default String getProducerId() {
        return null;
    }

    default String getEmail() {
        return null;
    }

    default String getFullName() {
        return getUsername();
    }
}
