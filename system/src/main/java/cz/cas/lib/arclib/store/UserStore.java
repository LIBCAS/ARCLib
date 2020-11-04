package cz.cas.lib.arclib.store;

import com.querydsl.jpa.impl.JPAQuery;
import cz.cas.lib.arclib.domain.Producer;
import cz.cas.lib.arclib.domain.QUser;
import cz.cas.lib.arclib.domain.User;
import cz.cas.lib.arclib.exception.BadRequestException;
import cz.cas.lib.arclib.index.solr.entity.IndexedUser;
import cz.cas.lib.arclib.security.authorization.data.Permissions;
import cz.cas.lib.arclib.security.authorization.data.QUserRole;
import cz.cas.lib.core.index.solr.IndexedDatedStore;
import lombok.Getter;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class UserStore extends IndexedDatedStore<User, QUser, IndexedUser> {

    @Override
    public IndexedUser toIndexObject(User obj) {
        IndexedUser indexObject = super.toIndexObject(obj);
        indexObject.setEmail(obj.getEmail());
        indexObject.setFirstName(obj.getFirstName());
        indexObject.setLastName(obj.getLastName());
        indexObject.setLdapDn(obj.getLdapDn());
        indexObject.setUsername(obj.getUsername());
        Producer producer = obj.getProducer();
        if (producer != null) {
            indexObject.setProducerId(producer.getId());
            indexObject.setProducerName(producer.getName());
        }
        return indexObject;
    }

    @Getter
    private final String indexType = "user";

    public UserStore() {
        super(User.class, QUser.class, IndexedUser.class);
    }

    public User findUserByUsername(String username) {
        QUser qUser = qObject();
        User user = query()
                .select(qUser)
                .where(qUser.username.eq(username))
                .fetchFirst();
        detachAll();
        return user;
    }

    public User findEvenDeleted(String id) {
        QUser qUser = qObject();
        User user = query()
                .select(qUser)
                .where(qUser.id.eq(id))
                .fetchFirst();
        detachAll();
        return user;
    }

    /**
     * Finds user by ldap credentials (fullDn).
     *
     * @param fullDn LDAP DN of user
     * @return user
     */
    public User findByLdapCredentials(String fullDn) {
        QUser qUser = QUser.user;
        JPAQuery<User> query = query(qUser)
                .select(qUser)
                .where(qUser.deleted.isNull())
                .where(qUser.ldapDn.eq(fullDn));
        User user = query.fetchFirst();
        detachAll();
        return user;
    }

    public List<User> findByRole(String roleId) {
        QUser qUser = qObject();
        List<User> user = query()
                .select(qUser)
                .innerJoin(qUser.roles)
                .fetchJoin()
                .where(qUser.roles.any().id.eq(roleId))
                .fetch();
        detachAll();
        return user;
    }

    public List<User> findByPermission(String permission) {
        if (!Permissions.ALL_PERMISSIONS.contains(permission))
            throw new BadRequestException("Provided string:" + permission + " is not a permissions for ALL_PERMISSIONS set.");

        QUser qUser = qObject();
        List<User> users = query()
                .select(qUser)
                .innerJoin(qUser.roles, QUserRole.userRole)
                .fetchJoin()
                .where(QUserRole.userRole.permissions.any().contains(permission))
                .distinct()
                .fetch();
        detachAll();
        return users;
    }

}
