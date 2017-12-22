package cz.inqool.uas.security.authorization.role;

import cz.inqool.uas.index.IndexedDatedStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.List;

@ConditionalOnProperty(prefix = "security.roles.internal", name = "enabled", havingValue = "true")
@Repository
public class RoleStore extends IndexedDatedStore<Role, QRole, IndexedRole> {

    public RoleStore() {
        super(Role.class, QRole.class, IndexedRole.class);
    }

    @Override
    public IndexedRole toIndexObject(Role o) {
        IndexedRole indexedRole = super.toIndexObject(o);

        indexedRole.setName(o.getName());

        return indexedRole;
    }

    /**
     * Finds all roles which contains the specified permission
     * @param permission Permission
     * @return List of roles
     */
    public List<Role> findAllWithPermission(String permission) {
        QRole qRole = qObject();

        List<Role> roles = query().select(qRole)
                                  .from(qRole)
                                  .where(qRole.permissions.contains(permission))
                                  .fetch();

        detachAll();

        return roles;
    }
}
