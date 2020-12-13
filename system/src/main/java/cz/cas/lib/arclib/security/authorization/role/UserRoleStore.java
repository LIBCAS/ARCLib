package cz.cas.lib.arclib.security.authorization.role;

import cz.cas.lib.arclib.domainbase.store.DomainStore;
import org.springframework.stereotype.Repository;

@Repository
public class UserRoleStore extends DomainStore<UserRole, QUserRole> {
    public UserRoleStore() {
        super(UserRole.class, QUserRole.class);
    }
}
