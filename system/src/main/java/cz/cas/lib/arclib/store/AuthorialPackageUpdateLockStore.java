package cz.cas.lib.arclib.store;

import com.querydsl.jpa.impl.JPAQuery;
import cz.cas.lib.arclib.domain.packages.QAuthorialPackageUpdateLock;
import cz.cas.lib.arclib.domain.packages.AuthorialPackageUpdateLock;
import cz.cas.lib.arclib.domainbase.store.DomainStore;
import org.springframework.stereotype.Repository;

import jakarta.transaction.Transactional;

@Repository
public class AuthorialPackageUpdateLockStore
        extends DomainStore<AuthorialPackageUpdateLock, QAuthorialPackageUpdateLock> {
    public AuthorialPackageUpdateLockStore() {
        super(AuthorialPackageUpdateLock.class, QAuthorialPackageUpdateLock.class);
    }

    @Transactional
    public AuthorialPackageUpdateLock findByAuthorialPackageId(String authorialPackageId) {
        QAuthorialPackageUpdateLock authorialPackageUpdateLock = qObject();

        JPAQuery<AuthorialPackageUpdateLock> query = query()
                .select(authorialPackageUpdateLock)
                .where(authorialPackageUpdateLock.authorialPackage.id.eq(authorialPackageId));

        AuthorialPackageUpdateLock authorialPackageUpdateLockFound = query.fetchFirst();

        detachAll();
        return authorialPackageUpdateLockFound;
    }
}
