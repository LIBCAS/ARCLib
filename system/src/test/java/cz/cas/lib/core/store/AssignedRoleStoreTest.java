package cz.cas.lib.core.store;

import com.querydsl.jpa.impl.JPAQueryFactory;
import cz.cas.lib.arclib.security.authorization.assign.AssignedRole;
import cz.cas.lib.arclib.security.authorization.assign.AssignedRoleStore;
import cz.cas.lib.arclib.security.authorization.role.Role;
import cz.cas.lib.arclib.security.authorization.role.RoleStore;
import helper.DbTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Set;

import static cz.cas.lib.core.util.Utils.asSet;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class AssignedRoleStoreTest extends DbTest {
    protected AssignedRoleStore store;

    protected RoleStore roleStore;

    @Before
    public void setUp() throws SQLException {

        MockitoAnnotations.initMocks(this);

        store = new AssignedRoleStore();

        JPAQueryFactory jpaQueryFactory = new JPAQueryFactory(getEm());

        store.setQueryFactory(jpaQueryFactory);
        store.setEntityManager(getEm());

        roleStore = new RoleStore();
        roleStore.setEntityManager(getEm());
        roleStore.setQueryFactory(new JPAQueryFactory(getEm()));
    }

    @Test
    public void findAssignedRolesTest() {
        String userId = "123";

        Role role1 = new Role();
        role1 = roleStore.save(role1);

        Role role2 = new Role();
        role2 = roleStore.save(role2);

        Role role3 = new Role();
        role3 = roleStore.save(role3);
        flushCache();

        Set<Role> roles = asSet(role1, role2, role3);

        AssignedRole assignedRole1 = new AssignedRole();
        assignedRole1.setRole(role1);
        assignedRole1.setUserId(userId);
        store.save(assignedRole1);

        AssignedRole assignedRole2 = new AssignedRole();
        assignedRole2.setRole(role2);
        assignedRole2.setUserId(userId);
        store.save(assignedRole2);

        AssignedRole assignedRole3 = new AssignedRole();
        assignedRole3.setRole(role3);
        assignedRole3.setUserId(userId);
        store.save(assignedRole3);

        flushCache();

        Set<Role> assignedRoles = store.findAssignedRoles(userId);
        assertThat(assignedRoles, hasSize(roles.size()));
        assertThat(assignedRoles, containsInAnyOrder(role1, role2, role3));
    }

    @Test
    public void deleteRoleTest() {
        String userId = "123";

        Role role = new Role();
        role = roleStore.save(role);
        AssignedRole assignedRole = new AssignedRole();
        assignedRole.setUserId(userId);
        assignedRole.setRole(role);
        store.save(assignedRole);

        Role role1 = new Role();
        role1 = roleStore.save(role1);
        AssignedRole assignedRole1 = new AssignedRole();
        assignedRole1.setUserId(userId);
        assignedRole1.setRole(role1);
        store.save(assignedRole1);

        flushCache();

        Collection<AssignedRole> all = store.findAll();
        assertThat(all, containsInAnyOrder(assignedRole, assignedRole1));

        store.deleteRole(userId, role1);
        flushCache();

        all = store.findAll();

        assertThat(all, containsInAnyOrder(assignedRole));
    }


    @Test
    public void addRoleTest() {
        Collection<Role> all = roleStore.findAll();
        assertThat(all, is(empty()));

        String userId = "123";

        Role role = new Role();
        role = roleStore.save(role);
        store.addRole(userId, role);

        Role role1 = new Role();
        role1 = roleStore.save(role1);
        store.addRole(userId, role1);

        flushCache();

        all = roleStore.findAll();
        assertThat(all, containsInAnyOrder(role, role1));
    }
}
