package cz.cas.lib.arclib.store;

import com.querydsl.jpa.impl.JPAQuery;
import cz.cas.lib.arclib.domain.Producer;
import cz.cas.lib.arclib.domain.QUser;
import cz.cas.lib.arclib.domain.User;
import cz.cas.lib.arclib.index.solr.entity.SolrUser;
import cz.cas.lib.core.index.solr.SolrDatedStore;
import org.springframework.stereotype.Repository;

@Repository
public class UserStore extends SolrDatedStore<User, QUser, SolrUser> {

    @Override
    public SolrUser toIndexObject(User obj) {
        SolrUser indexObject = super.toIndexObject(obj);
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

    public UserStore() {
        super(User.class, QUser.class, SolrUser.class);
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
