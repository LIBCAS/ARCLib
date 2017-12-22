package cz.inqool.uas.security.ldap.authorities;

import org.springframework.ldap.core.ContextSource;
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator;

import java.util.Set;

/**
 * Loads authorities directly from ldap user data using the parameter memberAttribute. The group name is specified
 * by nameAttribute.
 */
public class OtherAuthoritiesPopulator extends DefaultLdapAuthoritiesPopulator implements UasAuthoritiesPopulator {

    private String groupRoleFilter;

    private String groupMemberAttribute;

    public OtherAuthoritiesPopulator(ContextSource contextSource, String groupSearchBase) {
        super(contextSource, groupSearchBase);
    }

    public Set<String> getUserNamesWithAuthority(String authority) {
        return getLdapTemplate().searchForSingleAttributeValues(
                getGroupSearchBase(), getGroupRoleFilter(), new Object[]{constructGroupName(authority)}, getGroupMemberAttribute()
        );
    }

    private String constructGroupName(String role) {
        String prefix = this.getRolePrefix();

        if (prefix != null) {
            role = role.substring(prefix.length());
        }

        if (this.isConvertToUpperCase()) {
            role = role.toLowerCase();
        }

        return role;
    }

    public void setGroupRoleFilter(String groupRoleFilter) {
        this.groupRoleFilter = groupRoleFilter;
    }

    public String getGroupRoleFilter() {
        return groupRoleFilter;
    }

    public String getGroupMemberAttribute() {
        return groupMemberAttribute;
    }

    public void setGroupMemberAttribute(String groupMemberAttribute) {
        this.groupMemberAttribute = groupMemberAttribute;
    }
}
