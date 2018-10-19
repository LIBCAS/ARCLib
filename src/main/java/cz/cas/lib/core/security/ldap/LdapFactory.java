package cz.cas.lib.core.security.ldap;

import cz.cas.lib.core.security.ldap.LdapProperties.GroupProperties;
import cz.cas.lib.core.security.ldap.LdapProperties.UserProperties;
import cz.cas.lib.core.security.ldap.authorities.ADAuthoritiesPopulator;
import cz.cas.lib.core.security.ldap.authorities.OtherAuthoritiesPopulator;
import cz.cas.lib.core.security.ldap.authorities.RewriteAuthoritiesPopulator;
import cz.cas.lib.core.security.ldap.authorities.UasAuthoritiesPopulator;
import cz.cas.lib.core.security.ldap.ssl.DummySSLLdapContextSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.security.ldap.search.LdapUserSearch;

import javax.naming.directory.SearchControls;
import java.util.Map;

/**
 * Factory methods for creating LDAP specific components based on common properties
 */
@Slf4j
public class LdapFactory {
    /**
     * Creates LDAP connection
     *
     * @param properties Properties POJO
     * @return LDAP connection
     */
    public static LdapContextSource createContext(LdapProperties properties) {
        DummySSLLdapContextSource source = new DummySSLLdapContextSource();
        source.setUrl(properties.getServer());
        source.setUserDn(properties.getBind().getDn());
        source.setPassword(properties.getBind().getPwd());
        return source;
    }

    /**
     * Creates LDAP User search
     *
     * @param properties Properties POJO
     * @return LDAP User search
     */
    public static LdapUserSearch createUserSearch(LdapProperties properties, LdapContextSource source) {
        UserProperties userProperties = properties.getUser();

        switch(userProperties.getType()) {
            case kerberos:
                return ldapKerberosUserSearch(userProperties, source);
            case filter:
                return ldapFilteredUserSearch(userProperties, source);
            case dn:
                return ldapFullDnUserSearch(source);
            default:
                log.warn("Failed to create LDAP User search with type {}.", userProperties.getType());
                return null;
        }
    }

    /**
     * Creates LDAP authorities populator
     *
     * @param properties Properties POJO
     * @return LDAP authorities populator
     */
    public static UasAuthoritiesPopulator createAuthoritiesPopulator(LdapProperties properties, LdapContextSource source) {
        GroupProperties groupProperties = properties.getGroup();

        UasAuthoritiesPopulator populator;
        switch(groupProperties.getType()) {
            case ad:
                populator = ldapADAuthoritiesPopulator(groupProperties);
                break;
            case other:
                populator = ldapOtherAuthoritiesPopulator(groupProperties, source);
                break;
            default:
                log.warn("Failed to create LDAP Authorities populator with type {}.", groupProperties.getType());
                return null;
        }

        Map<String, String> permissionMap = groupProperties.getPermissionMap();
        if(permissionMap != null && !permissionMap.isEmpty()) {
            return new RewriteAuthoritiesPopulator(populator, permissionMap);
        } else {
            return populator;
        }
    }


    private static LdapUserSearch ldapKerberosUserSearch(UserProperties properties, LdapContextSource source) {
        KerberosFilterBasedLdapUserSearch search = new KerberosFilterBasedLdapUserSearch(
                properties.getFilter(), source
        );

        search.setSearchScope(SearchControls.SUBTREE_SCOPE);
        return search;
    }

    private static LdapUserSearch ldapFilteredUserSearch(UserProperties properties, LdapContextSource source) {
        return new FilterBasedLdapUserSearch(
                properties.getSearchBase(), properties.getFilter(), source
        );
    }

    private static LdapUserSearch ldapFullDnUserSearch(LdapContextSource source) {
        LdapFullDnSearch search = new LdapFullDnSearch(source);
        search.setSearchScope(SearchControls.OBJECT_SCOPE);
        return search;
    }

    private static UasAuthoritiesPopulator ldapADAuthoritiesPopulator(GroupProperties properties) {
        return new ADAuthoritiesPopulator(
                properties.getMemberAttribute(), properties.getNameAttribute()
        );
    }

    private static UasAuthoritiesPopulator ldapOtherAuthoritiesPopulator(GroupProperties properties, LdapContextSource source) {
        String groupMemberFilter = "(" + properties.getMemberAttribute() + "={1})"; // first param is userDn, second is provided username
        String groupRoleFilter = "(" + properties.getNameAttribute() + "={0})";

        OtherAuthoritiesPopulator populator = new OtherAuthoritiesPopulator(source, properties.getSearchBase());
        populator.setGroupRoleAttribute(properties.getNameAttribute());
        populator.setGroupMemberAttribute(properties.getMemberAttribute());
        populator.setGroupSearchFilter(groupMemberFilter);
        populator.setGroupRoleFilter(groupRoleFilter);

        return populator;
    }
}
