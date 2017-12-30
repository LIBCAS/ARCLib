package cz.inqool.uas.security.ldap.authorities;

import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.support.LdapUtils;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Loads authorities directly from ldap user data using the parameter memberAttribute. The group name is specified
 * by nameAttribute.
 *
 * fixme: implement getUserNamesWithAuthority
 */
public class ADAuthoritiesPopulator implements UasAuthoritiesPopulator {
    private String memberAttribute;
    private String nameAttribute;

    public ADAuthoritiesPopulator(String memberAttribute, String nameAttribute) {
        this.memberAttribute = memberAttribute;
        this.nameAttribute = nameAttribute;
    }

    @Override
    public Collection<? extends GrantedAuthority> getGrantedAuthorities(DirContextOperations userData, String username) {
        String[] groups = userData.getStringAttributes(memberAttribute);

        if (groups == null) {
            return Collections.emptySet();
        }

        return Stream.of(groups)
                     .map(LdapUtils::newLdapName)
                     .map(name -> (String)LdapUtils.getValue(name, nameAttribute))
                     .map(SimpleGrantedAuthority::new)
                     .collect(Collectors.toSet());
    }

    @Override
    public Set<String> getUserNamesWithAuthority(String authority) {
        throw new UnsupportedOperationException();
    }
}