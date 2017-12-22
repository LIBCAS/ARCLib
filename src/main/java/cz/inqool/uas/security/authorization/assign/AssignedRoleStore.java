package cz.inqool.uas.security.authorization.assign;

import cz.inqool.uas.security.authorization.role.Role;
import cz.inqool.uas.store.DomainStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static cz.inqool.uas.util.Utils.asSet;

@ConditionalOnProperty(prefix = "security.roles.internal", name = "enabled", havingValue = "true")
@Repository
public class AssignedRoleStore extends DomainStore<AssignedRole, QAssignedRole> {

    public AssignedRoleStore() {
        super(AssignedRole.class, QAssignedRole.class);
    }

    public Set<Role> findAssignedRoles(String userId) {
        QAssignedRole qAssignedRole = qObject();

        List<Role> roles = query().select(qAssignedRole.role)
                                  .where(qAssignedRole.userId.eq(userId))
                                  .fetch();

        detachAll();

        return asSet(roles);
    }

    public void deleteRole(String userId, Role role) {
        QAssignedRole qAssignedRole = qObject();

        queryFactory.delete(qAssignedRole)
                    .where(qAssignedRole.userId.eq(userId))
                    .where(qAssignedRole.role.eq(role))
                    .execute();
    }

    public void addRole(String userId, Role role) {
        AssignedRole assignedRole = new AssignedRole();
        assignedRole.setUserId(userId);
        assignedRole.setRole(role);

        save(assignedRole);
    }

    public Collection<String> getUsersWithRole(Role role) {
        QAssignedRole qObject = qObject();

        List<String> userIds = query().select(qObject.userId)
                                      .where(qObject.role.eq(role))
                                      .fetch();

        detachAll();

        return userIds;
    }
}
