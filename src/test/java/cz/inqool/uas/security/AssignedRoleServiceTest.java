package cz.inqool.uas.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.jpa.impl.JPAQueryFactory;
import cz.inqool.uas.audit.AuditLogger;
import cz.inqool.uas.security.authorization.assign.AssignedRole;
import cz.inqool.uas.security.authorization.assign.AssignedRoleService;
import cz.inqool.uas.security.authorization.assign.AssignedRoleStore;
import cz.inqool.uas.security.authorization.role.Role;
import cz.inqool.uas.security.authorization.role.RoleStore;
import helper.DbTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Set;

import static cz.inqool.uas.util.Utils.asSet;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class AssignedRoleServiceTest extends DbTest {
    protected AssignedRoleService service;

    private AssignedRoleStore store;

    private AuditLogger logger;

    private ObjectMapper objectMapper;

    private RoleStore roleStore;

    @Mock
    private ElasticsearchTemplate template;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        store = new AssignedRoleStore();
        store.setEntityManager(getEm());
        store.setQueryFactory(new JPAQueryFactory(getEm()));

        roleStore = new RoleStore();
        roleStore.setEntityManager(getEm());
        roleStore.setQueryFactory(new JPAQueryFactory(getEm()));
        roleStore.setTemplate(template);

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
        Set<String> permissions = asSet("permission1", "permission2");
        role1.setPermissions(permissions);
        role1 = roleStore.save(role1);

        Role role2 = new Role();
        Set<String> permissions2 = asSet("permission3", "permission4");
        role2.setPermissions(permissions2);
        role2 = roleStore.save(role2);

        Role role2parent = new Role();
        Set<String> permissions3 = asSet("permission5");
        role2parent.setPermissions(permissions3);
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
                new SimpleGrantedAuthority("permission2"), new SimpleGrantedAuthority("permission3"),
                new SimpleGrantedAuthority("permission4"), new SimpleGrantedAuthority("permission5")));
    }
}
