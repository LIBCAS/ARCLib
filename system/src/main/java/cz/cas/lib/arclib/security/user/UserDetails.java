package cz.cas.lib.arclib.security.user;

import cz.cas.lib.arclib.domain.User;

/**
 * Extends base Spring UserDetails and adds specific access methods for ARClib
 */
public interface UserDetails extends org.springframework.security.core.userdetails.UserDetails {

    String getId();

    String getProducerId();

    User getUser();


    @Override
    default String getPassword() {
        return null;
    }

    @Override
    default boolean isAccountNonExpired() {
        return true;
    }

    @Override
    default boolean isAccountNonLocked() {
        return true;
    }

    @Override
    default boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    default boolean isEnabled() {
        return true;
    }

}
