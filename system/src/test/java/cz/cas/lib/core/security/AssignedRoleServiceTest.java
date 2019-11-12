package cz.cas.lib.core.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.jpa.impl.JPAQueryFactory;
import cz.cas.lib.arclib.domainbase.audit.AuditLogger;
import cz.cas.lib.arclib.security.authorization.assign.AssignedRole;
import cz.cas.lib.arclib.security.authorization.assign.AssignedRoleService;
import cz.cas.lib.arclib.security.authorization.assign.AssignedRoleStore;
import cz.cas.lib.arclib.security.authorization.role.Role;
import cz.cas.lib.arclib.security.authorization.role.RoleStore;
import helper.DbTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Set;

import static cz.cas.lib.core.util.Utils.asSet;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

public class AssignedRoleServiceTest extends DbTest {
    protected AssignedRoleService service;

    private AssignedRoleStore store;

    private AuditLogger logger;

    private ObjectMapper objectMapper;

    private RoleStore roleStore;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        store = new AssignedRoleStore();
        store.setEntityManager(getEm());
        store.setQueryFactory(new JPAQueryFactory(getEm()));

        roleStore = new RoleStore();
        roleStore.setEntityManager(getEm());
        roleStore.setQueryFactory(new JPAQueryFactory(getEm()));

        objectMapper = new ObjectMapper();

        logger = new AuditLogger();
        logger.setMapper(objectMapper);

        service = new AssignedRoleService();
        service.setLogger(logger);
        service.setStore(store);
    }

    @Test
    public void getAssignedRolesTest() {
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

        Set<Role> assignedRoles = service.getAssignedRoles(userId);
        assertThat(assignedRoles, hasSize(roles.size()));
        assertThat(assignedRoles, containsInAnyOrder(role1, role2, role3));
    }

    @Test
    public void saveAssignedRolesTest() {
        String userId = "123";

        /*
        Initial roles
         */
        Role role1 = new Role();
        role1 = roleStore.save(role1);

        Role role2 = new Role();
        role2 = roleStore.save(role2);

        Set<Role> roles = asSet(role1, role2);

        service.saveAssignedRoles(userId, roles);
        Set<Role> assignedRoles = store.findAssignedRoles(userId);
        assertThat(assignedRoles, containsInAnyOrder(role1, role2));

        /*
        New roles
         */
        Role role4 = new Role();
        role4 = roleStore.save(role4);

        Role role5 = new Role();
        role5 = roleStore.save(role5);

        Set<Role> roles2 = asSet(role4, role5);

        service.saveAssignedRoles(userId, roles2);
        assignedRoles = store.findAssignedRoles(userId);
        assertThat(assignedRoles, containsInAnyOrder(role4, role5));
    }

    @Test
    public void getAuthoritiesTest() {
        String userId = "123";

        Role role1 = new Role();
        role1.setName("permission1");
        role1 = roleStore.save(role1);

        Role role2 = new Role();
        role2.setName("permission2");
        role2 = roleStore.save(role2);

        Role role2parent = new Role();
        role2parent.setName("permission3");
        role2parent = roleStore.save(role2parent);

        AssignedRole assignedRole1 = new AssignedRole();
        assignedRole1.setRole(role1);
        assignedRole1.setUserId(userId);
        store.save(assignedRole1);

        AssignedRole assignedRole2 = new AssignedRole();
        assignedRole2.setRole(role2);
        assignedRole2.setUserId(userId);
        store.save(assignedRole2);

        AssignedRole assignedRole3 = new AssignedRole();
        assignedRole3.setRole(role2parent);
        assignedRole3.setUserId(userId);
        store.save(assignedRole3);
        flushCache();

        Set<GrantedAuthority> authorities = service.getAuthorities(userId);

        assertThat(authorities, containsInAnyOrder(new SimpleGrantedAuthority("permission1"),
                new SimpleGrantedAuthority("permission2"), new SimpleGrantedAuthority("permission3")));
    }
}
