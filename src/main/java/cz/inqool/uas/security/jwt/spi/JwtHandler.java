package cz.inqool.uas.security.jwt.spi;

import cz.inqool.uas.security.UserDetails;

import java.util.Map;

/**
 * Extension point for developer to store and retrieve necessary attributes of user to/from
 * the {@link cz.inqool.uas.security.jwt.JwtToken}.
 *
 * <p>
 *     It is developer responsibility to provide Spring bean implementing this interface.
 * </p>
 * <p>
 *     For further information about JWT tokens see {@link cz.inqool.uas.security.jwt.JwtToken}.
 * </p>
 */
public interface JwtHandler {
    /**
     * Rehydrates instance of concrete {@link UserDetails} from {@link Map} of attributes.
     *
     * @param claims A {@link Map} of attributes
     * @return instance of concrete implementation of user
     */
    UserDetails parseClaims(Map<String, Object> claims);

    Map<String, Object> createClaims(UserDetails user);
}
