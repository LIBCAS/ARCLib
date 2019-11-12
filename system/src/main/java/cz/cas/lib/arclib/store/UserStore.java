package cz.cas.lib.arclib.store;

import com.querydsl.jpa.impl.JPAQuery;
import cz.cas.lib.arclib.domain.Producer;
import cz.cas.lib.arclib.domain.QUser;
import cz.cas.lib.arclib.domain.User;
import cz.cas.lib.arclib.index.solr.entity.IndexedUser;
import cz.cas.lib.core.index.solr.IndexedDatedStore;
import lombok.Getter;
import org.springframework.stereotype.Repository;

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

}
