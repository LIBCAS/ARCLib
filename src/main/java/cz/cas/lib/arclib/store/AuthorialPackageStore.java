package cz.cas.lib.arclib.store;

import com.querydsl.jpa.impl.JPAQuery;
import cz.cas.lib.arclib.domain.packages.QAuthorialPackage;
import cz.cas.lib.arclib.domain.packages.AuthorialPackage;
import cz.cas.lib.core.store.DatedStore;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;

@Repository
public class AuthorialPackageStore
        extends DatedStore<AuthorialPackage, QAuthorialPackage> {
    public AuthorialPackageStore() {
        super(AuthorialPackage.class, QAuthorialPackage.class);
    }


    @Transactional
    public AuthorialPackage findByAuthorialIdAndProducerProfileId(String authorialId, String producerProfileId) {
        QAuthorialPackage authorialPackage = qObject();

        JPAQuery<AuthorialPackage> query = query()
                .select(authorialPackage)
                .where(authorialPackage.authorialId.eq(authorialId))
                .where(authorialPackage.producerProfile.id.eq(producerProfileId))
                .where(authorialPackage.deleted.isNull());

        AuthorialPackage authorialPackageFound = query.fetchFirst();

        detachAll();
        return authorialPackageFound;
    }
}
