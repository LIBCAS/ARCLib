package cz.inqool.uas.security.ldap;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * Configuration POJO for LDAP subsystem
 */
@Getter
@Setter
public class LdapProperties {
    private Boolean enabled;

    private String server;

    private BindProperties bind;

    private UserProperties user;

    private GroupProperties group;

    @Getter
    @Setter
    public static class BindProperties {
        private String dn;
        private String pwd;
    }

    @Getter
    @Setter
    public static class UserProperties {
        private UserType type;
        private String searchBase;
        private String filter;
    }

    @Getter
    @Setter
    public static class GroupProperties {
        private GroupType type;
        private String searchBase;
        private String nameAttribute;
        private String memberAttribute;

        private Map<String, String> permissionMap;  //like, ldap group to app role
    }

    public static enum UserType {
        kerberos,
        dn,
        filter
    }

    public static enum GroupType {
        ad,
        other
    }
}
