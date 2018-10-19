package cz.cas.lib.core.security.ldap.authorities;

import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator;

import java.util.Set;

public interface UasAuthoritiesPopulator extends LdapAuthoritiesPopulator {
    /**
     * Gets User names of users with role.
     * <p>
     * Role has removed specified prefix and converted to lowercase. Therefore ldap roles needs to be lower-cased.
     *
     * @param authority Group/Role Ldap name
     * @return Set of User names
     */
    Set<String> getUserNamesWithAuthority(String authority);
}
