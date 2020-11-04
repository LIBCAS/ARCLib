package cz.cas.lib.arclib.security.authorization.logic;

import cz.cas.lib.arclib.domainbase.store.DomainStore;
import cz.cas.lib.arclib.security.authorization.data.QUserRole;
import cz.cas.lib.arclib.security.authorization.data.UserRole;
import org.springframework.stereotype.Repository;

@Repository
public class UserRoleStore extends DomainStore<UserRole, QUserRole> {
    public UserRoleStore() {
        super(UserRole.class, QUserRole.class);
    }
}
