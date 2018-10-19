package cz.cas.lib.arclib.security.authorization.role;

import cz.cas.lib.core.store.DatedStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@ConditionalOnProperty(prefix = "security.roles.internal", name = "enabled", havingValue = "true")
@Repository
public class RoleStore extends DatedStore<Role, QRole> {

    public RoleStore() {
        super(Role.class, QRole.class);
    }
}